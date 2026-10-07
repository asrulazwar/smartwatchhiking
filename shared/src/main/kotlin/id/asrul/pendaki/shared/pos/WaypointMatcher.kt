package id.asrul.pendaki.shared.pos

import id.asrul.pendaki.shared.geo.Geo
import id.asrul.pendaki.shared.model.Waypoint

/**
 * Mencocokkan posisi sekarang dengan waypoint pendakian lama di jalur yang sama (radius 60 m)
 * untuk menawarkan "Sampai Pos 3?". Tawaran yang sudah ditolak tidak diulang untuk waypoint itu.
 */
class WaypointMatcher(
    private val waypointLama: List<Waypoint>,
    private val radiusM: Double = 60.0,
) {
    private val ditolak = mutableSetOf<String>()
    private val sudahDicatat = mutableSetOf<String>()

    fun cocokkan(lat: Double, lon: Double): Waypoint? =
        waypointLama
            .filter { it.nama !in ditolak && it.nama !in sudahDicatat }
            .map { it to Geo.jarakM(lat, lon, it.lat, it.lon) }
            .filter { it.second <= radiusM }
            .minByOrNull { it.second }
            ?.first

    fun tolak(w: Waypoint) { ditolak += w.nama }
    fun tandaiDicatat(nama: String) { sudahDicatat += nama }
}

/** Preset nama pos untuk jalur tanpa data. */
object PresetNamaPos {
    val daftar = listOf("Pos", "Shelter", "Sumber air", "Camp", "Persimpangan")
}
