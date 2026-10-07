package id.asrul.pendaki.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import id.asrul.pendaki.R
import id.asrul.pendaki.domain.StatusKirim
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.ui.komponen.DuaTombol
import id.asrul.pendaki.ui.komponen.KolomTengah
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LabelKecil
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.komponen.TombolAksi
import id.asrul.pendaki.ui.komponen.TombolSekunder
import id.asrul.pendaki.ui.theme.Warna

@Composable
fun KonfirmasiSelesaiScreen(vm: HikeViewModel, onBatal: () -> Unit, onSelesai: () -> Unit) {
    LayarDasar {
        KolomTengah(spasi = 12.dp) {
            Text(stringResource(R.string.selesai_konfirmasi), color = Warna.Teks, style = MaterialTheme.typography.title1, textAlign = TextAlign.Center)
            DuaTombol(
                kiri = stringResource(R.string.batal), onKiri = onBatal,
                kanan = stringResource(R.string.selesai_akhiri), onKanan = { vm.selesai(); onSelesai() },
                warnaKanan = Warna.Merah, warnaTeksKanan = Warna.Teks,
            )
        }
    }
}

/** Layar Selesai (mockup 12): ringkasan dan "Kirim ke HP & ekspor". Bisa digulir agar tidak terpotong. */
@Composable
fun SelesaiScreen(vm: HikeViewModel, onTutup: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    LayarDasar {
        Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                item { Label(stringResource(R.string.selesai_judul), warna = Warna.Hijau) }
                item { Text(st.judul.replace(" · ", " "), color = Warna.Teks, style = MaterialTheme.typography.title2, textAlign = TextAlign.Center, maxLines = 2) }
                item { BarisStat(stringResource(R.string.selesai_waktu), Format.durasi(st.durasiMs), stringResource(R.string.selesai_jarak), Format.jarak(st.jarakM)) }
                item { BarisStat(stringResource(R.string.selesai_naik), "${Format.ribuan(st.naikTotalM)} m", stringResource(R.string.selesai_detak), st.hrRata?.toString() ?: "—") }
                item { BarisStat(stringResource(R.string.selesai_langkah), Format.ribuan(st.langkah), stringResource(R.string.selesai_pos), st.jumlahPosTercatat.toString()) }
                item { Label(stringResource(R.string.selesai_titik_pos, Format.ribuan(st.jumlahTitik), st.jumlahPosTercatat)) }
                item {
                    when (st.statusKirim) {
                        StatusKirim.BELUM -> TombolAksi(stringResource(R.string.selesai_kirim), onClick = { vm.kirimKeHp() }, modifier = Modifier.padding(top = 4.dp))
                        StatusKirim.MENGIRIM -> TombolSekunder(stringResource(R.string.selesai_mengirim), onClick = {})
                        StatusKirim.TERKIRIM -> Label(stringResource(R.string.selesai_terkirim), warna = Warna.Hijau)
                        StatusKirim.ANTRE -> Label(stringResource(R.string.selesai_antre), warna = Warna.Oranye)
                    }
                }
                item { TombolSekunder(stringResource(R.string.selesai_mulai_baru), onClick = onTutup) }
            }
        }
    }
}

@Composable
private fun BarisStat(label1: String, nilai1: String, label2: String, nilai2: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        Statistik(label1, nilai1)
        Statistik(label2, nilai2)
    }
}

@Composable
private fun Statistik(label: String, nilai: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 2.dp)) {
        LabelKecil(label)
        Text(nilai, color = Warna.Teks, style = MaterialTheme.typography.title1, maxLines = 1)
    }
}
