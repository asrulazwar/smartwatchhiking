package id.asrul.pendaki.shared

import com.google.common.truth.Truth.assertThat
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.shared.gpx.GpxOptions
import id.asrul.pendaki.shared.gpx.GpxWriter
import id.asrul.pendaki.shared.model.JenisWaypoint
import id.asrul.pendaki.shared.model.SampelSpO2
import id.asrul.pendaki.shared.model.SesiPendakian
import id.asrul.pendaki.shared.model.TitikJejak
import id.asrul.pendaki.shared.model.Waypoint
import org.junit.Test
import org.w3c.dom.Document
import java.io.StringReader
import java.time.ZoneId
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import org.xml.sax.InputSource

class GpxWriterTest {
    private val mulai = 1_790_000_000_000L
    private val sesi = SesiPendakian(
        id = "s1", gunungId = "merbabu", namaGunung = "Merbabu", namaJalur = "Selo",
        elevasiPuncak = 3145, basecampElevasi = 1690, mulai = mulai, selesai = mulai + 6 * 3_600_000L,
        titik = (0 until 100).map {
            TitikJejak(mulai + it * 15_000L, -7.495 + it * 0.0004, 110.453 - it * 0.0001, altGps = 1690.0 + it * 12, altBaro = 1692.0 + it * 12, hr = if (it % 2 == 0) 120 + it % 20 else null)
        },
        waypoint = listOf(
            Waypoint("Basecamp", mulai, -7.495, 110.453, 1690.0, JenisWaypoint.BASECAMP),
            Waypoint("Pos 1 & \"Dua\"", mulai + 60 * 60_000L, -7.480, 110.45, 1960.0, JenisWaypoint.POS, urutan = 1, hr = 130),
            Waypoint("Puncak", mulai + 5 * 3_600_000L, -7.455, 110.44, 3145.0, JenisWaypoint.PUNCAK),
        ),
        spo2 = listOf(SampelSpO2(mulai + 61 * 60_000L, 93, 1960.0)),
    )

    private fun parse(xml: String): Document {
        val f = DocumentBuilderFactory.newInstance()
        f.isNamespaceAware = true
        return f.newDocumentBuilder().parse(InputSource(StringReader(xml)))
    }

    private fun validasiSkema(xml: String) {
        val xsd = javaClass.getResource("/gpx.xsd")!!
        val schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsd)
        schema.newValidator().validate(StreamSource(StringReader(xml)))
    }

    @Test
    fun `gpx valid secara skema dan jumlah titik sesuai`() {
        val xml = GpxWriter.tulis(sesi)
        validasiSkema(xml)
        val doc = parse(xml)
        val ns = "http://www.topografix.com/GPX/1/1"
        assertThat(doc.getElementsByTagNameNS(ns, "trkpt").length).isEqualTo(100)
        assertThat(doc.getElementsByTagNameNS(ns, "wpt").length).isEqualTo(3)
        assertThat(doc.getElementsByTagNameNS(ns, "trk").length).isEqualTo(1)
        assertThat(doc.getElementsByTagNameNS("http://www.garmin.com/xmlschemas/TrackPointExtension/v1", "hr").length).isEqualTo(50)
        assertThat(doc.getElementsByTagNameNS(ns, "name").item(0).textContent).isEqualTo("Gn. Merbabu via Selo")
        assertThat(xml).contains("SpO2 93%")
        assertThat(xml).contains("Pos 1 &amp; &quot;Dua&quot;")
    }

    @Test
    fun `opsi mematikan detak, waypoint, spo2 dan memakai elevasi GPS`() {
        val xml = GpxWriter.tulis(sesi, GpxOptions(sertakanDetak = false, sertakanWaypoint = false, elevasiBarometer = false, sertakanSpO2 = false))
        validasiSkema(xml)
        val doc = parse(xml)
        val ns = "http://www.topografix.com/GPX/1/1"
        assertThat(doc.getElementsByTagNameNS(ns, "wpt").length).isEqualTo(0)
        assertThat(xml).doesNotContain("gpxtpx:hr")
        assertThat(xml).doesNotContain("SpO2")
        val ele = doc.getElementsByTagNameNS(ns, "ele").item(0).textContent
        assertThat(ele).isEqualTo("1690.0") // GPS, bukan barometer (1692)
    }

    @Test
    fun `nama file`() {
        val zona = ZoneId.of("Asia/Jakarta")
        // 2026-10-04 08:00 WIB
        val t = java.time.ZonedDateTime.of(2026, 10, 4, 8, 0, 0, 0, zona).toInstant().toEpochMilli()
        assertThat(Format.namaFileGpx("Merbabu", "Selo", t, zona)).isEqualTo("merbabu-selo-2026-10-04.gpx")
        assertThat(Format.namaFileGpx("Gn. Puncak Jaya (Carstensz)", null, t, zona)).isEqualTo("puncak-jaya-carstensz-2026-10-04.gpx")
    }
}
