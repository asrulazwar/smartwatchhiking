package id.asrul.pendaki.ui.komponen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import id.asrul.pendaki.ui.theme.Warna

/** Latar hitam penuh dengan isi di tengah. */
@Composable
fun LayarDasar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize().background(Warna.Hitam), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier, warna: Color = Warna.Sekunder, align: TextAlign = TextAlign.Center) {
    Text(text, modifier = modifier, color = warna, style = MaterialTheme.typography.caption1, textAlign = align, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable
fun LabelKecil(text: String, modifier: Modifier = Modifier, warna: Color = Warna.Sekunder) {
    Text(text.uppercase(), modifier = modifier, color = warna, style = MaterialTheme.typography.caption3.copy(letterSpacing = 1.2.sp), textAlign = TextAlign.Center, maxLines = 1)
}

@Composable
fun AngkaBesar(text: String, modifier: Modifier = Modifier, warna: Color = Warna.Teks, style: TextStyle = MaterialTheme.typography.display1) {
    Text(text, modifier = modifier, color = warna, style = style, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
}

/** Tombol lebar minimal 48 dp. */
@Composable
fun TombolAksi(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    warnaLatar: Color = Warna.Oranye,
    warnaTeks: Color = Warna.Hitam,
    tinggi: Dp = 48.dp,
) {
    Chip(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = tinggi),
        colors = ChipDefaults.chipColors(backgroundColor = warnaLatar, contentColor = warnaTeks),
        label = {
            Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.button, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun TombolSekunder(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, warnaTeks: Color = Warna.Teks) =
    TombolAksi(text, onClick, modifier, warnaLatar = Warna.PermukaanTerang, warnaTeks = warnaTeks)

/** Dua tombol berdampingan (mis. "Belum sampai" / "Mulai turun"). */
@Composable
fun DuaTombol(
    kiri: String, onKiri: () -> Unit,
    kanan: String, onKanan: () -> Unit,
    modifier: Modifier = Modifier,
    warnaKanan: Color = Warna.Oranye,
    warnaTeksKanan: Color = Warna.Hitam,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TombolSekunder(kiri, onKiri, Modifier.weight(1f))
        TombolAksi(kanan, onKanan, Modifier.weight(1f), warnaLatar = warnaKanan, warnaTeks = warnaTeksKanan)
    }
}

/** Tombol bulat ± untuk pengaturan angka. */
@Composable
fun TombolPlusMinus(plus: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = Warna.PermukaanTerang, contentColor = Warna.Teks),
    ) {
        if (plus) Icon(Icons.Rounded.Add, contentDescription = "+")
        else Text("−", style = MaterialTheme.typography.title1, color = Warna.Teks)
    }
}

/** Cincin progres di tepi layar bulat. */
@Composable
fun CincinProgres(progres: Float, modifier: Modifier = Modifier, warna: Color = Warna.Oranye, tebal: Dp = 6.dp, mulaiDeg: Float = 135f, sapuanDeg: Float = 270f) {
    Canvas(modifier = modifier.fillMaxSize().padding(4.dp)) {
        val stroke = Stroke(width = tebal.toPx(), cap = StrokeCap.Round)
        drawArc(color = Warna.PermukaanTerang, startAngle = mulaiDeg, sweepAngle = sapuanDeg, useCenter = false, style = stroke)
        drawArc(color = warna, startAngle = mulaiDeg, sweepAngle = sapuanDeg * progres.coerceIn(0f, 1f), useCenter = false, style = stroke)
    }
}

/** Chip kecil nilai (detak, SpO2). */
@Composable
fun ChipNilai(ikon: @Composable () -> Unit, nilai: String, modifier: Modifier = Modifier, warna: Color = Warna.Teks) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(50)).background(Warna.Permukaan).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ikon()
        Spacer(Modifier.width(5.dp))
        Text(nilai, color = warna, style = MaterialTheme.typography.title3, maxLines = 1)
    }
}

/** Grafik garis sederhana (mis. detak 2 menit terakhir). */
@Composable
fun GrafikGaris(nilai: List<Int>, modifier: Modifier = Modifier, warna: Color = Warna.Merah, garisBatas: Int? = null, warnaBatas: Color = Warna.Sekunder) {
    Canvas(modifier = modifier.fillMaxWidth().height(40.dp)) {
        if (nilai.size < 2) return@Canvas
        val min = (nilai.min().coerceAtMost(garisBatas ?: Int.MAX_VALUE) - 5).toFloat()
        val max = (nilai.max().coerceAtLeast(garisBatas ?: Int.MIN_VALUE) + 5).toFloat()
        val rentang = (max - min).coerceAtLeast(1f)
        val dx = size.width / (nilai.size - 1)
        fun y(v: Float) = size.height - (v - min) / rentang * size.height
        garisBatas?.let { b ->
            drawLine(warnaBatas, androidx.compose.ui.geometry.Offset(0f, y(b.toFloat())), androidx.compose.ui.geometry.Offset(size.width, y(b.toFloat())), strokeWidth = 1.5f)
        }
        for (i in 1 until nilai.size) {
            drawLine(warna, androidx.compose.ui.geometry.Offset((i - 1) * dx, y(nilai[i - 1].toFloat())), androidx.compose.ui.geometry.Offset(i * dx, y(nilai[i].toFloat())), strokeWidth = 3f, cap = StrokeCap.Round)
        }
    }
}

/** Grafik batang (stres sejak basecamp). */
@Composable
fun GrafikBatang(nilai: List<Int>, modifier: Modifier = Modifier, maks: Int = 100, warnaUntuk: (Int) -> Color = { Warna.Oranye }) {
    Canvas(modifier = modifier.fillMaxWidth().height(36.dp)) {
        if (nilai.isEmpty()) return@Canvas
        val n = nilai.size
        val lebar = size.width / n
        nilai.forEachIndexed { i, v ->
            val h = (v.toFloat() / maks).coerceIn(0.04f, 1f) * size.height
            drawRoundRect(
                color = warnaUntuk(v),
                topLeft = androidx.compose.ui.geometry.Offset(i * lebar + lebar * 0.15f, size.height - h),
                size = androidx.compose.ui.geometry.Size(lebar * 0.7f, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
            )
        }
    }
}

@Composable
fun KolomTengah(modifier: Modifier = Modifier, spasi: Dp = 4.dp, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spasi, Alignment.CenterVertically),
        content = content,
    )
}
