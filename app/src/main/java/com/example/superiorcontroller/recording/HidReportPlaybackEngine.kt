package com.example.superiorcontroller.recording

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * Replays raw HID report bytes directly to Bluetooth, bypassing
 * ViewModel/state for maximum timing fidelity.
 *
 * Frame-aligned mode (default): preprocesses the recording into
 * fixed-interval slots (125Hz / 8ms) for regular, predictable delivery.
 * This reduces sensitivity to BT jitter by ensuring the host receives
 * updates at a steady cadence.
 */
class HidReportPlaybackEngine {

    fun interface ReportSender {
        fun sendRaw(report: ByteArray): Boolean
    }

    fun interface CompletionListener {
        fun onPlaybackComplete()
    }

    fun interface UiUpdateListener {
        fun onFrameSent(report: ByteArray, elapsedMs: Long)
    }

    var sender: ReportSender? = null
    var completionListener: CompletionListener? = null
    var uiUpdateListener: UiUpdateListener? = null

    private val _progress = MutableStateFlow(PlaybackProgress())
    val progress: StateFlow<PlaybackProgress> = _progress.asStateFlow()

    private val _stats = MutableStateFlow(PlaybackStats())
    val stats: StateFlow<PlaybackStats> = _stats.asStateFlow()

    private var playThread: Thread? = null
    private val stopped = AtomicBoolean(false)

    @Volatile
    private var paused: Boolean = false
    @Volatile
    private var pausedAtNs: Long = 0

    val isRunning: Boolean get() = playThread?.isAlive == true
    val isPlaying: Boolean get() = isRunning && !paused

    fun play(data: HidRecordingData) {
        stop()
        stopped.set(false)
        paused = false
        pausedAtNs = 0
        _stats.value = PlaybackStats()

        val slots = preprocessToSlots(data.frames, data.durationMs)

        _progress.value = PlaybackProgress(
            status = PlaybackStatus.PLAYING,
            totalMs = data.durationMs,
            recordingId = data.id,
            recordingName = data.name
        )

        val thread = Thread({
            try {
                runFrameAligned(slots, data.durationMs)
            } catch (_: InterruptedException) {
            } finally {
                sendNeutral()
                completionListener?.onPlaybackComplete()
                _progress.value = PlaybackProgress()
            }
        }, "HidReportPlayback")
        thread.priority = Thread.MAX_PRIORITY
        thread.isDaemon = true
        playThread = thread
        thread.start()
    }

    fun pause() {
        pausedAtNs = System.nanoTime()
        paused = true
        _progress.value = _progress.value.copy(status = PlaybackStatus.PAUSED)
    }

    fun resume() {
        paused = false
        _progress.value = _progress.value.copy(status = PlaybackStatus.PLAYING)
    }

    fun stop() {
        stopped.set(true)
        paused = false
        playThread?.interrupt()
        playThread?.join(500)
        playThread = null
        _progress.value = PlaybackProgress()
    }

    // ── Preprocessing: irregular frames → fixed-rate slots ──────────────

    private fun preprocessToSlots(
        frames: List<HidReportFrame>,
        durationMs: Long
    ): List<ByteArray> {
        if (frames.isEmpty()) return emptyList()

        val slotCount = (durationMs / SLOT_INTERVAL_MS).toInt() + 1
        val slots = ArrayList<ByteArray>(slotCount)
        var frameIdx = 0

        val neutral = ByteArray(9).apply {
            this[3] = 128.toByte()
            this[4] = 128.toByte()
            this[5] = 128.toByte()
            this[6] = 128.toByte()
        }
        var currentReport: ByteArray = frames.firstOrNull()?.report ?: neutral

        for (slot in 0 until slotCount) {
            val slotEndNs = (slot + 1).toLong() * SLOT_INTERVAL_NS

            while (frameIdx < frames.size && frames[frameIdx].relativeNs <= slotEndNs) {
                currentReport = frames[frameIdx].report
                frameIdx++
            }
            slots.add(currentReport)
        }

        return slots
    }

    // ── Frame-aligned playback loop ─────────────────────────────────────

