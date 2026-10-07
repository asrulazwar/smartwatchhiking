package id.asrul.pendaki.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/** Heading kompas (derajat dari utara) dari rotation vector. */
class CompassSource @Inject constructor(private val sm: SensorManager) {
    val tersedia: Boolean get() = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null

    fun heading(): Flow<Float> = callbackFlow {
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) { close(); return@callbackFlow }
        val rot = FloatArray(9)
        val orient = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, orient)
                val az = (Math.toDegrees(orient[0].toDouble()).toFloat() + 360f) % 360f
                trySend(az)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sm.unregisterListener(listener) }
    }
}
