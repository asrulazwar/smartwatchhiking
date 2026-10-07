package id.asrul.pendaki.shared

import com.google.common.truth.Truth.assertThat
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.shared.model.JenisWaypoint
import id.asrul.pendaki.shared.model.TitikJejak
import id.asrul.pendaki.shared.model.Waypoint
import id.asrul.pendaki.shared.nav.Navigation
import id.asrul.pendaki.shared.pos.WaypointMatcher
import id.asrul.pendaki.shared.stats.SessionStats
import id.asrul.pendaki.shared.sun.SunCalc
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FormatStatsTest {
    @Test
    fun `format angka Indonesia`() {
        assertThat(Format.ribuan(2410)).isEqualTo("2.410")
        assertThat(Format.ribuan(999)).isEqualTo("999")
        assertThat(Format.ribuan(1234567)).isEqualTo("1.234.567")
        assertThat(Format.ribuan(-1500)).isEqualTo("-1.500")
        assertThat(Format.jarak(12_400.0)).isEqualTo("12,4 km")
        assertThat(Format.jarak(420.0)).isEqualTo("420 m")
        assertThat(Format.durasi(6 * 3_600_000L + 22 * 60_000L)).isEqualTo("6 j 22 m")
        assertThat(Format.durasi(45 * 60_000L)).isEqualTo("45 m")
        assertThat(Format.durasiJamMenit(11 * 3_600_000L + 48 * 60_000L)).isEqualTo("11:48")
    }

    @Test
    fun `naik total dengan ambang dan kecepatan naik`() {
        val t0 = 0L
        val titik = (0 until 60).map { TitikJejak(t0 + it * 15_000L, -7.49 + it * 1e-4, 110.45, altBaro = 1000.0 + it * 5 + (if (it % 2 == 0) 1.0 else 0.0)) }
        val naik = SessionStats.naikTotal(titik)
        assertThat(naik).isWithin(10.0).of(295.0)
        // 15 menit terakhir: 60 titik x 15 s = 15 menit, naik 295 m -> ~1180 m/jam
        val v = SessionStats.kecepatanNaikMPerJam(titik, titik.last().waktu)
        assertThat(v!!).isWithin(50.0).of(1200.0)
        assertThat(SessionStats.perkiraanTiba(0, 600.0, 1200.0)).isEqualTo(30 * 60_000L)
        assertThat(SessionStats.perkiraanTiba(0, 600.0, null)).isNull()
        assertThat(SessionStats.progres(2410.0, 1690.0, 3145.0)).isWithin(0.01f).of(0.495f)
    }

    @Test
    fun `navigasi balik memilih pos di bawah yang belum dilewati`() {
        val wp = listOf(
            Waypoint("Basecamp", 0, -7.495, 110.453, 1690.0, JenisWaypoint.BASECAMP),
            Waypoint("Pos 1", 1, -7.485, 110.450, 1960.0),
            Waypoint("Pos 2", 2, -7.475, 110.447, 2230.0),
            Waypoint("Pos 3", 3, -7.465, 110.444, 2520.0),
            Waypoint("Puncak", 4, -7.455, 110.440, 3145.0, JenisWaypoint.PUNCAK),
        )
        // sedang turun di 2.700 m, di antara puncak dan pos 3
        val target = Navigation.tujuanTurun(wp, -7.460, 110.442, 2700.0)
        assertThat(target!!.nama).isEqualTo("Pos 3")
        // sudah di pos 3 (dalam 60 m) -> tujuan pos 2
        assertThat(Navigation.tujuanTurun(wp, -7.465, 110.444, 2520.0)!!.nama).isEqualTo("Pos 2")
        val jejak = wp.map { TitikJejak(it.waktu, it.lat, it.lon, altBaro = it.alt) }
        val arah = Navigation.hitung(target, -7.460, 110.442, 2700.0, headingDeg = 90.0, jejak = jejak)
        assertThat(arah.jarakM).isWithin(50.0).of(600.0)
        assertThat(arah.selisihAltM).isWithin(0.1).of(-180.0)
        assertThat(arah.jarakKeBasecampM).isGreaterThan(arah.jarakM)
        assertThat(arah.arahJam).isEqualTo(2) // bearing ≈160°, heading 90° -> relatif ≈70°
    }

    @Test
    fun `pencocokan waypoint lama radius 60 m`() {
        val lama = listOf(Waypoint("Pos 3", 0, -7.465, 110.444, 2520.0))
        val m = WaypointMatcher(lama)
        assertThat(m.cocokkan(-7.4653, 110.4442)!!.nama).isEqualTo("Pos 3")  // ~40 m
        assertThat(m.cocokkan(-7.4660, 110.4450)).isNull()                     // >60 m
        m.tolak(lama[0])
        assertThat(m.cocokkan(-7.465, 110.444)).isNull()
    }

    @Test
    fun `matahari terbenam di Merbabu sekitar 17-18 WIB`() {
        val z = ZoneId.of("Asia/Jakarta")
        val t = SunCalc.terbenam(-7.455, 110.44, LocalDate.of(2026, 10, 4), z)!!
        assertThat(t.hour).isEqualTo(17)
        assertThat(t.minute).isIn(30..50)
        val terbit = SunCalc.terbit(-7.455, 110.44, LocalDate.of(2026, 10, 4), z)!!
        assertThat(terbit.hour).isEqualTo(5)
    }
}
