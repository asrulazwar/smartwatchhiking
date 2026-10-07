package id.asrul.pendaki.mobile.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import dagger.hilt.android.qualifiers.ApplicationContext
import id.asrul.pendaki.shared.datalayer.HcSnapshot
import id.asrul.pendaki.shared.model.SesiPendakian
import id.asrul.pendaki.shared.stats.SessionStats
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

enum class StatusHc { TIDAK_TERPASANG, PERLU_UPDATE, TERSEDIA }

/** Tulis sesi Hiking ke Health Connect dan baca SpO2/HRV terbaru untuk dikirim ke jam. */
@Singleton
class HealthConnectManager @Inject constructor(@ApplicationContext private val ctx: Context) {

    val izin: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(ElevationGainedRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE,
    )

    fun status(): StatusHc = when (HealthConnectClient.getSdkStatus(ctx)) {
        HealthConnectClient.SDK_AVAILABLE -> StatusHc.TERSEDIA
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> StatusHc.PERLU_UPDATE
        else -> StatusHc.TIDAK_TERPASANG
    }

    private fun client(): HealthConnectClient? =
        if (status() == StatusHc.TERSEDIA) HealthConnectClient.getOrCreate(ctx) else null

    suspend fun izinDiberikan(): Set<String> =
        runCatching { client()?.permissionController?.getGrantedPermissions() ?: emptySet() }.getOrDefault(emptySet())

    suspend fun semuaIzinAda(): Boolean = izinDiberikan().containsAll(izin)

    private fun meta() = Metadata.autoRecorded(device = Device(type = Device.TYPE_WATCH))

    /** Tulis ExerciseSessionRecord HIKING + rute, HeartRateRecord, ElevationGainedRecord, DistanceRecord. */
    suspend fun tulisSesi(s: SesiPendakian): Result<Unit> = runCatching {
        val client = client() ?: error("Health Connect tidak tersedia")
        val zona = ZoneId.systemDefault()
        val mulai = Instant.ofEpochMilli(s.mulai)
        val selesai = Instant.ofEpochMilli(s.selesai ?: s.titik.lastOrNull()?.waktu ?: s.mulai + 60_000)
        val offset = zona.rules.getOffset(mulai)
        val r = SessionStats.ringkasan(s)

        val rute = s.titik.map { t ->
            ExerciseRoute.Location(
                time = Instant.ofEpochMilli(t.waktu), latitude = t.lat, longitude = t.lon,
                altitude = (t.altBaro ?: t.altGps)?.let { Length.meters(it) },
            )
        }
        val records = mutableListOf<androidx.health.connect.client.records.Record>()
        records += ExerciseSessionRecord(
            metadata = meta(),
            startTime = mulai, startZoneOffset = offset, endTime = selesai, endZoneOffset = offset,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
            title = s.judul,
            exerciseRoute = if (rute.isNotEmpty()) ExerciseRoute(route = rute) else null,
        )
        val hr = s.titik.filter { it.hr != null }.map { HeartRateRecord.Sample(Instant.ofEpochMilli(it.waktu), it.hr!!.toLong()) }
        if (hr.isNotEmpty()) {
            records += HeartRateRecord(metadata = meta(), startTime = mulai, startZoneOffset = offset, endTime = selesai, endZoneOffset = offset, samples = hr)
        }
        records += ElevationGainedRecord(metadata = meta(), startTime = mulai, startZoneOffset = offset, endTime = selesai, endZoneOffset = offset, elevation = Length.meters(r.naikTotalM))
        records += DistanceRecord(metadata = meta(), startTime = mulai, startZoneOffset = offset, endTime = selesai, endZoneOffset = offset, distance = Length.meters(r.jarakM))
        client.insertRecords(records)
        Timber.i("Sesi %s ditulis ke Health Connect", s.id)
    }

    /** SpO2 dan HRV RMSSD terbaru (2 hari terakhir) untuk fallback di jam. */
    suspend fun bacaSnapshot(): HcSnapshot {
        val client = client() ?: return HcSnapshot(dibuat = System.currentTimeMillis())
        val sejak = TimeRangeFilter.after(Instant.now().minus(Duration.ofDays(2)))
        val spo2 = runCatching {
            client.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, sejak, ascendingOrder = false, pageSize = 1)).records.firstOrNull()
        }.getOrNull()
        val hrv = runCatching {
            client.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, sejak, ascendingOrder = false, pageSize = 1)).records.firstOrNull()
        }.getOrNull()
        return HcSnapshot(
            spo2Persen = spo2?.percentage?.value?.toInt(),
            spo2Waktu = spo2?.time?.toEpochMilli(),
            hrvRmssdMs = hrv?.heartRateVariabilityMillis,
            hrvWaktu = hrv?.time?.toEpochMilli(),
            dibuat = System.currentTimeMillis(),
        )
    }
}
