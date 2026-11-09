# Zonify

Zonify is a free and open-source local music player for Android designed for high quality sound reproduction, bit-perfect playback, and native custom ROM integration.

## Features

- Local audio playback supporting FLAC, ALAC, WAV, DSD (DSF/DFF), Opus, and MP3.
- Native C++ audio engine with RBJ Parametric Equalizer and preamp control.
- Direct bit-perfect playback support for external USB DACs.
- Media3 MediaSessionService integration for lockscreen, notification controls, and Bluetooth AVRCP.
- Modern Jetpack Compose user interface with dynamic backdrop gradients and mini player.

## Module Structure

- app: Main application module and UI navigation.
- core/model: Domain data models for tracks, technical audio formats, and equalizer bands.
- core/audio-engine: C++ native audio engine, biquad filter DSP, and JNI bridge.
- core/database: Room SQLite persistence for track caching, favorites, and playlists.
- core/media: AndroidX Media3 background playback service and media notification.
- core/ui: Theme tokens, components, and widgets.
- feature/player: Full-screen player and technical stream information.
- feature/library: Local media scanner and library browser.
- feature/equalizer: Parametric equalizer and bit-perfect DAC controls.
- customrom: Blueprint and permissions for integration into AOSP / Custom ROM trees.

## Custom ROM Integration

Zonify can be integrated as a pre-installed privileged system app in AOSP-based Custom ROMs (LineageOS, crDroid, PixelOS, etc.).

### 1. Build Release APK

Run the Gradle build command:

```bash
./gradlew :app:assembleRelease
```

The APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

### 2. Add to ROM Source Tree

Copy the `customrom/` files into your ROM source tree (for example under `packages/apps/Zonify/` or `vendor/<rom>/packages/apps/Zonify/`):

```text
packages/apps/Zonify/
├── Android.bp
├── privapp-permissions-zonify.xml
└── prebuilt/
    └── ZonifyMusic.apk
```

Rename `app-release.apk` to `prebuilt/ZonifyMusic.apk`.

### 3. Include in Device Configuration

Add the package to your device or product makefile (`device.mk` or `common.mk`):

```makefile
PRODUCT_PACKAGES += \
    ZonifyMusic
```

### 4. Build the ROM

Compile the ROM using standard build commands:

```bash
brunch <device_codename>
```

The application will be installed into `/product/priv-app/ZonifyMusic/` with its privileged permissions whitelisted.

## Building from Source

### Prerequisites

- Android SDK (API Level 35)
- Android NDK (Version 26 or higher)
- CMake 3.22.1 or higher
- JDK 17 or higher

### Build Commands

```bash
git clone https://github.com/romiyusnandar/Zonify.git
cd Zonify
./gradlew build
```

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
