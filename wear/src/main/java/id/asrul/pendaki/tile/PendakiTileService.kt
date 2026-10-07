package id.asrul.pendaki.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.google.android.horologist.tiles.SuspendingTileService
import dagger.hilt.android.AndroidEntryPoint
import id.asrul.pendaki.R
import id.asrul.pendaki.data.prefs.Pengaturan
import id.asrul.pendaki.data.prefs.SnapshotTile
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.shared.sun.SunCalc
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Tile ringkas (mockup 6): nama gunung, durasi, ketinggian + sisa naik, naik total, detak,
 * SpO2 terakhir, matahari terbenam & sisa waktu. Ketuk → buka aplikasi.
 */
@AndroidEntryPoint
class PendakiTileService : SuspendingTileService() {

    @Inject lateinit var pengaturan: Pengaturan

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources =
        ResourceBuilders.Resources.Builder().setVersion(VERSI).build()

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val s = pengaturan.snapshotTile.first()
        val layout = if (s.aktif) layoutAktif(this, s) else layoutIdle(this)
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(VERSI)
            .setFreshnessIntervalMillis(60_000)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
    }

    companion object {
        const val VERSI = "1"
        private const val ORANYE = 0xFFF2A23A.toInt()
        private const val TEKS = 0xFFF4F1EA.toInt()
        private const val SEKUNDER = 0xFFA8A49C.toInt()
        private const val MERAH = 0xFFD94848.toInt()
        private const val BIRU = 0xFF5BB7D8.toInt()

        private fun teks(t: String, ukuranSp: Float, warna: Int, tebal: Boolean = false): Text =
            Text.Builder().setText(t).setMaxLines(1)
                .setFontStyle(FontStyle.Builder().setSize(sp(ukuranSp)).setColor(argb(warna)).setWeight(if (tebal) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL).build())
                .build()

        private fun bukaApp(ctx: Context) = ModifiersBuilders.Modifiers.Builder()
            .setClickable(
                ModifiersBuilders.Clickable.Builder().setId("buka").setOnClick(
                    ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder().setPackageName(ctx.packageName).setClassName("id.asrul.pendaki.MainActivity").build()
                    ).build()
                ).build()
            ).build()

        private fun kolomStat(label: String, nilai: String, warna: Int = TEKS): Column =
            Column.Builder().setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(teks(label, 9f, SEKUNDER))
                .addContent(teks(nilai, 18f, warna, tebal = true))
                .build()

        fun layoutAktif(ctx: Context, s: SnapshotTile): LayoutElementBuilders.LayoutElement {
            val now = System.currentTimeMillis()
            val durasi = Format.durasi(now - s.mulai)
            val terbenam = if (s.lat != null && s.lon != null) {
                val z = ZoneId.systemDefault()
                val t = SunCalc.terbenam(s.lat, s.lon, Instant.ofEpochMilli(now).atZone(z).toLocalDate(), z)
                val sisa = SunCalc.sisaKeTerbenamMs(s.lat, s.lon, Instant.ofEpochMilli(now), z)
                if (t != null && sisa != null) "Terbenam ${Format.jam(t.toInstant().toEpochMilli())} · ${Format.durasi(sisa)} lagi" else ""
            } else ""
            return Column.Builder()
                .setWidth(expand()).setHeight(expand())
                .setModifiers(bukaApp(ctx))
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(teks(ctx.getString(R.string.tile_label), 10f, SEKUNDER))
                .addContent(teks("Gn. ${s.namaGunung ?: ""}", 16f, TEKS, tebal = true))
                .addContent(teks("$durasi berjalan" + (s.posTerakhir?.let { " · $it" } ?: ""), 10f, SEKUNDER))
                .addContent(Spacer.Builder().setHeight(dp(6f)).build())
                .addContent(teks("KETINGGIAN", 9f, SEKUNDER))
                .addContent(teks(s.ketinggianM?.let { Format.ribuan(it) } ?: "—", 34f, TEKS, tebal = true))
                .addContent(teks(s.sisaNaikM?.let { "↑ ${Format.ribuan(it.coerceAtLeast(0.0))} m lagi" } ?: "", 11f, ORANYE))
                .addContent(Spacer.Builder().setHeight(dp(6f)).build())
                .addContent(
                    Row.Builder()
                        .addContent(kolomStat("NAIK TOTAL", Format.ribuan(s.naikTotalM)))
                        .addContent(Spacer.Builder().setWidth(dp(14f)).build())
                        .addContent(kolomStat("DETAK", s.hr?.toString() ?: "—", MERAH))
                        .addContent(Spacer.Builder().setWidth(dp(14f)).build())
                        .addContent(kolomStat("SpO₂", s.spo2?.let { "$it%" } ?: "—", BIRU))
                        .build()
                )
                .addContent(Spacer.Builder().setHeight(dp(6f)).build())
                .addContent(teks(terbenam, 10f, SEKUNDER))
                .build()
        }

        fun layoutIdle(ctx: Context): LayoutElementBuilders.LayoutElement =
            Column.Builder().setWidth(expand()).setHeight(expand())
                .setModifiers(bukaApp(ctx))
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(teks(ctx.getString(R.string.app_name), 16f, ORANYE, tebal = true))
                .addContent(Spacer.Builder().setHeight(dp(6f)).build())
                .addContent(teks(ctx.getString(R.string.mulai_judul), 12f, TEKS))
                .build()
    }
}
