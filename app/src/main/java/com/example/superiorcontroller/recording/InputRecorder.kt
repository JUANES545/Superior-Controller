package com.example.superiorcontroller.recording

import android.os.SystemClock
import kotlin.math.abs

class InputRecorder {

    private val events = mutableListOf<RecordedEvent>()
    private var startNs: Long = 0
    private var hwBaseMs: Long = 0

    private var lastLeftAxisMs: Long = 0
    private var lastRightAxisMs: Long = 0
    private var lastLeftTriggerMs: Long = 0
    private var lastRightTriggerMs: Long = 0

    private var lastLeftX = 0f
    private var lastLeftY = 0f
    private var lastRightX = 0f
    private var lastRightY = 0f
    private var lastLT = 0f
    private var lastRT = 0f

    var initialSnapshot: GamepadSnapshot? = null; private set
    var eventsRecorded: Int = 0; private set
    var eventsThrottled: Int = 0; private set
    var eventsDeltaSkipped: Int = 0; private set
    var buttonEventsRecorded: Int = 0; private set
    var axisEventsRecorded: Int = 0; private set
    var isRecording: Boolean = false; private set

    fun start(snapshot: GamepadSnapshot? = null, hwBaseMs: Long = 0L) {
        events.clear()
        startNs = System.nanoTime()
        this.hwBaseMs = if (hwBaseMs > 0) hwBaseMs else SystemClock.uptimeMillis()
        initialSnapshot = snapshot
        eventsRecorded = 0; eventsThrottled = 0; eventsDeltaSkipped = 0
        buttonEventsRecorded = 0; axisEventsRecorded = 0
        lastLeftX = 0f; lastLeftY = 0f; lastRightX = 0f; lastRightY = 0f
        lastLT = 0f; lastRT = 0f
        lastLeftAxisMs = 0; lastRightAxisMs = 0
        lastLeftTriggerMs = 0; lastRightTriggerMs = 0
        isRecording = true
    }

    fun stop(): List<RecordedEvent> {
        isRecording = false
        return events.toList()
    }

    fun elapsedMs(): Long = (System.nanoTime() - startNs) / NS_PER_MS

    private fun timestampMs(hwEventTimeMs: Long): Long {
        return if (hwEventTimeMs > 0) {
            hwEventTimeMs - hwBaseMs
        } else {
            elapsedMs()
        }.coerceAtLeast(0)
    }

    fun recordButtonPress(button: Int, hwEventTimeMs: Long = 0L) {
        if (!isRecording) return
        events.add(RecordedEvent(t = timestampMs(hwEventTimeMs), type = EventType.BUTTON_PRESS, btn = button))
        eventsRecorded++; buttonEventsRecorded++
    }

    fun recordButtonRelease(button: Int, hwEventTimeMs: Long = 0L) {
        if (!isRecording) return
        events.add(RecordedEvent(t = timestampMs(hwEventTimeMs), type = EventType.BUTTON_RELEASE, btn = button))
        eventsRecorded++; buttonEventsRecorded++
    }

    fun recordLeftAxis(x: Float, y: Float, hwEventTimeMs: Long = 0L) {
        if (!isRecording) return
        val now = timestampMs(hwEventTimeMs)
        if (now - lastLeftAxisMs < AXIS_THROTTLE_MS) { eventsThrottled++; return }
        if (abs(x - lastLeftX) < AXIS_DELTA && abs(y - lastLeftY) < AXIS_DELTA) { eventsDeltaSkipped++; return }
        lastLeftAxisMs = now; lastLeftX = x; lastLeftY = y
        events.add(RecordedEvent(t = now, type = EventType.LEFT_AXIS, x = x, y = y))
        eventsRecorded++; axisEventsRecorded++
    }

    fun recordRightAxis(x: Float, y: Float, hwEventTimeMs: Long = 0L) {
        if (!isRecording) return
        val now = timestampMs(hwEventTimeMs)
        if (now - lastRightAxisMs < AXIS_THROTTLE_MS) { eventsThrottled++; return }
        if (abs(x - lastRightX) < AXIS_DELTA && abs(y - lastRightY) < AXIS_DELTA) { eventsDeltaSkipped++; return }
        lastRightAxisMs = now; lastRightX = x; lastRightY = y
        events.add(RecordedEvent(t = now, type = EventType.RIGHT_AXIS, x = x, y = y))
        eventsRecorded++; axisEventsRecorded++
    }

    fun recordLeftTrigger(value: Float, hwEventTimeMs: Long = 0L) {
        if (!isRecording) return
        val now = timestampMs(hwEventTimeMs)
        if (now - lastLeftTriggerMs < AXIS_THROTTLE_MS) { eventsThrottled++; return }
        if (abs(value - lastLT) < TRIGGER_DELTA) { eventsDeltaSkipped++; return }
        lastLeftTriggerMs = now; lastLT = value
        events.add(RecordedEvent(t = now, type = EventType.LEFT_TRIGGER, x = value))
        eventsRecorded++; axisEventsRecorded++
    }

    fun recordRightTrigger(value: Float, hwEventTimeMs: Long = 0L) {
        if (!isRecording) return
        val now = timestampMs(hwEventTimeMs)
        if (now - lastRightTriggerMs < AXIS_THROTTLE_MS) { eventsThrottled++; return }
        if (abs(value - lastRT) < TRIGGER_DELTA) { eventsDeltaSkipped++; return }
        lastRightTriggerMs = now; lastRT = value
        events.add(RecordedEvent(t = now, type = EventType.RIGHT_TRIGGER, x = value))
        eventsRecorded++; axisEventsRecorded++
    }

    fun statsString(): String =
        "rec=$eventsRecorded btn=$buttonEventsRecorded axis=$axisEventsRecorded " +
        "throttled=$eventsThrottled deltaSkip=$eventsDeltaSkipped"

    companion object {
        private const val NS_PER_MS = 1_000_000L
        private const val AXIS_THROTTLE_MS = 4L
        private const val AXIS_DELTA = 0.01f
        private const val TRIGGER_DELTA = 0.02f
    }
}
