package id.asrul.pendaki.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import dagger.hilt.android.AndroidEntryPoint
import id.asrul.pendaki.MainActivity
import id.asrul.pendaki.data.prefs.Pengaturan
import id.asrul.pendaki.data.prefs.SnapshotTile
import id.asrul.pendaki.shared.format.Format
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Dasar komplikasi SHORT_TEXT / RANGED_VALUE yang membaca snapshot dari DataStore. */
abstract class DasarKomplikasi : SuspendingComplicationDataSourceService() {
    @Inject lateinit var pengaturan: Pengaturan

    protected abstract fun teks(s: SnapshotTile?): String
    protected abstract fun judul(): String
    protected abstract fun nilai(s: SnapshotTile?): Triple<Float, Float, Float> // nilai, min, maks

    private fun bukaApp(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    override fun getPreviewData(type: ComplicationType): ComplicationData? = buat(type, null, preview = true)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val s = pengaturan.snapshotTile.first()
        return buat(request.complicationType, if (s.aktif) s else null, preview = false)
    }

    private fun buat(type: ComplicationType, s: SnapshotTile?, preview: Boolean): ComplicationData? {
        val t = if (preview) "2.410" else teks(s)
        val desc = PlainComplicationText.Builder(judul()).build()
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(PlainComplicationText.Builder(t).build(), desc)
                .setTitle(PlainComplicationText.Builder(judul()).build())
                .setTapAction(bukaApp()).build()
            ComplicationType.RANGED_VALUE -> {
                val (v, min, max) = if (preview) Triple(0.5f, 0f, 1f) else nilai(s)
                RangedValueComplicationData.Builder(value = v.coerceIn(min, max), min = min, max = max, contentDescription = desc)
                    .setText(PlainComplicationText.Builder(t).build())
                    .setTitle(PlainComplicationText.Builder(judul()).build())
                    .setTapAction(bukaApp()).build()
            }
            else -> null
        }
    }
}

@AndroidEntryPoint
class KetinggianComplicationService : DasarKomplikasi() {
    override fun judul() = "mdpl"
    override fun teks(s: SnapshotTile?) = s?.ketinggianM?.let { Format.ribuan(it) } ?: "—"
    override fun nilai(s: SnapshotTile?): Triple<Float, Float, Float> {
        val min = (s?.basecampElev ?: 0).toFloat()
        val max = (s?.puncakElev ?: 1).toFloat().coerceAtLeast(min + 1)
        return Triple((s?.ketinggianM ?: min.toDouble()).toFloat(), min, max)
    }
}

@AndroidEntryPoint
class SisaNaikComplicationService : DasarKomplikasi() {
    override fun judul() = "sisa"
    override fun teks(s: SnapshotTile?) = s?.sisaNaikM?.let { "${Format.ribuan(it.coerceAtLeast(0.0))} m" } ?: "—"
    override fun nilai(s: SnapshotTile?): Triple<Float, Float, Float> {
        val total = ((s?.puncakElev ?: 1) - (s?.basecampElev ?: 0)).toFloat().coerceAtLeast(1f)
        return Triple((s?.sisaNaikM ?: total.toDouble()).toFloat(), 0f, total)
    }
}
