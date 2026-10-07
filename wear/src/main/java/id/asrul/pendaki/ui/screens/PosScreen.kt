package id.asrul.pendaki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.wear.compose.material.TimeText
import id.asrul.pendaki.R
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.shared.model.JenisWaypoint
import id.asrul.pendaki.shared.pos.PresetNamaPos
import id.asrul.pendaki.ui.komponen.DuaTombol
import id.asrul.pendaki.ui.komponen.KolomTengah
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.komponen.TombolAksi
import id.asrul.pendaki.ui.komponen.TombolSekunder
import id.asrul.pendaki.ui.theme.Warna

/** Catat pos (mockup 2): daftar pos dengan status sudah/sekarang/berikutnya + tombol besar. */
@Composable
fun PosScreen(vm: HikeViewModel, onNamaManual: () -> Unit, onSelesai: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    val adaData = st.jalur != null && st.daftarPos.isNotEmpty()
    val tercatat = st.jumlahPosTercatat
    LayarDasar {
        Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        if (adaData) stringResource(R.string.pos_judul, tercatat, st.daftarPos.size) else stringResource(R.string.pos_judul_manual, tercatat),
                        color = Warna.Sekunder, style = MaterialTheme.typography.caption1, textAlign = TextAlign.Center,
                    )
                }
                // basecamp
                st.waypoint.firstOrNull { it.jenis == JenisWaypoint.BASECAMP }?.let { b ->
                    item { BarisPos(nama = stringResource(R.string.pos_basecamp), keterangan = "${Format.jam(b.waktu)} · ${Format.ribuan(b.alt)}", status = StatusBaris.SUDAH) }
                }
                if (adaData) {
                    items(st.daftarPos.size) { i ->
                        val p = st.daftarPos[i]
                        val berikut = st.posBerikutnya?.indeks == i
                        BarisPos(
                            nama = p.pos.nama,
                            keterangan = p.waypoint?.let { "${Format.jam(it.waktu)} · ${Format.ribuan(it.alt)}" }
                                ?: p.pos.elevasiPerkiraan?.let { stringResource(R.string.pos_perkiraan, Format.ribuan(it)) } ?: "",
                            status = when { p.tercatat -> StatusBaris.SUDAH; berikut -> StatusBaris.SEKARANG; else -> StatusBaris.BERIKUTNYA },
                        )
                    }
                } else {
                    val pos = st.waypoint.filter { it.jenis == JenisWaypoint.POS }
                    items(pos.size) { i -> BarisPos(pos[i].nama, "${Format.jam(pos[i].waktu)} · ${Format.ribuan(pos[i].alt)}", StatusBaris.SUDAH) }
                }
                st.waypoint.firstOrNull { it.jenis == JenisWaypoint.PUNCAK }?.let { p ->
                    item { BarisPos(stringResource(R.string.pos_puncak), "${Format.jam(p.waktu)} · ${Format.ribuan(p.alt)}", StatusBaris.SUDAH) }
                }
                item {
                    val berikut = st.posBerikutnya
                    if (adaData && berikut != null) {
                        TombolAksi(stringResource(R.string.pos_catat_n, berikut.pos.nama), onClick = { vm.catatPos(); onSelesai() }, modifier = Modifier.padding(top = 6.dp), tinggi = 52.dp)
                    } else {
                        TombolAksi(stringResource(R.string.pos_catat), onClick = { vm.catatPos(); onSelesai() }, modifier = Modifier.padding(top = 6.dp), tinggi = 52.dp)
                    }
                }
                item { TombolSekunder(stringResource(R.string.pos_pilih_nama), onClick = onNamaManual) }
            }
        }
    }
}

private enum class StatusBaris { SUDAH, SEKARANG, BERIKUTNYA }

@Composable
private fun BarisPos(nama: String, keterangan: String, status: StatusBaris) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(10.dp).background(
                when (status) { StatusBaris.SUDAH -> Warna.Hijau; StatusBaris.SEKARANG -> Warna.Oranye; StatusBaris.BERIKUTNYA -> Warna.PermukaanTerang },
                CircleShape,
            )
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(nama, color = if (status == StatusBaris.BERIKUTNYA) Warna.Sekunder else Warna.Teks, style = MaterialTheme.typography.body1, maxLines = 1)
            Text(keterangan, color = Warna.Sekunder, style = MaterialTheme.typography.caption2, maxLines = 1)
        }
    }
}

/** Pilih nama preset untuk pos manual. */
@Composable
fun NamaPosScreen(vm: HikeViewModel, onSelesai: () -> Unit) {
    val listState = rememberScalingLazyListState()
    LayarDasar {
        Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                item { Label(stringResource(R.string.pos_pilih_nama)) }
                items(PresetNamaPos.daftar.size) { i ->
                    val nama = PresetNamaPos.daftar[i]
                    TombolSekunder(nama, onClick = { vm.catatPos(nama); onSelesai() })
                }
            }
        }
    }
}

/** "Sampai Pos 3?" dari pendakian sebelumnya di jalur yang sama. */
@Composable
fun TawaranPosScreen(vm: HikeViewModel, onTutup: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val w = st.tawaranPos
    LayarDasar {
        KolomTengah(spasi = 10.dp) {
            Text(stringResource(R.string.pos_tawaran, w?.nama ?: ""), color = Warna.Teks, style = MaterialTheme.typography.title1, textAlign = TextAlign.Center)
            w?.let { Label("${Format.ribuan(it.alt)} ${stringResource(R.string.mdpl)}") }
            DuaTombol(
                kiri = stringResource(R.string.tidak), onKiri = { vm.tolakTawaranPos(); onTutup() },
                kanan = stringResource(R.string.ya), onKanan = { vm.terimaTawaranPos(); onTutup() },
            )
        }
    }
}
