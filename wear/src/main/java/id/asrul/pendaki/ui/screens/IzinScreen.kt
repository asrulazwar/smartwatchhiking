package id.asrul.pendaki.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.MaterialTheme
import id.asrul.pendaki.R
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.komponen.TombolAksi
import id.asrul.pendaki.ui.komponen.TombolSekunder
import id.asrul.pendaki.ui.theme.Warna

/** Penjelasan singkat tiap izin, lalu meminta semuanya sekaligus. Penolakan tidak membuat crash. */
@Composable
fun IzinScreen(onSelesai: () -> Unit) {
    val ctx = LocalContext.current
    var ditolak by remember { mutableStateOf(false) }
    val izin = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.BODY_SENSORS)
            add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { hasil ->
        if (hasil[Manifest.permission.ACCESS_FINE_LOCATION] == true) onSelesai() else ditolak = true
    }
    val state = rememberScalingLazyListState()
    LayarDasar {
        Scaffold {
            ScalingLazyColumn(state = state, modifier = Modifier.fillMaxWidth()) {
                item { Text(stringResource(R.string.izin_judul), style = MaterialTheme.typography.title2, color = Warna.Oranye, textAlign = TextAlign.Center) }
                item { Label(stringResource(R.string.izin_lokasi), warna = Warna.Teks) }
                item { Label(stringResource(R.string.izin_sensor), warna = Warna.Teks) }
                item { Label(stringResource(R.string.izin_aktivitas), warna = Warna.Teks) }
                item { Label(stringResource(R.string.izin_notifikasi), warna = Warna.Teks) }
                if (ditolak) {
                    item { Label(stringResource(R.string.izin_ditolak), warna = Warna.Merah) }
                    item {
                        TombolSekunder(stringResource(R.string.izin_buka_pengaturan), onClick = {
                            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        })
                    }
                }
                item { TombolAksi(stringResource(R.string.izin_beri), onClick = { launcher.launch(izin) }, modifier = Modifier.padding(top = 4.dp)) }
            }
        }
    }
}
