package id.asrul.pendaki.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import id.asrul.pendaki.R
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.shared.stress.StressCalculator
import id.asrul.pendaki.ui.komponen.AngkaBesar
import id.asrul.pendaki.ui.komponen.CincinProgres
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.theme.Warna

/**
 * Dasbor utama (mockup 1). Semua isi dijaga di dalam cincin progres: lebar konten dibatasi
 * agar tidak menabrak garis di tepi layar bulat. Ketuk di mana saja → menu.
 */
@Composable
fun UtamaScreen(vm: HikeViewModel, ambient: Boolean, onMenu: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val g = st.gunung
    val alt = st.ketinggianM ?: st.altGps

    if (ambient) {
        // Versi hemat always-on: hanya ketinggian & sisa naik, tanpa animasi/warna.
        LayarDasar {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AngkaBesar(alt?.let { Format.ribuan(it) } ?: "—", warna = Warna.Sekunder)
                Label(stringResource(R.string.mdpl), warna = Warna.Sekunder)
                Spacer(Modifier.height(8.dp))
                Text(st.sisaNaikM?.let { "${Format.ribuan(it.coerceAtLeast(0.0))} m ${stringResource(R.string.utama_lagi)}" } ?: "", color = Warna.Sekunder, style = MaterialTheme.typography.title2)
            }
        }
        return
    }

    LayarDasar(modifier = Modifier.clickable(onClick = onMenu)) {
        CincinProgres(progres = st.progres, tebal = 3.dp, mulaiDeg = 120f, sapuanDeg = 300f)
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(Format.jam(st.sekarang), color = Warna.Sekunder, style = MaterialTheme.typography.caption3)
            val judul = g?.let { if (st.jalur != null) stringResource(R.string.utama_gunung_jalur, it.nama, st.jalur!!.namaJalur) else stringResource(R.string.utama_gunung, it.nama) } ?: ""
            val pos = when {
                st.daftarPos.isNotEmpty() -> " · ${stringResource(R.string.utama_pos_n, st.jumlahPosTercatat, st.daftarPos.size)}"
                st.jumlahPosTercatat > 0 -> " · ${stringResource(R.string.utama_pos, st.jumlahPosTercatat)}"
                else -> ""
            }
            Text(judul + pos, color = Warna.Teks, style = MaterialTheme.typography.caption1, textAlign = TextAlign.Center, maxLines = 1)

            AngkaBesar(alt?.let { Format.ribuan(it) } ?: "—", style = MaterialTheme.typography.display2)
            Label(
                when {
                    alt == null && st.barometerAda -> stringResource(R.string.utama_menunggu_baro)
                    alt == null -> stringResource(R.string.utama_menunggu_gps)
                    !st.kalibrasiSiap -> stringResource(R.string.utama_belum_kalibrasi)
                    else -> "${stringResource(R.string.mdpl)} · ${stringResource(R.string.utama_puncak, Format.ribuan(g?.elevasi ?: 0))}"
                },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, tint = Warna.Oranye, modifier = Modifier.size(16.dp))
                Text(st.sisaNaikM?.let { "${Format.ribuan(it.coerceAtLeast(0.0))} m" } ?: "— m", color = Warna.Oranye, style = MaterialTheme.typography.title2)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.utama_lagi), color = Warna.Sekunder, style = MaterialTheme.typography.caption2)
            }
            Spacer(Modifier.height(5.dp))

            // Kondisi tubuh: detak · SpO2 · stres
            val stres = st.stres
            val warnaStres = stres?.let {
                when (StressCalculator.label(it.nilai)) {
                    StressCalculator.Label.RENDAH -> Warna.Hijau
                    StressCalculator.Label.SEDANG -> Warna.Oranye
                    StressCalculator.Label.TINGGI -> Warna.Merah
                }
            } ?: Warna.Sekunder
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                Nilai(ikon = { Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Warna.Merah, modifier = Modifier.size(11.dp)) }, nilai = st.hr?.toString() ?: "—", label = stringResource(R.string.bpm), warna = Warna.Merah)
                Nilai(nilai = st.spo2?.let { "${it.nilai}%" } ?: "—", label = stringResource(R.string.utama_spo2), warna = Warna.BiruSpO2)
                Nilai(nilai = stres?.nilai?.toString() ?: "—", label = stringResource(R.string.utama_stres), warna = warnaStres)
            }
            Spacer(Modifier.height(4.dp))

            // Perjalanan: jarak · langkah, lalu kecepatan naik · perkiraan tiba
            Text("${Format.jarak(st.jarakM)} · ${Format.ribuan(st.langkah)} ${stringResource(R.string.utama_langkah)}", color = Warna.Sekunder, style = MaterialTheme.typography.caption2, maxLines = 1)
            Text(
                "${st.kecepatanNaikMPerJam?.let { Format.ribuan(it) } ?: "—"} ${stringResource(R.string.utama_m_per_jam)} · " +
                    stringResource(R.string.utama_tiba_jam, st.perkiraanTiba?.let { Format.jam(it) } ?: "—"),
                color = Warna.Sekunder, style = MaterialTheme.typography.caption2, maxLines = 1,
            )
        }
    }
}

@Composable
private fun Nilai(nilai: String, label: String, warna: Color, ikon: (@Composable () -> Unit)? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ikon?.let { it(); Spacer(Modifier.width(2.dp)) }
            Text(nilai, color = warna, style = MaterialTheme.typography.title3, maxLines = 1)
        }
        Text(label, color = Warna.Sekunder, style = MaterialTheme.typography.caption3)
    }
}
