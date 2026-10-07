package id.asrul.pendaki.domain

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import id.asrul.pendaki.data.prefs.Pengaturan
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Pengumuman suara lewat Text-to-Speech sistem (bahasa Indonesia). Dilewati jika pengaturan "Suara" mati. */
@Singleton
class Suara @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val pengaturan: Pengaturan,
) {
    private var tts: TextToSpeech? = null
    @Volatile private var siap = false
    private val antre = ArrayDeque<String>()

    private fun pastikanInit() {
        if (tts != null) return
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("id", "ID"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) Timber.w("TTS bahasa Indonesia tidak tersedia, pakai bahasa bawaan")
                siap = true
                synchronized(antre) { while (antre.isNotEmpty()) bicara(antre.removeFirst()) }
            } else {
                Timber.w("TTS tidak tersedia (status %d)", status)
            }
        }
    }

    suspend fun ucapkan(@StringRes resId: Int, vararg args: Any) {
        if (!pengaturan.data.first().suara) return
        val teks = ctx.getString(resId, *args)
        pastikanInit()
        if (siap) bicara(teks) else synchronized(antre) { antre.addLast(teks) }
    }

    private fun bicara(teks: String) {
        runCatching { tts?.speak(teks, TextToSpeech.QUEUE_ADD, null, "pendaki-${System.currentTimeMillis()}") }
            .onFailure { Timber.w(it, "TTS gagal") }
    }
}
