package id.asrul.pendaki.di

import android.content.Context
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.room.Room
import id.asrul.pendaki.data.db.PendakiDatabase
import id.asrul.pendaki.data.sensor.HealthConnectSpO2Source
import id.asrul.pendaki.data.sensor.HealthConnectStressSource
import id.asrul.pendaki.data.sensor.SensorManagerSpO2Source
import id.asrul.pendaki.data.sensor.SensorManagerStressSource
import id.asrul.pendaki.data.sensor.SpO2Source
import id.asrul.pendaki.data.sensor.StressSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Primer

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Fallback

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun database(@ApplicationContext ctx: Context): PendakiDatabase =
        Room.databaseBuilder(ctx, PendakiDatabase::class.java, "pendaki.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun sesiDao(db: PendakiDatabase) = db.sesiDao()
    @Provides fun titikDao(db: PendakiDatabase) = db.titikDao()
    @Provides fun waypointDao(db: PendakiDatabase) = db.waypointDao()
    @Provides fun sampelDao(db: PendakiDatabase) = db.sampelDao()

    @Provides
    fun sensorManager(@ApplicationContext ctx: Context): SensorManager =
        ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    @Provides
    fun locationManager(@ApplicationContext ctx: Context): LocationManager =
        ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Provides @Primer fun spo2Primer(s: SensorManagerSpO2Source): SpO2Source = s
    @Provides @Fallback fun spo2Fallback(s: HealthConnectSpO2Source): SpO2Source = s
    @Provides @Primer fun stresPrimer(s: SensorManagerStressSource): StressSource = s
    @Provides @Fallback fun stresFallback(s: HealthConnectStressSource): StressSource = s
}
