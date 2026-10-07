package id.asrul.pendaki.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.asrul.pendaki.data.assets.GunungRepository
import id.asrul.pendaki.data.location.LocationSource
import id.asrul.pendaki.data.prefs.Pengaturan
import id.asrul.pendaki.shared.geo.GunungTerdekat
import id.asrul.pendaki.shared.model.Gunung
import id.asrul.pendaki.shared.model.Jalur
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MulaiState(
    val mencariGps: Boolean = true,
    val gpsTerkunci: Boolean = false,
    val altGps: Double? = null,
    val terdekat: List<GunungTerdekat> = emptyList(),
    val fallback: Boolean = false,
    val gunungTerakhir: Gunung? = null,
    val semua: List<Gunung> = emptyList(),
    val query: String = "",
    val gunungDipilih: Gunung? = null,
    val jalur: List<Jalur> = emptyList(),
)

@HiltViewModel
class MulaiViewModel @Inject constructor(
    private val repo: GunungRepository,
    private val location: LocationSource,
    private val pengaturan: Pengaturan,
) : ViewModel() {
    private val _state = MutableStateFlow(MulaiState())
    val state: StateFlow<MulaiState> = _state

    init {
        viewModelScope.launch {
            val semua = repo.semuaGunung()
            val terakhirId = pengaturan.data.first().gunungTerakhirId
            val terakhir = semua.firstOrNull { it.id == terakhirId }
            _state.update { it.copy(semua = urutkan(semua, terakhir), gunungTerakhir = terakhir) }
            // satu fix GPS, maksimal 20 detik; kalau gagal langsung fallback
            val fix = location.satuFix(20_000)
            if (fix == null) {
                _state.update { it.copy(mencariGps = false, fallback = true) }
            } else {
                val dekat = repo.finder().terdekat(fix.lat, fix.lon)
                _state.update { it.copy(mencariGps = false, gpsTerkunci = true, altGps = fix.altM, terdekat = dekat, fallback = dekat.isEmpty()) }
            }
        }
    }

    private fun urutkan(semua: List<Gunung>, terakhir: Gunung?): List<Gunung> {
        val sisanya = semua.filter { it.id != terakhir?.id }.sortedBy { it.nama }
        return if (terakhir != null) listOf(terakhir) + sisanya else sisanya
    }

    fun cari(q: String) = viewModelScope.launch {
        val semua = repo.semuaGunung()
        val hasil = repo.finder().cari(q)
        _state.update { it.copy(query = q, semua = if (q.isBlank()) urutkan(semua, it.gunungTerakhir) else hasil) }
    }

    fun pilihGunung(id: String) = viewModelScope.launch {
        val g = repo.gunung(id) ?: return@launch
        _state.update { it.copy(gunungDipilih = g, jalur = repo.jalurUntuk(id)) }
    }
}
