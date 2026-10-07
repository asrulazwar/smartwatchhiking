package id.asrul.pendaki.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.asrul.pendaki.domain.HikeEngine
import id.asrul.pendaki.di.AppScope
import id.asrul.pendaki.domain.HikeState
import kotlinx.coroutines.CoroutineScope
import id.asrul.pendaki.service.RekamService
import id.asrul.pendaki.shared.model.Gunung
import id.asrul.pendaki.shared.model.Jalur
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel bersama untuk semua layar pendakian: hanya meneruskan state & aksi [HikeEngine]. */
@HiltViewModel
class HikeViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val engine: HikeEngine,
    @AppScope private val appScope: CoroutineScope,
) : ViewModel() {
    val state: StateFlow<HikeState> = engine.state

    private val _izinSiap = MutableStateFlow(cekIzinSekarang())
    val izinSiap: StateFlow<Boolean> = _izinSiap

    init {
        // Pulihkan sesi aktif (mis. aplikasi dibuka kembali setelah proses mati) dan hidupkan service.
        viewModelScope.launch {
            if (!engine.state.value.sedangAktif && engine.pulihkan()) RekamService.mulai(ctx)
        }
    }

    fun cekIzin() { _izinSiap.value = cekIzinSekarang() }

    private fun cekIzinSekarang(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /**
     * Dijalankan di scope aplikasi, BUKAN viewModelScope: layar Mulai ditutup segera setelah
     * tombol ditekan, dan coroutine ViewModel-nya ikut dibatalkan sebelum sesi tersimpan.
     */
    fun mulai(gunung: Gunung, jalur: Jalur?) = appScope.launch {
        engine.mulai(gunung, jalur)
        RekamService.mulai(ctx)
    }

    fun catatPos(namaManual: String? = null) = viewModelScope.launch { engine.catatPos(namaManual) }
    fun terimaTawaranPos() = viewModelScope.launch { engine.terimaTawaranPos() }
    fun tolakTawaranPos() = engine.tolakTawaranPos()
    fun abaikanHr() = engine.abaikanPeringatanHr()
    fun istirahatHr() = engine.istirahatHr()
    fun tutupAms() = engine.tutupPeringatanAms()
    fun istirahatAms() = engine.istirahatAms()
    fun belumSampai() = engine.belumSampaiPuncak()
    fun konfirmasiPuncak() = viewModelScope.launch { engine.konfirmasiPuncak() }
    fun ukurSpO2() = viewModelScope.launch { engine.ukurSpO2() }
    fun tutupPengingatSpO2() = engine.tutupPengingatSpO2()
    fun tutupPengingatMinum() = engine.tutupPengingatMinum()
    fun bersihkanKejadian() = engine.bersihkanKejadian()

    fun selesai() = appScope.launch {
        engine.selesai()
        RekamService.hentikan(ctx)
        id.asrul.pendaki.data.sync.SyncWorker.jadwalkan(ctx)
    }

    fun kirimKeHp() = viewModelScope.launch { engine.kirimKeHp() }
    fun reset() = engine.reset()
}
