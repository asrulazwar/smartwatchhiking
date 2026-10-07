package id.asrul.pendaki.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import id.asrul.pendaki.R
import id.asrul.pendaki.data.prefs.Pengaturan
import id.asrul.pendaki.data.prefs.PengaturanData
import id.asrul.pendaki.shared.heart.HrThresholdMonitor
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.nav.Rute
import id.asrul.pendaki.ui.theme.Warna
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PengaturanViewModel @Inject constructor(private val pengaturan: Pengaturan) : ViewModel() {
    val data: StateFlow<PengaturanData> = pengaturan.data.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PengaturanData())
    fun batas(v: Int) = viewModelScope.launch { pengaturan.setBatasDetak(v) }
    fun usia(v: Int) = viewModelScope.launch { pengaturan.setUsia(v) }
    fun ambang(v: Int) = viewModelScope.launch { pengaturan.setAmbangSpo2(v) }
    fun gpsHemat(v: Boolean) = viewModelScope.launch { pengaturan.setGpsHemat(v) }
    fun minum(v: Int) = viewModelScope.launch { pengaturan.setPengingatMinum(v) }
    fun getar(v: Boolean) = viewModelScope.launch { pengaturan.setGetar(v) }
}

@Composable
fun PengaturanScreen(onRute: (String) -> Unit, vm: PengaturanViewModel = hiltViewModel()) {
    val d by vm.data.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    LayarDasar {
        Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                item { Label(stringResource(R.string.set_judul)) }
                item { BarisPengaturan(stringResource(R.string.set_batas_detak), stringResource(R.string.set_nilai_bpm, d.batasDetak)) { onRute(Rute.BATAS_DETAK) } }
                item { BarisPengaturan(stringResource(R.string.set_usia), stringResource(R.string.set_nilai_tahun, d.usia)) { onRute(Rute.USIA) } }
                item { BarisPengaturan(stringResource(R.string.set_ambang_spo2), stringResource(R.string.set_nilai_persen, d.ambangSpo2)) { onRute(Rute.AMBANG_SPO2) } }
                item {
                    ToggleChip(
                        checked = d.gpsHemat, onCheckedChange = { vm.gpsHemat(it) }, modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.set_interval_gps)) },
                        secondaryLabel = { Text(if (d.gpsHemat) stringResource(R.string.set_gps_hemat) else stringResource(R.string.set_gps_normal), color = Warna.Sekunder) },
                        toggleControl = { Switch(checked = d.gpsHemat) },
                        colors = ToggleChipDefaults.toggleChipColors(checkedStartBackgroundColor = Warna.Permukaan, checkedEndBackgroundColor = Warna.Permukaan, uncheckedStartBackgroundColor = Warna.Permukaan, uncheckedEndBackgroundColor = Warna.Permukaan),
                    )
                }
                item { BarisPengaturan(stringResource(R.string.set_minum), if (d.pengingatMinumMenit == 0) stringResource(R.string.set_minum_mati) else stringResource(R.string.set_minum_menit, d.pengingatMinumMenit)) { onRute(Rute.MINUM) } }
                item {
                    ToggleChip(
                        checked = d.getar, onCheckedChange = { vm.getar(it) }, modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.set_getar)) },
                        toggleControl = { Switch(checked = d.getar) },
                        colors = ToggleChipDefaults.toggleChipColors(checkedStartBackgroundColor = Warna.Permukaan, checkedEndBackgroundColor = Warna.Permukaan, uncheckedStartBackgroundColor = Warna.Permukaan, uncheckedEndBackgroundColor = Warna.Permukaan),
                    )
                }
                item { BarisPengaturan(stringResource(R.string.set_debug), "") { onRute(Rute.DEBUG) } }
            }
        }
    }
}

@Composable
private fun BarisPengaturan(label: String, nilai: String, onClick: () -> Unit) {
    Chip(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = ChipDefaults.chipColors(backgroundColor = Warna.Permukaan, contentColor = Warna.Teks, secondaryContentColor = Warna.Oranye),
        label = { Text(label, maxLines = 1) },
        secondaryLabel = if (nilai.isNotEmpty()) ({ Text(nilai) }) else null,
    )
}

/** Atur batas detak (mockup 9). */
@Composable
fun BatasDetakScreen(vm: PengaturanViewModel = hiltViewModel()) {
    val d by vm.data.collectAsStateWithLifecycle()
    PengaturAngka(
        judul = stringResource(R.string.batas_judul), sub = stringResource(R.string.batas_sub),
        nilai = d.batasDetak, min = HrThresholdMonitor.MIN_BATAS, max = HrThresholdMonitor.MAX_BATAS, langkah = 1,
        satuan = stringResource(R.string.bpm), keterangan = stringResource(R.string.batas_keterangan), onUbah = { vm.batas(it) },
        preset = listOf(
            stringResource(R.string.batas_santai) to HrThresholdMonitor.PRESET_SANTAI,
            stringResource(R.string.batas_normal) to HrThresholdMonitor.PRESET_NORMAL,
            stringResource(R.string.batas_kuat) to HrThresholdMonitor.PRESET_KUAT,
        ),
    )
}

@Composable
fun UsiaScreen(vm: PengaturanViewModel = hiltViewModel()) {
    val d by vm.data.collectAsStateWithLifecycle()
    PengaturAngka(stringResource(R.string.set_judul), stringResource(R.string.set_usia), d.usia, 10, 100, 1, stringResource(R.string.set_nilai_tahun, 0).replace("0 ", ""), null, { vm.usia(it) })
}

@Composable
fun AmbangSpO2Screen(vm: PengaturanViewModel = hiltViewModel()) {
    val d by vm.data.collectAsStateWithLifecycle()
    PengaturAngka(stringResource(R.string.set_judul), stringResource(R.string.set_ambang_spo2), d.ambangSpo2, 70, 95, 1, stringResource(R.string.satuan_persen), null, { vm.ambang(it) })
}

@Composable
fun MinumScreen(vm: PengaturanViewModel = hiltViewModel()) {
    val d by vm.data.collectAsStateWithLifecycle()
    PengaturAngka(stringResource(R.string.set_judul), stringResource(R.string.set_minum), d.pengingatMinumMenit, 0, 120, 5, "menit (0 = mati)", null, { vm.minum(it) })
}
