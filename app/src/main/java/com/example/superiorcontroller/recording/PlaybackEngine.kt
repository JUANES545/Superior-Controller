package com.example.superiorcontroller.recording

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    private var playJob: Job? = null
    @Volatile
    private var paused: Boolean = false

    val isRunning: Boolean get() = playJob?.isActive == true
    val isPlaying: Boolean get() = isRunning && !paused

    fun play(data: RecordingData, scope: CoroutineScope) {
        stop()
        _progress.value = PlaybackProgress(
            status = PlaybackStatus.PLAYING,
            totalMs = data.durationMs,
            recordingId = data.id,
            recordingName = data.name
        )

        sink?.onPlaybackReset()
        val snapshot = data.initialSnapshot
        if (snapshot != null) {
            sink?.onPlaybackApplySnapshot(snapshot)
        }

        playJob = scope.launch {
            delay(SNAPSHOT_SETTLE_MS)
            val startNs = System.nanoTime()
            var lastProgressNs = startNs

            for (event in data.events) {
                if (!isActive) break

                while (paused && isActive) delay(50)

                val targetNs = startNs + event.t * NS_PER_MS
                while (System.nanoTime() < targetNs && isActive) {
                    val remaining = (targetNs - System.nanoTime()) / NS_PER_MS
                    if (remaining > COARSE_THRESHOLD_MS) {
                        delay(remaining - EARLY_WAKE_MS)
                    } else if (remaining > FINE_THRESHOLD_MS) {
                        delay(1)
                    }
                }

                if (!isActive) break
                dispatchEvent(event)

                val nowNs = System.nanoTime()
                if (nowNs - lastProgressNs >= PROGRESS_INTERVAL_NS) {
                    val currentMs = (nowNs - startNs) / NS_PER_MS
                    _progress.value = _progress.value.copy(
                        currentMs = currentMs,
                        status = PlaybackStatus.PLAYING
                    )
                    lastProgressNs = nowNs
                }
            }

            delay(RESET_SETTLE_MS)
            sink?.onPlaybackReset()
            sink?.onPlaybackComplete()
            _progress.value = PlaybackProgress()
        }
    }

    fun pause() {
        paused = true
        _progress.value = _progress.value.copy(status = PlaybackStatus.PAUSED)
    }

    fun resume() {
        paused = false
        _progress.value = _progress.value.copy(status = PlaybackStatus.PLAYING)
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        paused = false
        _progress.value = PlaybackProgress()
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
        private const val COARSE_THRESHOLD_MS = 10L
        private const val EARLY_WAKE_MS = 2L
        private const val FINE_THRESHOLD_MS = 1L
        private const val PROGRESS_INTERVAL_NS = 100_000_000L
        private const val RESET_SETTLE_MS = 50L
        private const val SNAPSHOT_SETTLE_MS = 30L
    }
}
