package com.example.superiorcontroller.recording

import android.util.Base64

/**
 * A single captured HID report with nanosecond-precision relative timestamp.
 * This is the lowest-level recording unit — the exact bytes sent over Bluetooth.
 */
data class HidReportFrame(
    val relativeNs: Long,
    val report: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HidReportFrame) return false
        return relativeNs == other.relativeNs && report.contentEquals(other.report)
    }

    override fun hashCode(): Int = 31 * relativeNs.hashCode() + report.contentHashCode()
}

data class HidRecordingData(
    val id: String,
    val name: String,
    val createdAt: Long,
    val durationMs: Long,
    val frameCount: Int,
    val frames: List<HidReportFrame>,
    val profileUsed: String = "xbox"
)

data class HidRecordingMeta(
    val id: String,
    val name: String,
    val createdAt: Long,
    val durationMs: Long,
    val frameCount: Int,
    val profileUsed: String = "xbox"
)

object HidFrameSerializer {

    fun encodeReport(report: ByteArray): String =
        Base64.encodeToString(report, Base64.NO_WRAP)

    fun decodeReport(encoded: String): ByteArray =
        Base64.decode(encoded, Base64.NO_WRAP)
}
