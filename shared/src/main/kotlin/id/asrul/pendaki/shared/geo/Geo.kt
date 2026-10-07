package id.asrul.pendaki.shared.geo

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    const val RADIUS_BUMI_M = 6_371_008.8

    /** Jarak great-circle (haversine) dalam meter. */
    fun jarakM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * RADIUS_BUMI_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** Bearing awal dari titik 1 ke titik 2, 0..360 derajat (0 = utara, searah jarum jam). */
    fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Normalisasi sudut ke rentang [-180, 180). */
    fun normalisasiSudut(deg: Double): Double {
        var d = deg % 360.0
        if (d >= 180.0) d -= 360.0
        if (d < -180.0) d += 360.0
        return d
    }

    /** Konversi arah relatif (0 = lurus di depan, searah jarum jam) ke "arah jam" 1..12. */
    fun arahJam(relatifDeg: Double): Int {
        val d = ((relatifDeg % 360.0) + 360.0) % 360.0
        val jam = Math.round(d / 30.0).toInt() % 12
        return if (jam == 0) 12 else jam
    }

    fun degToRad(deg: Double) = deg * PI / 180.0
}
