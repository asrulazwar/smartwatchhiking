package id.asrul.pendaki.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sesi")
data class SesiEntity(
    @PrimaryKey val id: String,
    val gunungId: String,
    val namaGunung: String,
    val namaJalur: String?,
    val elevasiPuncak: Int,
    val puncakLat: Double,
    val puncakLon: Double,
    val basecampElevasi: Int?,
    val mulai: Long,
    val selesai: Long? = null,
    val waktuPuncak: Long? = null,
    /** Offset kalibrasi barometer terakhir (m), disimpan agar bisa dipulihkan setelah proses mati. */
    val offsetBaro: Double = 0.0,
    val hrIstirahatBasecamp: Int? = null,
    val baselineRmssd: Double? = null,
    val aktif: Boolean = true,
    val terkirim: Boolean = false,
    /** Nilai TYPE_STEP_COUNTER saat sesi mulai (null = belum ada bacaan). */
    val langkahAwal: Long? = null,
    val langkah: Int = 0,
)

@Entity(tableName = "titik", indices = [Index("sesiId", "waktu")])
data class TitikEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sesiId: String,
    val waktu: Long,
    val lat: Double,
    val lon: Double,
    val altGps: Double?,
    val altBaro: Double?,
    val hr: Int?,
)

@Entity(tableName = "waypoint", indices = [Index("sesiId", "waktu")])
data class WaypointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sesiId: String,
    val nama: String,
    val jenis: String,
    val urutan: Int,
    val waktu: Long,
    val lat: Double,
    val lon: Double,
    val alt: Double,
    val hr: Int?,
    val spo2: Int?,
)

/** Sampel SpO2 / stres / detak disatukan dalam satu tabel dengan kolom jenis. */
@Entity(tableName = "sampel", indices = [Index("sesiId", "jenis", "waktu")])
data class SampelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sesiId: String,
    val jenis: String,         // "spo2" | "stres" | "hr"
    val waktu: Long,
    val nilai: Double,
    val ekstra: Double? = null, // spo2: alt; stres: rmssd
    val flag: Boolean = false,  // spo2: meragukan
) {
    companion object {
        const val SPO2 = "spo2"
        const val STRES = "stres"
        const val HR = "hr"
    }
}
