package com.example.superiorcontroller.recording

/**
 * Captures raw HID report bytes as they are sent over Bluetooth,
 * with nanosecond-precision relative timestamps.
 */
class HidReportRecorder {

    private val frames = mutableListOf<HidReportFrame>()
    private var startNs: Long = 0

    var isRecording: Boolean = false; private set
    var framesCaptured: Int = 0; private set

    fun start() {
        frames.clear()
        framesCaptured = 0
        startNs = System.nanoTime()
        isRecording = true
    }

    fun captureFrame(report: ByteArray, sendTimeNs: Long) {
        if (!isRecording) return
        val relativeNs = sendTimeNs - startNs
        frames.add(HidReportFrame(relativeNs = relativeNs, report = report.copyOf()))
        framesCaptured++
    }

    fun stop(): List<HidReportFrame> {
        isRecording = false
        return frames.toList()
    }

    fun elapsedMs(): Long =
        if (startNs > 0) (System.nanoTime() - startNs) / 1_000_000L else 0L
}
