package id.asrul.pendaki.ui.screens

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import id.asrul.pendaki.ui.komponen.AngkaBesar
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LabelKecil
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.theme.Warna

/**
 * Pengatur angka untuk layar bulat: angka besar di tengah, tombol − dan + di bawahnya (tidak
 * terpotong tepi), preset sebagai chip lebar penuh. Rotary crown mengubah nilai; geser untuk
 * melihat keterangan/preset di bawah.
 */
@Composable
fun PengaturAngka(
    judul: String, sub: String?, nilai: Int, min: Int, max: Int, langkah: Int, satuan: String,
    keterangan: String?, onUbah: (Int) -> Unit, preset: List<Pair<String, Int>> = emptyList(),
) {
    val focus = remember { FocusRequester() }
    val akumulasi = remember { floatArrayOf(0f) }
    val scroll = rememberScrollState()
    LaunchedEffect(Unit) { focus.requestFocus() }
    LayarDasar {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent { e ->
                    akumulasi[0] += e.verticalScrollPixels
                    val tahap = (akumulasi[0] / 40f).toInt()
                    if (tahap != 0) { onUbah((nilai + tahap * langkah).coerceIn(min, max)); akumulasi[0] = 0f }
                    true
                }
                .focusRequester(focus)
                .focusable()
                .verticalScroll(scroll)
                .padding(horizontal = 28.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Label(judul)
            sub?.let { Text(it, color = Warna.Teks, style = MaterialTheme.typography.title3, textAlign = TextAlign.Center) }
            AngkaBesar(nilai.toString(), style = MaterialTheme.typography.display1, warna = Warna.Oranye)
            LabelKecil(satuan)
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { onUbah((nilai - langkah).coerceAtLeast(min)) },
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Warna.PermukaanTerang, contentColor = Warna.Teks),
                ) { Text("−", style = MaterialTheme.typography.title1) }
                Button(
                    onClick = { onUbah((nilai + langkah).coerceAtMost(max)) },
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Warna.Oranye, contentColor = Warna.Hitam),
                ) { Icon(Icons.Rounded.Add, contentDescription = "+") }
            }
            keterangan?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = Warna.Sekunder, style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center)
            }
            if (preset.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                preset.forEach { (nama, v) ->
                    Chip(
                        onClick = { onUbah(v) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (v == nilai) Warna.Oranye else Warna.Permukaan,
                            contentColor = if (v == nilai) Warna.Hitam else Warna.Teks,
                        ),
                        label = { Text(nama, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.button) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
