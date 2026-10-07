package id.asrul.pendaki.shared.sun

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** Perhitungan matahari terbenam/terbit tanpa internet (algoritme NOAA, akurasi ±2 menit). */
object SunCalc {

    /** Waktu terbenam pada [tanggal] (tanggal lokal) untuk koordinat; null jika tidak terbenam (kutub). */
    fun terbenam(lat: Double, lon: Double, tanggal: LocalDate, zona: ZoneId = ZoneId.systemDefault()): ZonedDateTime? =
        hitung(lat, lon, tanggal, zona, terbit = false)

    fun terbit(lat: Double, lon: Double, tanggal: LocalDate, zona: ZoneId = ZoneId.systemDefault()): ZonedDateTime? =
        hitung(lat, lon, tanggal, zona, terbit = true)

    /** Sisa waktu ke matahari terbenam berikutnya dari [sekarang]. */
    fun sisaKeTerbenamMs(lat: Double, lon: Double, sekarang: Instant, zona: ZoneId = ZoneId.systemDefault()): Long? {
        val hariIni = sekarang.atZone(zona).toLocalDate()
        val t = terbenam(lat, lon, hariIni, zona)?.toInstant()
        if (t != null && t.isAfter(sekarang)) return t.toEpochMilli() - sekarang.toEpochMilli()
        val besok = terbenam(lat, lon, hariIni.plusDays(1), zona)?.toInstant() ?: return null
        return besok.toEpochMilli() - sekarang.toEpochMilli()
    }

    private fun hitung(lat: Double, lon: Double, tanggal: LocalDate, zona: ZoneId, terbit: Boolean): ZonedDateTime? {
        val jd = julianDay(tanggal)
        val t = (jd - 2451545.0) / 36525.0
        val m = rad((357.52911 + t * (35999.05029 - 0.0001537 * t)) % 360.0)
        val l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)) % 360.0
        val c = sin(m) * (1.914602 - t * (0.004817 + 0.000014 * t)) + sin(2 * m) * (0.019993 - 0.000101 * t) + sin(3 * m) * 0.000289
        val trueLong = l0 + c
        val omega = rad(125.04 - 1934.136 * t)
        val lambda = rad(trueLong - 0.00569 - 0.00478 * sin(omega))
        val eps0 = 23.0 + (26.0 + ((21.448 - t * (46.815 + t * (0.00059 - t * 0.001813)))) / 60.0) / 60.0
        val eps = rad(eps0 + 0.00256 * cos(omega))
        val decl = asin(sin(eps) * sin(lambda))
        val y = tan(eps / 2) * tan(eps / 2)
        val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
        val l0r = rad(l0)
        val eqTime = 4.0 * deg(
            y * sin(2 * l0r) - 2 * e * sin(m) + 4 * e * y * sin(m) * cos(2 * l0r) -
                0.5 * y * y * sin(4 * l0r) - 1.25 * e * e * sin(2 * m)
        )
        val latR = rad(lat)
        val zenith = rad(90.833)
        val cosHa = (cos(zenith) / (cos(latR) * cos(decl))) - tan(latR) * tan(decl)
        if (cosHa > 1 || cosHa < -1) return null
        val ha = deg(acos(cosHa))
        val menitUtc = 720.0 - 4.0 * (lon + (if (terbit) ha else -ha)) - eqTime
        val detik = (menitUtc * 60.0).toLong()
        val utc = tanggal.atStartOfDay(ZoneOffset.UTC).plusSeconds(detik)
        return utc.withZoneSameInstant(zona)
    }

    private fun julianDay(d: LocalDate): Double {
        var y = d.year
        var m = d.monthValue
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d.dayOfMonth + b - 1524.5
    }

    private fun rad(x: Double) = Math.toRadians(x)
    private fun deg(x: Double) = Math.toDegrees(x)
}
