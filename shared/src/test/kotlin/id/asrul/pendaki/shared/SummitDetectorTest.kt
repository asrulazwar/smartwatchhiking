package id.asrul.pendaki.shared

import com.google.common.truth.Truth.assertThat
import id.asrul.pendaki.shared.summit.SummitDetector
import org.junit.Test

class SummitDetectorTest {
    private val lat = -7.4550
    private val lon = 110.4400
    private fun det() = SummitDetector(lat, lon, 3145.0)

    @Test
    fun `terdeteksi hanya jika dekat DAN ketinggian cocok`() {
        val d = det()
        assertThat(d.cek(0, lat, lon, 3130.0).terdeteksi).isTrue()
        // 30 m ke timur, alt dalam toleransi
        assertThat(d.cek(0, lat, lon + 0.00027, 3110.0).terdeteksi).isTrue()
        // dekat tapi ketinggian 100 m di bawah
        assertThat(d.cek(0, lat, lon, 3045.0).terdeteksi).isFalse()
        // ketinggian cocok tapi 200 m jauhnya
        assertThat(d.cek(0, lat, lon + 0.0018, 3145.0).terdeteksi).isFalse()
        // tanpa barometer tidak pernah terdeteksi
        assertThat(d.cek(0, lat, lon, null).terdeteksi).isFalse()
    }

    @Test
    fun `belum sampai menunda lalu aktif lagi`() {
        val d = det()
        assertThat(d.cek(0, lat, lon, 3140.0).terdeteksi).isTrue()
        d.belumSampai(0)
        assertThat(d.cek(60_000, lat, lon, 3140.0).terdeteksi).isFalse()
        assertThat(d.cek(5 * 60_000L + 1, lat, lon, 3140.0).terdeteksi).isTrue()
    }

    @Test
    fun `konfirmasi menghentikan deteksi`() {
        val d = det()
        d.konfirmasi()
        assertThat(d.cek(0, lat, lon, 3145.0).terdeteksi).isFalse()
        assertThat(d.dikonfirmasi).isTrue()
    }
}
