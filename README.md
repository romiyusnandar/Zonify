# Zonify Music (Audiophile Edition) 🎵✨

Aplikasi Pemutar Musik Android dengan estetika visual **Apple Music** dan engine audio kelas **Audiophile** (Bit-Perfect, Parametric EQ, Hi-Res Lossless, DSD Native/DoP), dirancang untuk siap dijadikan aplikasi bawaan (*Privileged System App*) pada Custom ROM (AOSP, LineageOS, crDroid, PixelOS, dll.).

---

## 🛠️ Tech Stack

* **UI & Framework**: Kotlin + Jetpack Compose (Material 3)
* **Audio DSP & Engine**: C++20 (NDK) + Google AAudio / Oboe + RBJ Biquad Filter Cascade
* **System Integration**: AndroidX Media3 (`MediaSessionService`, Notification Controls, Bluetooth AVRCP)
* **Image Loading & Dynamic Blur**: Coil 3 + AndroidX Palette + Hardware Blur
* **Target OS**: Android 8.0+ (API 26) hingga Android 15 (API 35)
* **Custom ROM Build System**: Android Blueprint (`Android.bp`) + Privileged Permission Whitelist

---

## 📐 Arsitektur Modular

```
Zonify/
├── app/                      # Main Application, Navigation & Lifecycle
├── core/
│   ├── model/                # Domain models: Track, AudioFormatInfo, EqualizerProfile
│   ├── audio-engine/         # Native C++ DSP Engine, Parametric EQ, Bit-perfect JNI Bridge
│   ├── media/                # Media3 MediaSessionService & System Playback Controller
│   └── ui/                   # Apple Music Design System, Dynamic Blur, Mini Player
├── feature/
│   ├── player/               # Full-screen Apple Music Player, Scrubber, Badges
│   ├── library/              # 2-column album grid, category navigation, media scanner
│   └── equalizer/            # Multi-band Parametric EQ, Preamp, AutoEQ presets
└── customrom/                # Android.bp & privapp-permissions-zonify.xml untuk ROM tree
```

---

## 🎧 Fitur Audiophile

1. **Direct Bit-Perfect Mode**: Bypass Android AudioFlinger resampling untuk output langsung ke external USB DAC.
2. **True Parametric Equalizer (PEQ)**: Implementasi biquad filter RBJ (Peaking, Low Shelf, High Shelf, Low Pass, High Pass) di C++ dengan zero-latency buffer rendering.
3. **Hi-Res Lossless & DSD Support**: Indikator status audio real-time (FLAC, ALAC, WAV, DSD64-512).
4. **Digital Pre-Amp & Soft Limiter**: Mencegah digital clipping saat boost frekuensi EQ.

---

## 🍎 Fitur UI Apple Music

1. **Signature Animated Mesh Gradient / Blur**: Ekstraksi palet warna album artwork secara dinamis dengan efek animasi halus.
2. **Expanding Mini Player**: Mini player mengambang di atas bottom bar dengan indikator progress tipis, bertransisi mulus ke modal Full Player.
3. **Hi-Res Audio Badges**: Badge interaktif "Hi-Res Lossless", "Lossless", dan "Bit-Perfect" dengan dialog metadata teknis lengkap saat ditekan.

---

## 📦 Kompilasi & Integrasi Custom ROM

Lihat panduan lengkap di [`customrom/README.md`](customrom/README.md).
