package id.asrul.pendaki.shared.geo

import id.asrul.pendaki.shared.model.Gunung

data class GunungTerdekat(val gunung: Gunung, val jarakM: Double)

/** Pencarian gunung terdekat dari satu fix GPS. */
class PeakFinder(private val daftar: List<Gunung>) {

    /**
     * Mengembalikan paling banyak [maks] gunung dalam radius [radiusM], diurutkan dari yang terdekat.
     * Kasus Selo: Merbabu (±6 km) dan Merapi (±7 km) sama-sama masuk.
     */
    fun terdekat(lat: Double, lon: Double, radiusM: Double = 20_000.0, maks: Int = 3): List<GunungTerdekat> =
        daftar.asSequence()
            .map { GunungTerdekat(it, Geo.jarakM(lat, lon, it.lat, it.lon)) }
            .filter { it.jarakM <= radiusM }
            .sortedBy { it.jarakM }
            .take(maks)
            .toList()

    /** Pencarian teks sederhana untuk daftar fallback. */
    fun cari(query: String): List<Gunung> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return daftar.sortedBy { it.nama }
        return daftar.filter { it.nama.lowercase().contains(q) || it.provinsi.lowercase().contains(q) }
            .sortedBy { it.nama }
    }

    fun byId(id: String): Gunung? = daftar.firstOrNull { it.id == id }
}
