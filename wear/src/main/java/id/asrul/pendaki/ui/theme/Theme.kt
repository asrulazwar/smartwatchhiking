package id.asrul.pendaki.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography
import id.asrul.pendaki.R

object Warna {
    val Hitam = Color(0xFF000000)
    val Oranye = Color(0xFFF2A23A)
    val Teks = Color(0xFFF4F1EA)
    val Sekunder = Color(0xFFA8A49C)
    val Merah = Color(0xFFD94848)
    val Hijau = Color(0xFF7BC67E)
    val BiruSpO2 = Color(0xFF5BB7D8)
    val Permukaan = Color(0xFF161513)
    val PermukaanTerang = Color(0xFF26241F)
}

private val warna = Colors(
    primary = Warna.Oranye,
    primaryVariant = Warna.Oranye,
    secondary = Warna.BiruSpO2,
    secondaryVariant = Warna.BiruSpO2,
    error = Warna.Merah,
    background = Warna.Hitam,
    surface = Warna.Permukaan,
    onPrimary = Warna.Hitam,
    onSecondary = Warna.Hitam,
    onError = Warna.Teks,
    onBackground = Warna.Teks,
    onSurface = Warna.Teks,
    onSurfaceVariant = Warna.Sekunder,
)

val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.manrope, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.manrope, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.manrope, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.manrope, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

/** Angka tabular untuk semua angka besar. */
const val TNUM = "tnum"

private val tipografi = Typography(
    defaultFontFamily = Manrope,
    display1 = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, fontFeatureSettings = TNUM),
    display2 = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, fontFeatureSettings = TNUM),
    display3 = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, fontFeatureSettings = TNUM),
    title1 = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, fontFeatureSettings = TNUM),
    title2 = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, fontFeatureSettings = TNUM),
    title3 = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, fontFeatureSettings = TNUM),
    body1 = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp),
    body2 = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp),
    button = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    caption1 = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, fontFeatureSettings = TNUM),
    caption2 = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, fontFeatureSettings = TNUM),
    caption3 = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, fontFeatureSettings = TNUM),
)

@Composable
fun PendakiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = warna, typography = tipografi, content = content)
}
