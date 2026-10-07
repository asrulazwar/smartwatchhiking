package id.asrul.pendaki.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import id.asrul.pendaki.R
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.shared.nav.Navigation
import id.asrul.pendaki.ui.komponen.AngkaBesar
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.theme.Warna
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Navigasi balik tanpa peta (mockup 3): panah besar relatif heading, jarak, selisih tinggi, arah jam. */
@Composable
fun NavigasiScreen(vm: HikeViewModel) {
    val st by vm.state.collectAsStateWithLifecycle()
    val lat = st.lat
    val lon = st.lon
    val heading = (st.headingDeg ?: 0f).toDouble()
    val tujuan = remember(st.waypoint, lat, lon, st.ketinggianM) {
        if (lat == null || lon == null) null else Navigation.tujuanTurun(st.waypoint, lat, lon, st.ketinggianM)
    }
    val arah = remember(tujuan, lat, lon, heading, st.jumlahTitik) {
        if (tujuan == null || lat == null || lon == null) null
        else Navigation.hitung(tujuan, lat, lon, st.ketinggianM, heading, emptyList()).let {
            it.copy(jarakKeBasecampM = jarakBasecamp(st, lat, lon))
        }
    }

    LayarDasar {
        // Tanda utara di tepi layar: berputar berlawanan heading
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val r = size.minDimension / 2
            val sudut = Math.toRadians(-heading - 90.0)
            val c = Offset(center.x + r * cos(sudut).toFloat(), center.y + r * sin(sudut).toFloat())
            drawCircle(Warna.Merah, radius = 6.dp.toPx(), center = c)
        }
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            if (arah == null) {
                Label(stringResource(R.string.nav_tanpa_tujuan), warna = Warna.Teks)
            } else {
                Panah(relatifDeg = arah.relatifDeg.toFloat(), modifier = Modifier.size(84.dp))
                Spacer(Modifier.height(4.dp))
                Label(stringResource(R.string.nav_kembali_ke))
                Text(arah.tujuan.nama, color = Warna.Teks, style = MaterialTheme.typography.title1, textAlign = TextAlign.Center, maxLines = 1)
                AngkaBesar(Format.ribuan(arah.jarakM), style = MaterialTheme.typography.display2)
                Label(stringResource(R.string.nav_m))
                val selisih = arah.selisihAltM.roundToInt()
                Label(
                    if (selisih <= 0) stringResource(R.string.nav_arah_jam_turun, arah.arahJam, Format.ribuan(-selisih))
                    else stringResource(R.string.nav_arah_jam_naik, arah.arahJam, Format.ribuan(selisih)),
                    warna = Warna.Teks,
                )
                Spacer(Modifier.height(6.dp))
                Label("${stringResource(R.string.nav_jejak)} · ${stringResource(R.string.nav_basecamp_jarak, Format.jarak(arah.jarakKeBasecampM))}")
            }
        }
    }
}

/** Jarak ke basecamp mengikuti jejak: pendekatan dari jarak total yang sudah direkam jika jejak tidak dimuat. */
private fun jarakBasecamp(st: id.asrul.pendaki.domain.HikeState, lat: Double, lon: Double): Double {
    val basecamp = st.waypoint.firstOrNull { it.jenis == id.asrul.pendaki.shared.model.JenisWaypoint.BASECAMP }
    val lurus = basecamp?.let { id.asrul.pendaki.shared.geo.Geo.jarakM(lat, lon, it.lat, it.lon) } ?: 0.0
    // Saat turun, jarak sepanjang jejak ≈ jarak naik yang terekam; sebelum puncak ≈ jarak rekaman total.
    return if (st.waktuPuncak == null) st.jarakM.coerceAtLeast(lurus) else (st.jarakM / 2).coerceAtLeast(lurus)
}

@Composable
private fun Panah(relatifDeg: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        rotate(relatifDeg) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w / 2, 0f)
                lineTo(w * 0.88f, h * 0.78f)
                lineTo(w / 2, h * 0.6f)
                lineTo(w * 0.12f, h * 0.78f)
                close()
            }
            drawPath(path, Warna.Oranye)
        }
    }
}
