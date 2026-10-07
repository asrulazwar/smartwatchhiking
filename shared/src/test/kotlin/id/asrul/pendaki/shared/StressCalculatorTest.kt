package id.asrul.pendaki.shared

import com.google.common.truth.Truth.assertThat
import id.asrul.pendaki.shared.stress.StressCalculator
import org.junit.Test

class StressCalculatorTest {
    @Test
    fun `rmssd dari interval RR`() {
        assertThat(StressCalculator.rmssd(listOf(800.0))).isNull()
        assertThat(StressCalculator.rmssd(listOf(800.0, 800.0, 800.0))).isWithin(0.001).of(0.0)
        // selisih 10, -10 -> sqrt((100+100)/2) = 10
        assertThat(StressCalculator.rmssd(listOf(800.0, 810.0, 800.0))).isWithin(0.001).of(10.0)
    }

    @Test
    fun `konversi ke skala 0-100 dengan baseline`() {
        assertThat(StressCalculator.keSkala(42.0, 42.0)).isEqualTo(30)
        assertThat(StressCalculator.keSkala(21.0, 42.0)).isEqualTo(70)
        assertThat(StressCalculator.keSkala(84.0, 42.0)).isEqualTo(0)
        assertThat(StressCalculator.keSkala(5.0, 42.0)).isEqualTo(100)
        assertThat(StressCalculator.keSkala(28.0, 50.0)).isWithin(2).of(63)
        assertThat(StressCalculator.keSkala(42.0)).isEqualTo(30) // baseline default
    }

    @Test
    fun `label`() {
        assertThat(StressCalculator.label(20)).isEqualTo(StressCalculator.Label.RENDAH)
        assertThat(StressCalculator.label(35)).isEqualTo(StressCalculator.Label.SEDANG)
        assertThat(StressCalculator.label(65)).isEqualTo(StressCalculator.Label.SEDANG)
        assertThat(StressCalculator.label(66)).isEqualTo(StressCalculator.Label.TINGGI)
    }
}
