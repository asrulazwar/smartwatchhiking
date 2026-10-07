package id.asrul.pendaki.data.db

import id.asrul.pendaki.shared.model.SesiPendakian
import javax.inject.Inject
import javax.inject.Singleton

/** Akses gabungan ke tabel sesi + titik + waypoint + sampel. */
@Singleton
class SesiRepository @Inject constructor(
    private val sesiDao: SesiDao,
    private val titikDao: TitikDao,
    private val waypointDao: WaypointDao,
    private val sampelDao: SampelDao,
) {
    suspend fun muatLengkap(id: String): SesiPendakian? {
        val s = sesiDao.byId(id) ?: return null
        return s.keModel(
            titik = titikDao.semua(id),
            waypoint = waypointDao.semua(id),
            spo2 = sampelDao.semua(id, SampelEntity.SPO2),
            stres = sampelDao.semua(id, SampelEntity.STRES),
        )
    }

    suspend fun belumTerkirim(): List<SesiEntity> = sesiDao.belumTerkirim()
    suspend fun tandaiTerkirim(id: String) = sesiDao.tandaiTerkirim(id)
    suspend fun aktif(): SesiEntity? = sesiDao.aktif()

    suspend fun waypointPendakianLama(gunungId: String, jalur: String?): List<WaypointEntity> {
        if (jalur == null) return emptyList()
        val lama = sesiDao.terakhirDiJalur(gunungId, jalur) ?: return emptyList()
        return waypointDao.semua(lama.id)
    }
}
