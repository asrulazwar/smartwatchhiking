package id.asrul.pendaki.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/** Hitungan langkah kumulatif sejak boot dari `TYPE_STEP_COUNTER` (izin ACTIVITY_RECOGNITION). */
class StepSource @Inject constructor(private val sm: SensorManager) {
    val tersedia: Boolean get() = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    fun hitungan(): Flow<Long> = callbackFlow {
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) { close(); return@callbackFlow }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { trySend(e.values[0].toLong()) }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        // Batching sampai 10 detik: hemat daya, cukup untuk tampilan.
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, 10_000_000)
        awaitClose { sm.unregisterListener(listener) }
    }
}

/** Detak jantung langsung dari `TYPE_HEART_RATE` (fallback bila Health Services tidak tersedia). */
class HeartRateSensorSource @Inject constructor(private val sm: SensorManager) {
    val tersedia: Boolean get() = sm.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null

    fun detak(): Flow<Pair<Long, Int>> = callbackFlow {
        val sensor = sm.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (sensor == null) { close(); return@callbackFlow }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val bpm = e.values[0].toInt()
                if (bpm in 25..250) trySend(System.currentTimeMillis() to bpm)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sm.unregisterListener(listener) }
    }
}
