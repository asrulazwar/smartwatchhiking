# CLAUDE.md — Pendaki (Wear OS + Android pendamping)

Aplikasi pendakian gunung pribadi untuk **OnePlus Watch 2** + aplikasi pendamping Android.
Tidak dirilis ke Play Store; dipasang lewat ADB. Prioritas: offline penuh, hemat baterai, kode rapi.

## Target perangkat
- OnePlus Watch 2 — Wear OS 4 (API 33), layar bulat 466×466, Snapdragon W5 Gen 1 + co-processor hemat
  daya, GPS dual-band L1+L5, barometer, sensor detak jantung, SpO2.
- HP Android apa pun dengan minSdk 26 dan Health Connect (opsional).

## Stack
| Modul | Isi | Catatan |
|---|---|---|
| `:shared` | model data (kotlinx.serialization), GPX, kontrak Data Layer, **semua logika murni** (geo, altimeter, puncak, HR, AMS, stres, navigasi, statistik, matahari) | Kotlin JVM murni, unit test di sini |
| `:wear` | aplikasi jam: Compose for Wear OS 1.5 + Horologist 0.7, Hilt, Room, DataStore, WorkManager, Health Services, Tiles, Complications, Data Layer | minSdk 30, targetSdk 34 |
| `:mobile` | aplikasi HP: Compose Material 3, Hilt, Room, Health Connect, Data Layer, ekspor GPX | minSdk 26 |

Versi di `gradle/libs.versions.toml`. Kedua aplikasi memakai `applicationId = id.asrul.pendaki` (wajib sama agar Data Layer bekerja).

## Aturan arsitektur
- Clean-ish per modul: `ui/` (satu file per layar + ViewModel), `domain/` (use case, state mesin pendakian), `data/` (Room, sensor, lokasi, Health Services, Data Layer, DataStore).
- State ke UI selalu `StateFlow`. ViewModel per layar. Tidak ada logika bisnis di Composable.
- Logika yang bisa diuji tanpa Android **harus** ada di `:shared` dan punya unit test.
- UI bahasa Indonesia lewat string resources (`values/strings.xml`), jangan hard-code teks.
- Angka: ketinggian "2.410 mdpl" (pemisah ribuan titik), waktu 24 jam, desimal koma → gunakan `Format` di `:shared`.
- Warna: latar `#000000`, aksen `#F2A23A`, teks `#F4F1EA`, sekunder `#A8A49C`, merah `#D94848`, hijau `#7BC67E`, biru SpO2 `#5BB7D8`. Angka besar `FontFeatureSettings "tnum"`. Font Manrope (`wear/src/main/res/font`).
- Tombol minimal 48 dp. Layar utama mendukung ambient (versi hemat: ketinggian & sisa naik saja).
- Rekaman tidak boleh hilang: tiap titik langsung ke Room; sesi aktif = Health Services `ExerciseClient` + foreground service + `OngoingActivity`. Jangan mengandalkan service biasa untuk pekerjaan panjang; pakai Health Services, WorkManager, Tiles, Complications.
- Logging lewat Timber. Build release tidak menanam `DebugTree` → tidak ada log lokasi.

## Cara build
```bash
./gradlew :wear:assembleDebug :mobile:assembleDebug   # butuh Android SDK (ANDROID_HOME)
./gradlew testDebugUnitTest :shared:test              # unit test
./gradlew :shared:test                                # bisa tanpa Android SDK
```
Tanpa Android SDK, `settings.gradle.kts` otomatis hanya memuat `:shared` (lihat DECISIONS.md).
CI: `.github/workflows/build.yml` (JDK 17) → artefak `wear-debug` dan `mobile-debug`.

## Install lewat ADB wireless
Jam: Settings → About → ketuk Build number 7× → Developer options → Wireless debugging → Pair new device.
```bash
adb pair <ip>:<port-pair>      # masukkan kode dari jam
adb connect <ip>:<port>
adb -s <ip>:<port> install -r wear/build/outputs/apk/debug/wear-debug.apk
adb -s <serial-hp> install -r mobile/build/outputs/apk/debug/mobile-debug.apk
```

## Belum diverifikasi di perangkat nyata (WAJIB dicek sebelum dipakai mendaki)
1. Apakah OnePlus Watch 2 mengekspos sensor SpO2 ke aplikasi pihak ketiga lewat `SensorManager`
   (lihat layar Debug: daftar semua sensor; `SensorManagerSpO2Source` mencari nama/tipe berisi "spo2"/"oxygen").
2. Apakah Health Services di perangkat ini memberi HRV/RR interval (`HEART_RATE_VARIABILITY_RMSSD` / RR tidak ada di
   Health Services 1.0 publik → fallback Health Connect lewat HP).
3. Perilaku foreground service + exercise session terhadap pembatasan daya OnePlus (apakah rekaman tetap jalan 8+ jam).
4. Akurasi barometer setelah kalibrasi di lapangan (basecamp, pos, puncak).
5. Konsumsi baterai dengan interval GPS 15 detik selama 10 jam.
6. Data `gunung.json`/`jalur.json` (koordinat & elevasi) — lihat DECISIONS.md.
7. Komunikasi Data Layer (ChannelClient) antar jam–HP dengan applicationId yang sama.

## Dokumen lain
- `DECISIONS.md` — keputusan dan alasannya.
- `PROGRESS.md` — ringkasan tiap bagian yang selesai.
- `tools/build_peaks.py` — ambil puncak dari Overpass API (manual, bukan CI).
