package id.asrul.pendaki.domain

import id.asrul.pendaki.data.assets.GunungRepository
import id.asrul.pendaki.data.datalayer.PhoneLink
import id.asrul.pendaki.data.db.SampelDao
import id.asrul.pendaki.data.db.SampelEntity
import id.asrul.pendaki.data.db.SesiDao
import id.asrul.pendaki.data.db.SesiEntity
import id.asrul.pendaki.data.db.SesiRepository
import id.asrul.pendaki.data.db.TitikDao
import id.asrul.pendaki.data.db.TitikEntity
import id.asrul.pendaki.data.db.WaypointDao
import id.asrul.pendaki.data.db.WaypointEntity
import id.asrul.pendaki.data.db.keModel
import id.asrul.pendaki.data.health.HealthServicesManager
import id.asrul.pendaki.data.location.FixGps
import id.asrul.pendaki.data.location.LocationSource
import id.asrul.pendaki.data.prefs.Pengaturan
import id.asrul.pendaki.data.prefs.PengaturanData
import id.asrul.pendaki.data.prefs.SnapshotTile
import id.asrul.pendaki.data.sensor.BarometerSource
import id.asrul.pendaki.data.sensor.CompassSource
import id.asrul.pendaki.data.sensor.MotionDetector
import id.asrul.pendaki.data.sensor.SpO2Source
import id.asrul.pendaki.data.sensor.StressSource
import id.asrul.pendaki.di.AppScope
import id.asrul.pendaki.di.Fallback
import id.asrul.pendaki.di.Primer
import id.asrul.pendaki.shared.altitude.BaroAltimeter
import id.asrul.pendaki.shared.altitude.GpsAltitudeAverager
import id.asrul.pendaki.shared.ams.AmsRules
import id.asrul.pendaki.shared.geo.Geo
import id.asrul.pendaki.shared.heart.HrHistory
import id.asrul.pendaki.shared.heart.HrThresholdMonitor
import id.asrul.pendaki.shared.heart.HrZones
import id.asrul.pendaki.shared.heart.RestingHrTracker
import id.asrul.pendaki.shared.model.Gunung
import id.asrul.pendaki.shared.model.Jalur
import id.asrul.pendaki.shared.model.JenisWaypoint
import id.asrul.pendaki.shared.model.SampelSpO2
import id.asrul.pendaki.shared.model.SampelStres
import id.asrul.pendaki.shared.model.TitikJejak
import id.asrul.pendaki.shared.model.Waypoint
import id.asrul.pendaki.shared.pos.WaypointMatcher
import id.asrul.pendaki.shared.stats.SessionStats
import id.asrul.pendaki.shared.stress.StressCalculator
import id.asrul.pendaki.shared.summit.SummitDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Mesin pendakian: menggabungkan barometer, GPS, Health Services, deteksi diam, SpO2, stres,
 * dan menulis semuanya ke Room secara berkala. Dihidupkan oleh [id.asrul.pendaki.service.RekamService].
 */
