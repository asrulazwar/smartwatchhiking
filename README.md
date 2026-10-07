# Pendaki — aplikasi pendakian untuk OnePlus Watch 2 (Wear OS) + HP Android

Aplikasi pribadi, offline penuh: altimeter barometer, progres puncak, catat pos, navigasi balik,
detak jantung/SpO2/stres, deteksi puncak, kirim sesi ke HP, ekspor GPX, Health Connect.

## Build
Prasyarat: JDK 17, Android SDK (compileSdk 35).
```bash
./gradlew :wear:assembleDebug :mobile:assembleDebug
./gradlew testDebugUnitTest :shared:test
cd shared && ../gradlew test     # hanya :shared, tanpa Android SDK
```
APK: `wear/build/outputs/apk/debug/wear-debug.apk` dan `mobile/build/outputs/apk/debug/mobile-debug.apk`.
GitHub Actions membangun keduanya dan mengunggah artefak `wear-debug` dan `mobile-debug`.

## Pair ADB ke jam (wireless)
1. Di jam: **Settings → About → ketuk Build number 7×** untuk membuka Developer options.
2. **Developer options → Wireless debugging → aktifkan → Pair new device**; catat IP:port dan kode pairing.
3. Di komputer (jaringan Wi-Fi yang sama):
   ```bash
   adb pair 192.168.1.50:41234      # masukkan kode pairing
   adb connect 192.168.1.50:5555    # port "Wireless debugging" (bukan port pairing)
   adb devices
   ```

## Install kedua APK
```bash
adb -s 192.168.1.50:5555 install -r wear/build/outputs/apk/debug/wear-debug.apk
adb -s <serial-hp> install -r mobile/build/outputs/apk/debug/mobile-debug.apk
```
Kedua aplikasi memakai `applicationId` yang sama (`id.asrul.pendaki`) supaya Wearable Data Layer bisa berkomunikasi.

## Belum diverifikasi di perangkat nyata
1. Apakah OnePlus Watch 2 mengekspos sensor SpO2 ke aplikasi pihak ketiga lewat SensorManager.
2. Apakah Health Services di perangkat ini memberi HRV/RR interval.
3. Perilaku foreground service + exercise session terhadap pembatasan daya OnePlus (rekaman 8+ jam).
4. Akurasi barometer setelah kalibrasi di lapangan.
5. Konsumsi baterai dengan GPS interval 15 detik selama 10 jam.

Lihat `CLAUDE.md` untuk arsitektur dan `DECISIONS.md` untuk keputusan desain.

## Lisensi font
`wear/src/main/res/font/manrope.ttf` adalah Manrope (© The Manrope Project Authors), berlisensi SIL Open Font License 1.1 — https://github.com/googlefonts/manrope.
