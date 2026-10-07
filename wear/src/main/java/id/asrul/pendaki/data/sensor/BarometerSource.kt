package id.asrul.pendaki.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/** Tekanan udara (hPa) dari `TYPE_PRESSURE`. Flow kosong jika sensor tidak ada. */
class BarometerSource @Inject constructor(private val sm: SensorManager) {
    val tersedia: Boolean get() = sm.getDefaultSensor(Sensor.TYPE_PRESSURE) != null

    /** Emit (waktuMs, hPa). Sampling ±1 Hz cukup; nilai difilter di [id.asrul.pendaki.shared.altitude.BaroAltimeter]. */
    fun tekanan(): Flow<Pair<Long, Double>> = callbackFlow {
        val sensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (sensor == null) { close(); return@callbackFlow }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                trySend(System.currentTimeMillis() to e.values[0].toDouble())
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, 1_000_000) // 1 Hz
        awaitClose { sm.unregisterListener(listener) }
    }
}
