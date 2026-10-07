package id.asrul.pendaki.mobile.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.asrul.pendaki.mobile.data.db.SesiHpDao
import id.asrul.pendaki.mobile.data.db.SesiHpEntity
import id.asrul.pendaki.mobile.data.export.GpxExporter
import id.asrul.pendaki.mobile.data.export.HasilGpx
import id.asrul.pendaki.mobile.data.health.HealthConnectManager
import id.asrul.pendaki.mobile.data.health.StatusHc
import id.asrul.pendaki.shared.gpx.GpxOptions
import id.asrul.pendaki.shared.model.SesiPendakian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DaftarViewModel @Inject constructor(dao: SesiHpDao) : ViewModel() {
    val daftar: StateFlow<List<SesiHpEntity>> = dao.semua().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@HiltViewModel
class DetailViewModel @Inject constructor(dao: SesiHpDao, handle: SavedStateHandle) : ViewModel() {
    private val id: String = checkNotNull(handle["id"])
    val sesi: StateFlow<SesiPendakian?> = dao.byIdFlow(id).map { e -> withContext(Dispatchers.Default) { e?.sesi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class EksporState(
    val entity: SesiHpEntity? = null,
    val sesi: SesiPendakian? = null,
    val opsi: GpxOptions = GpxOptions(),
    val hasil: HasilGpx? = null,
    val statusHc: StatusHc = StatusHc.TIDAK_TERPASANG,
    val izinHc: Boolean = false,
    val tulisHc: Boolean = true,
    val pesan: String? = null,
    val sibuk: Boolean = false,
)

@HiltViewModel
class EksporViewModel @Inject constructor(
    private val dao: SesiHpDao,
    private val exporter: GpxExporter,
    val healthConnect: HealthConnectManager,
    handle: SavedStateHandle,
) : ViewModel() {
    private val id: String = checkNotNull(handle["id"])
    private val _state = MutableStateFlow(EksporState())
    val state: StateFlow<EksporState> = _state

    init {
        viewModelScope.launch {
            val e = dao.byId(id) ?: return@launch
            val s = withContext(Dispatchers.Default) { e.sesi() }
            _state.update { it.copy(entity = e, sesi = s) }
            buatUlang()
            cekHc()
        }
    }

    fun cekHc() = viewModelScope.launch {
        val status = healthConnect.status()
        val izin = status == StatusHc.TERSEDIA && healthConnect.semuaIzinAda()
        _state.update { it.copy(statusHc = status, izinHc = izin) }
    }

    fun setOpsi(opsi: GpxOptions) { _state.update { it.copy(opsi = opsi) }; buatUlang() }
    fun setTulisHc(v: Boolean) = _state.update { it.copy(tulisHc = v) }
    fun tutupPesan() = _state.update { it.copy(pesan = null) }

    private fun buatUlang() = viewModelScope.launch(Dispatchers.Default) {
        val s = _state.value.sesi ?: return@launch
        val h = exporter.buat(s, _state.value.opsi)
        _state.update { it.copy(hasil = h) }
    }

    fun bagikan() = viewModelScope.launch {
        val h = _state.value.hasil ?: return@launch
        runCatching { exporter.bagikan(h) }.onFailure { e -> _state.update { it.copy(pesan = e.message) } }
    }

    fun simpan() = viewModelScope.launch {
        val h = _state.value.hasil ?: return@launch
        runCatching { exporter.simpanKeDownload(h) }
            .onSuccess { p -> _state.update { it.copy(pesan = "Tersimpan: $p") } }
            .onFailure { e -> _state.update { it.copy(pesan = e.message) } }
    }

    /** "Unggah sekarang": Tahap 1 hanya menulis ke Health Connect (Strava = Tahap 2). */
    fun unggah() = viewModelScope.launch {
        val st = _state.value
        val s = st.sesi ?: return@launch
        if (!st.tulisHc) { _state.update { it.copy(pesan = "Tidak ada tujuan yang dipilih") }; return@launch }
        _state.update { it.copy(sibuk = true) }
        val r = healthConnect.tulisSesi(s)
        if (r.isSuccess) {
            dao.setHealthConnect(s.id, true)
            _state.update { it.copy(sibuk = false, pesan = "Sesi ditulis ke Health Connect", entity = it.entity?.copy(keHealthConnect = true)) }
        } else {
            _state.update { it.copy(sibuk = false, pesan = "Gagal menulis ke Health Connect: ${r.exceptionOrNull()?.message}") }
        }
    }
}
