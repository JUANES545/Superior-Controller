package com.example.superiorcontroller.hid

/**
 * Builds the 4-byte HID report that matches [HidDescriptor.GAMEPAD_DESCRIPTOR].
 *
 * Report layout:
 *   Byte 0: buttons [bit0..bit7] = A, B, X, Y, L1, R1, Select, Start
 *   Byte 1: hat switch (low nibble, 0–8) | padding (high nibble, always 0)
 *   Byte 2: X axis (0–255)
 *   Byte 3: Y axis (0–255)
 */
object GamepadReportBuilder {

    fun buildReport(state: GamepadState): ByteArray {
        return byteArrayOf(
            (state.buttons and 0xFF).toByte(),
            (state.hatSwitchValue() and 0x0F).toByte(),
            state.axisX.toByte(),
            state.axisY.toByte()
        )
    }

    fun neutralReport(): ByteArray {
        return buildReport(GamepadState())
    }

    // ── Debug utilities ─────────────────────────────────────────────────

    fun toHexString(report: ByteArray): String =
        report.joinToString(" ") { "%02X".format(it) }

    fun toBinaryString(report: ByteArray): String =
        report.joinToString(" ") {
            Integer.toBinaryString(it.toInt() and 0xFF).padStart(8, '0')
        }

    /**
     * Returns a human-readable summary of a report for debug logging.
     */
    fun describe(state: GamepadState): String {
        val report = buildReport(state)
        val pressedButtons = mutableListOf<String>()
        if (state.isButtonPressed(GamepadButtons.A)) pressedButtons += "A"
        if (state.isButtonPressed(GamepadButtons.B)) pressedButtons += "B"
        if (state.isButtonPressed(GamepadButtons.X)) pressedButtons += "X"
        if (state.isButtonPressed(GamepadButtons.Y)) pressedButtons += "Y"
        if (state.isButtonPressed(GamepadButtons.L1)) pressedButtons += "L1"
        if (state.isButtonPressed(GamepadButtons.R1)) pressedButtons += "R1"
        if (state.isButtonPressed(GamepadButtons.SELECT)) pressedButtons += "SEL"
        if (state.isButtonPressed(GamepadButtons.START)) pressedButtons += "STA"

        val hatName = when (state.hatSwitchValue()) {
            HatSwitch.UP -> "N"
            HatSwitch.UP_RIGHT -> "NE"
            HatSwitch.RIGHT -> "E"
            HatSwitch.DOWN_RIGHT -> "SE"
            HatSwitch.DOWN -> "S"
            HatSwitch.DOWN_LEFT -> "SW"
            HatSwitch.LEFT -> "W"
            HatSwitch.UP_LEFT -> "NW"
            else -> "-"
        }

        val btns = if (pressedButtons.isEmpty()) "none" else pressedButtons.joinToString("+")
        return "HEX=[${toHexString(report)}] BIN=[${toBinaryString(report)}] " +
                "btn=$btns hat=$hatName X=${state.axisX} Y=${state.axisY}"
    }
}
