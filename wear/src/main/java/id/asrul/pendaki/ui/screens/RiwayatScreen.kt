package id.asrul.pendaki.ui.screens

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
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import dagger.hilt.android.lifecycle.HiltViewModel
import id.asrul.pendaki.R
import id.asrul.pendaki.data.datalayer.PhoneLink
import id.asrul.pendaki.data.db.SesiEntity
import id.asrul.pendaki.data.db.SesiRepository
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.shared.stats.Ringkasan
import id.asrul.pendaki.shared.stats.SessionStats
import id.asrul.pendaki.ui.komponen.DuaTombol
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LabelKecil
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.komponen.TombolAksi
import id.asrul.pendaki.ui.komponen.TombolSekunder
import id.asrul.pendaki.ui.theme.Warna
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailRiwayat(val entity: SesiEntity, val ringkasan: Ringkasan, val langkah: Int?, val mengirim: Boolean = false, val pesan: String? = null)

@HiltViewModel
class RiwayatViewModel @Inject constructor(
    private val repo: SesiRepository,
    private val phone: PhoneLink,
) : ViewModel() {
    val daftar: StateFlow<List<SesiEntity>> = repo.semua().map { l -> l.filter { !it.aktif } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _detail = MutableStateFlow<DetailRiwayat?>(null)
    val detail: StateFlow<DetailRiwayat?> = _detail

    fun buka(e: SesiEntity) = viewModelScope.launch {
        val s = repo.muatLengkap(e.id) ?: return@launch
        _detail.value = DetailRiwayat(e, SessionStats.ringkasan(s), s.langkah)
    }

    fun tutup() { _detail.value = null }

    fun kirim() = viewModelScope.launch {
        val d = _detail.value ?: return@launch
        _detail.update { it?.copy(mengirim = true, pesan = null) }
        val s = repo.muatLengkap(d.entity.id)
        val ok = s != null && phone.kirimSesi(s)
        if (ok) repo.tandaiTerkirim(d.entity.id)
        _detail.update { it?.copy(mengirim = false, entity = it.entity.copy(terkirim = ok || it.entity.terkirim), pesan = if (ok) "terkirim" else "gagal") }
    }

    fun hapus() = viewModelScope.launch {
        val d = _detail.value ?: return@launch
        repo.hapus(d.entity.id)
        _detail.value = null
    }
}

/** Riwayat pendakian yang tersimpan di jam; tiap sesi bisa dikirim ulang ke HP atau dihapus. */
@Composable
fun RiwayatScreen(vm: RiwayatViewModel = hiltViewModel()) {
    val daftar by vm.daftar.collectAsStateWithLifecycle()
    val detail by vm.detail.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    val d = detail
    LayarDasar {
        Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                if (d == null) {
                    item { Label(stringResource(R.string.riwayat_judul)) }
                    if (daftar.isEmpty()) item { Label(stringResource(R.string.riwayat_kosong), warna = Warna.Teks) }
                    items(daftar.size) { i ->
                        val e = daftar[i]
                        Chip(
                            onClick = { vm.buka(e) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ChipDefaults.chipColors(backgroundColor = Warna.Permukaan, contentColor = Warna.Teks, secondaryContentColor = if (e.terkirim) Warna.Hijau else Warna.Oranye),
                            label = { Text(e.namaJalur?.let { "Gn. ${e.namaGunung} · $it" } ?: "Gn. ${e.namaGunung}", maxLines = 1) },
                            secondaryLabel = { Text(Format.tanggalPendek(e.mulai) + " · " + stringResource(if (e.terkirim) R.string.riwayat_terkirim else R.string.riwayat_belum), maxLines = 1) },
                        )
                    }
                } else {
                    val r = d.ringkasan
                    item { Text(d.entity.namaJalur?.let { "Gn. ${d.entity.namaGunung} · $it" } ?: "Gn. ${d.entity.namaGunung}", color = Warna.Teks, style = MaterialTheme.typography.title2, textAlign = TextAlign.Center, maxLines = 2) }
                    item { Label(Format.tanggalPendek(d.entity.mulai) + " · " + Format.jam(d.entity.mulai)) }
                    item { Label("${Format.durasi(r.durasiMs)} · ${Format.jarak(r.jarakM)} · ↑${Format.ribuan(r.naikTotalM)} m", warna = Warna.Teks) }
                    item { Label("${r.jumlahPos} pos · ${Format.ribuan(r.jumlahTitik)} titik" + (d.langkah?.let { " · ${Format.ribuan(it)} langkah" } ?: ""), warna = Warna.Teks) }
                    item { LabelKecil(stringResource(if (d.entity.terkirim) R.string.riwayat_terkirim else R.string.riwayat_belum), warna = if (d.entity.terkirim) Warna.Hijau else Warna.Oranye) }
                    item {
                        if (d.mengirim) TombolSekunder(stringResource(R.string.selesai_mengirim), onClick = {})
                        else TombolAksi(stringResource(R.string.riwayat_kirim), onClick = { vm.kirim() })
                    }
                    d.pesan?.let { item { Label(if (it == "terkirim") stringResource(R.string.selesai_terkirim) else stringResource(R.string.selesai_antre), warna = if (it == "terkirim") Warna.Hijau else Warna.Oranye) } }
                    item { Label(stringResource(R.string.riwayat_tersimpan_hp)) }
                    item {
                        DuaTombol(
                            kiri = stringResource(R.string.kembali), onKiri = { vm.tutup() },
                            kanan = stringResource(R.string.riwayat_hapus), onKanan = { vm.hapus() },
                            warnaKanan = Warna.PermukaanTerang, warnaTeksKanan = Warna.Merah,
                        )
                    }
                }
            }
        }
    }
}
