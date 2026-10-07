package id.asrul.pendaki.mobile.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import id.asrul.pendaki.shared.datalayer.SesiCodec
import id.asrul.pendaki.shared.model.SesiPendakian
import id.asrul.pendaki.shared.stats.SessionStats
import kotlinx.coroutines.flow.Flow

/** Sesi yang diterima dari jam. Payload lengkap disimpan sebagai JSON gzip. */
@Entity(tableName = "sesi_hp")
data class SesiHpEntity(
    @PrimaryKey val id: String,
    val judul: String,
    val namaGunung: String,
    val namaJalur: String?,
    val mulai: Long,
    val selesai: Long?,
    val durasiMs: Long,
    val jarakM: Double,
    val naikTotalM: Double,
    val hrRata: Int?,
    val jumlahPos: Int,
    val jumlahTitik: Int,
    val diterima: Long,
    val payload: ByteArray,
    val keHealthConnect: Boolean = false,
    val keStrava: Boolean = false,
) {
    fun sesi(): SesiPendakian = SesiCodec.dariGzip(payload)

    companion object {
        fun dari(s: SesiPendakian, payload: ByteArray, diterima: Long = System.currentTimeMillis()): SesiHpEntity {
            val r = SessionStats.ringkasan(s)
            return SesiHpEntity(
                id = s.id, judul = s.judul, namaGunung = s.namaGunung, namaJalur = s.namaJalur,
                mulai = s.mulai, selesai = s.selesai, durasiMs = r.durasiMs, jarakM = r.jarakM, naikTotalM = r.naikTotalM,
                hrRata = r.hrRata, jumlahPos = r.jumlahPos, jumlahTitik = r.jumlahTitik, diterima = diterima, payload = payload,
            )
        }
    }
}

@Dao
interface SesiHpDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun simpan(s: SesiHpEntity)
    @Query("SELECT * FROM sesi_hp ORDER BY mulai DESC") fun semua(): Flow<List<SesiHpEntity>>
    @Query("SELECT * FROM sesi_hp WHERE id = :id") suspend fun byId(id: String): SesiHpEntity?
    @Query("SELECT * FROM sesi_hp WHERE id = :id") fun byIdFlow(id: String): Flow<SesiHpEntity?>
    @Query("UPDATE sesi_hp SET keHealthConnect = :v WHERE id = :id") suspend fun setHealthConnect(id: String, v: Boolean)
    @Query("DELETE FROM sesi_hp WHERE id = :id") suspend fun hapus(id: String)
}

@Database(entities = [SesiHpEntity::class], version = 1, exportSchema = false)
abstract class HpDatabase : RoomDatabase() {
    abstract fun sesiDao(): SesiHpDao
}
