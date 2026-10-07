package id.asrul.pendaki.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import id.asrul.pendaki.R
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.shared.stress.StressCalculator
import id.asrul.pendaki.ui.komponen.AngkaBesar
import id.asrul.pendaki.ui.komponen.CincinProgres
import id.asrul.pendaki.ui.komponen.GrafikBatang
import id.asrul.pendaki.ui.komponen.KolomTengah
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LayarDasar
import id.asrul.pendaki.ui.theme.Warna

/** Level stres (mockup 7). Tidak mengarang angka: tanpa sumber HRV tampil "tidak tersedia". */
@Composable
fun StresScreen(vm: HikeViewModel) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.stres
    val warna = { n: Int -> when (StressCalculator.label(n)) { StressCalculator.Label.RENDAH -> Warna.Hijau; StressCalculator.Label.SEDANG -> Warna.Oranye; StressCalculator.Label.TINGGI -> Warna.Merah } }
    LayarDasar {
        if (s != null) CincinProgres(progres = s.nilai / 100f, warna = warna(s.nilai))
        KolomTengah(spasi = 2.dp) {
            Label(stringResource(R.string.stres_judul))
            when {
                !st.stresTersedia -> Text(stringResource(R.string.tidak_tersedia_perangkat), color = Warna.Teks, style = MaterialTheme.typography.body1, textAlign = TextAlign.Center)
                s == null -> Text(stringResource(R.string.stres_butuh_diam), color = Warna.Teks, style = MaterialTheme.typography.body1, textAlign = TextAlign.Center)
                else -> {
                    AngkaBesar(s.nilai.toString(), warna = warna(s.nilai))
                    Text(
                        when (StressCalculator.label(s.nilai)) {
                            StressCalculator.Label.RENDAH -> stringResource(R.string.stres_rendah)
                            StressCalculator.Label.SEDANG -> stringResource(R.string.stres_sedang)
                            StressCalculator.Label.TINGGI -> stringResource(R.string.stres_tinggi)
                        },
                        color = warna(s.nilai), style = MaterialTheme.typography.title2,
                    )
                    Label(stringResource(R.string.stres_hrv, s.rmssdMs.toInt(), Format.relatif(s.waktu, st.sekarang)))
                    Spacer(Modifier.height(6.dp))
                    GrafikBatang(st.riwayatStres.map { it.nilai }, modifier = Modifier.padding(horizontal = 24.dp), warnaUntuk = warna)
                    Label(stringResource(R.string.stres_sejak_basecamp))
                    st.stresSumber?.let { Label(it) }
                }
            }
        }
    }
}
