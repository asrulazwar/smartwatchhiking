package id.asrul.pendaki.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

private val skema = darkColorScheme(
    primary = Warna.Oranye, onPrimary = Warna.Hitam,
    secondary = Warna.BiruSpO2, onSecondary = Warna.Hitam,
    background = Warna.Hitam, onBackground = Warna.Teks,
    surface = Warna.Permukaan, onSurface = Warna.Teks,
    surfaceVariant = Warna.PermukaanTerang, onSurfaceVariant = Warna.Sekunder,
    error = Warna.Merah, onError = Warna.Teks,
)

private val tipografi = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, fontFeatureSettings = "tnum"),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, fontFeatureSettings = "tnum"),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.sp),
)

@Composable
fun PendakiMobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = skema, typography = tipografi, content = content)
}
