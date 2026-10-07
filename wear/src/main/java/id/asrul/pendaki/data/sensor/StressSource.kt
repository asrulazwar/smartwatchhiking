package id.asrul.pendaki.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import id.asrul.pendaki.data.datalayer.PhoneLink
import id.asrul.pendaki.shared.stress.StressCalculator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class HasilHrv(val rmssdMs: Double, val waktuMs: Long, val sumber: String)

/** Sumber HRV (RMSSD) untuk stres. */
interface StressSource {
    val nama: String
    val tersedia: Boolean
    /** Satu pengukuran RMSSD (butuh pengguna diam ≥ 3 menit). */
    fun ukurRmssd(): Flow<HasilHrv>
}

/**
 * (a) Hitung sendiri dari interval RR bila ada sensor yang memberinya. Health Services 1.0 tidak
 * mengekspos RR; dicari sensor vendor bernama "rr"/"hrv"/"heart_beat". PERLU VERIFIKASI di perangkat.
 */
@Singleton
class SensorManagerStressSource @Inject constructor(private val sm: SensorManager) : StressSource {
    override val nama = "Sensor RR"
    val sensor: Sensor? by lazy {
        sm.getSensorList(Sensor.TYPE_ALL).firstOrNull { s ->
            val n = (s.name + " " + s.stringType).lowercase()
            n.contains("hrv") || n.contains("rr_interval") || n.contains("rr-interval") || n.contains("ibi")
        }
    }
    override val tersedia: Boolean get() = sensor != null

    /** Kumpulkan interval RR selama 60 detik lalu emit satu RMSSD. */
    override fun ukurRmssd(): Flow<HasilHrv> = callbackFlow {
        val s = sensor
        if (s == null) { close(); return@callbackFlow }
        val rr = mutableListOf<Double>()
        val mulai = System.currentTimeMillis()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val v = e.values.firstOrNull()?.toDouble() ?: return
                // interval RR biasanya dalam ms (300–2000). Nilai lain diabaikan.
                if (v in 300.0..2000.0) rr += v
                if (System.currentTimeMillis() - mulai >= 60_000 && rr.size >= 20) {
                    StressCalculator.rmssd(rr)?.let { trySend(HasilHrv(it, System.currentTimeMillis(), nama)) }
                    close()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, s, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sm.unregisterListener(listener) }
    }
}

/** (b) Fallback: `HeartRateVariabilityRmssdRecord` dari Health Connect lewat HP. */
@Singleton
class HealthConnectStressSource @Inject constructor(private val phone: PhoneLink) : StressSource {
    override val nama = "Health Connect (HP)"
    override val tersedia: Boolean get() = true

    override fun ukurRmssd(): Flow<HasilHrv> = flow {
        val sebelum = phone.hcSnapshot.value?.hrvWaktu
        phone.mintaSnapshotHc()
        val snap = withTimeoutOrNull(25_000) {
            phone.hcSnapshot.first { it != null && it.hrvRmssdMs != null && it.hrvWaktu != sebelum }
        } ?: phone.hcSnapshot.value
        val v = snap?.hrvRmssdMs
        val w = snap?.hrvWaktu
        if (v != null && w != null) emit(HasilHrv(v, w, nama))
    }
}
