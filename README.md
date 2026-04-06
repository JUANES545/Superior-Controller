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
- **11 Digital Buttons + 2 Analog Triggers** — A, B, X, Y, LB, RB, Back, Start, L3, R3, Home + L2/R2 triggers.
- **D-Pad with Diagonals** — Full 8-direction hat switch (N, NE, E, SE, S, SW, W, NW) using standard POV encoding.
- **Dual Analog Joysticks** — Left and right virtual thumbsticks with X/Y axes (0–255).
- **Controller Profiles** — Switch between Xbox and PlayStation button layouts.
- **Hardware Gamepad Pass-through** — Connect a physical USB/BT controller and relay inputs over Bluetooth HID.
- **Input Recording & Playback** — Record gamepad sessions and replay them with nanosecond-precision timing. Supports both event-based and digital frame formats.
- **Assist Tempo** — Configurable temporal quantization for left/right inputs.
- **Device Management** — Save, rename, and quick-connect to known Bluetooth hosts.
- **Foreground Service** — Persistent Bluetooth connection that survives Activity lifecycle.
- **Debug Console** — Real-time log viewer with overlay mode, hex/binary HID report visualization.
- **Haptic & Sound Feedback** — Configurable vibration and click sounds on button press.
- **Onboarding** — First-time setup guide for new users.
- **Spanish Localization** — Full `es` translation.
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
├── MainActivity.kt                  # Entry point, permissions, hardware event dispatch
├── bluetooth/
│   ├── BluetoothHidManager.kt       # Bluetooth HID Device profile lifecycle
│   ├── KnownDevice.kt              # Saved device model
│   └── KnownDevicesRepository.kt   # Persistent known-devices storage
├── hid/
│   ├── HidDescriptor.kt             # USB HID Report Descriptor
│   ├── GamepadConstants.kt          # Button bitmasks, hat switch, axis/trigger defaults
│   ├── GamepadState.kt              # Immutable gamepad state snapshot
│   └── GamepadReportBuilder.kt      # Builds HID reports from state
├── input/
│   ├── HardwareGamepadManager.kt    # Physical controller event processing
│   ├── InputQuantizer.kt            # Deduplicates redundant axis/trigger events
│   └── TemporalQuantizer.kt         # Assist tempo quantization
├── recording/
│   ├── InputRecorder.kt             # Event-based session recorder
│   ├── HidReportRecorder.kt         # Digital frame-based recorder
│   ├── PlaybackEngine.kt            # Event playback with timing
│   ├── HidReportPlaybackEngine.kt   # Frame playback with nanosecond precision
│   ├── RecordedEvent.kt             # Event/snapshot/meta data models
│   ├── HidReportFrame.kt            # Frame data model
│   └── RecordingRepository.kt       # Persistent recording storage
├── settings/
│   └── SettingsRepository.kt        # DataStore-backed preferences
├── viewmodel/
│   └── GamepadViewModel.kt          # MVVM ViewModel, bridges UI ↔ BT ↔ Recording
└── ui/
    ├── GamepadScreen.kt             # Main gamepad layout (Compose)
    ├── OnboardingScreen.kt          # First-time user guide
    └── components/
        ├── ActionButtons.kt         # A, B, X, Y face buttons
        ├── DPad.kt                  # 8-direction D-Pad
        ├── ShoulderButtons.kt       # LB / RB shoulder buttons
        ├── VirtualJoystick.kt       # Analog thumbstick with touch tracking
        ├── StatusPanel.kt           # Connection status indicators
        ├── DebugLog.kt              # Real-time HID report log (inline + overlay)
        ├── DeviceSelectorSheet.kt   # Known device manager
        ├── RecordButton.kt          # Record toggle with timer
        ├── RecordingsSheet.kt       # Recording list management
        ├── PlaybackBar.kt           # Playback controls
        ├── SettingsSheet.kt         # App settings + profiles + about
        ├── ButtonHaptics.kt         # Vibration feedback
        └── ButtonSoundPlayer.kt     # Audio feedback
```

### Design Decisions

| Layer | Technology | Purpose |
|---|---|---|
| **UI** | Jetpack Compose + Material 3 | Declarative, reactive gamepad interface |
| **State** | Kotlin `StateFlow` + MVVM | Unidirectional data flow from inputs to Bluetooth |
| **Bluetooth** | `BluetoothHidDevice` API | Native HID profile — no drivers needed on the host |
| **HID Protocol** | Custom report descriptor | Maximizes compatibility with Windows, Linux, macOS HID drivers |
| **Persistence** | DataStore + JSON files | Settings, known devices, and recordings survive app restarts |
| **Recording** | Dual-mode (event + frame) | Event-based for editing, frame-based for exact reproduction |

### Data Flow

```
Touch / Hardware Input → Compose UI / HardwareGamepadManager
    → GamepadViewModel → GamepadState
    → GamepadReportBuilder → BluetoothHidManager → Host Device
                          ↘ InputRecorder / HidReportRecorder (if recording)
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

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a detailed version history.

## License

This project is provided as-is for personal and educational use.
