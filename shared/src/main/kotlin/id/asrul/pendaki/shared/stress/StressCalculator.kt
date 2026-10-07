package id.asrul.pendaki.shared.stress

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Stres dari HRV (RMSSD). Skala 0–100 dengan kalibrasi personal sederhana:
 * baseline = median RMSSD saat diam di basecamp. stres = 30 − 40·log2(rmssd / baseline), dibatasi 0..100.
 *  - rmssd == baseline  -> 30 (Rendah)
 *  - rmssd == ½ baseline -> 70 (Tinggi)
 *  - rmssd == 2× baseline -> 0
 * Tanpa baseline personal dipakai [BASELINE_DEFAULT_MS].
 */
object StressCalculator {
    const val BASELINE_DEFAULT_MS = 42.0
    const val BATAS_RENDAH = 35
    const val BATAS_SEDANG = 65

    enum class Label { RENDAH, SEDANG, TINGGI }

    /** RMSSD dari daftar interval RR (ms). Butuh minimal 2 interval; null jika kurang. */
    fun rmssd(rrMs: List<Double>): Double? {
        if (rrMs.size < 2) return null
        var sum = 0.0
        for (i in 1 until rrMs.size) {
            val d = rrMs[i] - rrMs[i - 1]
            sum += d * d
        }
        return sqrt(sum / (rrMs.size - 1))
    }

    fun keSkala(rmssdMs: Double, baselineMs: Double? = null): Int {
        val base = (baselineMs ?: BASELINE_DEFAULT_MS).coerceAtLeast(1.0)
        val r = (rmssdMs.coerceAtLeast(0.1)) / base
        val nilai = 30.0 - 40.0 * (ln(r) / ln(2.0))
        return nilai.toInt().coerceIn(0, 100)
    }

    fun label(nilai: Int): Label = when {
        nilai < BATAS_RENDAH -> Label.RENDAH
        nilai <= BATAS_SEDANG -> Label.SEDANG
        else -> Label.TINGGI
    }

    fun median(nilai: List<Double>): Double? {
        if (nilai.isEmpty()) return null
        val s = nilai.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2
    }
}
