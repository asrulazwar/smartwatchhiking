package id.asrul.pendaki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import id.asrul.pendaki.R
import id.asrul.pendaki.shared.ams.AmsRules
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.ui.komponen.AngkaBesar
import id.asrul.pendaki.ui.komponen.DuaTombol
import id.asrul.pendaki.ui.komponen.GrafikGaris
import id.asrul.pendaki.ui.komponen.KolomTengah
import id.asrul.pendaki.ui.komponen.Label
import id.asrul.pendaki.ui.komponen.LabelKecil
import id.asrul.pendaki.ui.theme.Warna

/** Peringatan detak tinggi (mockup 8): latar merah gelap, angka, batas, durasi, grafik 2 menit. */
@Composable
fun PeringatanHrScreen(vm: HikeViewModel, onTutup: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val p = st.peringatanHr
    val bpm = st.hr ?: p?.bpm ?: 0
    val batas = p?.batas ?: 0
    Box(modifier = Modifier.fillMaxSize().background(Warna.Merah.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
        KolomTengah(spasi = 2.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Warna.Merah, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                LabelKecil(stringResource(R.string.hr_tinggi), warna = Warna.Merah)
            }
            AngkaBesar(bpm.toString(), warna = Warna.Merah)
            Label(stringResource(R.string.bpm), warna = Warna.Teks)
            Label(stringResource(R.string.hr_di_atas_batas, batas, (p?.durasiAtasMs ?: 0) / 1000), warna = Warna.Teks)
            GrafikGaris(st.riwayatHr.map { it.second }, modifier = Modifier.padding(horizontal = 30.dp, vertical = 4.dp), garisBatas = batas)
            Text(stringResource(R.string.hr_saran, batas), color = Warna.Sekunder, style = MaterialTheme.typography.caption1, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            DuaTombol(
                kiri = stringResource(R.string.hr_abaikan), onKiri = { vm.abaikanHr(); onTutup() },
                kanan = stringResource(R.string.hr_istirahat), onKanan = { vm.istirahatHr(); onTutup() },
                warnaKanan = Warna.Merah, warnaTeksKanan = Warna.Teks,
            )
        }
    }
}

/** Peringatan AMS (mockup 5): oranye, saran, tombol "Ukur ulang" dan "Istirahat 10 mnt". */
@Composable
fun PeringatanAmsScreen(vm: HikeViewModel, onTutup: () -> Unit, onUkurUlang: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val p = st.peringatanAms
    Box(modifier = Modifier.fillMaxSize().background(Warna.Oranye.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        KolomTengah(spasi = 2.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Warna.Oranye, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                LabelKecil(stringResource(R.string.ams_peringatan), warna = Warna.Oranye)
            }
            if (p?.spo2 != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    AngkaBesar(p.spo2.toString(), warna = Warna.Oranye)
                    Text(stringResource(R.string.satuan_persen), color = Warna.Oranye, style = MaterialTheme.typography.title1, modifier = Modifier.padding(bottom = 8.dp))
                }
            } else if (p?.hrIstirahat != null) {
                AngkaBesar(p.hrIstirahat.toString(), warna = Warna.Oranye)
                Label(stringResource(R.string.bpm), warna = Warna.Teks)
            }
            p?.alasan?.forEach { a ->
                Label(
                    when (a) {
                        AmsRules.Alasan.SPO2_RENDAH -> stringResource(R.string.ams_rendah, p.ambang)
                        AmsRules.Alasan.SPO2_TURUN -> stringResource(R.string.ams_turun_dari, p.spo2Sebelumnya ?: 0, p.namaPosSebelumnya ?: "sebelumnya")
                        AmsRules.Alasan.HR_ISTIRAHAT_NAIK -> stringResource(R.string.ams_hr_naik, (p.hrIstirahat ?: 0) - (p.hrIstirahatBasecamp ?: 0))
                    },
                    warna = Warna.Teks,
                )
            }
            Text(stringResource(R.string.ams_saran), color = Warna.Sekunder, style = MaterialTheme.typography.caption1, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
            DuaTombol(
                kiri = stringResource(R.string.ams_ukur_ulang), onKiri = { vm.tutupAms(); onUkurUlang() },
                kanan = stringResource(R.string.ams_istirahat), onKanan = { vm.istirahatAms(); onTutup() },
            )
        }
    }
}

/** Puncak terdeteksi otomatis (mockup 11). */
@Composable
fun PuncakScreen(vm: HikeViewModel, onTutup: () -> Unit, onTurun: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val info = st.puncak
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().background(Warna.Hitam), contentAlignment = Alignment.Center) {
        KolomTengah(spasi = 2.dp) {
            LabelKecil(stringResource(R.string.puncak_terdeteksi), warna = Warna.Hijau)
            Text("Gn. ${st.gunung?.nama ?: ""}", color = Warna.Teks, style = MaterialTheme.typography.title2)
            AngkaBesar(Format.ribuan(info?.ketinggianM ?: st.ketinggianM ?: 0.0), warna = Warna.Hijau)
            Label(stringResource(R.string.puncak_jarak, Format.ribuan(info?.jarakKeTitikPuncakM ?: 0.0)))
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp)) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LabelKecil(stringResource(R.string.puncak_summit))
                    Text(Format.jam(info?.waktu ?: st.sekarang), color = Warna.Teks, style = MaterialTheme.typography.title1)
                }
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LabelKecil(stringResource(R.string.puncak_dari_basecamp))
                    Text(Format.durasi(info?.durasiDariBasecampMs ?: st.durasiMs), color = Warna.Teks, style = MaterialTheme.typography.title1)
                }
            }
            Spacer(Modifier.height(6.dp))
            DuaTombol(
                kiri = stringResource(R.string.puncak_belum), onKiri = { vm.belumSampai(); onTutup() },
                kanan = stringResource(R.string.puncak_turun), onKanan = { vm.konfirmasiPuncak(); onTurun() },
                warnaKanan = Warna.Hijau,
            )
        }
    }
}
