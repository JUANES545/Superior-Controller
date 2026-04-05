package com.example.superiorcontroller.hid

object GamepadReportBuilder {

    private const val L2_DIGITAL_THRESHOLD = 128

    fun buildReport(state: GamepadState, psProfile: Boolean = false): ByteArray {
        return if (psProfile) buildPsReport(state) else buildXboxReport(state)
    }

    fun neutralReport(psProfile: Boolean = false): ByteArray =
        buildReport(GamepadState(), psProfile)

    /**
     * Xbox: 11 buttons in bits 0-10
     *   0=A, 1=B, 2=X, 3=Y, 4=LB, 5=RB, 6=Back, 7=Start, 8=L3, 9=R3, 10=Home
     */
    private fun buildXboxReport(state: GamepadState): ByteArray {
        return byteArrayOf(
            (state.buttons and 0xFF).toByte(),
            ((state.buttons shr 8) and 0x07).toByte(),
            (state.hatSwitchValue() and 0x0F).toByte(),
            state.leftX.toByte(),
            state.leftY.toByte(),
            state.rightX.toByte(),
            state.rightY.toByte(),
            state.leftTrigger.toByte(),
            state.rightTrigger.toByte()
        )
    }

    /**
     * PlayStation: 14 buttons in bits 0-13
     *   0=Square(X), 1=Cross(A), 2=Circle(B), 3=Triangle(Y),
     *   4=L1(LB), 5=R1(RB), 6=L2(digital), 7=R2(digital),
     *   8=Create(Back), 9=Options(Start), 10=L3, 11=R3, 12=PS(Home), 13=Touchpad(0)
     */
    private fun buildPsReport(state: GamepadState): ByteArray {
        val xb = state.buttons
        var ps = 0
        if (xb and GamepadButtons.X != 0)     ps = ps or (1 shl 0)   // Square
        if (xb and GamepadButtons.A != 0)     ps = ps or (1 shl 1)   // Cross
        if (xb and GamepadButtons.B != 0)     ps = ps or (1 shl 2)   // Circle
        if (xb and GamepadButtons.Y != 0)     ps = ps or (1 shl 3)   // Triangle
        if (xb and GamepadButtons.LB != 0)    ps = ps or (1 shl 4)   // L1
        if (xb and GamepadButtons.RB != 0)    ps = ps or (1 shl 5)   // R1
        if (state.leftTrigger >= L2_DIGITAL_THRESHOLD)
                                               ps = ps or (1 shl 6)   // L2 digital
        if (state.rightTrigger >= L2_DIGITAL_THRESHOLD)
                                               ps = ps or (1 shl 7)   // R2 digital
        if (xb and GamepadButtons.BACK != 0)  ps = ps or (1 shl 8)   // Create
        if (xb and GamepadButtons.START != 0) ps = ps or (1 shl 9)   // Options
        if (xb and GamepadButtons.L3 != 0)    ps = ps or (1 shl 10)  // L3
        if (xb and GamepadButtons.R3 != 0)    ps = ps or (1 shl 11)  // R3
        if (xb and GamepadButtons.HOME != 0)  ps = ps or (1 shl 12)  // PS
        // bit 13 = Touchpad, always 0

        return byteArrayOf(
            (ps and 0xFF).toByte(),
            ((ps shr 8) and 0x3F).toByte(),
            (state.hatSwitchValue() and 0x0F).toByte(),
            state.leftX.toByte(),
            state.leftY.toByte(),
            state.rightX.toByte(),
            state.rightY.toByte(),
            state.leftTrigger.toByte(),
            state.rightTrigger.toByte()
        )
    }

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
