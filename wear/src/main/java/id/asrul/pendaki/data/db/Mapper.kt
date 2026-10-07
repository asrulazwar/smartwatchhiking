package id.asrul.pendaki.data.db

import id.asrul.pendaki.shared.model.JenisWaypoint
import id.asrul.pendaki.shared.model.SampelSpO2
import id.asrul.pendaki.shared.model.SampelStres
import id.asrul.pendaki.shared.model.SesiPendakian
import id.asrul.pendaki.shared.model.TitikJejak
import id.asrul.pendaki.shared.model.Waypoint

fun TitikEntity.keModel() = TitikJejak(waktu, lat, lon, altGps, altBaro, hr)

fun WaypointEntity.keModel() = Waypoint(
    nama = nama, waktu = waktu, lat = lat, lon = lon, alt = alt,
    jenis = runCatching { JenisWaypoint.valueOf(jenis) }.getOrDefault(JenisWaypoint.POS),
    urutan = urutan, hr = hr, spo2 = spo2,
)

fun SampelEntity.keSpO2() = SampelSpO2(waktu, nilai.toInt(), ekstra ?: 0.0, flag)
fun SampelEntity.keStres() = SampelStres(waktu, nilai.toInt(), ekstra ?: 0.0)

fun SesiEntity.keModel(
    titik: List<TitikEntity>,
    waypoint: List<WaypointEntity>,
    spo2: List<SampelEntity>,
    stres: List<SampelEntity>,
) = SesiPendakian(
    id = id, gunungId = gunungId, namaGunung = namaGunung, namaJalur = namaJalur,
    elevasiPuncak = elevasiPuncak, puncakLat = puncakLat, puncakLon = puncakLon, basecampElevasi = basecampElevasi,
    mulai = mulai, selesai = selesai, waktuPuncak = waktuPuncak,
    titik = titik.map { it.keModel() },
    waypoint = waypoint.map { it.keModel() },
    spo2 = spo2.map { it.keSpO2() },
    stres = stres.map { it.keStres() },
)
