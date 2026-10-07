package id.asrul.pendaki.shared.gpx

import id.asrul.pendaki.shared.model.JenisWaypoint
import id.asrul.pendaki.shared.model.SesiPendakian
import id.asrul.pendaki.shared.model.TitikJejak
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Opsi isi GPX sesuai centang di halaman ekspor HP. */
data class GpxOptions(
    val sertakanDetak: Boolean = true,
    val sertakanWaypoint: Boolean = true,
    /** true: <ele> dari barometer (fallback GPS); false: selalu GPS (fallback barometer). */
    val elevasiBarometer: Boolean = true,
    /** SpO2 ditulis sebagai <desc> di waypoint terdekat. */
    val sertakanSpO2: Boolean = true,
)

/**
 * Penulis GPX 1.1 dengan ekstensi Garmin TrackPointExtension v1 (gpxtpx:hr).
 * Urutan elemen mengikuti skema: metadata, wpt*, trk*. Dalam wpt/trkpt: ele, time, name, desc, extensions.
 */
object GpxWriter {
    private const val NS_GPX = "http://www.topografix.com/GPX/1/1"
    private const val NS_TPX = "http://www.garmin.com/xmlschemas/TrackPointExtension/v1"
    private const val NS_XSI = "http://www.w3.org/2001/XMLSchema-instance"
    private const val SCHEMA_LOC = "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd " +
        "http://www.garmin.com/xmlschemas/TrackPointExtension/v1 https://www8.garmin.com/xmlschemas/TrackPointExtensionv1.xsd"

    fun tulis(s: SesiPendakian, opsi: GpxOptions = GpxOptions(), creator: String = "Pendaki Wear OS"): String {
        val sb = StringBuilder(s.titik.size * 160 + 2048)
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"").append(esc(creator)).append("\"")
        sb.append(" xmlns=\"").append(NS_GPX).append("\"")
        sb.append(" xmlns:gpxtpx=\"").append(NS_TPX).append("\"")
        sb.append(" xmlns:xsi=\"").append(NS_XSI).append("\"")
        sb.append(" xsi:schemaLocation=\"").append(SCHEMA_LOC).append("\">\n")

        // metadata
        sb.append("  <metadata>\n")
        sb.append("    <name>").append(esc(s.judul)).append("</name>\n")
        val desk = buildString {
            append("Pendakian ${s.judul}. Puncak ${s.elevasiPuncak} mdpl.")
            s.basecampElevasi?.let { append(" Basecamp $it mdpl.") }
        }
        sb.append("    <desc>").append(esc(desk)).append("</desc>\n")
        sb.append("    <time>").append(iso(s.mulai)).append("</time>\n")
        sb.append("  </metadata>\n")

        // waypoints (pos, basecamp, puncak)
        if (opsi.sertakanWaypoint) {
            for (w in s.waypoint.sortedBy { it.waktu }) {
                sb.append("  <wpt lat=\"").append(koord(w.lat)).append("\" lon=\"").append(koord(w.lon)).append("\">\n")
                sb.append("    <ele>").append(ele(w.alt)).append("</ele>\n")
                sb.append("    <time>").append(iso(w.waktu)).append("</time>\n")
                sb.append("    <name>").append(esc(w.nama)).append("</name>\n")
                val desc = mutableListOf<String>()
                w.hr?.let { desc += "Detak $it bpm" }
                if (opsi.sertakanSpO2) {
                    val spo2 = w.spo2 ?: spo2Terdekat(s, w.waktu)
                    spo2?.let { desc += "SpO2 $it%" }
                }
                if (desc.isNotEmpty()) sb.append("    <desc>").append(esc(desc.joinToString(" · "))).append("</desc>\n")
                sb.append("    <sym>").append(simbol(w.jenis)).append("</sym>\n")
                sb.append("    <type>").append(w.jenis.name.lowercase()).append("</type>\n")
                sb.append("  </wpt>\n")
            }
        }

        // track
        sb.append("  <trk>\n")
        sb.append("    <name>").append(esc(s.judul)).append("</name>\n")
        sb.append("    <type>hiking</type>\n")
        sb.append("    <trkseg>\n")
        for (t in s.titik) {
            sb.append("      <trkpt lat=\"").append(koord(t.lat)).append("\" lon=\"").append(koord(t.lon)).append("\">\n")
            elevasi(t, opsi)?.let { sb.append("        <ele>").append(ele(it)).append("</ele>\n") }
            sb.append("        <time>").append(iso(t.waktu)).append("</time>\n")
            if (opsi.sertakanDetak && t.hr != null) {
                sb.append("        <extensions><gpxtpx:TrackPointExtension><gpxtpx:hr>")
                    .append(t.hr).append("</gpxtpx:hr></gpxtpx:TrackPointExtension></extensions>\n")
            }
            sb.append("      </trkpt>\n")
        }
        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>\n")
        return sb.toString()
    }

    fun elevasi(t: TitikJejak, opsi: GpxOptions): Double? =
        if (opsi.elevasiBarometer) t.altBaro ?: t.altGps else t.altGps ?: t.altBaro

    private fun spo2Terdekat(s: SesiPendakian, waktu: Long, toleransiMs: Long = 20 * 60_000L): Int? =
        s.spo2.filter { kotlin.math.abs(it.waktu - waktu) <= toleransiMs }.minByOrNull { kotlin.math.abs(it.waktu - waktu) }?.nilai

    private fun simbol(j: JenisWaypoint) = when (j) {
        JenisWaypoint.BASECAMP -> "Trail Head"
        JenisWaypoint.POS -> "Flag, Blue"
        JenisWaypoint.PUNCAK -> "Summit"
        JenisWaypoint.LAIN -> "Waypoint"
    }

    private fun iso(epochMs: Long): String = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(epochMs))
    private fun koord(d: Double) = String.format(Locale.ROOT, "%.6f", d)
    private fun ele(d: Double) = String.format(Locale.ROOT, "%.1f", d)

    fun esc(s: String): String = buildString(s.length + 8) {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(c)
        }
    }
}
