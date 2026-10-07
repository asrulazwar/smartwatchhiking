package id.asrul.pendaki.shared.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Pemformatan angka/waktu gaya Indonesia: ribuan titik, desimal koma, jam 24 jam. */
object Format {
    private val jamFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val tanggalFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val tanggalPendekFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale("id", "ID"))

    /** 2410 -> "2.410"; -12 -> "-12". */
    fun ribuan(n: Int): String {
        val s = abs(n).toString()
        val sb = StringBuilder()
        var count = 0
        for (i in s.length - 1 downTo 0) {
            sb.append(s[i])
            count++
            if (count % 3 == 0 && i != 0) sb.append('.')
        }
        if (n < 0) sb.append('-')
        return sb.reverse().toString()
    }

    fun ribuan(n: Double): String = ribuan(n.roundToInt())

    /** 2410.0 -> "2.410 mdpl" */
    fun mdpl(altM: Double?): String = if (altM == null) "— mdpl" else "${ribuan(altM)} mdpl"

    /** 12400 m -> "12,4 km"; 420 m -> "420 m". */
    fun jarak(meter: Double): String =
        if (meter < 1000) "${meter.roundToInt()} m" else String.format(Locale.ROOT, "%.1f", meter / 1000.0).replace('.', ',') + " km"

    /** 22920000 ms -> "6 j 22 m"; < 1 jam -> "45 m". */
    fun durasi(ms: Long): String {
        val totalMenit = (ms / 60_000L).coerceAtLeast(0)
        val jam = totalMenit / 60
        val menit = totalMenit % 60
        return if (jam == 0L) "$menit m" else "$jam j $menit m"
    }

    /** "11:48" (jam:menit) untuk durasi ringkas gaya mockup. */
    fun durasiJamMenit(ms: Long): String {
        val totalMenit = (ms / 60_000L).coerceAtLeast(0)
        return String.format(Locale.ROOT, "%d:%02d", totalMenit / 60, totalMenit % 60)
    }

    fun jam(epochMs: Long, zona: ZoneId = ZoneId.systemDefault()): String =
        jamFmt.format(Instant.ofEpochMilli(epochMs).atZone(zona))

    fun tanggalIso(epochMs: Long, zona: ZoneId = ZoneId.systemDefault()): String =
        tanggalFmt.format(Instant.ofEpochMilli(epochMs).atZone(zona))

    fun tanggalPendek(epochMs: Long, zona: ZoneId = ZoneId.systemDefault()): String =
        tanggalPendekFmt.format(Instant.ofEpochMilli(epochMs).atZone(zona))

    /** "12 mnt lalu" / "baru saja". */
    fun relatif(epochMs: Long, sekarangMs: Long): String {
        val menit = (sekarangMs - epochMs) / 60_000L
        return when {
            menit < 1 -> "baru saja"
            menit < 60 -> "$menit mnt lalu"
            else -> "${menit / 60} j ${menit % 60} m lalu"
        }
    }

    /** "merbabu-selo-2026-10-04.gpx" */
    fun namaFileGpx(namaGunung: String, namaJalur: String?, mulaiMs: Long, zona: ZoneId = ZoneId.systemDefault()): String {
        val parts = listOfNotNull(slug(namaGunung), namaJalur?.let { slug(it) }?.takeIf { it.isNotEmpty() })
        return parts.joinToString("-") + "-" + tanggalIso(mulaiMs, zona) + ".gpx"
    }

    fun slug(s: String): String = s.lowercase()
        .replace("gn.", "").replace("gunung", "")
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
}
