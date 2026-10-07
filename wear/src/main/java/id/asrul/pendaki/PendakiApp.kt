package id.asrul.pendaki

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PendakiApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Release: hanya WARN/ERROR tanpa isi pesan yang bisa memuat lokasi.
            Timber.plant(ReleaseTree())
        }
        buatChannel()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    private fun buatChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REKAM, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PERINGATAN, getString(R.string.notif_channel_peringatan), NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private class ReleaseTree : Timber.Tree() {
        override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= android.util.Log.WARN
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Jangan tulis pesan (bisa berisi koordinat); cukup kelas exception.
            if (t != null) android.util.Log.println(priority, tag ?: "Pendaki", t.javaClass.simpleName)
        }
    }

    companion object {
        const val CHANNEL_REKAM = "rekam"
        const val CHANNEL_PERINGATAN = "peringatan"
    }
}
