package id.asrul.pendaki.shared

import com.google.common.truth.Truth.assertThat
import id.asrul.pendaki.shared.heart.HrThresholdMonitor
import id.asrul.pendaki.shared.heart.HrZones
import id.asrul.pendaki.shared.heart.RestingHrTracker
import org.junit.Test

class HrThresholdMonitorTest {
    private val s = 1_000L

    @Test
    fun `peringatan hanya setelah lebih dari 30 detik terus-menerus`() {
        val m = HrThresholdMonitor(batasAwal = 155)
        var t = 0L
        repeat(30) { assertThat(m.sampel(t, 160)).isNull(); t += s }  // 0..29 s
        assertThat(m.sampel(30 * s, 160)).isNull()                      // tepat 30 s: belum
        val p = m.sampel(31 * s, 162)
        assertThat(p).isNotNull()
        assertThat(p!!.batas).isEqualTo(155)
        assertThat(p.durasiAtasMs).isEqualTo(31 * s)
    }

    @Test
    fun `turun di bawah batas mereset hitungan`() {
        val m = HrThresholdMonitor(batasAwal = 155)
        for (i in 0 until 25) m.sampel(i * s, 160)
        m.sampel(25 * s, 150) // reset
        for (i in 26 until 50) assertThat(m.sampel(i * s, 160)).isNull()
        assertThat(m.sampel(57 * s, 160)).isNotNull() // 26..57 = 31 s
    }

    @Test
    fun `cooldown 2 menit antar peringatan`() {
        val m = HrThresholdMonitor(batasAwal = 155)
        var t = 0L
        var pertama: Long? = null
        while (t < 5 * 60 * s) {
            if (m.sampel(t, 170) != null) { pertama = t; break }
            t += s
        }
        assertThat(pertama).isEqualTo(31 * s)
        var kedua: Long? = null
        t += s
        while (t < 10 * 60 * s) {
            if (m.sampel(t, 170) != null) { kedua = t; break }
            t += s
        }
        assertThat(kedua).isEqualTo(pertama!! + 120 * s)
    }

    @Test
    fun `abaikan 10 menit menunda`() {
        val m = HrThresholdMonitor(batasAwal = 155)
        for (i in 0..31) m.sampel(i * s, 170)
        m.abaikan(31 * s)
        var t = 32 * s
        while (t < 31 * s + 10 * 60 * s) { assertThat(m.sampel(t, 170)).isNull(); t += s }
        assertThat(m.sampel(31 * s + 10 * 60 * s + s, 170)).isNotNull()
    }

    @Test
    fun `istirahat tetap memantau`() {
        val m = HrThresholdMonitor(batasAwal = 155)
        m.istirahat(0)
        assertThat(m.sedangIstirahat(5 * 60 * s)).isTrue()
        assertThat(m.sisaIstirahatMs(5 * 60 * s)).isEqualTo(5 * 60 * s)
        for (i in 0..30) m.sampel(i * s, 170)
        assertThat(m.sampel(31 * s, 170)).isNotNull()
    }

    @Test
    fun `batas dibatasi 100-200`() {
        val m = HrThresholdMonitor(batasAwal = 50)
        assertThat(m.batas).isEqualTo(100)
        m.batas = 250
        assertThat(m.batas).isEqualTo(200)
    }

    @Test
    fun `zona dan detak istirahat`() {
        assertThat(HrZones.hrMaks(35)).isEqualTo(185)
        assertThat(HrZones.zona(128, 35)).isEqualTo(2) // 69%
        assertThat(HrZones.zona(150, 35)).isEqualTo(4) // 81%
        val r = RestingHrTracker()
        var t = 0L
        repeat(200) { r.tambah(t, 60 + (it % 5), diam = true); t += s }
        assertThat(r.terakhir).isEqualTo(62)
        r.tambah(t, 120, diam = false)
        assertThat(r.terakhir).isEqualTo(62) // nilai lama dipertahankan
    }
}
