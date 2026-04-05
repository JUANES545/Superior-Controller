package com.example.superiorcontroller.recording

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

enum class PlaybackStatus { IDLE, PLAYING, PAUSED }

data class PlaybackProgress(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentMs: Long = 0,
    val totalMs: Long = 0,
    val recordingId: String = "",
    val recordingName: String = ""
)

class PlaybackEngine {

    interface EventSink {
        fun onPlaybackReset()
        fun onPlaybackApplySnapshot(snapshot: GamepadSnapshot)
        fun onPlaybackButtonPress(button: Int)
        fun onPlaybackButtonRelease(button: Int)
        fun onPlaybackLeftAxis(x: Float, y: Float)
        fun onPlaybackRightAxis(x: Float, y: Float)
        fun onPlaybackLeftTrigger(value: Float)
        fun onPlaybackRightTrigger(value: Float)
        fun onPlaybackComplete()
        fun onPlaybackLog(message: String)
    }

    var sink: EventSink? = null

    private val _progress = MutableStateFlow(PlaybackProgress())
    val progress: StateFlow<PlaybackProgress> = _progress.asStateFlow()

    private var playThread: Thread? = null
    private val stopped = AtomicBoolean(false)

    @Volatile
    private var paused: Boolean = false
    @Volatile
    private var pausedAtNs: Long = 0

    val isRunning: Boolean get() = playThread?.isAlive == true
    val isPlaying: Boolean get() = isRunning && !paused

    @Suppress("unused")
    fun play(data: RecordingData, scope: CoroutineScope) {
        stop()
        stopped.set(false)
        paused = false
        pausedAtNs = 0

        _progress.value = PlaybackProgress(
            status = PlaybackStatus.PLAYING,
            totalMs = data.durationMs,
            recordingId = data.id,
            recordingName = data.name
        )

        sink?.onPlaybackReset()
        data.initialSnapshot?.let { sink?.onPlaybackApplySnapshot(it) }

        val thread = Thread({
            try {
                runPlayback(data)
            } catch (_: InterruptedException) {
                // stopped externally
            } finally {
                sink?.onPlaybackReset()
                sink?.onPlaybackComplete()
                _progress.value = PlaybackProgress()
            }
        }, "PlaybackEngine")
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

    private fun runPlayback(data: RecordingData) {
        Thread.sleep(SNAPSHOT_SETTLE_MS)
        if (stopped.get()) return

        val events = data.events
        if (events.isEmpty()) return

        val startNs = System.nanoTime()
        var pauseAccumulatedNs = 0L
        var lastProgressNs = startNs

        for (event in events) {
            if (stopped.get()) return

            while (paused) {
                if (stopped.get()) return
                Thread.sleep(20)
            }
            if (pausedAtNs > 0) {
                pauseAccumulatedNs += System.nanoTime() - pausedAtNs
                pausedAtNs = 0
            }

            val targetNs = startNs + pauseAccumulatedNs + event.t * NS_PER_MS
            waitUntilNs(targetNs)
            if (stopped.get()) return

            dispatchEvent(event)

            val nowNs = System.nanoTime()
            if (nowNs - lastProgressNs >= PROGRESS_INTERVAL_NS) {
                val elapsedMs = (nowNs - startNs - pauseAccumulatedNs) / NS_PER_MS
                _progress.value = _progress.value.copy(
                    currentMs = elapsedMs,
                    status = PlaybackStatus.PLAYING
                )
                lastProgressNs = nowNs
            }
        }

        Thread.sleep(RESET_SETTLE_MS)
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
                // else: busy-wait (spin) for sub-millisecond precision
            }
        }
    }

    private fun dispatchEvent(event: RecordedEvent) {
        val s = sink ?: return
        when (event.type) {
            EventType.BUTTON_PRESS -> s.onPlaybackButtonPress(event.btn)
            EventType.BUTTON_RELEASE -> s.onPlaybackButtonRelease(event.btn)
            EventType.LEFT_AXIS -> s.onPlaybackLeftAxis(event.x, event.y)
            EventType.RIGHT_AXIS -> s.onPlaybackRightAxis(event.x, event.y)
            EventType.LEFT_TRIGGER -> s.onPlaybackLeftTrigger(event.x)
            EventType.RIGHT_TRIGGER -> s.onPlaybackRightTrigger(event.x)
        }
    }

    companion object {
        private const val NS_PER_MS = 1_000_000L
        private const val PROGRESS_INTERVAL_NS = 80_000_000L
        private const val RESET_SETTLE_MS = 30L
        private const val SNAPSHOT_SETTLE_MS = 20L
        private const val SLEEP_THRESHOLD_MS = 4L
        private const val SLEEP_MARGIN_MS = 2L
        private const val YIELD_THRESHOLD_MS = 1L
    }
}
