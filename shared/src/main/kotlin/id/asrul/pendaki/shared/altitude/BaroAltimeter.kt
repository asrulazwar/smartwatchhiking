package id.asrul.pendaki.shared.altitude

import kotlin.math.pow

/**
 * Altimeter barometer: tekanan (hPa) -> ketinggian (m) dengan rumus atmosfer standar,
 * low-pass filter, dan offset kalibrasi yang bisa disimpan/dipulihkan.
 *
 * Alur kalibrasi (lihat prompt 3.2):
 *  1. Saat mulai: acuan = elevasi basecamp dari jalur.json (lebih akurat), atau rata-rata GPS 30 detik.
 *  2. Saat mencatat pos yang punya elevasi di data jalur: kalibrasi ulang ke elevasi itu.
 *  3. Saat puncak dikonfirmasi: kalibrasi ulang ke elevasi puncak.
 */
class BaroAltimeter(
    /** Faktor low-pass 0..1; makin kecil makin halus tapi makin lambat. */
    private val alpha: Double = 0.2,
    offsetAwal: Double = 0.0,
) {
    var offsetM: Double = offsetAwal
        private set

    private var terfilterRawM: Double? = null

    /** Ketinggian mentah terfilter (sebelum offset), null jika belum ada sampel. */
    val rawTerfilterM: Double? get() = terfilterRawM

    /** Ketinggian terkalibrasi, null jika belum ada sampel. */
    val ketinggianM: Double? get() = terfilterRawM?.let { it + offsetM }

    /** Masukkan sampel tekanan (hPa). Mengembalikan ketinggian terkalibrasi. */
    fun sampel(tekananHpa: Double): Double {
        val raw = tekananKeKetinggian(tekananHpa)
        val prev = terfilterRawM
        terfilterRawM = if (prev == null) raw else prev + alpha * (raw - prev)
        return terfilterRawM!! + offsetM
    }

    /** Kalibrasi: ketinggian sekarang dianggap = [acuanM]. Mengembalikan offset baru. */
    fun kalibrasi(acuanM: Double): Double {
        val raw = terfilterRawM ?: return offsetM
        offsetM = acuanM - raw
        return offsetM
    }

    fun pulihkanOffset(offset: Double) {
        offsetM = offset
    }

    companion object {
        const val TEKANAN_LAUT_HPA = 1013.25

        /** Rumus hipsometrik ISA (sama dengan SensorManager.getAltitude). */
        fun tekananKeKetinggian(tekananHpa: Double, tekananLautHpa: Double = TEKANAN_LAUT_HPA): Double =
            44_330.0 * (1.0 - (tekananHpa / tekananLautHpa).pow(1.0 / 5.255))
    }
}

/** Rata-rata ketinggian GPS dalam jendela waktu (default 30 detik) untuk kalibrasi awal. */
class GpsAltitudeAverager(private val jendelaMs: Long = 30_000) {
    private val sampel = ArrayDeque<Pair<Long, Double>>()

    fun tambah(waktuMs: Long, altM: Double) {
        sampel.addLast(waktuMs to altM)
        while (sampel.isNotEmpty() && waktuMs - sampel.first().first > jendelaMs) sampel.removeFirst()
    }

    val jumlah: Int get() = sampel.size

    /** Durasi yang sudah terkumpul (ms). */
    val durasiMs: Long get() = if (sampel.size < 2) 0 else sampel.last().first - sampel.first().first

    val rataRata: Double? get() = if (sampel.isEmpty()) null else sampel.sumOf { it.second } / sampel.size

    /** Siap dipakai jika jendela penuh (>= jendelaMs) dan minimal 3 sampel. */
    val siap: Boolean get() = sampel.size >= 3 && durasiMs >= jendelaMs - 1_000
}
