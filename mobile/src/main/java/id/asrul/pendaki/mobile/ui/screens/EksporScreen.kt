package id.asrul.pendaki.mobile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asrul.pendaki.mobile.R
import id.asrul.pendaki.mobile.data.health.StatusHc
import id.asrul.pendaki.mobile.ui.theme.Warna
import id.asrul.pendaki.shared.format.Format

/** Halaman ekspor (mockup 13): ringkasan + profil, tujuan (Strava/Relive/Health Connect), chip isi GPX, tombol. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EksporScreen(id: String, onKembali: () -> Unit, vm: EksporViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val izinLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { vm.cekHc() }
    val s = st.sesi
    val e = st.entity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ekspor_judul)) },
                navigationIcon = { IconButton(onClick = onKembali) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.kembali)) } },
            )
        },
        snackbarHost = { st.pesan?.let { Snackbar(action = { Text("OK", modifier = Modifier.padding(8.dp)) }, modifier = Modifier.padding(12.dp)) { Text(it) } } },
    ) { pad ->
        if (s == null || e == null) return@Scaffold
        Column(modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.ekspor_diterima, Format.relatif(e.diterima, System.currentTimeMillis())), color = Warna.Sekunder)
            Card(colors = CardDefaults.cardColors(containerColor = Warna.Permukaan)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.judul, style = MaterialTheme.typography.titleLarge, color = Warna.Teks)
                        Text(Format.tanggalPendek(s.mulai), color = Warna.Sekunder)
                    }
                    ProfilKetinggian(s, modifier = Modifier.fillMaxWidth().height(90.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat(Format.jarak(e.jarakM).removeSuffix(" km"), stringResource(R.string.ekspor_km))
                        Stat(Format.ribuan(e.naikTotalM), stringResource(R.string.ekspor_naik))
                        Stat(Format.durasiJamMenit(e.durasiMs), stringResource(R.string.ekspor_jam))
                        Stat(e.jumlahPos.toString(), stringResource(R.string.ekspor_pos))
                    }
                }
            }

            Text(stringResource(R.string.ekspor_kirim_ke), style = MaterialTheme.typography.titleMedium, color = Warna.Oranye)
            BarisTujuan(stringResource(R.string.ekspor_strava), stringResource(R.string.ekspor_strava_sub)) {
                Switch(checked = false, onCheckedChange = null, enabled = false)
            }
            BarisTujuan(stringResource(R.string.ekspor_relive), stringResource(R.string.ekspor_relive_sub)) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.ekspor_relive_auto)) })
            }
            val subHc = when {
                st.statusHc == StatusHc.TIDAK_TERPASANG -> stringResource(R.string.ekspor_hc_tidak_terpasang)
                st.statusHc == StatusHc.PERLU_UPDATE -> stringResource(R.string.ekspor_hc_perlu_update)
                e.keHealthConnect -> stringResource(R.string.ekspor_hc_sudah)
                !st.izinHc -> stringResource(R.string.hc_izin_teks)
                else -> stringResource(R.string.ekspor_hc_sub)
            }
            BarisTujuan(stringResource(R.string.ekspor_hc), subHc) {
                when {
                    st.statusHc != StatusHc.TERSEDIA -> OutlinedButton(onClick = {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { Text(stringResource(R.string.hc_buka)) }
                    !st.izinHc -> OutlinedButton(onClick = { izinLauncher.launch(vm.healthConnect.izin) }) { Text(stringResource(R.string.hc_izin_tombol)) }
                    else -> Switch(checked = st.tulisHc, onCheckedChange = { vm.setTulisHc(it) })
                }
            }

            Text(stringResource(R.string.ekspor_isi), style = MaterialTheme.typography.titleMedium, color = Warna.Oranye)
            val o = st.opsi
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ChipOpsi(stringResource(R.string.ekspor_chip_hr), o.sertakanDetak) { vm.setOpsi(o.copy(sertakanDetak = it)) }
                ChipOpsi(stringResource(R.string.ekspor_chip_wpt), o.sertakanWaypoint) { vm.setOpsi(o.copy(sertakanWaypoint = it)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ChipOpsi(stringResource(R.string.ekspor_chip_baro), o.elevasiBarometer) { vm.setOpsi(o.copy(elevasiBarometer = it)) }
                ChipOpsi(stringResource(R.string.ekspor_chip_spo2), o.sertakanSpO2) { vm.setOpsi(o.copy(sertakanSpO2 = it)) }
            }

            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.bagikan() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.ekspor_bagikan)) }
                Button(
                    onClick = { vm.unggah() }, modifier = Modifier.weight(1f), enabled = !st.sibuk && st.izinHc && st.tulisHc,
                    colors = ButtonDefaults.buttonColors(containerColor = Warna.Oranye, contentColor = Warna.Hitam),
                ) { Text(stringResource(R.string.ekspor_unggah)) }
            }
            OutlinedButton(onClick = { vm.simpan() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ekspor_simpan)) }
            st.hasil?.let { h ->
                Text(stringResource(R.string.ekspor_info_file, h.namaFile, Format.ribuan(h.jumlahTitik), Format.ribuan(h.ukuranByte / 1024)), color = Warna.Sekunder, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BarisTujuan(judul: String, sub: String, aksi: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Warna.Permukaan)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(judul, color = Warna.Teks, style = MaterialTheme.typography.titleMedium)
                Text(sub, color = Warna.Sekunder, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(8.dp))
            aksi()
        }
    }
}

@Composable
private fun ChipOpsi(label: String, aktif: Boolean, onUbah: (Boolean) -> Unit) {
    FilterChip(
        selected = aktif, onClick = { onUbah(!aktif) }, label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Warna.Oranye, selectedLabelColor = Warna.Hitam),
    )
}
