package com.example.superiorcontroller.hid

object GamepadReportBuilder {

    fun buildReport(state: GamepadState): ByteArray {
        return byteArrayOf(
            (state.buttons and 0xFF).toByte(),              // Byte 0: buttons [0..7]
            ((state.buttons shr 8) and 0x07).toByte(),      // Byte 1: buttons [8..10] + 5-bit pad
            (state.hatSwitchValue() and 0x0F).toByte(),     // Byte 2: hat switch
            state.leftX.toByte(),                            // Byte 3: X
            state.leftY.toByte(),                            // Byte 4: Y
            state.rightX.toByte(),                           // Byte 5: Rx
            state.rightY.toByte(),                           // Byte 6: Ry
            state.leftTrigger.toByte(),                      // Byte 7: Z  (LT)
            state.rightTrigger.toByte()                      // Byte 8: Rz (RT)
        )
    }

    fun neutralReport(): ByteArray = buildReport(GamepadState())

    fun toHexString(report: ByteArray): String =
        report.joinToString(" ") { "%02X".format(it) }

    fun toBinaryString(report: ByteArray): String =
        report.joinToString(" ") {
            Integer.toBinaryString(it.toInt() and 0xFF).padStart(8, '0')
        }

    fun describeBytes(state: GamepadState): String {
        val mask = state.buttons
        val b0 = mask and 0xFF
        val b1 = (mask shr 8) and 0x07
        val hat = state.hatSwitchValue()
        return buildString {
            append("b0=")
            append(Integer.toBinaryString(b0).padStart(8, '0'))
            append(" b1=")
            append(Integer.toBinaryString(b1).padStart(3, '0'))
            append("00000")
            append(" hat=$hat")
            append(" lt=${state.leftTrigger} rt=${state.rightTrigger}")
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
        if (state.isButtonPressed(GamepadButtons.L3)) pressed += "L3"
        if (state.isButtonPressed(GamepadButtons.R3)) pressed += "R3"
        if (state.isButtonPressed(GamepadButtons.HOME)) pressed += "HM"

        val hat = when (state.hatSwitchValue()) {
            HatSwitch.UP -> "N"; HatSwitch.UP_RIGHT -> "NE"; HatSwitch.RIGHT -> "E"
            HatSwitch.DOWN_RIGHT -> "SE"; HatSwitch.DOWN -> "S"; HatSwitch.DOWN_LEFT -> "SW"
            HatSwitch.LEFT -> "W"; HatSwitch.UP_LEFT -> "NW"; else -> "-"
        }
        val btns = pressed.ifEmpty { listOf("none") }.joinToString("+")
        return "[${toHexString(report)}] btn=$btns hat=$hat lt=${state.leftTrigger} rt=${state.rightTrigger}"
    }
}
