package id.asrul.pendaki.ui.screens

import android.hardware.SensorManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import id.asrul.pendaki.R
import id.asrul.pendaki.data.datalayer.PhoneLink
import id.asrul.pendaki.data.health.HealthServicesManager
import id.asrul.pendaki.data.sensor.SensorManagerSpO2Source
import id.asrul.pendaki.data.sensor.SensorManagerStressSource
import id.asrul.pendaki.data.sensor.daftarSensor
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LabelKecil
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.theme.Warna
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class DebugState(
    val sensor: List<String> = emptyList(),
    val spo2: String = "—",
    val hrv: String = "—",
    val hs: String = "—",
    val hsStatus: String = "—",
    val hp: String? = null,
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val sm: SensorManager,
    private val spo2: SensorManagerSpO2Source,
    private val hrv: SensorManagerStressSource,
    private val health: HealthServicesManager,
    private val phone: PhoneLink,
) : ViewModel() {
    private val _state = MutableStateFlow(DebugState())
    val state: StateFlow<DebugState> = _state

    init {
        viewModelScope.launch {
            val daftar = sm.daftarSensor()
            // Log semua sensor ke Logcat (hanya debug build yang menanam DebugTree).
            daftar.forEach { Timber.i("Sensor: %s", it) }
            _state.value = DebugState(
                sensor = daftar,
                spo2 = spo2.sensor?.name ?: "tidak ditemukan",
                hrv = hrv.sensor?.name ?: "tidak ditemukan",
                hs = health.kapabilitas(),
                hsStatus = health.status.value,
                hp = phone.namaNodeHp(),
            )
        }
    }
}

@Composable
fun DebugScreen(vm: DebugViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    LayarDasar {
        Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                item { Label(stringResource(R.string.debug_judul)) }
                item { LabelKecil(stringResource(R.string.debug_hp)) }
                item { Label(st.hp?.let { stringResource(R.string.debug_hp_terhubung, it) } ?: stringResource(R.string.debug_hp_tidak), warna = Warna.Teks) }
                item { LabelKecil(stringResource(R.string.debug_hs)) }
                item { Text(st.hs, color = Warna.Teks, style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center) }
                item { Label(st.hsStatus) }
                item { Label(stringResource(R.string.debug_spo2_sensor, st.spo2), warna = Warna.Teks) }
                item { Label(stringResource(R.string.debug_hrv_sensor, st.hrv), warna = Warna.Teks) }
                item { LabelKecil(stringResource(R.string.debug_sensor)) }
                items(st.sensor.size) { i -> Text(st.sensor[i], color = Warna.Sekunder, style = MaterialTheme.typography.caption3, textAlign = TextAlign.Center) }
            }
        }
    }
}
