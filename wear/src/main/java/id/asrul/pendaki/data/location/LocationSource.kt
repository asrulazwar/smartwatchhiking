package id.asrul.pendaki.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class FixGps(
    val waktuMs: Long,
    val lat: Double,
    val lon: Double,
    val altM: Double?,
    val akurasiM: Float?,
    val kecepatanMps: Float?,
    val bearingDeg: Float?,
)

/** GPS lewat LocationManager (tanpa Play Services Location). */
@Singleton
class LocationSource @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val lm: LocationManager,
) {
    val izinAda: Boolean
        get() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val gpsAktif: Boolean get() = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    fun aliran(intervalMs: Long, jarakMinM: Float = 0f): Flow<FixGps> = callbackFlow {
        if (!izinAda) { close(); return@callbackFlow }
        val listener = LocationListener { loc -> trySend(loc.keFix()) }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalMs, jarakMinM, listener, Looper.getMainLooper())
        awaitClose { lm.removeUpdates(listener) }
    }

    /** Satu fix dalam batas waktu; null jika tidak terkunci. */
    suspend fun satuFix(timeoutMs: Long = 20_000L): FixGps? {
        if (!izinAda) return null
        return withTimeoutOrNull(timeoutMs) { aliran(1_000L).first() }
    }

    @SuppressLint("MissingPermission")
    fun terakhirDikenal(): FixGps? =
        if (!izinAda) null else runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.keFix() }.getOrNull()

    private fun Location.keFix() = FixGps(
        waktuMs = time.takeIf { it > 0 } ?: System.currentTimeMillis(),
        lat = latitude, lon = longitude,
        altM = if (hasAltitude()) altitude else null,
        akurasiM = if (hasAccuracy()) accuracy else null,
        kecepatanMps = if (hasSpeed()) speed else null,
        bearingDeg = if (hasBearing()) bearing else null,
    )
}
