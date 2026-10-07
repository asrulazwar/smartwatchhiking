package id.asrul.pendaki.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import id.asrul.pendaki.R
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.ui.komponen.AngkaBesar
import id.asrul.pendaki.ui.komponen.GrafikGaris
import id.asrul.pendaki.ui.komponen.KolomTengah
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LabelKecil
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.komponen.TombolAksi
import id.asrul.pendaki.ui.theme.Warna

/** Detak jantung & SpO2 (mockup 4). */
@Composable
fun KondisiScreen(vm: HikeViewModel, onUkur: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    LayarDasar {
        Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                item { Label(stringResource(R.string.kondisi_judul, Format.ribuan(st.ketinggianM ?: 0.0))) }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Warna.Merah, modifier = Modifier.size(14.dp))
                            LabelKecil(stringResource(R.string.kondisi_detak), warna = Warna.Merah)
                        }
                        AngkaBesar(st.hr?.toString() ?: "—", style = MaterialTheme.typography.display2)
                        Label(stringResource(R.string.kondisi_zona, st.zona))
                        GrafikGaris(st.riwayatHr.map { it.second }, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
                    }
                }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LabelKecil(stringResource(R.string.utama_spo2), warna = Warna.BiruSpO2)
                        Row(verticalAlignment = Alignment.Bottom) {
                            AngkaBesar(st.spo2?.nilai?.toString() ?: "—", warna = Warna.BiruSpO2, style = MaterialTheme.typography.display2)
                            Text(stringResource(R.string.satuan_persen), color = Warna.BiruSpO2, style = MaterialTheme.typography.title2, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Label(st.spo2?.let { Format.relatif(it.waktu, st.sekarang) + if (it.meragukan) " · ${stringResource(R.string.spo2_meragukan)}" else "" } ?: stringResource(R.string.kondisi_spo2_belum))
                    }
                }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(stringResource(R.string.kondisi_istirahat_tadi), warna = Warna.Teks)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(st.hrIstirahat?.toString() ?: "—", color = Warna.Teks, style = MaterialTheme.typography.display3)
                            Text(st.hrIstirahatBasecamp?.let { stringResource(R.string.kondisi_biasanya, it) } ?: stringResource(R.string.bpm), color = Warna.Sekunder, style = MaterialTheme.typography.caption1, modifier = Modifier.padding(bottom = 5.dp))
                        }
                        st.hrRata?.let { Label(stringResource(R.string.kondisi_rata_sesi, it)) }
                    }
                }
                item { TombolAksi(stringResource(R.string.kondisi_ukur_spo2), onClick = onUkur, modifier = Modifier.padding(top = 4.dp), warnaLatar = Warna.BiruSpO2) }
            }
        }
    }
}

/** Pengukuran SpO2 30 detik dengan hitung mundur. */
@Composable
fun UkurSpO2Screen(vm: HikeViewModel, onSelesai: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.ukurSpO2() }
    val p = st.pengukuranSpO2
    LayarDasar {
        KolomTengah(spasi = 8.dp) {
            LabelKecil(stringResource(R.string.utama_spo2), warna = Warna.BiruSpO2)
            if (p != null) {
                CircularProgressIndicator(progress = 1f - p.sisaDetik / 30f, modifier = Modifier.size(96.dp), indicatorColor = Warna.BiruSpO2, trackColor = Warna.Permukaan, strokeWidth = 6.dp)
                Text(stringResource(R.string.spo2_mengukur, p.sisaDetik), color = Warna.Teks, style = MaterialTheme.typography.title1)
                p.nilaiSementara?.let { Label("$it%", warna = Warna.BiruSpO2) }
            } else {
                val s = st.spo2
                if (s != null && st.sekarang - s.waktu < 2 * 60_000L) {
                    AngkaBesar("${s.nilai}%", warna = Warna.BiruSpO2)
                    if (s.meragukan) Label(stringResource(R.string.spo2_meragukan), warna = Warna.Oranye)
                    st.spo2Sumber?.let { Label(it) }
                } else {
                    Text(stringResource(R.string.spo2_gagal), color = Warna.Sekunder, style = MaterialTheme.typography.body2, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(4.dp))
                TombolAksi(stringResource(R.string.kembali), onClick = onSelesai, warnaLatar = Warna.PermukaanTerang, warnaTeks = Warna.Teks)
            }
        }
    }
}
