package id.asrul.pendaki.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import id.asrul.pendaki.domain.Fase
import id.asrul.pendaki.ui.screens.AmbangSpO2Screen
import id.asrul.pendaki.ui.screens.BatasDetakScreen
import id.asrul.pendaki.ui.screens.DebugScreen
import id.asrul.pendaki.ui.screens.HikeViewModel
import id.asrul.pendaki.ui.screens.IzinScreen
import id.asrul.pendaki.ui.screens.KondisiScreen
import id.asrul.pendaki.ui.screens.KonfirmasiSelesaiScreen
import id.asrul.pendaki.ui.screens.MenuScreen
import id.asrul.pendaki.ui.screens.MinumScreen
import id.asrul.pendaki.ui.screens.MulaiScreen
import id.asrul.pendaki.ui.screens.NamaPosScreen
import id.asrul.pendaki.ui.screens.NavigasiScreen
import id.asrul.pendaki.ui.screens.PengaturanScreen
import id.asrul.pendaki.ui.screens.PeringatanAmsScreen
import id.asrul.pendaki.ui.screens.PeringatanHrScreen
import id.asrul.pendaki.ui.screens.PilihGunungScreen
import id.asrul.pendaki.ui.screens.PilihJalurScreen
import id.asrul.pendaki.ui.screens.PosScreen
import id.asrul.pendaki.ui.screens.PuncakScreen
import id.asrul.pendaki.ui.screens.RiwayatScreen
import id.asrul.pendaki.ui.screens.SelesaiScreen
import id.asrul.pendaki.ui.screens.StresScreen
import id.asrul.pendaki.ui.screens.TawaranPosScreen
import id.asrul.pendaki.ui.screens.UkurSpO2Screen
import id.asrul.pendaki.ui.screens.UsiaScreen
import id.asrul.pendaki.ui.screens.UtamaScreen

@Composable
fun PendakiNavHost(ambient: Boolean) {
    val nav = rememberSwipeDismissableNavController()
    val vm: HikeViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val izinSiap by vm.izinSiap.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val ruteSekarang = backStack?.destination?.route

    // Buka layar peringatan/puncak secara otomatis saat kejadian muncul.
    LaunchedEffect(state.peringatanHr?.waktu) {
        if (state.peringatanHr != null && ruteSekarang != Rute.PERINGATAN_HR) nav.navigate(Rute.PERINGATAN_HR)
    }
    LaunchedEffect(state.peringatanAms?.waktu) {
        if (state.peringatanAms != null && ruteSekarang != Rute.PERINGATAN_AMS) nav.navigate(Rute.PERINGATAN_AMS)
    }
    LaunchedEffect(state.fase) {
        if (state.fase == Fase.PUNCAK_TERDETEKSI && ruteSekarang != Rute.PUNCAK) nav.navigate(Rute.PUNCAK)
    }
    LaunchedEffect(state.tawaranPos?.nama) {
        if (state.tawaranPos != null && ruteSekarang != Rute.TAWARAN_POS) nav.navigate(Rute.TAWARAN_POS)
    }

    val awal = when {
        !izinSiap -> Rute.IZIN
        state.sedangAktif -> Rute.UTAMA
        else -> Rute.MULAI
    }

    SwipeDismissableNavHost(navController = nav, startDestination = awal) {
        composable(Rute.IZIN) { IzinScreen(onSelesai = { vm.cekIzin(); nav.navigate(Rute.MULAI) { popUpTo(Rute.IZIN) { inclusive = true } } }) }
        composable(Rute.MULAI) {
            MulaiScreen(
                hike = vm,
                onMulai = { nav.navigate(Rute.UTAMA) { popUpTo(Rute.MULAI) { inclusive = true } } },
                onCariLain = { nav.navigate(Rute.PILIH_GUNUNG) },
                onRiwayat = { nav.navigate(Rute.RIWAYAT) },
                onPilihJalur = { id -> nav.navigate(Rute.pilihJalur(id)) },
            )
        }
        composable(Rute.PILIH_GUNUNG) { PilihGunungScreen(onPilih = { id -> nav.navigate(Rute.pilihJalur(id)) }) }
        composable(Rute.PILIH_JALUR) { entry ->
            val gunungId = entry.arguments?.getString("gunungId") ?: return@composable
            PilihJalurScreen(gunungId = gunungId, hike = vm, onMulai = { nav.navigate(Rute.UTAMA) { popUpTo(Rute.MULAI) { inclusive = true } } })
        }
        composable(Rute.UTAMA) { UtamaScreen(vm, ambient = ambient, onMenu = { nav.navigate(Rute.MENU) }) }
        composable(Rute.MENU) { MenuScreen(vm, onRute = { nav.navigate(it) }) }
        composable(Rute.POS) { PosScreen(vm, onNamaManual = { nav.navigate(Rute.NAMA_POS) }, onSelesai = { nav.popBackStack() }) }
        composable(Rute.NAMA_POS) { NamaPosScreen(vm, onSelesai = { nav.popBackStack() }) }
        composable(Rute.TAWARAN_POS) { TawaranPosScreen(vm, onTutup = { nav.popBackStack() }) }
        composable(Rute.NAVIGASI) { NavigasiScreen(vm) }
        composable(Rute.KONDISI) { KondisiScreen(vm, onUkur = { nav.navigate(Rute.UKUR_SPO2) }) }
        composable(Rute.UKUR_SPO2) { UkurSpO2Screen(vm, onSelesai = { nav.popBackStack() }) }
        composable(Rute.STRES) { StresScreen(vm) }
        composable(Rute.PERINGATAN_HR) { PeringatanHrScreen(vm, onTutup = { nav.popBackStack() }) }
        composable(Rute.PERINGATAN_AMS) { PeringatanAmsScreen(vm, onTutup = { nav.popBackStack() }, onUkurUlang = { nav.navigate(Rute.UKUR_SPO2) }) }
        composable(Rute.PUNCAK) { PuncakScreen(vm, onTutup = { nav.popBackStack() }, onTurun = { nav.navigate(Rute.NAVIGASI) { popUpTo(Rute.UTAMA) } }) }
        composable(Rute.KONFIRMASI_SELESAI) { KonfirmasiSelesaiScreen(vm, onBatal = { nav.popBackStack() }, onSelesai = { nav.navigate(Rute.SELESAI) { popUpTo(Rute.UTAMA) { inclusive = true } } }) }
        composable(Rute.SELESAI) { SelesaiScreen(vm, onTutup = { vm.reset(); nav.navigate(Rute.MULAI) { popUpTo(0) } }) }
        composable(Rute.PENGATURAN) { PengaturanScreen(onRute = { nav.navigate(it) }) }
        composable(Rute.BATAS_DETAK) { BatasDetakScreen() }
        composable(Rute.USIA) { UsiaScreen() }
        composable(Rute.AMBANG_SPO2) { AmbangSpO2Screen() }
        composable(Rute.MINUM) { MinumScreen() }
        composable(Rute.DEBUG) { DebugScreen() }
        composable(Rute.RIWAYAT) { RiwayatScreen() }
    }
}
