package com.example.superiorcontroller.hid

/**
 * HID Report Descriptor with configurable button count for debugging.
 *
 * Set [EXTENDED_BUTTONS] to false, rebuild, and re-pair to test with 8 buttons.
 * Set to true for the full 12-button Xbox-like layout.
 *
 * 12-button mode (7 bytes):
 *   Byte 0: buttons[0..7]  = A, B, X, Y, LB, RB, Back, Start
 *   Byte 1: buttons[8..11] = LT, RT, L3, R3 | padding
 *   Byte 2: hat switch | padding
 *   Byte 3-6: LX, LY, RX, RY
 *
 * 8-button mode (6 bytes):
 *   Byte 0: buttons[0..7]  = A, B, X, Y, LB, RB, Back, Start
 *   Byte 1: hat switch | padding
 *   Byte 2-5: LX, LY, RX, RY
 */
object HidDescriptor {

    const val EXTENDED_BUTTONS = true

    val REPORT_SIZE: Int = if (EXTENDED_BUTTONS) 7 else 6

    @JvmField
    val GAMEPAD_DESCRIPTOR: ByteArray = if (EXTENDED_BUTTONS) descriptor12() else descriptor8()

    private fun descriptor8(): ByteArray = byteArrayOf(
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x05,                    // Usage (Gamepad)
        0xA1.toByte(), 0x01,           // Collection (Application)

        // ── 8 Buttons (1 byte, byte-aligned) ───────────────────
        0x05, 0x09,                    //   Usage Page (Button)
        0x19, 0x01,                    //   Usage Minimum (Button 1)
        0x29, 0x08,                    //   Usage Maximum (Button 8)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x25, 0x01,                    //   Logical Maximum (1)
        0x75, 0x01,                    //   Report Size (1)
        0x95.toByte(), 0x08,           //   Report Count (8)
        0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)

        // ── Hat Switch (4 bits + 4 padding = 1 byte) ───────────
        0x05, 0x01,                    //   Usage Page (Generic Desktop)
        0x09, 0x39,                    //   Usage (Hat Switch)
        0x15, 0x01,                    //   Logical Minimum (1)
        0x25, 0x08,                    //   Logical Maximum (8)
        0x35, 0x00,                    //   Physical Minimum (0)
        0x46, 0x3B, 0x01,             //   Physical Maximum (315°)
        0x65, 0x14,                    //   Unit (Eng Rot: Degree)
        0x75, 0x04,                    //   Report Size (4)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x81.toByte(), 0x42,           //   Input (Data, Var, Abs, Null State)
        0x75, 0x04,                    //   Report Size (4)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x81.toByte(), 0x01,           //   Input (Constant) — padding

        // ── Axes: X, Y, Rx, Ry (4 bytes) ──────────────────────
        0x05, 0x01,                    //   Usage Page (Generic Desktop)
        0x09, 0x30,                    //   Usage (X)
        0x09, 0x31,                    //   Usage (Y)
        0x09, 0x33,                    //   Usage (Rx)
        0x09, 0x34,                    //   Usage (Ry)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x00,     //   Logical Maximum (255)
        0x35, 0x00,                    //   Physical Minimum (0)
        0x45, 0x00,                    //   Physical Maximum (0)
        0x65, 0x00,                    //   Unit (None)
        0x75, 0x08,                    //   Report Size (8)
        0x95.toByte(), 0x04,           //   Report Count (4)
        0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)

        0xC0.toByte()                  // End Collection
    )

    private fun descriptor12(): ByteArray = byteArrayOf(
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x05,                    // Usage (Gamepad)
        0xA1.toByte(), 0x01,           // Collection (Application)

        // ── 12 Buttons + 4 padding (2 bytes) ───────────────────
        0x05, 0x09,                    //   Usage Page (Button)
        0x19, 0x01,                    //   Usage Minimum (Button 1)
        0x29, 0x0C,                    //   Usage Maximum (Button 12)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x25, 0x01,                    //   Logical Maximum (1)
        0x75, 0x01,                    //   Report Size (1)
        0x95.toByte(), 0x0C,           //   Report Count (12)
        0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)
        0x75, 0x01,                    //   Report Size (1)
        0x95.toByte(), 0x04,           //   Report Count (4)
        0x81.toByte(), 0x01,           //   Input (Constant) — padding

        // ── Hat Switch (4 bits + 4 padding = 1 byte) ───────────
        0x05, 0x01,                    //   Usage Page (Generic Desktop)
        0x09, 0x39,                    //   Usage (Hat Switch)
        0x15, 0x01,                    //   Logical Minimum (1)
        0x25, 0x08,                    //   Logical Maximum (8)
        0x35, 0x00,                    //   Physical Minimum (0)
        0x46, 0x3B, 0x01,             //   Physical Maximum (315°)
        0x65, 0x14,                    //   Unit (Eng Rot: Degree)
        0x75, 0x04,                    //   Report Size (4)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x81.toByte(), 0x42,           //   Input (Data, Var, Abs, Null State)
        0x75, 0x04,                    //   Report Size (4)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x81.toByte(), 0x01,           //   Input (Constant) — padding

        // ── Axes: X, Y, Rx, Ry (4 bytes) ──────────────────────
        0x05, 0x01,                    //   Usage Page (Generic Desktop)
        0x09, 0x30,                    //   Usage (X)
        0x09, 0x31,                    //   Usage (Y)
        0x09, 0x33,                    //   Usage (Rx)
        0x09, 0x34,                    //   Usage (Ry)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x00,     //   Logical Maximum (255)
        0x35, 0x00,                    //   Physical Minimum (0)
        0x45, 0x00,                    //   Physical Maximum (0)
        0x65, 0x00,                    //   Unit (None)
        0x75, 0x08,                    //   Report Size (8)
        0x95.toByte(), 0x04,           //   Report Count (4)
        0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)

        0xC0.toByte()                  // End Collection
    )
}