    private fun runFrameAligned(slots: List<ByteArray>, totalMs: Long) {
        if (slots.isEmpty()) return

        Thread.sleep(SETTLE_MS)
        if (stopped.get()) return

        val startNs = System.nanoTime()
        var pauseAccumulatedNs = 0L
        var lastUiUpdateNs = startNs
        var lastProgressNs = startNs

        var sendCount = 0L
        var failCount = 0L
        var lateCount = 0L
        val intervals = LongArray(slots.size.coerceAtMost(MAX_INTERVAL_SAMPLES))
        var prevSendNs = startNs

        for ((index, report) in slots.withIndex()) {
            if (stopped.get()) return

            while (paused) {
                if (stopped.get()) return
                Thread.sleep(20)
            }
            if (pausedAtNs > 0) {
                pauseAccumulatedNs += System.nanoTime() - pausedAtNs
                pausedAtNs = 0
                prevSendNs = System.nanoTime()
            }

            val targetNs = startNs + pauseAccumulatedNs + index.toLong() * SLOT_INTERVAL_NS
            val beforeWait = System.nanoTime()
            waitUntilNs(targetNs)
            if (stopped.get()) return

            val sendNs = System.nanoTime()
            val late = sendNs - targetNs
            if (late > LATE_THRESHOLD_NS) lateCount++

            val sent = sender?.sendRaw(report) ?: false
            if (sent) sendCount++ else failCount++

            if (index > 0 && index < intervals.size) {
                intervals[index] = sendNs - prevSendNs
            }
            prevSendNs = sendNs

            val nowNs = System.nanoTime()
            if (nowNs - lastUiUpdateNs >= UI_UPDATE_INTERVAL_NS) {
                val elapsedMs = (nowNs - startNs - pauseAccumulatedNs) / NS_PER_MS
                uiUpdateListener?.onFrameSent(report, elapsedMs)
                lastUiUpdateNs = nowNs
            }
            if (nowNs - lastProgressNs >= PROGRESS_INTERVAL_NS) {
                val elapsedMs = (nowNs - startNs - pauseAccumulatedNs) / NS_PER_MS
                _progress.value = _progress.value.copy(
                    currentMs = elapsedMs,
                    status = PlaybackStatus.PLAYING
                )
                lastProgressNs = nowNs
            }
        }

        val endNs = System.nanoTime()
        val actualDurationMs = (endNs - startNs - pauseAccumulatedNs) / NS_PER_MS
        val driftMs = actualDurationMs - totalMs

        val validIntervals = intervals.drop(1).filter { it > 0 }
        val avgIntervalUs = if (validIntervals.isNotEmpty())
            validIntervals.map { it / 1000.0 }.average() else 0.0
        val stdDevUs = if (validIntervals.size > 1) {
            val mean = avgIntervalUs
            sqrt(validIntervals.map { v -> val d = v / 1000.0 - mean; d * d }.average())
        } else 0.0

        _stats.value = PlaybackStats(
            totalSlots = slots.size,
            sentCount = sendCount,
            failCount = failCount,
            lateCount = lateCount,
            avgIntervalUs = avgIntervalUs,
            stdDevIntervalUs = stdDevUs,
            driftMs = driftMs,
            slotIntervalMs = SLOT_INTERVAL_MS
        )

        Thread.sleep(SETTLE_MS)
    }

    private fun waitUntilNs(targetNs: Long) {
        while (true) {
            val remainingNs = targetNs - System.nanoTime()
            if (remainingNs <= 0) return
            if (stopped.get()) return

            val remainingMs = remainingNs / NS_PER_MS
            when {
                remainingMs > SLEEP_THRESHOLD_MS -> Thread.sleep(remainingMs - SLEEP_MARGIN_MS)
                remainingMs > YIELD_THRESHOLD_MS -> Thread.yield()
            }
        }
    }

    private fun sendNeutral() {
        val neutral = ByteArray(9).apply {
            this[3] = 128.toByte()
            this[4] = 128.toByte()
            this[5] = 128.toByte()
            this[6] = 128.toByte()
        }
        sender?.sendRaw(neutral)
    }

    companion object {
        private const val NS_PER_MS = 1_000_000L

        const val SLOT_INTERVAL_MS = 8L
        private const val SLOT_INTERVAL_NS = SLOT_INTERVAL_MS * NS_PER_MS

        private const val PROGRESS_INTERVAL_NS = 80_000_000L
        private const val UI_UPDATE_INTERVAL_NS = 33_000_000L
        private const val SETTLE_MS = 20L
        private const val SLEEP_THRESHOLD_MS = 4L
        private const val SLEEP_MARGIN_MS = 2L
        private const val YIELD_THRESHOLD_MS = 1L
        private const val LATE_THRESHOLD_NS = 2_000_000L // 2ms late = "late"
        private const val MAX_INTERVAL_SAMPLES = 20_000
    }
}

data class PlaybackStats(
    val totalSlots: Int = 0,
    val sentCount: Long = 0,
    val failCount: Long = 0,
    val lateCount: Long = 0,
    val avgIntervalUs: Double = 0.0,
    val stdDevIntervalUs: Double = 0.0,
    val driftMs: Long = 0,
    val slotIntervalMs: Long = 0
) {
    fun summary(): String {
        if (totalSlots == 0) return "no data"
        return "slots=$totalSlots sent=$sentCount fail=$failCount late=$lateCount " +
            "avg=%.1fµs σ=%.1fµs drift=${driftMs}ms slot=${slotIntervalMs}ms".format(avgIntervalUs, stdDevIntervalUs)
    }
}
