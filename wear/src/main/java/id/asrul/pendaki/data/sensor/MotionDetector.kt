package id.asrul.pendaki.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Deteksi diam/bergerak dari step detector + varians akselerometer.
 * `diam` = tidak ada langkah selama [jendelaMs] DAN varians magnitudo akselerasi rendah.
 * Juga mengekspos "tangan bergerak" untuk menandai pengukuran SpO2 yang meragukan.
 */
class MotionDetector @Inject constructor(private val sm: SensorManager) {
    private val _diam = MutableStateFlow(false)
    val diam: StateFlow<Boolean> = _diam

    private val _diamSejak = MutableStateFlow<Long?>(null)
    /** Waktu (ms) mulai diam; null jika sedang bergerak. */
    val diamSejak: StateFlow<Long?> = _diamSejak

    @Volatile private var langkahTerakhir = 0L
    @Volatile var variansAkselerasi: Double = 0.0
        private set
    private val mag = ArrayDeque<Double>()

    private var listenerLangkah: SensorEventListener? = null
    private var listenerAksel: SensorEventListener? = null
    private var job: Job? = null

    val stepTersedia: Boolean get() = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null

    fun mulai(scope: CoroutineScope, jendelaMs: Long = 20_000L) {
        if (job != null) return
        langkahTerakhir = System.currentTimeMillis()
        sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let { s ->
            listenerLangkah = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) { langkahTerakhir = System.currentTimeMillis() }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }.also { sm.registerListener(it, s, SensorManager.SENSOR_DELAY_NORMAL) }
        }
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { s ->
            listenerAksel = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    val m = sqrt((e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]).toDouble())
                    synchronized(mag) {
                        mag.addLast(m)
                        while (mag.size > 50) mag.removeFirst()
                        if (mag.size >= 10) {
                            val mean = mag.average()
                            variansAkselerasi = mag.sumOf { (it - mean) * (it - mean) } / mag.size
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }.also { sm.registerListener(it, s, 200_000) } // 5 Hz, hemat daya
        }
        job = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val tanpaLangkah = now - langkahTerakhir >= jendelaMs
                val tenang = variansAkselerasi < 0.6
                val diamSekarang = if (stepTersedia) tanpaLangkah && tenang else tenang
                if (diamSekarang != _diam.value) {
                    _diam.value = diamSekarang
                    _diamSejak.value = if (diamSekarang) now else null
                }
                delay(2_000)
            }
        }
    }

    /** Dipakai saat ukur SpO2: true jika tangan bergerak cukup kuat dalam beberapa detik terakhir. */
    fun tanganBergerak(): Boolean = variansAkselerasi > 1.5

    fun berhenti() {
        listenerLangkah?.let { sm.unregisterListener(it) }
        listenerAksel?.let { sm.unregisterListener(it) }
        listenerLangkah = null; listenerAksel = null
        job?.cancel(); job = null
        _diam.value = false; _diamSejak.value = null
    }
}
