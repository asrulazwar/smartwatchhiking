package id.asrul.pendaki.shared.nav

import id.asrul.pendaki.shared.geo.Geo
import id.asrul.pendaki.shared.model.JenisWaypoint
import id.asrul.pendaki.shared.model.TitikJejak
import id.asrul.pendaki.shared.model.Waypoint

/** Hasil perhitungan navigasi balik (3.4). */
data class ArahBalik(
    val tujuan: Waypoint,
    val jarakM: Double,
    val bearingDeg: Double,
    /** Sudut panah relatif terhadap heading perangkat; 0 = lurus ke depan. */
    val relatifDeg: Double,
    val arahJam: Int,
    val selisihAltM: Double,
    val jarakKeBasecampM: Double,
)

object Navigation {
    /**
     * Tujuan berikutnya ke arah turun: waypoint terakhir (urutan waktu) yang belum "dilewati"
     * (jarak > [radiusLewatM]) dan posisinya lebih rendah atau sama dari posisi sekarang.
     * Jika semua sudah dilewati, kembali ke basecamp (waypoint pertama).
     */
    fun tujuanTurun(
        waypoint: List<Waypoint>,
        lat: Double,
        lon: Double,
        altM: Double?,
        radiusLewatM: Double = 60.0,
    ): Waypoint? {
        if (waypoint.isEmpty()) return null
        val urut = waypoint.filter { it.jenis != JenisWaypoint.PUNCAK }.sortedBy { it.waktu }
        if (urut.isEmpty()) return null
        for (w in urut.asReversed()) {
            val jarak = Geo.jarakM(lat, lon, w.lat, w.lon)
            if (jarak <= radiusLewatM) continue
            if (altM != null && w.alt > altM + 30.0) continue
            return w
        }
        return urut.first()
    }

    /**
     * Jarak ke basecamp mengikuti jejak rekaman: cari titik jejak terdekat dari posisi sekarang,
     * lalu jumlahkan jarak dari titik itu kembali ke titik pertama.
     */
    fun jarakKeBasecampViaJejak(jejak: List<TitikJejak>, lat: Double, lon: Double): Double {
        if (jejak.isEmpty()) return 0.0
        var idx = 0
        var best = Double.MAX_VALUE
        for (i in jejak.indices) {
            val d = Geo.jarakM(lat, lon, jejak[i].lat, jejak[i].lon)
            if (d < best) { best = d; idx = i }
        }
        var total = best
        for (i in idx downTo 1) {
            total += Geo.jarakM(jejak[i].lat, jejak[i].lon, jejak[i - 1].lat, jejak[i - 1].lon)
        }
        return total
    }

    fun hitung(
        tujuan: Waypoint,
        lat: Double,
        lon: Double,
        altM: Double?,
        headingDeg: Double,
        jejak: List<TitikJejak>,
    ): ArahBalik {
        val jarak = Geo.jarakM(lat, lon, tujuan.lat, tujuan.lon)
        val bearing = Geo.bearingDeg(lat, lon, tujuan.lat, tujuan.lon)
        val relatif = Geo.normalisasiSudut(bearing - headingDeg)
        return ArahBalik(
            tujuan = tujuan,
            jarakM = jarak,
            bearingDeg = bearing,
            relatifDeg = relatif,
            arahJam = Geo.arahJam(relatif),
            selisihAltM = tujuan.alt - (altM ?: tujuan.alt),
            jarakKeBasecampM = jarakKeBasecampViaJejak(jejak, lat, lon),
        )
    }
}
