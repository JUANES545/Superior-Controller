package com.example.superiorcontroller.hid

/**
 * Immutable snapshot of the gamepad state.
 *
 * @property buttons  8-bit bitmask for face/shoulder/menu buttons (A,B,X,Y,L1,R1,Select,Start)
 * @property dpad     Bitmask of D-pad directions using [GamepadButtons.DPAD_*] constants
 * @property axisX    Left stick X axis, 0–255, center 128
 * @property axisY    Left stick Y axis, 0–255, center 128
 */
data class GamepadState(
    val buttons: Int = 0,
    val dpad: Int = 0,
    val axisX: Int = AxisDefaults.CENTER,
    val axisY: Int = AxisDefaults.CENTER
) {
    fun isButtonPressed(button: Int): Boolean = (buttons and button) != 0

    fun withButtonPressed(button: Int): GamepadState =
        copy(buttons = buttons or (button and 0xFF))

    fun withButtonReleased(button: Int): GamepadState =
        copy(buttons = buttons and (button and 0xFF).inv())

    fun withDpadPressed(direction: Int): GamepadState =
        copy(dpad = dpad or direction)

    fun withDpadReleased(direction: Int): GamepadState =
        copy(dpad = dpad and direction.inv())

    fun withAxes(x: Int, y: Int): GamepadState =
        copy(
            axisX = x.coerceIn(AxisDefaults.MIN, AxisDefaults.MAX),
            axisY = y.coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        )

    fun neutral(): GamepadState = GamepadState()

    /**
     * Converts the current D-pad bitmask into a single hat switch value (0–8)
     * compatible with the HID descriptor. Supports diagonals.
     */
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
