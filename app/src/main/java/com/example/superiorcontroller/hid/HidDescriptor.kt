package com.example.superiorcontroller.hid

/**
 * HID Report Descriptors for gamepad profiles.
 *
 * Both profiles produce a 9-byte report with the same button/hat layout
 * (bytes 0-4) but different axis assignments for bytes 5-8:
 *
 * Xbox profile:   ... Rx Ry Z  Rz   (right stick = Rx/Ry, triggers = Z/Rz)
 * PlayStation:    ... Z  Rz Rx Ry   (right stick = Z/Rz,  triggers = Rx/Ry)
 */
object HidDescriptor {

    const val REPORT_SIZE = 9

    @JvmField
    val XBOX_DESCRIPTOR: ByteArray = createXboxDescriptor()

    @JvmField
    val PLAYSTATION_DESCRIPTOR: ByteArray = createPlayStationDescriptor()

    @JvmField
    val GAMEPAD_DESCRIPTOR: ByteArray = XBOX_DESCRIPTOR

    fun descriptorForProfile(profile: String): ByteArray =
        if (profile == "playstation") PLAYSTATION_DESCRIPTOR else XBOX_DESCRIPTOR

    private fun buttonsAndHat(): ByteArray = byteArrayOf(
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x05,                    // Usage (Gamepad)
        0xA1.toByte(), 0x01,           // Collection (Application)

        // 11 Buttons + 5-bit padding (2 bytes)
        0x05, 0x09,                    //   Usage Page (Button)
        0x19, 0x01,                    //   Usage Minimum (Button 1)
        0x29, 0x0B,                    //   Usage Maximum (Button 11)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x25, 0x01,                    //   Logical Maximum (1)
        0x75, 0x01,                    //   Report Size (1)
        0x95.toByte(), 0x0B,           //   Report Count (11)
        0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)
        0x75, 0x01,                    //   Report Size (1)
        0x95.toByte(), 0x05,           //   Report Count (5)
        0x81.toByte(), 0x01,           //   Input (Constant) — padding

        // Hat Switch (4 bits + 4 padding = 1 byte)
        0x05, 0x01,                    //   Usage Page (Generic Desktop)
        0x09, 0x39,                    //   Usage (Hat Switch)
        0x15, 0x01,                    //   Logical Minimum (1)
        0x25, 0x08,                    //   Logical Maximum (8)
        0x35, 0x00,                    //   Physical Minimum (0)
        0x46, 0x3B, 0x01,             //   Physical Maximum (315)
        0x65, 0x14,                    //   Unit (Eng Rot: Degree)
        0x75, 0x04,                    //   Report Size (4)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x81.toByte(), 0x42,           //   Input (Data, Var, Abs, Null State)
        0x75, 0x04,                    //   Report Size (4)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x81.toByte(), 0x01,           //   Input (Constant) — padding

        // Left Stick: X (0x30), Y (0x31) — 2 bytes
        0x05, 0x01,                    //   Usage Page (Generic Desktop)
        0x09, 0x30,                    //   Usage (X)
        0x09, 0x31,                    //   Usage (Y)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x00,     //   Logical Maximum (255)
        0x35, 0x00,                    //   Physical Minimum (0)
        0x45, 0x00,                    //   Physical Maximum (0)
        0x65, 0x00,                    //   Unit (None)
        0x75, 0x08,                    //   Report Size (8)
        0x95.toByte(), 0x02,           //   Report Count (2)
        0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)
    )

    /**
     * Xbox: bytes 5-6 = Rx(0x33), Ry(0x34) → right stick
     *       bytes 7-8 = Z(0x32),  Rz(0x35) → triggers
     */
    private fun createXboxDescriptor(): ByteArray =
        buttonsAndHat() + byteArrayOf(
            // Right Stick: Rx (0x33), Ry (0x34)
            0x09, 0x33,                    //   Usage (Rx)
            0x09, 0x34,                    //   Usage (Ry)
            0x95.toByte(), 0x02,           //   Report Count (2)
            0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)

            // Triggers: Z (0x32) then Rz (0x35) — separate blocks for ascending order
            0x09, 0x32,                    //   Usage (Z) — left trigger
            0x95.toByte(), 0x01,           //   Report Count (1)
            0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)
            0x09, 0x35,                    //   Usage (Rz) — right trigger
            0x95.toByte(), 0x01,           //   Report Count (1)
            0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)

            0xC0.toByte()                  // End Collection
        )

    /**
     * PlayStation: bytes 5-6 = Z(0x32),  Rz(0x35) → right stick
     *              bytes 7-8 = Rx(0x33), Ry(0x34) → triggers
     */
    private fun createPlayStationDescriptor(): ByteArray =
        buttonsAndHat() + byteArrayOf(
            // Right Stick: Z (0x32) then Rz (0x35) — separate blocks for ascending order
            0x09, 0x32,                    //   Usage (Z)
            0x95.toByte(), 0x01,           //   Report Count (1)
            0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)
            0x09, 0x35,                    //   Usage (Rz)
            0x95.toByte(), 0x01,           //   Report Count (1)
            0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)

            // Triggers: Rx (0x33), Ry (0x34)
            0x09, 0x33,                    //   Usage (Rx) — left trigger
            0x09, 0x34,                    //   Usage (Ry) — right trigger
            0x95.toByte(), 0x02,           //   Report Count (2)
            0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)

            0xC0.toByte()                  // End Collection
        )
}
