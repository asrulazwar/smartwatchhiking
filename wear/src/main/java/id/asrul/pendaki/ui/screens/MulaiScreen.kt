package id.asrul.pendaki.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import id.asrul.pendaki.R
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.shared.geo.GunungTerdekat
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LabelKecil
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.komponen.TombolAksi
import id.asrul.pendaki.ui.komponen.TombolSekunder
import id.asrul.pendaki.ui.theme.Warna

/** Layar Mulai: satu fix GPS → maksimal 3 gunung terdekat; fallback daftar jika GPS gagal 20 detik. */
@Composable
fun MulaiScreen(
    onMulai: () -> Unit,
    onCariLain: () -> Unit,
    onPilihJalur: (String) -> Unit,
    hike: HikeViewModel,
    vm: MulaiViewModel = hiltViewModel(),
) {
    val st by vm.state.collectAsStateWithLifecycle()
    val hikeState by hike.state.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

    LayarDasar {
        Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                if (hikeState.sedangAktif) {
                    item { Label(stringResource(R.string.mulai_sedang_merekam), warna = Warna.Oranye) }
                    item { TombolAksi(stringResource(R.string.mulai_lanjutkan), onClick = onMulai) }
                    return@ScalingLazyColumn
                }
                item {
                    when {
                        st.mencariGps -> Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), indicatorColor = Warna.Oranye, trackColor = Warna.Permukaan, strokeWidth = 3.dp)
                            Label(stringResource(R.string.mulai_mencari_gps), modifier = Modifier.padding(top = 6.dp))
                        }
                        st.gpsTerkunci -> Label(stringResource(R.string.mulai_gps_terkunci, Format.ribuan(st.altGps ?: 0.0)), warna = Warna.Hijau)
                        else -> Label(stringResource(R.string.mulai_tidak_ada_gps), warna = Warna.Sekunder)
                    }
                }
                if (st.terdekat.isNotEmpty()) {
                    item { LabelKecil(stringResource(R.string.mulai_dekat)) }
                    items(st.terdekat.size) { i -> KartuGunung(st.terdekat[i], utama = i == 0, onClick = { onPilihJalur(st.terdekat[i].gunung.id) }) }
                }
                if (!st.mencariGps) {
                    item {
                        TombolSekunder(stringResource(R.string.mulai_cari_lain), onClick = onCariLain)
                    }
                }
            }
        }
    }
}

@Composable
private fun KartuGunung(g: GunungTerdekat, utama: Boolean, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ChipDefaults.chipColors(
            backgroundColor = if (utama) Warna.Oranye else Warna.Permukaan,
            contentColor = if (utama) Warna.Hitam else Warna.Teks,
            secondaryContentColor = if (utama) Warna.Hitam.copy(alpha = 0.7f) else Warna.Sekunder,
        ),
        label = { Text("Gn. ${g.gunung.nama}", style = MaterialTheme.typography.title2, maxLines = 1) },
        secondaryLabel = { Text(stringResource(R.string.mulai_puncak_jarak, Format.ribuan(g.gunung.elevasi), Format.jarak(g.jarakM)), maxLines = 1) },
    )
}

@Composable
fun PilihGunungScreen(onPilih: (String) -> Unit, vm: MulaiViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    LayarDasar {
        Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                item {
                    Chip(
                        onClick = { /* pencarian lewat rotary: daftar diurutkan abjad, gunung terakhir di atas */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ChipDefaults.secondaryChipColors(),
                        icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        label = { Text(stringResource(R.string.mulai_semua), style = MaterialTheme.typography.caption1) },
                    )
                }
                st.gunungTerakhir?.let { item { LabelKecil(stringResource(R.string.mulai_terakhir)) } }
                items(st.semua.size) { i ->
                    val g = st.semua[i]
                    Chip(
                        onClick = { onPilih(g.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ChipDefaults.chipColors(backgroundColor = if (g.id == st.gunungTerakhir?.id) Warna.PermukaanTerang else Warna.Permukaan, contentColor = Warna.Teks, secondaryContentColor = Warna.Sekunder),
                        label = { Text("Gn. ${g.nama}", maxLines = 1) },
                        secondaryLabel = { Text("${Format.ribuan(g.elevasi)} mdpl · ${g.provinsi}", maxLines = 1) },
                    )
                }
            }
        }
    }
}

@Composable
fun PilihJalurScreen(gunungId: String, onMulai: () -> Unit, hike: HikeViewModel, vm: MulaiViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(gunungId) { vm.pilihGunung(gunungId) }
    val g = st.gunungDipilih
    val listState = rememberScalingLazyListState()
    LayarDasar {
        Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                item { Text(g?.let { "Gn. ${it.nama}" } ?: "", style = MaterialTheme.typography.title2, color = Warna.Oranye, textAlign = TextAlign.Center) }
                item { LabelKecil(stringResource(R.string.mulai_pilih_jalur)) }
                items(st.jalur.size) { i ->
                    val j = st.jalur[i]
                    Chip(
                        onClick = { if (g != null) { hike.mulai(g, j); onMulai() } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ChipDefaults.chipColors(backgroundColor = Warna.Oranye, contentColor = Warna.Hitam, secondaryContentColor = Warna.Hitam.copy(alpha = 0.7f)),
                        label = { Text("via ${j.namaJalur}", maxLines = 1) },
                        secondaryLabel = { Text(stringResource(R.string.mulai_basecamp, Format.ribuan(j.basecampElevasi), j.pos.size), maxLines = 1) },
                    )
                }
                item {
                    TombolSekunder(stringResource(R.string.mulai_jalur_tanpa_data), onClick = { if (g != null) { hike.mulai(g, null); onMulai() } })
                }
            }
        }
    }
}
