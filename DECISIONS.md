# DECISIONS.md — keputusan desain yang diambil tanpa bertanya

Format: tanggal · keputusan · alasan.

## 2026-10-07 · Lingkungan build agen tidak bisa mengakses dl.google.com
Agen yang mengerjakan repo ini tidak bisa mengunduh Android SDK maupun artefak Google Maven
(dl.google.com diblokir kebijakan jaringan). Akibatnya:
- Modul `:wear` dan `:mobile` hanya dikompilasi di GitHub Actions (runner Ubuntu sudah membawa SDK).
- `settings.gradle.kts` hanya memasukkan modul Android jika `ANDROID_HOME`/`sdk.dir` ada, sehingga
  `./gradlew :shared:test` tetap jalan di mesin tanpa SDK.
- Semua logika yang bisa diuji tanpa Android (pencarian gunung, kalibrasi barometer, deteksi puncak,
  ambang detak, aturan AMS, konversi RMSSD → stres, generator GPX, navigasi, statistik, matahari
  terbenam, format angka) ditaruh di `:shared` (Kotlin JVM murni). `domain/` di `:wear` hanya
  membungkusnya sebagai use case + state. Ini sedikit berbeda dari prompt (yang menaruh use case
  di `:wear/domain`), tetapi membuat unit test bisa dijalankan dan diverifikasi lokal.

## 2026-10-07 · Versi pustaka
Dipilih kombinasi yang terbukti saling kompatibel dari POM Horologist 0.7.15 (Maven Central):
Kotlin 2.2.0, Compose BOM 2025.07.00, Wear Compose 1.5.0, Tiles 1.5.0, ProtoLayout 1.3.0,
Health Services 1.0.0, Lifecycle 2.9.2; ditambah AGP 8.11.1, Gradle 8.14.3, Hilt 2.57,
Room 2.7.2, Health Connect 1.1.0, Play Services Wearable 19.0.0. Tidak memakai versi terbaru
(Okt 2026) agar tidak terkena perubahan API yang belum dikenal.

## 2026-10-07 · Satu cabang, commit per bagian (bukan PR per fitur)
Sesi ini dibatasi hanya boleh mendorong ke cabang `claude/nice-keller-m8kv09`. Maka "satu PR per
fitur" diganti menjadi commit terpisah per bagian (3.1, 3.2, …) di cabang itu; satu PR gabungan
bisa dibuka oleh pemilik repo. Workflow CI juga dipicu pada push ke cabang `claude/**` agar
hasil build terlihat tanpa PR.

## 2026-10-07 · Data gunung dan jalur PERLU DIVERIFIKASI
`gunung.json` (86 gunung) dan `jalur.json` (13 jalur) ditulis dari pengetahuan umum tanpa akses
ke Overpass/peta saat pengerjaan. Koordinat puncak dibulatkan ±0,5 km, elevasi pos adalah perkiraan.
Gunakan `tools/build_peaks.py` untuk mengambil data OSM dan bandingkan sebelum mendaki sungguhan.
Koordinat basecamp di `jalur.json` juga perkiraan, hanya dipakai untuk kalibrasi awal bila GPS
belum rata-rata 30 detik.

## 2026-10-07 · Ambang detak dihitung sendiri dari aliran data
Health Services punya `ExerciseGoal` untuk ambang, tetapi perilakunya terhadap "terus-menerus
> 30 detik" dan cooldown 2 menit tidak bisa dikendalikan. Logika dihitung sendiri
(`HrThresholdMonitor`) dari aliran `HEART_RATE_BPM`, deterministik dan teruji.

## 2026-10-07 · Skala stres
RMSSD → stres: `30 − 40·log2(rmssd / baseline)`, dibatasi 0–100. Baseline = median RMSSD saat
diam ≥ 3 menit di basecamp; tanpa baseline dipakai 42 ms (nilai populasi dewasa). Nilai ini
bukan ukuran klinis; label Rendah/Sedang/Tinggi sesuai prompt.

## 2026-10-07 · Deteksi diam
"Diam" = kecepatan GPS < 0,5 m/s selama ≥ 20 detik ATAU tidak ada langkah (step detector) selama
20 detik, dan varians akselerometer rendah. Dipakai untuk interval GPS 60 detik, detak istirahat,
dan pengukuran stres (butuh diam ≥ 3 menit).

## 2026-10-07 · Lokasi memakai LocationManager, bukan Fused Location
Menghindari ketergantungan Play Services Location di jam; `LocationManager.GPS_PROVIDER` cukup
dan interval bisa diatur langsung. Health Services juga diminta `LOCATION` sebagai cadangan.

## 2026-10-07 · Skema GPX untuk unit test
`shared/src/test/resources/gpx.xsd` adalah transkripsi skema resmi GPX 1.1 (topografix.com tidak
dapat diakses dari lingkungan agen). Validasi skema dilakukan offline dengan `javax.xml.validation`.

## 2026-10-07 · Pengiriman sesi ke HP
`ChannelClient` untuk payload (JSON gzip), `DataClient` untuk metadata ringan. Sesi yang belum
dikonfirmasi HP (`/pendaki/sesi-diterima`) ditandai `terkirim=false` di Room dan dikirim ulang oleh
`WorkManager` (`SyncWorker`) saat ada node HP terhubung.
