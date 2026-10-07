package id.asrul.pendaki.shared

import com.google.common.truth.Truth.assertThat
import id.asrul.pendaki.shared.altitude.BaroAltimeter
import id.asrul.pendaki.shared.altitude.GpsAltitudeAverager
import org.junit.Test

class BaroAltimeterTest {
    @Test
    fun `rumus tekanan ke ketinggian`() {
        assertThat(BaroAltimeter.tekananKeKetinggian(1013.25)).isWithin(0.01).of(0.0)
        // ±1500 m -> sekitar 845 hPa
        assertThat(BaroAltimeter.tekananKeKetinggian(845.0)).isWithin(30.0).of(1500.0)
    }

    @Test
    fun `kalibrasi menggeser ke acuan dan offset dipertahankan`() {
        val alt = BaroAltimeter(alpha = 1.0)
        alt.sampel(845.0)
        val sebelum = alt.ketinggianM!!
        val offset = alt.kalibrasi(1690.0)
        assertThat(alt.ketinggianM!!).isWithin(0.001).of(1690.0)
        assertThat(offset).isWithin(0.001).of(1690.0 - sebelum)

        // tekanan turun -> ketinggian naik relatif terhadap acuan
        alt.sampel(800.0)
        assertThat(alt.ketinggianM!!).isGreaterThan(1690.0)

        // offset bisa dipulihkan di instance baru
        val baru = BaroAltimeter(alpha = 1.0, offsetAwal = offset)
        baru.sampel(845.0)
        assertThat(baru.ketinggianM!!).isWithin(0.001).of(1690.0)
    }

    @Test
    fun `low-pass meredam lonjakan`() {
        val alt = BaroAltimeter(alpha = 0.2)
        repeat(50) { alt.sampel(845.0) }
        val stabil = alt.ketinggianM!!
        alt.sampel(840.0) // lonjakan ±50 m
        assertThat(alt.ketinggianM!! - stabil).isLessThan(15.0)
    }

    @Test
    fun `rata-rata GPS 30 detik`() {
        val avg = GpsAltitudeAverager()
        assertThat(avg.siap).isFalse()
        var t = 0L
        for (i in 0..30) { avg.tambah(t, 1700.0 + (i % 3)); t += 1_000 }
        assertThat(avg.siap).isTrue()
        assertThat(avg.rataRata!!).isWithin(1.0).of(1701.0)
        // sampel lama (> 30 s) dibuang
        avg.tambah(100_000, 2000.0)
        assertThat(avg.jumlah).isEqualTo(1)
    }
}
