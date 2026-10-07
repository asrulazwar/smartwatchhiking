package id.asrul.pendaki.shared

import com.google.common.truth.Truth.assertThat
import id.asrul.pendaki.shared.ams.AmsRules
import id.asrul.pendaki.shared.ams.AmsRules.Alasan
import org.junit.Test

class AmsRulesTest {
    @Test
    fun `spo2 di bawah ambang`() {
        val h = AmsRules.evaluasi(AmsRules.Masukan(spo2Sekarang = 84))
        assertThat(h.alasan).containsExactly(Alasan.SPO2_RENDAH)
        assertThat(AmsRules.evaluasi(AmsRules.Masukan(spo2Sekarang = 85)).peringatan).isFalse()
        assertThat(AmsRules.evaluasi(AmsRules.Masukan(spo2Sekarang = 89, ambangSpo2 = 90)).peringatan).isTrue()
    }

    @Test
    fun `turun 6 poin dari sebelumnya`() {
        assertThat(AmsRules.evaluasi(AmsRules.Masukan(spo2Sekarang = 90, spo2Sebelumnya = 96)).alasan)
            .containsExactly(Alasan.SPO2_TURUN)
        assertThat(AmsRules.evaluasi(AmsRules.Masukan(spo2Sekarang = 91, spo2Sebelumnya = 96)).peringatan).isFalse()
    }

    @Test
    fun `detak istirahat naik lebih dari 20`() {
        assertThat(AmsRules.evaluasi(AmsRules.Masukan(hrIstirahatSekarang = 84, hrIstirahatBasecamp = 62)).alasan)
            .containsExactly(Alasan.HR_ISTIRAHAT_NAIK)
        assertThat(AmsRules.evaluasi(AmsRules.Masukan(hrIstirahatSekarang = 82, hrIstirahatBasecamp = 62)).peringatan).isFalse()
    }

    @Test
    fun `kombinasi dan data kosong`() {
        val h = AmsRules.evaluasi(AmsRules.Masukan(spo2Sekarang = 80, spo2Sebelumnya = 93, hrIstirahatSekarang = 90, hrIstirahatBasecamp = 60))
        assertThat(h.alasan).containsExactly(Alasan.SPO2_RENDAH, Alasan.SPO2_TURUN, Alasan.HR_ISTIRAHAT_NAIK)
        assertThat(AmsRules.evaluasi(AmsRules.Masukan()).peringatan).isFalse()
    }
}
