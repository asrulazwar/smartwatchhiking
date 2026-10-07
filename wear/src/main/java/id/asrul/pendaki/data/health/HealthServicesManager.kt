package id.asrul.pendaki.data.health

import android.content.Context
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class SampelHr(val waktuMs: Long, val bpm: Int)

/**
 * Sesi olahraga Health Services (`ExerciseType.HIKING`) agar sistem mengizinkan sensor berjalan
 * di latar bersama foreground service. Mengalirkan detak jantung kontinu.
 */
@Singleton
class HealthServicesManager @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val client by lazy { HealthServices.getClient(ctx).exerciseClient }

    private val _hr = MutableSharedFlow<SampelHr>(extraBufferCapacity = 64)
    val hr: SharedFlow<SampelHr> = _hr

    private val _status = MutableStateFlow("belum mulai")
    val status: StateFlow<String> = _status

    private val _hrTersedia = MutableStateFlow<String>("—")
    val hrTersedia: StateFlow<String> = _hrTersedia

    @Volatile var sesiAktif: Boolean = false
        private set

    private val bootInstant: Instant
        get() = Instant.ofEpochMilli(System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime())

    private val callback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val boot = bootInstant
            for (dp in update.latestMetrics.getData(DataType.HEART_RATE_BPM)) {
                val bpm = dp.value.toInt()
                if (bpm in 25..250) {
                    _hr.tryEmit(SampelHr(dp.getTimeInstant(boot).toEpochMilli(), bpm))
                }
            }
            val st = update.exerciseStateInfo.state
            _status.value = "sesi ${st}"
            if (st == ExerciseState.ENDED) sesiAktif = false
        }
        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit
        override fun onRegistered() { Timber.d("Health Services callback terdaftar") }
        override fun onRegistrationFailed(throwable: Throwable) {
            Timber.w(throwable, "Health Services registrasi gagal")
            _status.value = "registrasi gagal: ${throwable.javaClass.simpleName}"
        }
        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            if (dataType == DataType.HEART_RATE_BPM) _hrTersedia.value = availability.toString()
        }
    }

    /** Ringkasan kapabilitas untuk layar debug. */
    suspend fun kapabilitas(): String = runCatching {
        val caps = client.getCapabilitiesAsync().await()
        val hiking = caps.getExerciseTypeCapabilities(ExerciseType.HIKING)
        val dt = hiking.supportedDataTypes.joinToString { it.name }
        "HIKING didukung: ${ExerciseType.HIKING in caps.supportedExerciseTypes}; data: $dt; " +
            "goal: ${hiking.supportedGoals.keys.joinToString { it.name }}"
    }.getOrElse { "gagal membaca kapabilitas: ${it.javaClass.simpleName}" }

    suspend fun mulai(): Boolean = runCatching {
        val info = client.getCurrentExerciseInfoAsync().await()
        if (info.exerciseTrackedStatus == ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS) {
            _status.value = "aplikasi lain sedang merekam"
            return false
        }
        val caps = client.getCapabilitiesAsync().await()
        val didukung = caps.getExerciseTypeCapabilities(ExerciseType.HIKING).supportedDataTypes
        val diminta = mutableSetOf<DataType<*, *>>()
        if (DataType.HEART_RATE_BPM in didukung) diminta += DataType.HEART_RATE_BPM
        if (DataType.LOCATION in didukung) diminta += DataType.LOCATION
        val config = ExerciseConfig.builder(ExerciseType.HIKING)
            .setDataTypes(diminta)
            .setIsAutoPauseAndResumeEnabled(false)
            .setIsGpsEnabled(DataType.LOCATION in diminta)
            .build()
        client.setUpdateCallback(callback)
        if (info.exerciseTrackedStatus == ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS) {
            // Proses sempat mati; sesi lama masih berjalan — cukup pasang callback lagi.
            sesiAktif = true
            _status.value = "melanjutkan sesi"
            return true
        }
        client.startExerciseAsync(config).await()
        sesiAktif = true
        _status.value = "sesi berjalan"
        true
    }.getOrElse {
        Timber.w(it, "Gagal memulai exercise session")
        _status.value = "gagal: ${it.javaClass.simpleName}"
        false
    }

    suspend fun selesai() {
        runCatching {
            if (sesiAktif) client.endExerciseAsync().await()
            client.clearUpdateCallbackAsync(callback).await()
        }.onFailure { Timber.w(it, "Gagal mengakhiri exercise session") }
        sesiAktif = false
        _status.value = "selesai"
    }
}
