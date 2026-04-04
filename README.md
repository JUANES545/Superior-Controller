# Superior Controller

**Turn your Android phone into a Bluetooth gamepad.**

Superior Controller is a native Android application that transforms your smartphone into a fully functional Bluetooth HID (Human Interface Device) gamepad. It connects wirelessly to any device that supports Bluetooth gamepads — PCs, laptops, tablets, TVs, and more — without requiring drivers, receivers, or third-party software.

<br>

<p align="center">

[![Download APK](https://img.shields.io/badge/%E2%AC%87%EF%B8%8F%20Download%20APK-Latest%20Release-brightgreen?style=for-the-badge&logo=android&logoColor=white)](https://github.com/JUANES545/Superior-Controller/releases/latest/download/app-release.apk)

</p>

<br>

## Features

- **Bluetooth HID Gamepad** — Registers as a standard HID device, recognized natively by Windows, Linux, macOS, and other Bluetooth hosts.
- **8 Digital Buttons** — A, B, X, Y, L1, R1, Select, Start mapped to HID buttons 1–8.
- **D-Pad with Diagonals** — Full 8-direction hat switch (N, NE, E, SE, S, SW, W, NW) using the standard POV encoding.
- **Analog Joystick** — Virtual thumbstick with X/Y axes (0–255 range, center at 128).
- **Real-time HID Reports** — 4-byte reports sent over Bluetooth with minimal latency.
- **Debug Console** — Built-in log viewer showing HID reports in hex/binary, connection events, and button states in real time.
- **No Root Required** — Uses the official Android `BluetoothHidDevice` API (API 28+).

## Requirements

| Requirement | Details |
|---|---|
| **Android** | 9.0 (Pie) or higher — API level 28+ |
| **Bluetooth** | Device must support Bluetooth HID Device profile |
| **Host** | Any device that accepts Bluetooth gamepads (PC, laptop, TV, etc.) |

## How to Use

1. **Install** the APK on your Android device.
2. **Grant** Bluetooth permissions when prompted.
3. **Register** the HID app by tapping "Register HID".
4. **Make Discoverable** — Tap "Discoverable" so the host can find your phone.
5. **Pair** from the host device (PC, laptop, etc.) — search for "Superior Controller" in Bluetooth settings.
6. **Play** — Once connected, all buttons and the joystick send real HID reports to the host.

## Architecture

```
com.example.superiorcontroller
├── MainActivity.kt                  # Entry point, permissions handling
├── bluetooth/
│   └── BluetoothHidManager.kt       # Bluetooth HID Device profile lifecycle
├── hid/
│   ├── HidDescriptor.kt             # USB HID Report Descriptor (4-byte gamepad)
│   ├── GamepadConstants.kt          # Button bitmasks, hat switch values, axis defaults
│   ├── GamepadState.kt              # Immutable gamepad state snapshot
│   └── GamepadReportBuilder.kt      # Builds 4-byte HID reports from state
├── viewmodel/
│   └── GamepadViewModel.kt          # MVVM ViewModel, bridges UI ↔ Bluetooth
└── ui/
    ├── GamepadScreen.kt             # Main screen layout (Compose)
    ├── components/
    │   ├── ActionButtons.kt         # A, B, X, Y face buttons
    │   ├── DPad.kt                  # 8-direction D-Pad
    │   ├── ShoulderButtons.kt       # L1 / R1 triggers
    │   ├── VirtualJoystick.kt       # Analog thumbstick with touch tracking
    │   ├── StatusPanel.kt           # Connection status indicators
    │   └── DebugLog.kt              # Real-time HID report log viewer
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

### Design Decisions

| Layer | Technology | Purpose |
|---|---|---|
| **UI** | Jetpack Compose + Material 3 | Declarative, reactive gamepad interface |
| **State** | Kotlin `StateFlow` + MVVM | Unidirectional data flow from inputs to Bluetooth |
| **Bluetooth** | `BluetoothHidDevice` API | Native HID profile — no drivers needed on the host |
| **HID Protocol** | Custom 4-byte descriptor | Maximizes compatibility with Windows `joy.cpl`, `gamepad-tester.com`, and generic HID drivers |

### HID Report Format (4 bytes)

```
Byte 0: [A][B][X][Y][L1][R1][SEL][STA]   ← 8 button bits
Byte 1: [Hat 0-8][----padding----]         ← D-Pad (low nibble) + 4-bit pad
Byte 2: [X axis 0-255]                    ← Joystick horizontal
Byte 3: [Y axis 0-255]                    ← Joystick vertical
```

### Data Flow

```
Touch Input → Compose UI → GamepadViewModel → GamepadState → GamepadReportBuilder → BluetoothHidManager → Host Device
```

## Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material 3
- **Architecture:** MVVM with StateFlow
- **Bluetooth:** Android BluetoothHidDevice API (API 28+)
- **Build:** Gradle with Version Catalogs
- **Min SDK:** 28 (Android 9.0 Pie)
- **Target SDK:** 36

## Building from Source

```bash
git clone git@github.com:JUANES545/Superior-Controller.git
cd Superior-Controller
./gradlew assembleRelease
```

The APK will be generated at `app/build/outputs/apk/release/app-release.apk`.

## License

This project is provided as-is for personal and educational use.
