package id.asrul.pendaki.shared.summit

import id.asrul.pendaki.shared.geo.Geo

/**
 * Deteksi puncak otomatis: posisi dalam [radiusM] dari koordinat puncak DAN ketinggian barometer
 * dalam ±[toleransiAltM] dari elevasi puncak. Setelah pengguna menjawab "Belum sampai",
 * deteksi ditunda sampai pengguna menjauh > [jarakResetM] atau [tundaMs] berlalu.
 */
class SummitDetector(
    private val puncakLat: Double,
    private val puncakLon: Double,
    private val puncakElevM: Double,
    private val radiusM: Double = 50.0,
    private val toleransiAltM: Double = 40.0,
    private val tundaMs: Long = 5 * 60_000L,
    private val jarakResetM: Double = 150.0,
) {
    data class Hasil(val terdeteksi: Boolean, val jarakM: Double, val selisihAltM: Double)

    private var tundaSampai: Long = 0
    var dikonfirmasi: Boolean = false
        private set

    fun cek(waktuMs: Long, lat: Double, lon: Double, altBaroM: Double?): Hasil {
        val jarak = Geo.jarakM(lat, lon, puncakLat, puncakLon)
        val selisih = (altBaroM ?: Double.NaN) - puncakElevM
        if (dikonfirmasi) return Hasil(false, jarak, selisih)
        if (jarak > jarakResetM) tundaSampai = 0
        if (waktuMs < tundaSampai) return Hasil(false, jarak, selisih)
        val altOk = altBaroM != null && kotlin.math.abs(selisih) <= toleransiAltM
        return Hasil(jarak <= radiusM && altOk, jarak, selisih)
    }

    /** Pengguna menjawab "Belum sampai". */
    fun belumSampai(waktuMs: Long) {
        tundaSampai = waktuMs + tundaMs
    }

    /** Pengguna menjawab "Mulai turun" (puncak dikonfirmasi). */
    fun konfirmasi() {
        dikonfirmasi = true
    }
}
