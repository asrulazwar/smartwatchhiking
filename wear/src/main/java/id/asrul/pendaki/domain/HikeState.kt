package id.asrul.pendaki.domain

import id.asrul.pendaki.shared.ams.AmsRules
import id.asrul.pendaki.shared.model.Gunung
import id.asrul.pendaki.shared.model.Jalur
import id.asrul.pendaki.shared.model.PosJalur
import id.asrul.pendaki.shared.model.SampelSpO2
import id.asrul.pendaki.shared.model.SampelStres
import id.asrul.pendaki.shared.model.Waypoint

enum class Fase { IDLE, MEREKAM, PUNCAK_TERDETEKSI, TURUN, SELESAI }

data class PeringatanHr(
    val bpm: Int,
    val batas: Int,
    val durasiAtasMs: Long,
    val riwayat: List<Pair<Long, Int>>,
    val waktu: Long,
)

data class PeringatanAms(
    val alasan: List<AmsRules.Alasan>,
    val spo2: Int?,
    val spo2Sebelumnya: Int?,
    val namaPosSebelumnya: String?,
    val hrIstirahat: Int?,
    val hrIstirahatBasecamp: Int?,
    val ambang: Int,
    val waktu: Long,
)

data class InfoPuncak(
    val waktu: Long,
    val durasiDariBasecampMs: Long,
    val ketinggianM: Double,
    val jarakKeTitikPuncakM: Double,
)

data class StatusPos(val pos: PosJalur, val indeks: Int, val waypoint: Waypoint?) {
    val tercatat: Boolean get() = waypoint != null
}

data class PengukuranSpO2(val sisaDetik: Int, val nilaiSementara: Int?)

data class HikeState(
    val fase: Fase = Fase.IDLE,
    val sesiId: String? = null,
    val gunung: Gunung? = null,
    val jalur: Jalur? = null,
    val mulai: Long = 0,
    val sekarang: Long = System.currentTimeMillis(),

    // posisi & ketinggian
    val lat: Double? = null,
    val lon: Double? = null,
    val altGps: Double? = null,
    val akurasiM: Float? = null,
    val kecepatanMps: Float? = null,
    val headingDeg: Float? = null,
    val ketinggianM: Double? = null,
    val kalibrasiSiap: Boolean = false,
    val barometerAda: Boolean = true,
    val diam: Boolean = false,

    // progres
    val sisaNaikM: Double? = null,
    val progres: Float = 0f,
    val kecepatanNaikMPerJam: Double? = null,
    val perkiraanTiba: Long? = null,
    val naikTotalM: Double = 0.0,
    val jarakM: Double = 0.0,
    val jumlahTitik: Int = 0,
    val langkah: Int = 0,

    // detak
    val hr: Int? = null,
    val hrWaktu: Long? = null,
    val zona: Int = 0,
    val hrRata: Int? = null,
    val hrIstirahat: Int? = null,
    val hrIstirahatBasecamp: Int? = null,
    val riwayatHr: List<Pair<Long, Int>> = emptyList(),
    val istirahatSampai: Long? = null,

    // SpO2 & stres
    val spo2: SampelSpO2? = null,
    val spo2Sebelumnya: SampelSpO2? = null,
    val spo2Sumber: String? = null,
    val pengukuranSpO2: PengukuranSpO2? = null,
    val pengingatSpO2: Boolean = false,
    val stres: SampelStres? = null,
    val riwayatStres: List<SampelStres> = emptyList(),
    val stresTersedia: Boolean = true,
    val stresSumber: String? = null,

    // pos & navigasi
    val waypoint: List<Waypoint> = emptyList(),
    val daftarPos: List<StatusPos> = emptyList(),
    val posBerikutnya: StatusPos? = null,
    val tawaranPos: Waypoint? = null,
    val nomorPosManual: Int = 1,

    // peringatan & kejadian
    val peringatanHr: PeringatanHr? = null,
    val peringatanAms: PeringatanAms? = null,
    val puncak: InfoPuncak? = null,
    val waktuPuncak: Long? = null,
    val pengingatMinum: Boolean = false,

    // pengiriman
    val statusKirim: StatusKirim = StatusKirim.BELUM,
) {
    val judul: String
        get() = gunung?.let { g -> if (jalur != null) "Gn. ${g.nama} · via ${jalur.namaJalur}" else "Gn. ${g.nama}" } ?: ""
    val sedangAktif: Boolean get() = fase == Fase.MEREKAM || fase == Fase.PUNCAK_TERDETEKSI || fase == Fase.TURUN
    val jumlahPosTercatat: Int get() = waypoint.count { it.jenis == id.asrul.pendaki.shared.model.JenisWaypoint.POS }
    val durasiMs: Long get() = if (mulai == 0L) 0 else (sekarang - mulai).coerceAtLeast(0)
}

enum class StatusKirim { BELUM, MENGIRIM, TERKIRIM, ANTRE }
