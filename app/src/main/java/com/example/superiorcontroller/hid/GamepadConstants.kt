package com.example.superiorcontroller.hid

/**
 * Button bitmask values (bits 0–7 map to HID Buttons 1–8).
 * D-pad constants use upper bits (0x100+) as identifiers only — they are NOT
 * part of the button byte but are routed to the hat switch in the ViewModel.
 */
object GamepadButtons {
    const val A          = 1 shl 0   // 0x01 → HID Button 1
    const val B          = 1 shl 1   // 0x02 → HID Button 2
    const val X          = 1 shl 2   // 0x04 → HID Button 3
    const val Y          = 1 shl 3   // 0x08 → HID Button 4
    const val L1         = 1 shl 4   // 0x10 → HID Button 5
    const val R1         = 1 shl 5   // 0x20 → HID Button 6
    const val SELECT     = 1 shl 6   // 0x40 → HID Button 7
    const val START      = 1 shl 7   // 0x80 → HID Button 8

    // D-pad identifiers — routed to hat switch, NOT to button bitmask
    const val DPAD_UP    = 1 shl 8   // 0x100
    const val DPAD_DOWN  = 1 shl 9   // 0x200
    const val DPAD_LEFT  = 1 shl 10  // 0x400
    const val DPAD_RIGHT = 1 shl 11  // 0x800

    fun isDpad(value: Int): Boolean = value and 0xFF00 != 0
}

/**
 * Hat switch values matching the HID descriptor (1-based, 0=null/centered).
 * Physical angles: 1→0°, 2→45°, 3→90° … 8→315°.
 */
object HatSwitch {
    const val CENTERED   = 0
    const val UP         = 1   // N     0°
    const val UP_RIGHT   = 2   // NE   45°
    const val RIGHT      = 3   // E    90°
    const val DOWN_RIGHT = 4   // SE  135°
    const val DOWN       = 5   // S   180°
    const val DOWN_LEFT  = 6   // SW  225°
    const val LEFT       = 7   // W   270°
    const val UP_LEFT    = 8   // NW  315°
}

object AxisDefaults {
    const val CENTER = 128
    const val MIN = 0
    const val MAX = 255
}
