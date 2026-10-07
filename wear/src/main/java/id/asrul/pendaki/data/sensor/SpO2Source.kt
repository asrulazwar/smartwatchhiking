package id.asrul.pendaki.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import id.asrul.pendaki.data.datalayer.PhoneLink
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class HasilSpO2(val persen: Int, val waktuMs: Long, val sumber: String)

/** Satu antarmuka untuk dua jalur SpO2: sensor vendor via SensorManager, atau Health Connect via HP. */
interface SpO2Source {
    val nama: String
    val tersedia: Boolean
    /** Emit pembacaan selama pengukuran berlangsung; pemanggil membatasi durasi (30 detik). */
    fun ukur(): Flow<HasilSpO2>
}

/**
 * (a) SensorManager: cari sensor vendor dengan nama/tipe mengandung "spo2"/"oxygen".
 * PERLU VERIFIKASI di OnePlus Watch 2: apakah sensor ini diekspos ke aplikasi pihak ketiga.
 */
@Singleton
class SensorManagerSpO2Source @Inject constructor(private val sm: SensorManager) : SpO2Source {
    override val nama = "SensorManager"
    val sensor: Sensor? by lazy {
        sm.getSensorList(Sensor.TYPE_ALL).firstOrNull { s ->
            val n = (s.name + " " + s.stringType).lowercase()
            n.contains("spo2") || n.contains("oxygen") || n.contains("blood_oxygen")
        }
    }
    override val tersedia: Boolean get() = sensor != null

    override fun ukur(): Flow<HasilSpO2> = callbackFlow {
        val s = sensor
        if (s == null) { close(); return@callbackFlow }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                // Konvensi vendor bervariasi: nilai pertama biasanya persen (0–100) atau fraksi (0–1).
                val v = e.values.firstOrNull() ?: return
                val persen = if (v <= 1.0f) (v * 100).toInt() else v.toInt()
                if (persen in 50..100) trySend(HasilSpO2(persen, System.currentTimeMillis(), nama))
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, s, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sm.unregisterListener(listener) }
    }
}

/** (b) Fallback: pembacaan `OxygenSaturationRecord` terbaru dari Health Connect di HP lewat Data Layer. */
@Singleton
class HealthConnectSpO2Source @Inject constructor(private val phone: PhoneLink) : SpO2Source {
    override val nama = "Health Connect (HP)"
    override val tersedia: Boolean get() = true

    override fun ukur(): Flow<HasilSpO2> = flow {
        val sebelum = phone.hcSnapshot.value?.spo2Waktu
        phone.mintaSnapshotHc()
        val snap = withTimeoutOrNull(25_000) {
            phone.hcSnapshot.first { it != null && it.spo2Persen != null && it.spo2Waktu != sebelum }
        } ?: phone.hcSnapshot.value
        val persen = snap?.spo2Persen
        val waktu = snap?.spo2Waktu
        if (persen != null && waktu != null) emit(HasilSpO2(persen, waktu, nama))
        else Timber.w("Tidak ada SpO2 dari Health Connect")
    }
}

/** Daftar semua sensor (untuk layar debug + log pertama kali). */
fun SensorManager.daftarSensor(): List<String> =
    getSensorList(Sensor.TYPE_ALL).map { "${it.name} · ${it.stringType} · ${it.vendor}" }

