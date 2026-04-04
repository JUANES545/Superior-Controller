package com.example.superiorcontroller.hid

data class GamepadState(
    val buttons: Int = 0,
    val dpad: Int = 0,
    val leftX: Int = AxisDefaults.CENTER,
    val leftY: Int = AxisDefaults.CENTER,
    val rightX: Int = AxisDefaults.CENTER,
    val rightY: Int = AxisDefaults.CENTER,
    val leftTrigger: Int = TriggerDefaults.REST,
    val rightTrigger: Int = TriggerDefaults.REST
) {
    fun isButtonPressed(button: Int): Boolean =
        (buttons and (button and GamepadButtons.BUTTON_MASK)) != 0

    fun withButtonPressed(button: Int): GamepadState =
        copy(buttons = buttons or (button and GamepadButtons.BUTTON_MASK))

    fun withButtonReleased(button: Int): GamepadState =
        copy(buttons = buttons and (button and GamepadButtons.BUTTON_MASK).inv())

    fun withDpadPressed(direction: Int): GamepadState =
        copy(dpad = dpad or direction)

    fun withDpadReleased(direction: Int): GamepadState =
        copy(dpad = dpad and direction.inv())

    fun withLeftAxis(x: Int, y: Int): GamepadState =
        copy(
            leftX = x.coerceIn(AxisDefaults.MIN, AxisDefaults.MAX),
            leftY = y.coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        )

    fun withRightAxis(x: Int, y: Int): GamepadState =
        copy(
            rightX = x.coerceIn(AxisDefaults.MIN, AxisDefaults.MAX),
            rightY = y.coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        )

    fun withLeftTrigger(value: Int): GamepadState =
        copy(leftTrigger = value.coerceIn(TriggerDefaults.REST, TriggerDefaults.MAX))

    fun withRightTrigger(value: Int): GamepadState =
        copy(rightTrigger = value.coerceIn(TriggerDefaults.REST, TriggerDefaults.MAX))

    fun neutral(): GamepadState = GamepadState()

    fun hatSwitchValue(): Int {
        val up    = (dpad and GamepadButtons.DPAD_UP) != 0
        val down  = (dpad and GamepadButtons.DPAD_DOWN) != 0
        val left  = (dpad and GamepadButtons.DPAD_LEFT) != 0
        val right = (dpad and GamepadButtons.DPAD_RIGHT) != 0
        return when {
            up && right    -> HatSwitch.UP_RIGHT
            up && left     -> HatSwitch.UP_LEFT
            down && right  -> HatSwitch.DOWN_RIGHT
            down && left   -> HatSwitch.DOWN_LEFT
            up             -> HatSwitch.UP
            down           -> HatSwitch.DOWN
            left           -> HatSwitch.LEFT
            right          -> HatSwitch.RIGHT
            else           -> HatSwitch.CENTERED
        }
    }
}
