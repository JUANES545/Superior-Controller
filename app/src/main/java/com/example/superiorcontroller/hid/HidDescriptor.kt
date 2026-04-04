package com.example.superiorcontroller.hid

/**
 * HID Report Descriptor for a Windows-compatible Bluetooth gamepad.
 *
 * Layout designed to maximize compatibility with joy.cpl, gamepad-tester.com,
 * and the Windows generic HID gamepad driver.
 *
 * Structure:
 *   - 8 digital buttons (A, B, X, Y, L1, R1, Select, Start) — byte-aligned
 *   - 1 Hat Switch for D-pad (4 bits + 4 bits padding) — standard POV encoding
 *   - 2 analog axes X/Y (8 bits each, 0–255, center 128)
 *
 * Total report: 4 bytes
 *   Byte 0: buttons bitmask [bit0=A, bit1=B, bit2=X, bit3=Y, bit4=L1, bit5=R1, bit6=Select, bit7=Start]
 *   Byte 1: hat switch value (low nibble 0x0–0x8) + padding (high nibble)
 *   Byte 2: X axis  (0=left, 128=center, 255=right)
 *   Byte 3: Y axis  (0=up, 128=center, 255=down)
 *
 * Hat switch values (1-based, 0=null/centered):
 *   0=centered, 1=N, 2=NE, 3=E, 4=SE, 5=S, 6=SW, 7=W, 8=NW
 */
object HidDescriptor {

    const val REPORT_SIZE = 4

    @JvmField
    val GAMEPAD_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x05,                    // Usage (Gamepad)
        0xA1.toByte(), 0x01,           // Collection (Application)

        // ── 8 Buttons (1 byte, byte-aligned) ────────────────────
        0x05, 0x09,                    //   Usage Page (Button)
        0x19, 0x01,                    //   Usage Minimum (Button 1)
        0x29, 0x08,                    //   Usage Maximum (Button 8)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x25, 0x01,                    //   Logical Maximum (1)
        0x75, 0x01,                    //   Report Size (1)
        0x95.toByte(), 0x08,           //   Report Count (8)
        0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)

        // ── Hat Switch / D-Pad (4 bits) ─────────────────────────
        0x05, 0x01,                    //   Usage Page (Generic Desktop)
        0x09, 0x39,                    //   Usage (Hat Switch)
        0x15, 0x01,                    //   Logical Minimum (1)
        0x25, 0x08,                    //   Logical Maximum (8)
        0x35, 0x00,                    //   Physical Minimum (0)
        0x46, 0x3B, 0x01,             //   Physical Maximum (315 degrees)
        0x65, 0x14,                    //   Unit (Eng Rot: Degree)
        0x75, 0x04,                    //   Report Size (4)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x81.toByte(), 0x42,           //   Input (Data, Var, Abs, Null State)

        // ── 4-bit padding (completes byte 1) ────────────────────
        0x75, 0x04,                    //   Report Size (4)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x81.toByte(), 0x01,           //   Input (Constant)

        // ── X and Y axes (1 byte each) ──────────────────────────
        0x05, 0x01,                    //   Usage Page (Generic Desktop)
        0x09, 0x30,                    //   Usage (X)
        0x09, 0x31,                    //   Usage (Y)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x00,     //   Logical Maximum (255)
        0x35, 0x00,                    //   Physical Minimum (0)
        0x45, 0x00,                    //   Physical Maximum (0) → same as logical
        0x65, 0x00,                    //   Unit (None)
        0x75, 0x08,                    //   Report Size (8)
        0x95.toByte(), 0x02,           //   Report Count (2)
        0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)

        0xC0.toByte()                  // End Collection
    )
}
