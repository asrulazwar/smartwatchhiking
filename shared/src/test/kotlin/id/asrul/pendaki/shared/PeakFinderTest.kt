package id.asrul.pendaki.shared

import com.google.common.truth.Truth.assertThat
import id.asrul.pendaki.shared.geo.Geo
import id.asrul.pendaki.shared.geo.PeakFinder
import id.asrul.pendaki.shared.model.DataGunung
import org.junit.Test

class PeakFinderTest {
    private val finder = PeakFinder(DataGunung.gunungBawaan())

    @Test
    fun `dari Selo menampilkan Merbabu dan Merapi berurutan jarak`() {
        // Basecamp Selo, Boyolali (di antara Merbabu dan Merapi)
        val hasil = finder.terdekat(lat = -7.4950, lon = 110.4530)
        val nama = hasil.map { it.gunung.id }
        assertThat(nama).containsAtLeast("merbabu", "merapi")
        assertThat(hasil.size).isAtMost(3)
        assertThat(nama.indexOf("merbabu")).isLessThan(nama.indexOf("merapi"))
        assertThat(hasil.map { it.jarakM }).isInOrder()
        hasil.forEach { assertThat(it.jarakM).isAtMost(20_000.0) }
    }

    @Test
    fun `di luar radius 20 km kosong`() {
        // Laut Jawa, jauh dari gunung mana pun
        assertThat(finder.terdekat(-5.5, 110.0)).isEmpty()
    }

    @Test
    fun `maksimal tiga hasil`() {
        // Dieng: Prau, Bismo, Pakuwaja, Kembang, Sindoro semua dekat
        val hasil = finder.terdekat(-7.21, 109.92)
        assertThat(hasil).hasSize(3)
        assertThat(hasil.first().gunung.id).isEqualTo("prau")
    }

    @Test
    fun `pencarian teks dan id`() {
        assertThat(finder.cari("mer").map { it.id }).containsAtLeast("merbabu", "merapi")
        assertThat(finder.byId("rinjani")?.elevasi).isEqualTo(3726)
        assertThat(finder.cari("").size).isAtLeast(60)
    }

    @Test
    fun `jarak dan bearing geo`() {
        assertThat(Geo.jarakM(0.0, 0.0, 0.0, 1.0)).isWithin(200.0).of(111_195.0)
        assertThat(Geo.bearingDeg(0.0, 0.0, 1.0, 0.0)).isWithin(0.01).of(0.0)
        assertThat(Geo.bearingDeg(0.0, 0.0, 0.0, 1.0)).isWithin(0.01).of(90.0)
        assertThat(Geo.arahJam(0.0)).isEqualTo(12)
        assertThat(Geo.arahJam(60.0)).isEqualTo(2)
        assertThat(Geo.arahJam(-90.0)).isEqualTo(9)
        assertThat(Geo.normalisasiSudut(350.0)).isWithin(0.001).of(-10.0)
    }

    @Test
    fun `data jalur merujuk gunung yang ada`() {
        val ids = DataGunung.gunungBawaan().map { it.id }.toSet()
        val jalur = DataGunung.jalurBawaan()
        assertThat(jalur.size).isAtLeast(13)
        jalur.forEach { assertThat(ids).contains(it.gunungId) }
    }
}
