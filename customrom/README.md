# Panduan Integrasi Zonify Music ke Custom ROM (AOSP / LineageOS)

Dokumen ini menjelaskan langkah-langkah memasukkan **Zonify Music** sebagai aplikasi pemutar musik bawaan (*pre-installed privileged system app*) ke dalam source tree Custom ROM Anda.

---

### 1. Build APK Release
Jalankan perintah build Gradle untuk menghasilkan APK:
```bash
./gradlew :app:assembleRelease
```
File APK akan berada di:
`app/build/outputs/apk/release/app-release.apk`

---

### 2. Struktur di Source Tree Custom ROM
Salin folder integrasi ke dalam direktori aplikasi ROM Anda, misalnya:
`vendor/<nama-rom>/packages/apps/Zonify/` atau `packages/apps/Zonify/`

Struktur folder:
```
packages/apps/Zonify/
├── Android.bp
├── privapp-permissions-zonify.xml
└── prebuilt/
    └── ZonifyMusic.apk  (Rename dari app-release.apk)
```

---

### 3. Daftarkan Package ke Makefile Device / ROM
Tambahkan package `ZonifyMusic` ke file konfigurasi produk ROM Anda (misal `device.mk`, `common.mk`, atau `<rom_name>.mk`):

```makefile
# Include Zonify Audiophile Music Player
PRODUCT_PACKAGES += \
    ZonifyMusic
```

---

### 4. Build Custom ROM
Lakukan kompilasi Custom ROM seperti biasa:
```bash
brunch <codename_device>
# atau
mka bacon
```

Setelah ROM di-flash ke perangkat, Zonify Music akan terinstall secara otomatis di:
* `/product/priv-app/ZonifyMusic/ZonifyMusic.apk`
* Permission whitelist di `/product/etc/permissions/privapp-permissions-zonify.xml`
