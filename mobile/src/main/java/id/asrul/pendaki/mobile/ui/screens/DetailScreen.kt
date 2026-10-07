package id.asrul.pendaki.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asrul.pendaki.mobile.R
import id.asrul.pendaki.mobile.ui.theme.Warna
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.shared.model.JenisWaypoint
import id.asrul.pendaki.shared.model.SesiPendakian
import id.asrul.pendaki.shared.stats.SessionStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(id: String, onEkspor: () -> Unit, onKembali: () -> Unit, vm: DetailViewModel = hiltViewModel()) {
    val sesi by vm.sesi.collectAsStateWithLifecycle()
    val s = sesi
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s?.judul ?: "") },
                navigationIcon = { IconButton(onClick = onKembali) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.kembali)) } },
            )
        },
    ) { pad ->
        if (s == null) return@Scaffold
        val r = remember(s) { SessionStats.ringkasan(s) }
        Column(modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(Format.tanggalPendek(s.mulai) + " · " + Format.jam(s.mulai), color = Warna.Sekunder)

            Judul(stringResource(R.string.detail_profil))
            ProfilKetinggian(s, modifier = Modifier.fillMaxWidth().height(180.dp))

            Judul(stringResource(R.string.detail_statistik))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(Format.durasiJamMenit(r.durasiMs), stringResource(R.string.stat_waktu))
                Stat(Format.jarak(r.jarakM), stringResource(R.string.stat_jarak))
                Stat("${Format.ribuan(r.naikTotalM)} m", stringResource(R.string.stat_naik))
                Stat("${Format.ribuan(r.turunTotalM)} m", stringResource(R.string.stat_turun))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(r.hrRata?.toString() ?: "—", stringResource(R.string.stat_hr), Warna.Merah)
                Stat(r.hrMaks?.toString() ?: "—", stringResource(R.string.stat_hr_maks), Warna.Merah)
                Stat(r.jumlahPos.toString(), stringResource(R.string.stat_pos))
                Stat(Format.ribuan(r.jumlahTitik), stringResource(R.string.stat_titik))
            }
            s.langkah?.let { Text("${stringResource(R.string.stat_langkah)}: ${Format.ribuan(it)}", color = Warna.Teks) }
            s.waktuPuncak?.let { Text("${stringResource(R.string.stat_puncak)}: ${Format.jam(it)} · ${Format.durasi(it - s.mulai)} dari basecamp", color = Warna.Hijau) }

            Judul(stringResource(R.string.detail_pos))
            TabelPos(s)

            Judul(stringResource(R.string.detail_grafik_hr))
            GrafikTerhadapKetinggian(
                titik = s.titik.filter { it.hr != null }.map { ((it.altBaro ?: it.altGps ?: 0.0)) to it.hr!!.toDouble() },
                warna = Warna.Merah, modifier = Modifier.fillMaxWidth().height(140.dp),
            )
            Judul(stringResource(R.string.detail_grafik_spo2))
            GrafikTerhadapKetinggian(
                titik = s.spo2.map { it.alt to it.nilai.toDouble() },
                warna = Warna.BiruSpO2, modifier = Modifier.fillMaxWidth().height(140.dp), minY = 80.0, maxY = 100.0,
            )

            Button(onClick = onEkspor, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.detail_ekspor)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Judul(t: String) = Text(t, style = MaterialTheme.typography.titleMedium, color = Warna.Oranye)

@Composable
private fun TabelPos(s: SesiPendakian) {
    val wp = s.waypoint.sortedBy { it.waktu }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.kolom_nama), Modifier.weight(2f), color = Warna.Sekunder, style = MaterialTheme.typography.labelSmall)
            Text(stringResource(R.string.kolom_waktu), Modifier.weight(1f), color = Warna.Sekunder, style = MaterialTheme.typography.labelSmall)
            Text(stringResource(R.string.kolom_ketinggian), Modifier.weight(1.2f), color = Warna.Sekunder, style = MaterialTheme.typography.labelSmall)
            Text(stringResource(R.string.kolom_hr), Modifier.weight(0.7f), color = Warna.Sekunder, style = MaterialTheme.typography.labelSmall)
            Text(stringResource(R.string.kolom_spo2), Modifier.weight(0.8f), color = Warna.Sekunder, style = MaterialTheme.typography.labelSmall)
        }
        wp.forEach { w ->
            val warna = when (w.jenis) { JenisWaypoint.PUNCAK -> Warna.Hijau; JenisWaypoint.BASECAMP -> Warna.Sekunder; else -> Warna.Teks }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(w.nama, Modifier.weight(2f), color = warna)
                Text(Format.jam(w.waktu), Modifier.weight(1f), color = Warna.Teks)
                Text(Format.ribuan(w.alt), Modifier.weight(1.2f), color = Warna.Teks)
                Text(w.hr?.toString() ?: "—", Modifier.weight(0.7f), color = Warna.Merah)
                Text(w.spo2?.let { "$it%" } ?: "—", Modifier.weight(0.8f), color = Warna.BiruSpO2)
            }
        }
    }
}

