package id.asrul.pendaki.shared.ams

/**
 * Aturan peringatan AMS (acute mountain sickness), lihat prompt 3.5:
 *  - SpO2 < ambang (default 85%), ATAU
 *  - SpO2 turun >= 6 poin dari pengukuran sebelumnya, ATAU
 *  - detak istirahat naik > 20 bpm dibanding detak istirahat basecamp.
 */
object AmsRules {
    const val DEFAULT_AMBANG_SPO2 = 85
    const val PENURUNAN_SPO2 = 6
    const val KENAIKAN_HR_ISTIRAHAT = 20

    enum class Alasan { SPO2_RENDAH, SPO2_TURUN, HR_ISTIRAHAT_NAIK }

    data class Masukan(
        val spo2Sekarang: Int? = null,
        val spo2Sebelumnya: Int? = null,
        val ambangSpo2: Int = DEFAULT_AMBANG_SPO2,
        val hrIstirahatSekarang: Int? = null,
        val hrIstirahatBasecamp: Int? = null,
    )

    data class Hasil(val alasan: List<Alasan>) {
        val peringatan: Boolean get() = alasan.isNotEmpty()
    }

    fun evaluasi(m: Masukan): Hasil {
        val alasan = mutableListOf<Alasan>()
        val s = m.spo2Sekarang
        if (s != null) {
            if (s < m.ambangSpo2) alasan += Alasan.SPO2_RENDAH
            val prev = m.spo2Sebelumnya
            if (prev != null && prev - s >= PENURUNAN_SPO2) alasan += Alasan.SPO2_TURUN
        }
        val hrNow = m.hrIstirahatSekarang
        val hrBase = m.hrIstirahatBasecamp
        if (hrNow != null && hrBase != null && hrNow - hrBase > KENAIKAN_HR_ISTIRAHAT) alasan += Alasan.HR_ISTIRAHAT_NAIK
        return Hasil(alasan)
    }
}
