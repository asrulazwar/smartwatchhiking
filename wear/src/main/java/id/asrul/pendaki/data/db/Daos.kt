package id.asrul.pendaki.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SesiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun simpan(s: SesiEntity)
    @Update suspend fun perbarui(s: SesiEntity)
    @Query("SELECT * FROM sesi WHERE id = :id") suspend fun byId(id: String): SesiEntity?
    @Query("SELECT * FROM sesi WHERE aktif = 1 ORDER BY mulai DESC LIMIT 1") suspend fun aktif(): SesiEntity?
    @Query("SELECT * FROM sesi WHERE aktif = 1 ORDER BY mulai DESC LIMIT 1") fun aktifFlow(): Flow<SesiEntity?>
    @Query("SELECT * FROM sesi WHERE aktif = 0 AND terkirim = 0 ORDER BY mulai") suspend fun belumTerkirim(): List<SesiEntity>
    @Query("SELECT * FROM sesi ORDER BY mulai DESC") fun semua(): Flow<List<SesiEntity>>
    @Query("SELECT * FROM sesi WHERE gunungId = :gunungId AND namaJalur = :jalur AND aktif = 0 ORDER BY mulai DESC LIMIT 1")
    suspend fun terakhirDiJalur(gunungId: String, jalur: String): SesiEntity?
    @Query("UPDATE sesi SET terkirim = 1 WHERE id = :id") suspend fun tandaiTerkirim(id: String)
    @Query("UPDATE sesi SET terkirim = 0 WHERE aktif = 0") suspend fun tandaiSemuaBelumTerkirim()
    @Query("UPDATE sesi SET offsetBaro = :offset WHERE id = :id") suspend fun simpanOffset(id: String, offset: Double)
    @Query("UPDATE sesi SET langkahAwal = :awal, langkah = :langkah WHERE id = :id") suspend fun simpanLangkah(id: String, awal: Long?, langkah: Int)
    @Query("DELETE FROM sesi WHERE id = :id") suspend fun hapus(id: String)
}

@Dao
interface TitikDao {
    @Insert suspend fun simpan(t: TitikEntity)
    @Query("SELECT * FROM titik WHERE sesiId = :sesiId ORDER BY waktu") suspend fun semua(sesiId: String): List<TitikEntity>
    @Query("SELECT COUNT(*) FROM titik WHERE sesiId = :sesiId") fun jumlah(sesiId: String): Flow<Int>
    @Query("SELECT * FROM titik WHERE sesiId = :sesiId AND waktu >= :sejak ORDER BY waktu") suspend fun sejak(sesiId: String, sejak: Long): List<TitikEntity>
    @Query("DELETE FROM titik WHERE sesiId = :sesiId") suspend fun hapus(sesiId: String)
}

@Dao
interface WaypointDao {
    @Insert suspend fun simpan(w: WaypointEntity): Long
    @Query("SELECT * FROM waypoint WHERE sesiId = :sesiId ORDER BY waktu") suspend fun semua(sesiId: String): List<WaypointEntity>
    @Query("SELECT * FROM waypoint WHERE sesiId = :sesiId ORDER BY waktu") fun semuaFlow(sesiId: String): Flow<List<WaypointEntity>>
    @Query("DELETE FROM waypoint WHERE sesiId = :sesiId") suspend fun hapus(sesiId: String)
}

@Dao
interface SampelDao {
    @Insert suspend fun simpan(s: SampelEntity)
    @Query("SELECT * FROM sampel WHERE sesiId = :sesiId AND jenis = :jenis ORDER BY waktu") suspend fun semua(sesiId: String, jenis: String): List<SampelEntity>
    @Query("SELECT * FROM sampel WHERE sesiId = :sesiId AND jenis = :jenis ORDER BY waktu") fun semuaFlow(sesiId: String, jenis: String): Flow<List<SampelEntity>>
    @Query("SELECT * FROM sampel WHERE sesiId = :sesiId AND jenis = :jenis ORDER BY waktu DESC LIMIT 1") suspend fun terakhir(sesiId: String, jenis: String): SampelEntity?
    @Query("DELETE FROM sampel WHERE sesiId = :sesiId") suspend fun hapus(sesiId: String)
}
