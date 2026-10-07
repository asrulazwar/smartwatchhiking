package id.asrul.pendaki.data.assets

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.asrul.pendaki.shared.geo.PeakFinder
import id.asrul.pendaki.shared.model.DataGunung
import id.asrul.pendaki.shared.model.Gunung
import id.asrul.pendaki.shared.model.Jalur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Memuat `assets/gunung.json` dan `assets/jalur.json` sekali, lalu menyediakan [PeakFinder]. */
@Singleton
class GunungRepository @Inject constructor(@ApplicationContext private val ctx: Context) {
    @Volatile private var gunung: List<Gunung>? = null
    @Volatile private var jalur: List<Jalur>? = null

    suspend fun semuaGunung(): List<Gunung> = gunung ?: withContext(Dispatchers.IO) {
        DataGunung.parseGunung(ctx.assets.open("gunung.json").bufferedReader().use { it.readText() }).also { gunung = it }
    }

    suspend fun semuaJalur(): List<Jalur> = jalur ?: withContext(Dispatchers.IO) {
        DataGunung.parseJalur(ctx.assets.open("jalur.json").bufferedReader().use { it.readText() }).also { jalur = it }
    }

    suspend fun finder(): PeakFinder = PeakFinder(semuaGunung())
    suspend fun gunung(id: String): Gunung? = semuaGunung().firstOrNull { it.id == id }
    suspend fun jalurUntuk(gunungId: String): List<Jalur> = semuaJalur().filter { it.gunungId == gunungId }
    suspend fun jalur(gunungId: String, nama: String?): Jalur? =
        if (nama == null) null else semuaJalur().firstOrNull { it.gunungId == gunungId && it.namaJalur == nama }
}
