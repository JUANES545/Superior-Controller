# Changelog

All notable changes to Superior Controller will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- HID report recording in digital frame format (binary capture of raw reports)
- HID report playback engine with nanosecond-precision timing
- Input quantizer for deduplicating redundant axis/trigger events
- Onboarding screen for first-time users
- Digital recording settings in Settings sheet (frame format toggle)
- Spanish localization (`values-es/strings.xml`)

### Changed
- Enhanced recording repository to support both event-based and frame-based recordings
- Expanded SettingsRepository with digital recording preferences
- Refactored GamepadViewModel for dual recording/playback modes

## [1.6.0] - 2026-04-04

### Added
- PlayStation controller profile (DualSense button labels and layout)
- Xbox controller profile with standard mapping
- Profile selector in Settings to switch between controller layouts
- Dynamic button labels based on active profile
- Profile-aware HID descriptor generation

## [1.5.0] - 2026-04-04

### Added
- Debug log overlay mode (layered transparent view over gamepad controls)
- Toggle to switch between inline and overlay debug log
- Bluetooth HID foreground service for persistent connections
- Notification channel for ongoing Bluetooth connection status

### Changed
- DeviceSelectorSheet UI: added Material icons for edit/delete, improved spacing
- Enhanced playback controls layout and responsiveness

## [1.4.0] - 2026-04-03

### Added
- Device connection management (save, rename, remove known devices)
- Known devices repository with persistent storage
- Quick-connect to previously paired hosts
- Device switcher sheet with alias support
- Input recording and playback system (event-based)
- Record button with elapsed time indicator
- Recordings sheet (list, rename, delete, play)
- Playback bar with pause/resume/stop controls

### Changed
- Enhanced Bluetooth HID Manager with diagnostic logging and state snapshots
- Improved connection recovery on Activity resume
- Lazy registration: HID app registers on-demand when connecting to a host

## [1.3.0] - 2026-04-03

### Added
- Hardware gamepad pass-through (physical controller → Bluetooth HID)
- Key event and motion event forwarding from connected USB/BT controllers
- Device connect/disconnect detection with vendor/product ID logging
- Visual feedback on button press (haptic vibration, sound click)

## [1.2.0] - 2026-04-03

### Added
- Analog triggers (L2/R2) with configurable mode (analog vs button)
- Haptic feedback settings (enable/disable vibration on press)
- Sound feedback settings (enable/disable click sound on press)
- Settings sheet with preferences stored via DataStore
- Debug log visibility toggle in settings
- Home button (Button 11) in gamepad layout

### Changed
- Updated button mapping to 11 buttons + 2 triggers
- Revamped DebugLog UI with monospace font and auto-scroll
- Refactored trigger handling to support both analog and digital modes

## [1.1.0] - 2026-04-03

### Changed
- Refactored gamepad color scheme for better contrast
- Updated UI strings and labels
- Enhanced gamepad state management with immutable snapshots
- Improved gesture handling for joystick touch tracking
- Optimized Bluetooth HID report dispatch with throttling

## [1.0.0] - 2026-04-03

### Added
- Bluetooth HID gamepad device profile (registered as standard HID device)
- 8 digital buttons: A, B, X, Y, L1, R1, Select, Start
- 8-direction D-Pad with hat switch encoding (N, NE, E, SE, S, SW, W, NW)
- Virtual analog joystick with X/Y axes (0–255, center 128)
- 4-byte HID report descriptor compatible with Windows, Linux, and macOS
- Real-time debug console with hex/binary report visualization
- Bluetooth permission handling for Android 12+ and legacy devices
- Material 3 dark theme UI with Jetpack Compose
- MVVM architecture with StateFlow
- README with project documentation and APK download link

[Unreleased]: https://github.com/JUANES545/Superior-Controller/compare/v1.6.0...HEAD
[1.6.0]: https://github.com/JUANES545/Superior-Controller/compare/v1.5.0...v1.6.0
[1.5.0]: https://github.com/JUANES545/Superior-Controller/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/JUANES545/Superior-Controller/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/JUANES545/Superior-Controller/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/JUANES545/Superior-Controller/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/JUANES545/Superior-Controller/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/JUANES545/Superior-Controller/releases/tag/v1.0.0
