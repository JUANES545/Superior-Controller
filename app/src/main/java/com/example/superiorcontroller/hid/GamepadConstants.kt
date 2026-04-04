package com.example.superiorcontroller.hid

/**
 * Button bitmask values (bits 0–11 map to HID Buttons 1–12).
 * D-pad identifiers use bits 16+ — routed to the hat switch by the ViewModel.
 */
object GamepadButtons {
    // Byte 0: HID Buttons 1–8
    const val A     = 1 shl 0    // 0x001
    const val B     = 1 shl 1    // 0x002
    const val X     = 1 shl 2    // 0x004
    const val Y     = 1 shl 3    // 0x008
    const val LB    = 1 shl 4    // 0x010
    const val RB    = 1 shl 5    // 0x020
    const val BACK  = 1 shl 6    // 0x040
    const val START = 1 shl 7    // 0x080

    // Byte 1 (low nibble): HID Buttons 9–12
    const val LT    = 1 shl 8    // 0x100
    const val RT    = 1 shl 9    // 0x200
    const val L3    = 1 shl 10   // 0x400
    const val R3    = 1 shl 11   // 0x800

    // D-pad direction identifiers — NOT in button bitmask, routed to hat switch
    const val DPAD_UP    = 1 shl 16  // 0x10000
    const val DPAD_DOWN  = 1 shl 17  // 0x20000
    const val DPAD_LEFT  = 1 shl 18  // 0x40000
    const val DPAD_RIGHT = 1 shl 19  // 0x80000

    const val BUTTON_MASK = 0x0FFF

    fun isDpad(value: Int): Boolean = value >= (1 shl 16)
}

object HatSwitch {
    const val CENTERED   = 0
    const val UP         = 1
    const val UP_RIGHT   = 2
    const val RIGHT      = 3
    const val DOWN_RIGHT = 4
    const val DOWN       = 5
    const val DOWN_LEFT  = 6
    const val LEFT       = 7
    const val UP_LEFT    = 8
}

object AxisDefaults {
    const val CENTER = 128
    const val MIN = 0
    const val MAX = 255
}
