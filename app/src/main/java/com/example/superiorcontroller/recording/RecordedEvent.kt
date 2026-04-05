package com.example.superiorcontroller.recording

object EventType {
    const val BUTTON_PRESS = 1
    const val BUTTON_RELEASE = 2
    const val LEFT_AXIS = 3
    const val RIGHT_AXIS = 4
    const val LEFT_TRIGGER = 5
    const val RIGHT_TRIGGER = 6
}

data class RecordedEvent(
    val t: Long,
    val type: Int,
    val btn: Int = 0,
    val x: Float = 0f,
    val y: Float = 0f
)

data class GamepadSnapshot(
    val buttons: Int = 0,
    val dpad: Int = 0,
    val leftX: Float = 0f,
    val leftY: Float = 0f,
    val rightX: Float = 0f,
    val rightY: Float = 0f,
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f
)

data class RecordingMeta(
    val id: String,
    val name: String,
    val createdAt: Long,
    val durationMs: Long,
    val eventCount: Int
)

data class RecordingData(
    val id: String,
    val name: String,
    val createdAt: Long,
    val durationMs: Long,
    val events: List<RecordedEvent>,
    val initialSnapshot: GamepadSnapshot? = null
)
