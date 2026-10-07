package id.asrul.pendaki.shared.stats

import id.asrul.pendaki.shared.geo.Geo
import id.asrul.pendaki.shared.model.SesiPendakian
import id.asrul.pendaki.shared.model.TitikJejak
import id.asrul.pendaki.shared.model.JenisWaypoint
import kotlin.math.roundToInt

data class Ringkasan(
    val durasiMs: Long,
    val jarakM: Double,
    val naikTotalM: Double,
    val turunTotalM: Double,
    val hrRata: Int?,
    val hrMaks: Int?,
    val jumlahPos: Int,
    val jumlahTitik: Int,
)

object SessionStats {
    /** Ambang kenaikan agar derau barometer tidak dihitung sebagai "naik". */
    const val AMBANG_NAIK_M = 3.0

    fun ringkasan(s: SesiPendakian, sekarangMs: Long = s.selesai ?: s.titik.lastOrNull()?.waktu ?: s.mulai): Ringkasan {
        val titik = s.titik
        val hr = titik.mapNotNull { it.hr }
        return Ringkasan(
            durasiMs = ((s.selesai ?: sekarangMs) - s.mulai).coerceAtLeast(0),
            jarakM = jarak(titik),
            naikTotalM = naikTotal(titik),
            turunTotalM = turunTotal(titik),
            hrRata = if (hr.isEmpty()) null else hr.average().roundToInt(),
            hrMaks = hr.maxOrNull(),
            jumlahPos = s.waypoint.count { it.jenis == JenisWaypoint.POS },
            jumlahTitik = titik.size,
        )
    }

    fun jarak(titik: List<TitikJejak>): Double {
        var d = 0.0
        for (i in 1 until titik.size) {
            d += Geo.jarakM(titik[i - 1].lat, titik[i - 1].lon, titik[i].lat, titik[i].lon)
        }
        return d
    }

    fun altTerbaik(t: TitikJejak): Double? = t.altBaro ?: t.altGps

    /** Total kenaikan dengan ambang (histeresis) agar derau tidak terakumulasi. */
    fun naikTotal(titik: List<TitikJejak>): Double = akumulasi(titik, naik = true)
    fun turunTotal(titik: List<TitikJejak>): Double = akumulasi(titik, naik = false)

    private fun akumulasi(titik: List<TitikJejak>, naik: Boolean): Double {
        var total = 0.0
        var acuan: Double? = null
        for (t in titik) {
            val a = altTerbaik(t) ?: continue
            val ref = acuan
            if (ref == null) { acuan = a; continue }
            val delta = a - ref
            if (naik && delta >= AMBANG_NAIK_M) { total += delta; acuan = a }
            else if (!naik && delta <= -AMBANG_NAIK_M) { total += -delta; acuan = a }
            else if (naik && delta < 0) acuan = a   // turun: geser acuan ke bawah
            else if (!naik && delta > 0) acuan = a
        }
        return total
    }

    /**
     * Kecepatan naik (m/jam) sebagai rata-rata bergerak dalam [jendelaMs] (default 15 menit).
     * Null jika data kurang dari 2 menit.
     */
    fun kecepatanNaikMPerJam(titik: List<TitikJejak>, sekarangMs: Long, jendelaMs: Long = 15 * 60_000L): Double? {
        val dalam = titik.filter { it.waktu >= sekarangMs - jendelaMs && altTerbaik(it) != null }
        if (dalam.size < 2) return null
        val awal = dalam.first()
        val akhir = dalam.last()
        val dt = akhir.waktu - awal.waktu
        if (dt < 2 * 60_000L) return null
        val dAlt = altTerbaik(akhir)!! - altTerbaik(awal)!!
        return dAlt / (dt / 3_600_000.0)
    }

    /** Perkiraan waktu tiba (epoch ms) di puncak; null jika tidak sedang naik. */
    fun perkiraanTiba(sekarangMs: Long, sisaNaikM: Double, kecepatanMPerJam: Double?): Long? {
        if (kecepatanMPerJam == null || kecepatanMPerJam < 20.0) return null
        if (sisaNaikM <= 0) return sekarangMs
        return sekarangMs + (sisaNaikM / kecepatanMPerJam * 3_600_000.0).toLong()
    }

    /** Persentase kemajuan 0..1 dari basecamp ke puncak. */
    fun progres(altSekarang: Double, basecampElev: Double, puncakElev: Double): Float {
        val rentang = puncakElev - basecampElev
        if (rentang <= 0) return 0f
        return ((altSekarang - basecampElev) / rentang).toFloat().coerceIn(0f, 1f)
    }
}