/** Profil ketinggian terhadap jarak, dengan titik pos. */
@Composable
fun ProfilKetinggian(s: SesiPendakian, modifier: Modifier = Modifier) {
    val data = remember(s) {
        var jarak = 0.0
        val out = ArrayList<Pair<Double, Double>>(s.titik.size)
        s.titik.forEachIndexed { i, t ->
            if (i > 0) jarak += id.asrul.pendaki.shared.geo.Geo.jarakM(s.titik[i - 1].lat, s.titik[i - 1].lon, t.lat, t.lon)
            SessionStats.altTerbaik(t)?.let { out += jarak to it }
        }
        out
    }
    val pos = remember(s, data) {
        s.waypoint.filter { it.jenis != JenisWaypoint.BASECAMP }.mapNotNull { w ->
            val idx = s.titik.indexOfFirst { it.waktu >= w.waktu }.takeIf { it >= 0 } ?: return@mapNotNull null
            val d = data.getOrNull(idx.coerceAtMost(data.size - 1)) ?: return@mapNotNull null
            Triple(d.first, w.alt, w.jenis == JenisWaypoint.PUNCAK)
        }
    }
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val maxX = data.last().first.coerceAtLeast(1.0)
        val minY = data.minOf { it.second } - 20
        val maxY = data.maxOf { it.second } + 20
        fun x(v: Double) = (v / maxX * size.width).toFloat()
        fun y(v: Double) = (size.height - (v - minY) / (maxY - minY) * size.height).toFloat()
        val path = Path().apply {
            moveTo(x(data[0].first), y(data[0].second))
            for (p in data.drop(1)) lineTo(x(p.first), y(p.second))
        }
        val isi = Path().apply { addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(isi, Warna.Oranye.copy(alpha = 0.18f))
        drawPath(path, Warna.Oranye, style = Stroke(width = 4f, cap = StrokeCap.Round))
        pos.forEach { (d, alt, puncak) ->
            drawCircle(if (puncak) Warna.Hijau else Warna.Teks, radius = 9f, center = Offset(x(d), y(alt)))
            drawCircle(Warna.Hitam, radius = 4f, center = Offset(x(d), y(alt)))
        }
    }
}

/** Grafik nilai (HR/SpO2) terhadap ketinggian sebagai titik-titik. */
@Composable
fun GrafikTerhadapKetinggian(titik: List<Pair<Double, Double>>, warna: Color, modifier: Modifier = Modifier, minY: Double? = null, maxY: Double? = null) {
    Canvas(modifier = modifier) {
        if (titik.isEmpty()) return@Canvas
        val minX = titik.minOf { it.first }
        val maxX = titik.maxOf { it.first }.coerceAtLeast(minX + 1)
        val lo = minY ?: (titik.minOf { it.second } - 5)
        val hi = maxY ?: (titik.maxOf { it.second } + 5)
        fun x(v: Double) = ((v - minX) / (maxX - minX) * size.width).toFloat()
        fun y(v: Double) = (size.height - (v - lo) / (hi - lo) * size.height).toFloat()
        drawLine(Warna.PermukaanTerang, Offset(0f, size.height), Offset(size.width, size.height), 2f)
        titik.forEach { (alt, v) -> drawCircle(warna, radius = 4f, center = Offset(x(alt), y(v))) }
    }
}
