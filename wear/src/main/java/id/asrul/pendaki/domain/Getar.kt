package id.asrul.pendaki.domain

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import dagger.hilt.android.qualifiers.ApplicationContext
import id.asrul.pendaki.data.prefs.Pengaturan
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Pembungkus getar yang menghormati pengaturan "getar on/off". */
@Singleton
class Getar @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val pengaturan: Pengaturan,
) {
    private val vibrator: Vibrator? by lazy { ctx.getSystemService(Vibrator::class.java) }

    suspend fun pendek() = jalankan(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
    suspend fun ganda() = jalankan(VibrationEffect.createWaveform(longArrayOf(0, 60, 80, 60), -1))
    suspend fun panjang() = jalankan(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 700), -1))
    suspend fun peringatan() = jalankan(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300, 150, 300), -1))

    private suspend fun jalankan(effect: VibrationEffect) {
        if (!pengaturan.data.first().getar) return
        runCatching { vibrator?.vibrate(effect) }
    }
}
