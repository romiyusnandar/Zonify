# Custom ROM Integration Guide

This guide explains how to include Zonify as a pre-installed privileged system app in an AOSP-based Custom ROM tree.

## 1. Build Release APK

Run the Gradle build command:

```bash
./gradlew :app:assembleRelease
```

The APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

## 2. Directory Structure in ROM Tree

Copy the integration files into your ROM tree (e.g. `packages/apps/Zonify/` or `vendor/<rom>/packages/apps/Zonify/`):

```text
packages/apps/Zonify/
├── Android.bp
├── privapp-permissions-zonify.xml
└── prebuilt/
    └── ZonifyMusic.apk
```

Copy and rename `app-release.apk` to `prebuilt/ZonifyMusic.apk`.

## 3. Register Package in Device Makefile

Add `ZonifyMusic` to your device or product makefile (such as `device.mk`, `common.mk`, or `<rom_name>.mk`):

```makefile
PRODUCT_PACKAGES += \
    ZonifyMusic
```

## 4. Build Custom ROM

Compile the ROM:

```bash
brunch <device_codename>
# or
mka bacon
```

After flashing, Zonify will be installed in `/product/priv-app/ZonifyMusic/` with permissions granted via `/product/etc/permissions/privapp-permissions-zonify.xml`.
