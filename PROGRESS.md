# PROGRESS.md

Ringkasan per bagian. Status: ✅ selesai · 🔄 sedang · ⏳ belum · ⚠️ belum bisa diverifikasi di perangkat.

## Bagian 1 — Struktur repo & tooling ✅
- Gradle multi-modul Kotlin DSL: `:shared` (JVM), `:wear`, `:mobile`; version catalog di `gradle/libs.versions.toml`; wrapper 8.14.3.
- `.github/workflows/build.yml`: JDK 17, cache Gradle, `assembleDebug` kedua modul + `testDebugUnitTest`, artefak `wear-debug` dan `mobile-debug`.
- `CLAUDE.md`, `README.md`, `DECISIONS.md`, `PROGRESS.md`.
- Logika inti teruji di `:shared` (lihat DECISIONS.md tentang keterbatasan SDK di lingkungan agen).

## 3.1 — Mulai pendakian & deteksi gunung ✅
- `assets/gunung.json` (86 gunung) dan `assets/jalur.json` (13 jalur, jumlah pos dari data) — ⚠️ koordinat/elevasi perlu verifikasi.
- `PeakFinder` (radius 20 km, maksimal 3, urut jarak; kasus Selo → Merbabu & Merapi) + unit test.
- Layar Mulai: satu fix GPS (timeout 20 detik → fallback daftar), gunung terakhir di atas, pilih jalur / tanpa data jalur.
- `tools/build_peaks.py` untuk memperluas daftar dari Overpass (manual).

## 3.2 — Altimeter & layar utama ✅
- `BaroAltimeter` (ISA, low-pass, offset kalibrasi tersimpan di Room) + `GpsAltitudeAverager` 30 detik + unit test.
- Kalibrasi: basecamp dari jalur.json → GPS rata-rata 30 detik → tiap pos dengan elevasi → puncak.
- Layar utama sesuai mockup 1 (jam, gunung·jalur, ketinggian besar tabular, puncak, sisa naik, cincin progres, m/jam 15 menit, perkiraan tiba, chip detak & SpO2); ambient = ketinggian + sisa naik saja.
- Rekam titik `{waktu, lat, lon, altGps, altBaro, hr}` tiap 15 dtk bergerak / 60 dtk diam (hemat: 30/120) ke Room.
- `RekamService` foreground (health|location) + `OngoingActivity` + Health Services `ExerciseClient` HIKING; START_STICKY + pemulihan sesi dari Room. ⚠️ perilaku 8+ jam di OnePlus belum diverifikasi.

## 3.3 — Catat pos ✅
- Daftar pos (sudah/sekarang/berikutnya), tombol besar "Catat Pos N", preset nama (Pos, Shelter, Sumber air, Camp, Persimpangan), getar konfirmasi.
- `WaypointMatcher` (radius 60 m) menawarkan "Sampai Pos 3?" dari pendakian lama di jalur yang sama + unit test.

## 3.4 — Navigasi balik ✅
- `Navigation.tujuanTurun` (pos berikutnya ke arah turun / basecamp) + panah relatif heading (rotation vector; fallback bearing GPS), jarak, selisih tinggi, arah jam, tanda utara, jarak ke basecamp + unit test.

## 3.5 — Detak jantung, SpO2, stres ✅
- HR dari Health Services; zona (220 − usia), rata-rata sesi, detak istirahat (median 3 menit diam).
- `HrThresholdMonitor`: > 30 detik terus-menerus, cooldown 2 menit, abaikan 10 menit, istirahat tetap memantau + unit test. Layar peringatan merah dengan grafik 2 menit (mockup 8). Pengaturan batas 100–200 dengan ± / rotary / preset (mockup 9).
- `SpO2Source`: (a) `SensorManagerSpO2Source` cari sensor "spo2"/"oxygen" ⚠️ perlu verifikasi; (b) `HealthConnectSpO2Source` via Data Layer. Ukur 30 detik dengan hitung mundur, tandai "meragukan" jika tangan bergerak, pengingat tiap pos / +400 m.
- `AmsRules` (SpO2 < ambang, turun ≥ 6 poin, HR istirahat +20) + unit test; layar peringatan oranye (mockup 5).
- `StressSource`: (a) sensor RR vendor ⚠️; (b) `HeartRateVariabilityRmssdRecord` dari HP. `StressCalculator` RMSSD → 0–100 dengan baseline basecamp + unit test; hanya saat diam ≥ 3 menit; "tidak tersedia di perangkat ini" bila tanpa sumber (mockup 7).

## 3.6 — Puncak otomatis & selesai ✅
- `SummitDetector` (≤ 50 m & ±40 m, tunda 5 menit setelah "Belum sampai") + unit test; layar puncak (mockup 11); kalibrasi ulang ke elevasi puncak.
- Layar Selesai (mockup 12) + "Kirim ke HP & ekspor": `ChannelClient` payload gzip + `DataClient` metadata; antrean `SyncWorker` bila HP tidak terhubung.

## 3.7 — Tile & komplikasi ✅
- `PendakiTileService` (Horologist SuspendingTileService): gunung, durasi, ketinggian + sisa naik, naik total, detak, SpO2, matahari terbenam (`SunCalc` offline) + sisa waktu; ketuk → buka aplikasi.
- Komplikasi `SHORT_TEXT`/`RANGED_VALUE` ketinggian dan sisa naik.

## 3.8 — Pengaturan ✅
- Batas detak, usia, ambang SpO2, interval GPS hemat/normal, pengingat minum (default 30 menit, bisa dimatikan), getar, layar debug (daftar sensor → Logcat, kapabilitas Health Services, koneksi HP).

## 4 — Aplikasi HP ✅ (Tahap 1)
- `HpListenerService` menerima sesi (ChannelClient), simpan Room, konfirmasi ke jam; melayani permintaan snapshot Health Connect.
- Daftar pendakian, detail (profil ketinggian + titik pos, statistik, tabel pos, grafik HR/SpO2 vs ketinggian).
- `GpxWriter` GPX 1.1 + `gpxtpx:hr`, waypoint pos/puncak, opsi isi, nama file `merbabu-selo-2026-10-04.gpx`; unit test validasi skema (XSD offline) & jumlah titik. Bagikan (FileProvider) / Simpan (Download).
- Health Connect: `ExerciseSessionRecord` HIKING + rute, `HeartRateRecord`, `ElevationGainedRecord`, `DistanceRecord`; baca `OxygenSaturationRecord` & `HeartRateVariabilityRmssdRecord`; izin + kasus belum terpasang.
- Halaman ekspor sesuai mockup 13 (Strava & Relive ditandai Tahap 2).

## Kualitas
- 35 unit test di `:shared` (`cd shared && ../gradlew test`), semua hijau lokal.
- Izin ditolak tidak crash: layar izin + fitur dinonaktifkan.
- Timber: DebugTree hanya di debug; release tidak mencatat pesan (tidak ada log lokasi).
- ⚠️ Modul Android hanya terverifikasi lewat GitHub Actions (lingkungan agen tanpa akses Google Maven).
