package id.asrul.pendaki.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import id.asrul.pendaki.R
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.nav.Rute
import id.asrul.pendaki.ui.theme.Warna

@Composable
fun MenuScreen(vm: HikeViewModel, onRute: (String) -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    val item = listOf(
        R.string.menu_pos to Rute.POS,
        R.string.menu_navigasi to Rute.NAVIGASI,
        R.string.menu_kondisi to Rute.KONDISI,
        R.string.menu_stres to Rute.STRES,
        R.string.menu_riwayat to Rute.RIWAYAT,
        R.string.menu_pengaturan to Rute.PENGATURAN,
        R.string.menu_selesai to Rute.KONFIRMASI_SELESAI,
    )
    LayarDasar {
        Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                items(item.size) { i ->
                    val (label, rute) = item[i]
                    val selesai = rute == Rute.KONFIRMASI_SELESAI
                    Chip(
                        onClick = { onRute(rute) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (selesai) Warna.PermukaanTerang else Warna.Permukaan,
                            contentColor = if (selesai) Warna.Merah else Warna.Teks,
                        ),
                        label = { Text(stringResource(label)) },
                        secondaryLabel = if (rute == Rute.POS && st.posBerikutnya != null) ({ Text(st.posBerikutnya!!.pos.nama, color = Warna.Sekunder) }) else null,
                    )
                }
            }
        }
    }
}
