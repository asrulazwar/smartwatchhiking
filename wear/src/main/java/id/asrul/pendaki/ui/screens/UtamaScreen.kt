package id.asrul.pendaki.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import id.asrul.pendaki.R
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.ui.komponen.AngkaBesar
import id.asrul.pendaki.ui.komponen.ChipNilai
import id.asrul.pendaki.ui.komponen.CincinProgres
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.theme.Warna

/** Layar utama: altimeter & progres puncak (mockup 1). Ketuk di mana saja → menu. */
@Composable
fun UtamaScreen(vm: HikeViewModel, ambient: Boolean, onMenu: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val g = st.gunung

    if (ambient) {
        // Versi hemat always-on: hanya ketinggian & sisa naik, tanpa animasi/warna.
        LayarDasar {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AngkaBesar(st.ketinggianM?.let { Format.ribuan(it) } ?: "—", warna = Warna.Sekunder)
                Label(stringResource(R.string.mdpl), warna = Warna.Sekunder)
                Spacer(Modifier.height(8.dp))
                Text(st.sisaNaikM?.let { "${Format.ribuan(it.coerceAtLeast(0.0))} m ${stringResource(R.string.utama_lagi)}" } ?: "", color = Warna.Sekunder, style = MaterialTheme.typography.title2)
            }
        }
        return
    }

    LayarDasar(modifier = Modifier.clickable(onClick = onMenu)) {
        CincinProgres(progres = st.progres, tebal = 4.dp, mulaiDeg = 120f, sapuanDeg = 300f)
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(Format.jam(st.sekarang), color = Warna.Sekunder, style = MaterialTheme.typography.caption2)
            Text(
                g?.let { if (st.jalur != null) stringResource(R.string.utama_gunung_jalur, it.nama, st.jalur!!.namaJalur) else stringResource(R.string.utama_gunung, it.nama) } ?: "",
                color = Warna.Teks, style = MaterialTheme.typography.caption1, textAlign = TextAlign.Center, maxLines = 1,
            )
            // Ketinggian: barometer terkalibrasi; sebelum ada, pakai bacaan mentah/GPS dengan keterangan.
            val alt = st.ketinggianM ?: st.altGps
            AngkaBesar(alt?.let { Format.ribuan(it) } ?: "—", style = MaterialTheme.typography.display2)
            Label(
                when {
                    alt == null && st.barometerAda -> stringResource(R.string.utama_menunggu_baro)
                    alt == null -> stringResource(R.string.utama_menunggu_gps)
                    !st.kalibrasiSiap -> stringResource(R.string.utama_belum_kalibrasi)
                    else -> "${stringResource(R.string.mdpl)} · ${stringResource(R.string.utama_puncak, Format.ribuan(g?.elevasi ?: 0))}"
                },
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, tint = Warna.Oranye, modifier = Modifier.size(16.dp))
                Text(st.sisaNaikM?.let { "${Format.ribuan(it.coerceAtLeast(0.0))} m" } ?: "— m", color = Warna.Oranye, style = MaterialTheme.typography.title2)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.utama_lagi), color = Warna.Sekunder, style = MaterialTheme.typography.caption1)
            }
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Kolom(nilai = Format.jarak(st.jarakM), label = stringResource(R.string.utama_jarak))
                Kolom(nilai = Format.ribuan(st.langkah), label = stringResource(R.string.utama_langkah))
                Kolom(nilai = st.kecepatanNaikMPerJam?.let { Format.ribuan(it) } ?: "—", label = stringResource(R.string.utama_m_per_jam))
                Kolom(nilai = st.perkiraanTiba?.let { Format.jam(it) } ?: "—", label = stringResource(R.string.utama_tiba))
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ChipNilai(ikon = { Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Warna.Merah, modifier = Modifier.size(14.dp)) }, nilai = st.hr?.toString() ?: "—")
                ChipNilai(ikon = { Text(stringResource(R.string.utama_spo2), color = Warna.BiruSpO2, style = MaterialTheme.typography.caption2) }, nilai = st.spo2?.let { "${it.nilai}%" } ?: "—", warna = Warna.BiruSpO2)
            }
        }
    }
}

@Composable
private fun Kolom(nilai: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(nilai, color = Warna.Teks, style = MaterialTheme.typography.title3, maxLines = 1)
        Text(label, color = Warna.Sekunder, style = MaterialTheme.typography.caption3)
    }
}
