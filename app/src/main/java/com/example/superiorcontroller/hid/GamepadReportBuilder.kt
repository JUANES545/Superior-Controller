package com.example.superiorcontroller.hid

object GamepadReportBuilder {

    fun buildReport(state: GamepadState): ByteArray {
        return if (HidDescriptor.EXTENDED_BUTTONS) {
            byteArrayOf(
                (state.buttons and 0xFF).toByte(),
                ((state.buttons shr 8) and 0x0F).toByte(),
                (state.hatSwitchValue() and 0x0F).toByte(),
                state.leftX.toByte(),
                state.leftY.toByte(),
                state.rightX.toByte(),
                state.rightY.toByte()
            )
        } else {
            byteArrayOf(
                (state.buttons and 0xFF).toByte(),
                (state.hatSwitchValue() and 0x0F).toByte(),
                state.leftX.toByte(),
                state.leftY.toByte(),
                state.rightX.toByte(),
                state.rightY.toByte()
            )
        }
    }

    fun neutralReport(): ByteArray = buildReport(GamepadState())

    fun toHexString(report: ByteArray): String =
        report.joinToString(" ") { "%02X".format(it) }

    fun toBinaryString(report: ByteArray): String =
        report.joinToString(" ") {
            Integer.toBinaryString(it.toInt() and 0xFF).padStart(8, '0')
        }

    /**
     * Byte-level decomposition for debug logging.
     * Shows the button mask split into its component bytes.
     */
    fun describeBytes(state: GamepadState): String {
        val mask = state.buttons
        val b0 = mask and 0xFF
        val b1 = (mask shr 8) and 0x0F
        val hat = state.hatSwitchValue()
        return buildString {
            append("b0=")
            append(Integer.toBinaryString(b0).padStart(8, '0'))
            if (HidDescriptor.EXTENDED_BUTTONS) {
                append(" b1=")
                append(Integer.toBinaryString(b1).padStart(4, '0'))
                append("0000")
            }
            append(" hat=$hat")
        }
    }

    fun describe(state: GamepadState): String {
        val report = buildReport(state)
        val pressed = mutableListOf<String>()
        if (state.isButtonPressed(GamepadButtons.A)) pressed += "A"
        if (state.isButtonPressed(GamepadButtons.B)) pressed += "B"
        if (state.isButtonPressed(GamepadButtons.X)) pressed += "X"
        if (state.isButtonPressed(GamepadButtons.Y)) pressed += "Y"
        if (state.isButtonPressed(GamepadButtons.LB)) pressed += "LB"
        if (state.isButtonPressed(GamepadButtons.RB)) pressed += "RB"
        if (state.isButtonPressed(GamepadButtons.BACK)) pressed += "BK"
        if (state.isButtonPressed(GamepadButtons.START)) pressed += "ST"
        if (state.isButtonPressed(GamepadButtons.LT)) pressed += "LT"
        if (state.isButtonPressed(GamepadButtons.RT)) pressed += "RT"
        if (state.isButtonPressed(GamepadButtons.L3)) pressed += "L3"
        if (state.isButtonPressed(GamepadButtons.R3)) pressed += "R3"

        val hat = when (state.hatSwitchValue()) {
            HatSwitch.UP -> "N"; HatSwitch.UP_RIGHT -> "NE"; HatSwitch.RIGHT -> "E"
            HatSwitch.DOWN_RIGHT -> "SE"; HatSwitch.DOWN -> "S"; HatSwitch.DOWN_LEFT -> "SW"
            HatSwitch.LEFT -> "W"; HatSwitch.UP_LEFT -> "NW"; else -> "-"
        }
        val btns = pressed.ifEmpty { listOf("none") }.joinToString("+")
        return "[${toHexString(report)}] btn=$btns hat=$hat"
    }
}