@Singleton
class HikeEngine @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    private val gunungRepo: GunungRepository,
    private val sesiDao: SesiDao,
    private val titikDao: TitikDao,
    private val waypointDao: WaypointDao,
    private val sampelDao: SampelDao,
    private val sesiRepo: SesiRepository,
    private val pengaturan: Pengaturan,
    private val barometer: BarometerSource,
    private val compass: CompassSource,
    private val motion: MotionDetector,
    private val location: LocationSource,
    private val health: HealthServicesManager,
    private val phone: PhoneLink,
    private val getar: Getar,
    @Primer private val spo2Primer: SpO2Source,
    @Fallback private val spo2Fallback: SpO2Source,
    @Primer private val stresPrimer: StressSource,
    @Fallback private val stresFallback: StressSource,
) {
    private val _state = MutableStateFlow(HikeState())
    val state: StateFlow<HikeState> = _state

    /** Kejadian satu kali untuk UI/notifikasi (peringatan muncul, pos tercatat, dsb.). */
    private val _kejadian = MutableStateFlow<Kejadian?>(null)
    val kejadian: StateFlow<Kejadian?> = _kejadian

    sealed class Kejadian(val waktu: Long = System.currentTimeMillis()) {
        class PeringatanDetak(val p: PeringatanHr) : Kejadian()
        class PeringatanAmsMuncul(val p: PeringatanAms) : Kejadian()
        class PuncakTerdeteksi(val info: InfoPuncak) : Kejadian()
        class PosTercatat(val nama: String) : Kejadian()
        class PengingatSpO2 : Kejadian()
        class PengingatMinum : Kejadian()
        class TawaranPos(val w: Waypoint) : Kejadian()
    }

    fun bersihkanKejadian() { _kejadian.value = null }

    private var scope: CoroutineScope? = null
    private val mutex = Mutex()

    // komponen per sesi
    private var altimeter = BaroAltimeter()
    private var gpsAvg = GpsAltitudeAverager()
    private var kalibrasiAwalSelesai = false
    private var hrMonitor = HrThresholdMonitor()
    private var hrHistory = HrHistory()
    private var restingHr = RestingHrTracker()
    private var summit: SummitDetector? = null
    private var matcher: WaypointMatcher? = null
    private var titikCache = ArrayList<TitikJejak>()
    private var hrSum = 0L
    private var hrCount = 0L
    private var hrTerakhir: Int? = null
    private var altSpO2Terakhir: Double? = null
    private var waktuStresTerakhir = 0L
    private var baselineRmssd: Double? = null
    private var pengaturanData = PengaturanData()
    private var titikTerakhirMs = 0L
    private var lokasiJob: Job? = null
    private var minumTerakhir = 0L

    // ---------------------------------------------------------------- memulai

    /** Memulai sesi baru. Dipanggil dari layar Mulai, lalu service dihidupkan. */
    suspend fun mulai(gunung: Gunung, jalur: Jalur?) = mutex.withLock {
        if (_state.value.sedangAktif) return
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = SesiEntity(
            id = id, gunungId = gunung.id, namaGunung = gunung.nama, namaJalur = jalur?.namaJalur,
            elevasiPuncak = gunung.elevasi, puncakLat = gunung.lat, puncakLon = gunung.lon,
            basecampElevasi = jalur?.basecampElevasi, mulai = now,
        )
        sesiDao.simpan(entity)
        pengaturan.setGunungTerakhir(gunung.id, jalur?.namaJalur)
        siapkanSesi(entity, gunung, jalur, emptyList())
        // basecamp sebagai waypoint pertama (posisi diisi saat fix pertama)
        _state.update { it.copy(fase = Fase.MEREKAM) }
        Timber.i("Sesi dimulai: ${gunung.nama} via ${jalur?.namaJalur}")
    }

    /** Memulihkan sesi aktif dari Room (setelah proses dimatikan sistem). */
    suspend fun pulihkan(): Boolean = mutex.withLock {
        if (_state.value.sedangAktif) return true
        val e = sesiDao.aktif() ?: return false
        val gunung = gunungRepo.gunung(e.gunungId) ?: Gunung(e.gunungId, e.namaGunung, e.puncakLat, e.puncakLon, e.elevasiPuncak, "")
        val jalur = gunungRepo.jalur(e.gunungId, e.namaJalur)
        val wp = waypointDao.semua(e.id).map { it.keModel() }
        siapkanSesi(e, gunung, jalur, wp)
        altimeter.pulihkanOffset(e.offsetBaro)
        kalibrasiAwalSelesai = e.offsetBaro != 0.0
        titikCache = ArrayList(titikDao.semua(e.id).map { it.keModel() })
        val spo2 = sampelDao.semua(e.id, SampelEntity.SPO2).map { SampelSpO2(it.waktu, it.nilai.toInt(), it.ekstra ?: 0.0, it.flag) }
        val stres = sampelDao.semua(e.id, SampelEntity.STRES).map { SampelStres(it.waktu, it.nilai.toInt(), it.ekstra ?: 0.0) }
        baselineRmssd = e.baselineRmssd
        val fase = when {
            e.waktuPuncak != null -> Fase.TURUN
            else -> Fase.MEREKAM
        }
        if (e.waktuPuncak != null) summit?.konfirmasi()
        _state.update {
            it.copy(
                fase = fase, waktuPuncak = e.waktuPuncak, hrIstirahatBasecamp = e.hrIstirahatBasecamp,
                spo2 = spo2.lastOrNull(), spo2Sebelumnya = spo2.dropLast(1).lastOrNull(),
                stres = stres.lastOrNull(), riwayatStres = stres,
                kalibrasiSiap = kalibrasiAwalSelesai,
                jumlahTitik = titikCache.size,
                naikTotalM = SessionStats.naikTotal(titikCache),
                jarakM = SessionStats.jarak(titikCache),
            )
        }
        Timber.i("Sesi dipulihkan: ${e.id}")
        true
    }

    private suspend fun siapkanSesi(e: SesiEntity, gunung: Gunung, jalur: Jalur?, wp: List<Waypoint>) {
        pengaturanData = pengaturan.sekarang()
        altimeter = BaroAltimeter()
        gpsAvg = GpsAltitudeAverager()
        kalibrasiAwalSelesai = false
        hrMonitor = HrThresholdMonitor(pengaturanData.batasDetak)
        hrHistory = HrHistory()
        restingHr = RestingHrTracker()
        summit = SummitDetector(gunung.lat, gunung.lon, gunung.elevasi.toDouble())
        val lama = sesiRepo.waypointPendakianLama(gunung.id, jalur?.namaJalur).map { it.keModel() }
        matcher = if (lama.isNotEmpty()) WaypointMatcher(lama) else null
        titikCache = ArrayList()
        hrSum = 0; hrCount = 0; hrTerakhir = null
        altSpO2Terakhir = null; waktuStresTerakhir = 0; minumTerakhir = e.mulai
        _state.value = HikeState(
            fase = Fase.MEREKAM, sesiId = e.id, gunung = gunung, jalur = jalur, mulai = e.mulai,
            waypoint = wp, barometerAda = barometer.tersedia,
            stresTersedia = stresPrimer.tersedia || stresFallback.tersedia,
            nomorPosManual = wp.count { it.jenis == JenisWaypoint.POS } + 1,
        ).let { perbaruiDaftarPos(it) }
    }

    // ---------------------------------------------------------------- sensor loop (dipanggil service)

    fun jalankanSensor() {
        if (scope != null) return
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = s
        motion.mulai(s)

        s.launch { pengaturan.data.collect { p -> pengaturanData = p; hrMonitor.batas = p.batasDetak; mulaiUlangLokasi() } }

        // barometer
        s.launch {
            barometer.tekanan().catch { Timber.w(it) }.collect { (t, hpa) ->
                val alt = altimeter.sampel(hpa)
                if (!kalibrasiAwalSelesai) kalibrasiAwal()
                perbaruiKetinggian(alt, t)
            }
        }

        // kompas
        s.launch { compass.heading().catch { }.collect { h -> _state.update { it.copy(headingDeg = h) } } }

        // diam/bergerak
        s.launch {
            motion.diam.collect { diam ->
                _state.update { it.copy(diam = diam) }
                mulaiUlangLokasi()
            }
        }

        // detak jantung dari Health Services
        s.launch { health.hr.collect { (t, bpm) -> terimaHr(t, bpm) } }

        // stres otomatis saat diam >= 3 menit, dan pengingat minum, snapshot tile
        s.launch {
            while (isActive) {
                delay(10_000)
                val now = System.currentTimeMillis()
                _state.update { it.copy(sekarang = now) }
                cekStresOtomatis(now)
                cekPengingatMinum(now)
                tulisSnapshotTile()
            }
        }
        mulaiUlangLokasi()
    }

    fun hentikanSensor() {
        scope?.cancel(); scope = null
        lokasiJob = null
        motion.berhenti()
    }

    private fun mulaiUlangLokasi() {
        val s = scope ?: return
        lokasiJob?.cancel()
        val diam = _state.value.diam
        val interval = if (diam) pengaturanData.intervalGpsDiamMs else pengaturanData.intervalGpsBergerakMs
        lokasiJob = s.launch {
            location.aliran(interval).catch { Timber.w(it) }.collect { fix -> terimaFix(fix, interval) }
        }
    }

    // ---------------------------------------------------------------- pemrosesan

    private suspend fun kalibrasiAwal() {
        val basecamp = _state.value.jalur?.basecampElevasi
        if (basecamp != null) {
            altimeter.kalibrasi(basecamp.toDouble()); selesaikanKalibrasi()
        } else if (gpsAvg.siap) {
            gpsAvg.rataRata?.let { altimeter.kalibrasi(it); selesaikanKalibrasi() }
        }
    }

    private suspend fun selesaikanKalibrasi() {
        kalibrasiAwalSelesai = true
        if (altSpO2Terakhir == null) altSpO2Terakhir = altimeter.ketinggianM
        _state.value.sesiId?.let { sesiDao.simpanOffset(it, altimeter.offsetM) }
        _state.update { it.copy(kalibrasiSiap = true) }
    }

    private fun perbaruiKetinggian(alt: Double, t: Long) {
        val st = _state.value
        val g = st.gunung ?: return
        val basecamp = st.jalur?.basecampElevasi?.toDouble()
            ?: titikCache.firstOrNull()?.let { SessionStats.altTerbaik(it) } ?: alt
        val sisa = g.elevasi - alt
        val v = SessionStats.kecepatanNaikMPerJam(titikCache, t)
        _state.update {
            it.copy(
                ketinggianM = alt, sisaNaikM = sisa,
                progres = SessionStats.progres(alt, basecamp, g.elevasi.toDouble()),
                kecepatanNaikMPerJam = v,
                perkiraanTiba = SessionStats.perkiraanTiba(t, sisa, v),
            )
        }
    }

    private suspend fun terimaFix(fix: FixGps, intervalMs: Long) {
        val st = _state.value
        if (!st.sedangAktif) return
        fix.altM?.let { gpsAvg.tambah(fix.waktuMs, it) }
        if (!kalibrasiAwalSelesai) kalibrasiAwal()

        // tanpa barometer: pakai GPS sebagai ketinggian
        val ketinggian = if (barometer.tersedia) st.ketinggianM else fix.altM
        if (!barometer.tersedia && fix.altM != null) perbaruiKetinggian(fix.altM, fix.waktuMs)

        val headingFallback = if ((fix.kecepatanMps ?: 0f) > 1.0f && !compass.tersedia) fix.bearingDeg else st.headingDeg
        _state.update {
            it.copy(lat = fix.lat, lon = fix.lon, altGps = fix.altM, akurasiM = fix.akurasiM,
                kecepatanMps = fix.kecepatanMps, headingDeg = headingFallback)
        }

        // basecamp waypoint saat fix pertama
        if (st.waypoint.isEmpty()) {
            simpanWaypoint("Basecamp", JenisWaypoint.BASECAMP, 0, fix.lat, fix.lon, ketinggian ?: fix.altM ?: 0.0)
        }

        // rekam titik sesuai interval
        if (fix.waktuMs - titikTerakhirMs >= intervalMs - 1_000) {
            titikTerakhirMs = fix.waktuMs
            val t = TitikJejak(fix.waktuMs, fix.lat, fix.lon, fix.altM, if (barometer.tersedia) ketinggian else null, hrTerakhir)
            titikCache.add(t)
            st.sesiId?.let { titikDao.simpan(TitikEntity(sesiId = it, waktu = t.waktu, lat = t.lat, lon = t.lon, altGps = t.altGps, altBaro = t.altBaro, hr = t.hr)) }
            _state.update {
                it.copy(jumlahTitik = titikCache.size, naikTotalM = SessionStats.naikTotal(titikCache), jarakM = SessionStats.jarak(titikCache))
            }
        }

        // deteksi puncak
        val sd = summit
        if (sd != null && _state.value.fase == Fase.MEREKAM && ketinggian != null) {
            val h = sd.cek(fix.waktuMs, fix.lat, fix.lon, ketinggian)
            if (h.terdeteksi) {
                val info = InfoPuncak(fix.waktuMs, fix.waktuMs - st.mulai, ketinggian, h.jarakM)
                _state.update { it.copy(fase = Fase.PUNCAK_TERDETEKSI, puncak = info) }
                _kejadian.value = Kejadian.PuncakTerdeteksi(info)
                getar.ganda()
            }
        }

        // tawaran pos dari pendakian lama
        matcher?.let { m ->
            if (_state.value.tawaranPos == null) {
                m.cocokkan(fix.lat, fix.lon)?.let { w ->
                    _state.update { it.copy(tawaranPos = w) }
                    _kejadian.value = Kejadian.TawaranPos(w)
                    getar.pendek()
                }
            }
        }

        // pengingat SpO2 tiap naik 400 m
        val lastSpO2Alt = altSpO2Terakhir
        if (ketinggian != null && lastSpO2Alt != null && ketinggian - lastSpO2Alt >= 400 && !_state.value.pengingatSpO2) {
            _state.update { it.copy(pengingatSpO2 = true) }
            _kejadian.value = Kejadian.PengingatSpO2()
            getar.pendek()
        }
    }

    private suspend fun terimaHr(t: Long, bpm: Int) {
        hrTerakhir = bpm
        hrSum += bpm; hrCount++
        hrHistory.tambah(t, bpm)
        val diam = _state.value.diam
        restingHr.tambah(t, bpm, diam)
        val istirahat = restingHr.terakhir
        val st = _state.value
        // detak istirahat basecamp = yang pertama kali terukur sebelum pos pertama
        if (istirahat != null && st.hrIstirahatBasecamp == null && st.jumlahPosTercatat == 0) {
            _state.update { it.copy(hrIstirahatBasecamp = istirahat) }
            st.sesiId?.let { id -> sesiDao.byId(id)?.let { sesiDao.perbarui(it.copy(hrIstirahatBasecamp = istirahat)) } }
        }
        val peringatan = hrMonitor.sampel(t, bpm)
        _state.update {
            it.copy(
                hr = bpm, hrWaktu = t, zona = HrZones.zona(bpm, pengaturanData.usia),
                hrRata = (hrSum / hrCount).toInt(), hrIstirahat = istirahat, riwayatHr = hrHistory.snapshot(),
                istirahatSampai = if (hrMonitor.sedangIstirahat(t)) t + hrMonitor.sisaIstirahatMs(t) else null,
            )
        }
        if (peringatan != null) {
            val p = PeringatanHr(peringatan.bpm, peringatan.batas, peringatan.durasiAtasMs, hrHistory.snapshot(), t)
            _state.update { it.copy(peringatanHr = p) }
            _kejadian.value = Kejadian.PeringatanDetak(p)
            getar.panjang()
        }
        // AMS dari detak istirahat
        if (istirahat != null && st.hrIstirahatBasecamp != null) {
            evaluasiAms(spo2Baru = null)
        }
    }

    // ---------------------------------------------------------------- aksi pengguna

    fun abaikanPeringatanHr() {
        val now = System.currentTimeMillis()
        hrMonitor.abaikan(now)
        _state.update { it.copy(peringatanHr = null) }
    }

    fun istirahatHr() {
        val now = System.currentTimeMillis()
        hrMonitor.istirahat(now)
        _state.update { it.copy(peringatanHr = null, istirahatSampai = now + 10 * 60_000L) }
    }

    fun tutupPeringatanAms() { _state.update { it.copy(peringatanAms = null) } }

    fun istirahatAms() {
        val now = System.currentTimeMillis()
        hrMonitor.istirahat(now)
        _state.update { it.copy(peringatanAms = null, istirahatSampai = now + 10 * 60_000L) }
    }

    /** Catat pos: nama dari data jalur (pos berikutnya) atau nama manual/preset. */
    suspend fun catatPos(namaManual: String? = null) {
        val st = _state.value
        val lat = st.lat ?: location.terakhirDikenal()?.lat ?: return
        val lon = st.lon ?: location.terakhirDikenal()?.lon ?: return
        val alt = st.ketinggianM ?: st.altGps ?: 0.0
        val berikut = st.posBerikutnya
        val nama: String
        val urutan: Int
        if (berikut != null && namaManual == null) {
            nama = berikut.pos.nama; urutan = berikut.indeks + 1
            berikut.pos.elevasiPerkiraan?.let { e ->
                altimeter.kalibrasi(e.toDouble())
                st.sesiId?.let { sesiDao.simpanOffset(it, altimeter.offsetM) }
                perbaruiKetinggian(e.toDouble(), System.currentTimeMillis())
            }
        } else {
            val dasar = namaManual ?: "Pos"
            urutan = st.nomorPosManual
            nama = if (dasar == "Pos") "Pos $urutan" else "$dasar $urutan"
        }
        simpanWaypoint(nama, JenisWaypoint.POS, urutan, lat, lon, alt, st.hr, st.spo2?.nilai)
        matcher?.tandaiDicatat(nama)
        altSpO2Terakhir = alt
        _state.update { it.copy(nomorPosManual = it.nomorPosManual + 1, tawaranPos = null, pengingatSpO2 = true) }
        _kejadian.value = Kejadian.PosTercatat(nama)
        getar.pendek()
    }

    /** Jawaban "Sampai Pos 3?" */
    suspend fun terimaTawaranPos() {
        val w = _state.value.tawaranPos ?: return
        catatPos(null)
        matcher?.tandaiDicatat(w.nama)
    }

    fun tolakTawaranPos() {
        val w = _state.value.tawaranPos ?: return
        matcher?.tolak(w)
        _state.update { it.copy(tawaranPos = null) }
    }

    fun belumSampaiPuncak() {
        summit?.belumSampai(System.currentTimeMillis())
        _state.update { it.copy(fase = Fase.MEREKAM, puncak = null) }
    }

    suspend fun konfirmasiPuncak() {
        val st = _state.value
        val g = st.gunung ?: return
        val now = System.currentTimeMillis()
        summit?.konfirmasi()
        altimeter.kalibrasi(g.elevasi.toDouble())
        st.sesiId?.let { id ->
            sesiDao.simpanOffset(id, altimeter.offsetM)
            sesiDao.byId(id)?.let { sesiDao.perbarui(it.copy(waktuPuncak = now)) }
        }
        simpanWaypoint("Puncak", JenisWaypoint.PUNCAK, 99, st.lat ?: g.lat, st.lon ?: g.lon, g.elevasi.toDouble(), st.hr, st.spo2?.nilai)
        _state.update { it.copy(fase = Fase.TURUN, waktuPuncak = now, puncak = null) }
        perbaruiKetinggian(g.elevasi.toDouble(), now)
        getar.ganda()
    }

    fun tutupPengingatSpO2() { _state.update { it.copy(pengingatSpO2 = false) } }
    fun tutupPengingatMinum() { _state.update { it.copy(pengingatMinum = false) } }

    /** Ukur SpO2 on-demand selama 30 detik. Mengembalikan sampel atau null. */
    suspend fun ukurSpO2(): SampelSpO2? {
        if (_state.value.pengukuranSpO2 != null) return null
        val st = _state.value
        val sumber = if (spo2Primer.tersedia) spo2Primer else spo2Fallback
        val nilai = mutableListOf<Int>()
        var bergerak = false
        _state.update { it.copy(pengukuranSpO2 = PengukuranSpO2(30, null), pengingatSpO2 = false) }
        val job = (scope ?: appScope).launch {
            sumber.ukur().catch { Timber.w(it) }.collect { h ->
                nilai += h.persen
                _state.update { it.copy(pengukuranSpO2 = it.pengukuranSpO2?.copy(nilaiSementara = h.persen)) }
            }
        }
        for (sisa in 30 downTo 1) {
            _state.update { it.copy(pengukuranSpO2 = it.pengukuranSpO2?.copy(sisaDetik = sisa)) }
            if (motion.tanganBergerak()) bergerak = true
            delay(1_000)
            if (!spo2Primer.tersedia && nilai.isNotEmpty()) break // fallback HC: cukup satu nilai
        }
        job.cancel()
        _state.update { it.copy(pengukuranSpO2 = null) }
        val hasil = HrZones.median(nilai) ?: return null
        val now = System.currentTimeMillis()
        val alt = _state.value.ketinggianM ?: _state.value.altGps ?: 0.0
        val sampel = SampelSpO2(now, hasil, alt, bergerak)
        st.sesiId?.let { sampelDao.simpan(SampelEntity(sesiId = it, jenis = SampelEntity.SPO2, waktu = now, nilai = hasil.toDouble(), ekstra = alt, flag = bergerak)) }
        altSpO2Terakhir = alt
        _state.update { it.copy(spo2Sebelumnya = it.spo2, spo2 = sampel, spo2Sumber = sumber.nama) }
        evaluasiAms(spo2Baru = sampel)
        return sampel
    }

    private suspend fun evaluasiAms(spo2Baru: SampelSpO2?) {
        val st = _state.value
        val hasil = AmsRules.evaluasi(
            AmsRules.Masukan(
                spo2Sekarang = spo2Baru?.nilai,
                spo2Sebelumnya = if (spo2Baru != null) st.spo2Sebelumnya?.nilai else null,
                ambangSpo2 = pengaturanData.ambangSpo2,
                hrIstirahatSekarang = st.hrIstirahat,
                hrIstirahatBasecamp = st.hrIstirahatBasecamp,
            )
        )
        if (!hasil.peringatan) return
        val now = System.currentTimeMillis()
        val sebelumnya = st.peringatanAms
        // jangan ulangi peringatan HR-istirahat lebih cepat dari 15 menit
        if (spo2Baru == null && sebelumnya != null && now - sebelumnya.waktu < 15 * 60_000L) return
        val posSebelum = st.waypoint.lastOrNull { it.jenis == JenisWaypoint.POS && it.waktu <= (st.spo2Sebelumnya?.waktu ?: 0) }?.nama
        val p = PeringatanAms(hasil.alasan, spo2Baru?.nilai, st.spo2Sebelumnya?.nilai, posSebelum,
            st.hrIstirahat, st.hrIstirahatBasecamp, pengaturanData.ambangSpo2, now)
        _state.update { it.copy(peringatanAms = p) }
        _kejadian.value = Kejadian.PeringatanAmsMuncul(p)
        getar.peringatan()
    }

    private suspend fun cekStresOtomatis(now: Long) {
        val st = _state.value
        if (!st.sedangAktif || !st.stresTersedia) return
        val diamSejak = motion.diamSejak.value ?: return
        if (now - diamSejak < 3 * 60_000L) return
        if (now - waktuStresTerakhir < 10 * 60_000L) return
        waktuStresTerakhir = now
        val sumber = if (stresPrimer.tersedia) stresPrimer else stresFallback
        val hasil = withTimeoutOrNull(90_000) { sumber.ukurRmssd().catch { Timber.w(it) }.first() } ?: return
        if (!motion.diam.value) return // bergerak selama pengukuran: buang
        if (baselineRmssd == null && st.jumlahPosTercatat == 0) {
            baselineRmssd = hasil.rmssdMs
            st.sesiId?.let { id -> sesiDao.byId(id)?.let { sesiDao.perbarui(it.copy(baselineRmssd = hasil.rmssdMs)) } }
        }
        val nilai = StressCalculator.keSkala(hasil.rmssdMs, baselineRmssd)
        val sampel = SampelStres(hasil.waktuMs, nilai, hasil.rmssdMs, st.ketinggianM)
        st.sesiId?.let { sampelDao.simpan(SampelEntity(sesiId = it, jenis = SampelEntity.STRES, waktu = sampel.waktu, nilai = nilai.toDouble(), ekstra = hasil.rmssdMs)) }
        _state.update { it.copy(stres = sampel, riwayatStres = it.riwayatStres + sampel, stresSumber = sumber.nama) }
    }

    private suspend fun cekPengingatMinum(now: Long) {
        val menit = pengaturanData.pengingatMinumMenit
        if (menit <= 0 || !_state.value.sedangAktif) return
        if (now - minumTerakhir >= menit * 60_000L) {
            minumTerakhir = now
            _state.update { it.copy(pengingatMinum = true) }
            _kejadian.value = Kejadian.PengingatMinum()
            getar.ganda()
        }
    }

    private suspend fun simpanWaypoint(nama: String, jenis: JenisWaypoint, urutan: Int, lat: Double, lon: Double, alt: Double, hr: Int? = null, spo2: Int? = null) {
        val id = _state.value.sesiId ?: return
        val now = System.currentTimeMillis()
        waypointDao.simpan(WaypointEntity(sesiId = id, nama = nama, jenis = jenis.name, urutan = urutan, waktu = now, lat = lat, lon = lon, alt = alt, hr = hr, spo2 = spo2))
        val w = Waypoint(nama, now, lat, lon, alt, jenis, urutan, hr, spo2)
        _state.update { perbaruiDaftarPos(it.copy(waypoint = it.waypoint + w)) }
    }

    private fun perbaruiDaftarPos(st: HikeState): HikeState {
        val jalur = st.jalur ?: return st.copy(daftarPos = emptyList(), posBerikutnya = null)
        val posWp = st.waypoint.filter { it.jenis == JenisWaypoint.POS }.sortedBy { it.waktu }
        val daftar = jalur.pos.mapIndexed { i, p ->
            StatusPos(p, i, posWp.firstOrNull { it.nama == p.nama || it.urutan == i + 1 })
        }
        return st.copy(daftarPos = daftar, posBerikutnya = daftar.firstOrNull { !it.tercatat })
    }

    // ---------------------------------------------------------------- selesai & kirim

    suspend fun selesai() = mutex.withLock {
        val st = _state.value
        val id = st.sesiId ?: return
        val now = System.currentTimeMillis()
        health.selesai()
        sesiDao.byId(id)?.let { sesiDao.perbarui(it.copy(aktif = false, selesai = now)) }
        _state.update { it.copy(fase = Fase.SELESAI, sekarang = now) }
        tulisSnapshotTile()
        Timber.i("Sesi selesai: $id")
    }

    suspend fun kirimKeHp(): StatusKirim {
        val id = _state.value.sesiId ?: return StatusKirim.BELUM
        _state.update { it.copy(statusKirim = StatusKirim.MENGIRIM) }
        val sesi = sesiRepo.muatLengkap(id)
        val ok = sesi != null && phone.kirimSesi(sesi)
        val status = if (ok) {
            sesiRepo.tandaiTerkirim(id); StatusKirim.TERKIRIM
        } else StatusKirim.ANTRE
        _state.update { it.copy(statusKirim = status) }
        return status
    }

    /** Kembali ke IDLE setelah layar selesai ditutup. */
    fun reset() {
        hentikanSensor()
        _state.value = HikeState()
    }

    private suspend fun tulisSnapshotTile() {
        val st = _state.value
        runCatching {
            pengaturan.simpanSnapshotTile(
                SnapshotTile(
                    aktif = st.sedangAktif, namaGunung = st.gunung?.nama, mulai = st.mulai,
                    ketinggianM = st.ketinggianM, sisaNaikM = st.sisaNaikM, naikTotalM = st.naikTotalM,
                    hr = st.hr, hrRata = st.hrRata, spo2 = st.spo2?.nilai, spo2Waktu = st.spo2?.waktu,
                    posTerakhir = st.waypoint.lastOrNull { it.jenis == JenisWaypoint.POS }?.nama,
                    lat = st.lat ?: st.gunung?.lat, lon = st.lon ?: st.gunung?.lon,
                    puncakElev = st.gunung?.elevasi, basecampElev = st.jalur?.basecampElevasi,
                    diperbarui = System.currentTimeMillis(),
                )
            )
        }
    }
}
