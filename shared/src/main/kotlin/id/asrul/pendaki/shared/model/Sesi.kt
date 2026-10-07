package id.asrul.pendaki.shared.model

import kotlinx.serialization.Serializable

/** Titik rekaman GPS. Waktu dalam epoch millis. Ketinggian dalam meter. */
@Serializable
data class TitikJejak(
    val waktu: Long,
    val lat: Double,
    val lon: Double,
    val altGps: Double? = null,
    val altBaro: Double? = null,
    val hr: Int? = null,
)

@Serializable
enum class JenisWaypoint { BASECAMP, POS, PUNCAK, LAIN }

/** Waypoint yang dicatat pengguna (pos, puncak, basecamp). */
@Serializable
data class Waypoint(
    val nama: String,
    val waktu: Long,
    val lat: Double,
    val lon: Double,
    val alt: Double,
    val jenis: JenisWaypoint = JenisWaypoint.POS,
    val urutan: Int = 0,
    val hr: Int? = null,
    val spo2: Int? = null,
)

@Serializable
data class SampelSpO2(
    val waktu: Long,
    val nilai: Int,
    val alt: Double,
    val meragukan: Boolean = false,
)

@Serializable
data class SampelStres(
    val waktu: Long,
    val nilai: Int,
    val rmssdMs: Double,
    val alt: Double? = null,
)

@Serializable
data class SampelDetak(
    val waktu: Long,
    val bpm: Int,
)

/** Sesi pendakian lengkap yang dikirim dari jam ke HP. */
@Serializable
data class SesiPendakian(
    val id: String,
    val gunungId: String,
    val namaGunung: String,
    val namaJalur: String? = null,
    val elevasiPuncak: Int,
    val puncakLat: Double? = null,
    val puncakLon: Double? = null,
    val basecampElevasi: Int? = null,
    val mulai: Long,
    val selesai: Long? = null,
    val waktuPuncak: Long? = null,
    val titik: List<TitikJejak> = emptyList(),
    val waypoint: List<Waypoint> = emptyList(),
    val spo2: List<SampelSpO2> = emptyList(),
    val stres: List<SampelStres> = emptyList(),
    val catatan: String? = null,
) {
    /** "Gn. Merbabu via Selo" */
    val judul: String
        get() = if (namaJalur.isNullOrBlank()) "Gn. $namaGunung" else "Gn. $namaGunung via $namaJalur"
}

/** Ringkasan sesi yang dikirim lebih dulu sebagai metadata ringan lewat DataClient. */
@Serializable
data class MetaSesi(
    val id: String,
    val judul: String,
    val mulai: Long,
    val selesai: Long?,
    val durasiMs: Long,
    val jarakM: Double,
    val naikTotalM: Double,
    val hrRata: Int?,
    val jumlahPos: Int,
    val jumlahTitik: Int,
    val ukuranByte: Long,
)
