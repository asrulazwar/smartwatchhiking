package id.asrul.pendaki.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asrul.pendaki.mobile.R
import id.asrul.pendaki.mobile.ui.theme.Warna
import id.asrul.pendaki.shared.format.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaftarScreen(onBuka: (String) -> Unit, vm: DaftarViewModel = hiltViewModel()) {
    val daftar by vm.daftar.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.daftar_judul)) }) }) { pad ->
        if (daftar.isEmpty()) {
            Text(stringResource(R.string.daftar_kosong), modifier = Modifier.padding(pad).padding(24.dp), color = Warna.Sekunder)
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(daftar, key = { it.id }) { s ->
                Card(onClick = { onBuka(s.id) }, colors = CardDefaults.cardColors(containerColor = Warna.Permukaan)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(s.judul, style = MaterialTheme.typography.titleLarge, color = Warna.Teks)
                        Text(Format.tanggalPendek(s.mulai) + " · " + Format.jam(s.mulai), color = Warna.Sekunder, style = MaterialTheme.typography.bodyMedium)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Stat(Format.jarak(s.jarakM), "jarak")
                            Stat("${Format.ribuan(s.naikTotalM)} m", "naik")
                            Stat(Format.durasiJamMenit(s.durasiMs), "jam")
                            Stat(s.jumlahPos.toString(), "pos")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Stat(nilai: String, label: String, warna: androidx.compose.ui.graphics.Color = Warna.Teks) {
    Column {
        Text(nilai, style = MaterialTheme.typography.headlineMedium, color = warna)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Warna.Sekunder)
    }
}
