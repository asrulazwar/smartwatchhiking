package id.asrul.pendaki.shared.heart

/**
 * Logika batas detak jantung (3.5):
 *  - peringatan jika detak di atas batas terus-menerus lebih dari [durasiAtasMs] (30 detik),
 *  - peringatan ulang paling cepat tiap [cooldownMs] (2 menit),
 *  - "Abaikan 10 mnt" menunda peringatan selama [tundaMs],
 *  - "Istirahat" memulai timer istirahat 10 menit tetapi pemantauan tetap berjalan.
 */
class HrThresholdMonitor(
    batasAwal: Int = DEFAULT_BATAS,
    private val durasiAtasMs: Long = 30_000L,
    private val cooldownMs: Long = 2 * 60_000L,
    private val tundaMs: Long = 10 * 60_000L,
    private val istirahatMs: Long = 10 * 60_000L,
) {
    data class Peringatan(val bpm: Int, val batas: Int, val durasiAtasMs: Long)

    var batas: Int = batasAwal.coerceIn(MIN_BATAS, MAX_BATAS)
        set(value) { field = value.coerceIn(MIN_BATAS, MAX_BATAS) }

    private var atasSejak: Long? = null
    private var peringatanTerakhir: Long = Long.MIN_VALUE / 2
    private var tundaSampai: Long = 0
    private var istirahatSampai: Long = 0

    /** Berapa lama detak sudah di atas batas (ms) pada waktu tertentu. */
    fun durasiAtasMs(waktuMs: Long): Long = atasSejak?.let { waktuMs - it } ?: 0

    fun sedangIstirahat(waktuMs: Long) = waktuMs < istirahatSampai
    fun sisaIstirahatMs(waktuMs: Long) = (istirahatSampai - waktuMs).coerceAtLeast(0)
    fun sedangDitunda(waktuMs: Long) = waktuMs < tundaSampai

    /**
     * Masukkan sampel detak. Mengembalikan [Peringatan] jika peringatan harus ditampilkan sekarang,
     * selain itu null.
     */
    fun sampel(waktuMs: Long, bpm: Int): Peringatan? {
        if (bpm > batas) {
            if (atasSejak == null) atasSejak = waktuMs
        } else {
            atasSejak = null
            return null
        }
        val durasi = waktuMs - atasSejak!!
        if (durasi <= durasiAtasMs) return null
        if (waktuMs < tundaSampai) return null
        if (waktuMs - peringatanTerakhir < cooldownMs) return null
        peringatanTerakhir = waktuMs
        return Peringatan(bpm, batas, durasi)
    }

    /** Tombol "Abaikan 10 mnt". */
    fun abaikan(waktuMs: Long) {
        tundaSampai = waktuMs + tundaMs
    }

    /** Tombol "Istirahat": timer 10 menit, pemantauan tetap jalan. */
    fun istirahat(waktuMs: Long) {
        istirahatSampai = waktuMs + istirahatMs
    }

    companion object {
        const val MIN_BATAS = 100
        const val MAX_BATAS = 200
        const val DEFAULT_BATAS = 155
        const val PRESET_SANTAI = 140
        const val PRESET_NORMAL = 155
        const val PRESET_KUAT = 170
    }
}

/** Ring buffer sampel detak untuk grafik 2 menit terakhir. */
class HrHistory(private val jendelaMs: Long = 2 * 60_000L) {
    private val data = ArrayDeque<Pair<Long, Int>>()
    fun tambah(waktuMs: Long, bpm: Int) {
        data.addLast(waktuMs to bpm)
        while (data.isNotEmpty() && waktuMs - data.first().first > jendelaMs) data.removeFirst()
    }
    fun snapshot(): List<Pair<Long, Int>> = data.toList()
}
