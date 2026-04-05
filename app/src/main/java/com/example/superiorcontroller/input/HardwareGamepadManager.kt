package com.example.superiorcontroller.input

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.superiorcontroller.hid.GamepadButtons

class HardwareGamepadManager(context: Context) {

    interface Listener {
        fun onHwButtonDown(button: Int, name: String, eventTimeMs: Long)
        fun onHwButtonUp(button: Int, name: String, eventTimeMs: Long)
        fun onHwLeftAxis(x: Float, y: Float, eventTimeMs: Long)
        fun onHwRightAxis(x: Float, y: Float, eventTimeMs: Long)
        fun onHwLeftTrigger(value: Float, eventTimeMs: Long)
        fun onHwRightTrigger(value: Float, eventTimeMs: Long)
        fun onHwDeviceConnected(name: String, vendorId: Int, productId: Int)
        fun onHwDeviceDisconnected(name: String)
    }

    var listener: Listener? = null

    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
    private var prevHatX = 0f
    private var prevHatY = 0f
    private val knownGamepads = mutableMapOf<Int, String>()

    private val deviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) { checkDevice(deviceId) }
        override fun onInputDeviceChanged(deviceId: Int) { checkDevice(deviceId) }
        override fun onInputDeviceRemoved(deviceId: Int) {
            val name = knownGamepads.remove(deviceId)
            if (name != null) listener?.onHwDeviceDisconnected(name)
        }
    }

    fun register() {
        inputManager.registerInputDeviceListener(deviceListener, null)
        scanExistingDevices()
    }

    fun unregister() {
        inputManager.unregisterInputDeviceListener(deviceListener)
        knownGamepads.clear()
    }

    fun processKeyEvent(event: KeyEvent): Boolean {
        val device = event.device ?: return false
        if (device.sources and InputDevice.SOURCE_GAMEPAD == 0 &&
            device.sources and InputDevice.SOURCE_JOYSTICK == 0) return false

        val button = mapKeyCode(event.keyCode) ?: return false
        val name = buttonName(button)
        val timeMs = event.eventTime

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) listener?.onHwButtonDown(button, name, timeMs)
                return true
            }
            KeyEvent.ACTION_UP -> {
                listener?.onHwButtonUp(button, name, timeMs)
                return true
            }
        }
        return false
    }

    fun processMotionEvent(event: MotionEvent): Boolean {
        val device = event.device ?: return false
        if (device.sources and InputDevice.SOURCE_JOYSTICK == 0) return false
        if (event.action != MotionEvent.ACTION_MOVE) return false

        val deviceId = event.deviceId
        val timeMs = event.eventTime

        dispatchAxes(event, device, deviceId, timeMs)
        processDpadHat(event, timeMs)
        return true
    }

    private fun dispatchAxes(event: MotionEvent, device: InputDevice, deviceId: Int, timeMs: Long) {
        val lx = applyDeadzone(axis(MotionEvent.AXIS_X, event, deviceId), device, deviceId, MotionEvent.AXIS_X)
        val ly = applyDeadzone(axis(MotionEvent.AXIS_Y, event, deviceId), device, deviceId, MotionEvent.AXIS_Y)
        listener?.onHwLeftAxis(lx, ly, timeMs)

        val rx = applyDeadzone(axis(MotionEvent.AXIS_Z, event, deviceId), device, deviceId, MotionEvent.AXIS_Z)
        val ry = applyDeadzone(axis(MotionEvent.AXIS_RZ, event, deviceId), device, deviceId, MotionEvent.AXIS_RZ)
        listener?.onHwRightAxis(rx, ry, timeMs)

        val lt = axis(MotionEvent.AXIS_LTRIGGER, event, deviceId).coerceIn(0f, 1f)
        val rt = axis(MotionEvent.AXIS_RTRIGGER, event, deviceId).coerceIn(0f, 1f)
        listener?.onHwLeftTrigger(lt, timeMs)
        listener?.onHwRightTrigger(rt, timeMs)
    }

    private fun processDpadHat(event: MotionEvent, timeMs: Long) {
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        if (hatX == prevHatX && hatY == prevHatY) return

        if (prevHatX < -0.5f) listener?.onHwButtonUp(GamepadButtons.DPAD_LEFT, "D←", timeMs)
        if (prevHatX > 0.5f) listener?.onHwButtonUp(GamepadButtons.DPAD_RIGHT, "D→", timeMs)
        if (prevHatY < -0.5f) listener?.onHwButtonUp(GamepadButtons.DPAD_UP, "D↑", timeMs)
        if (prevHatY > 0.5f) listener?.onHwButtonUp(GamepadButtons.DPAD_DOWN, "D↓", timeMs)

        if (hatX < -0.5f) listener?.onHwButtonDown(GamepadButtons.DPAD_LEFT, "D←", timeMs)
        if (hatX > 0.5f) listener?.onHwButtonDown(GamepadButtons.DPAD_RIGHT, "D→", timeMs)
        if (hatY < -0.5f) listener?.onHwButtonDown(GamepadButtons.DPAD_UP, "D↑", timeMs)
        if (hatY > 0.5f) listener?.onHwButtonDown(GamepadButtons.DPAD_DOWN, "D↓", timeMs)

        prevHatX = hatX; prevHatY = hatY
    }

    private fun scanExistingDevices() {
        inputManager.inputDeviceIds.forEach { checkDevice(it) }
    }

    private fun checkDevice(deviceId: Int) {
        val device = inputManager.getInputDevice(deviceId) ?: return
        val isGamepad = device.sources and InputDevice.SOURCE_GAMEPAD != 0 ||
                device.sources and InputDevice.SOURCE_JOYSTICK != 0
        if (isGamepad && !knownGamepads.containsKey(deviceId)) {
            val name = device.name
            knownGamepads[deviceId] = name
            listener?.onHwDeviceConnected(name, device.vendorId, device.productId)
        }
    }

    private fun applyDeadzone(value: Float, device: InputDevice, deviceId: Int, axis: Int): Float {
        val range = device.getMotionRange(axis, InputDevice.SOURCE_JOYSTICK)
        val flat = range?.flat ?: DEFAULT_DEADZONE
        val deadzone = if (flat > 0f) flat else DEFAULT_DEADZONE
        return if (kotlin.math.abs(value) < deadzone) 0f else value
    }

    private fun mapKeyCode(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> GamepadButtons.A
        KeyEvent.KEYCODE_BUTTON_B -> GamepadButtons.B
        KeyEvent.KEYCODE_BUTTON_X -> GamepadButtons.X
        KeyEvent.KEYCODE_BUTTON_Y -> GamepadButtons.Y
        KeyEvent.KEYCODE_BUTTON_L1 -> GamepadButtons.LB
        KeyEvent.KEYCODE_BUTTON_R1 -> GamepadButtons.RB
        KeyEvent.KEYCODE_BUTTON_SELECT -> GamepadButtons.BACK
        KeyEvent.KEYCODE_BUTTON_START -> GamepadButtons.START
        KeyEvent.KEYCODE_BUTTON_THUMBL -> GamepadButtons.L3
        KeyEvent.KEYCODE_BUTTON_THUMBR -> GamepadButtons.R3
        KeyEvent.KEYCODE_BUTTON_MODE -> GamepadButtons.HOME
        else -> null
    }

    private fun buttonName(button: Int): String = when (button) {
        GamepadButtons.A -> "A"; GamepadButtons.B -> "B"
        GamepadButtons.X -> "X"; GamepadButtons.Y -> "Y"
        GamepadButtons.LB -> "LB"; GamepadButtons.RB -> "RB"
        GamepadButtons.BACK -> "BACK"; GamepadButtons.START -> "START"
        GamepadButtons.L3 -> "L3"; GamepadButtons.R3 -> "R3"
        GamepadButtons.HOME -> "HOME"
        else -> "?"
    }

    companion object {
        private const val DEFAULT_DEADZONE = 0.1f

        private fun axis(axisCode: Int, event: MotionEvent, deviceId: Int): Float =
            event.getAxisValue(axisCode)
    }
}
