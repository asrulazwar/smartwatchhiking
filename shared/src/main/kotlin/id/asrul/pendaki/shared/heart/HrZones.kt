package id.asrul.pendaki.shared.heart

object HrZones {
    fun hrMaks(usia: Int): Int = 220 - usia.coerceIn(10, 100)

    /** Zona 1..5 berdasarkan persentase HR maksimum (50/60/70/80/90%). Di bawah 50% = zona 0. */
    fun zona(bpm: Int, usia: Int): Int {
        val maks = hrMaks(usia).toDouble()
        val p = bpm / maks
        return when {
            p < 0.5 -> 0
            p < 0.6 -> 1
            p < 0.7 -> 2
            p < 0.8 -> 3
            p < 0.9 -> 4
            else -> 5
        }
    }

    fun median(nilai: List<Int>): Int? {
        if (nilai.isEmpty()) return null
        val s = nilai.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2
    }
}

/**
 * Detak istirahat: median detak dalam 3 menit terakhir saat pengguna diam.
 * Sampel saat bergerak dibuang; sampel diam dijaga dalam jendela waktu.
 */
class RestingHrTracker(private val jendelaMs: Long = 3 * 60_000L, private val minSampel: Int = 10) {
    private val sampel = ArrayDeque<Pair<Long, Int>>()
    var terakhir: Int? = null
        private set

    fun tambah(waktuMs: Long, bpm: Int, diam: Boolean) {
        if (!diam) {
            sampel.clear()
            return
        }
        sampel.addLast(waktuMs to bpm)
        while (sampel.isNotEmpty() && waktuMs - sampel.first().first > jendelaMs) sampel.removeFirst()
        if (sampel.size >= minSampel && (sampel.last().first - sampel.first().first) >= jendelaMs - 5_000) {
            terakhir = HrZones.median(sampel.map { it.second })
        }
    }
}
