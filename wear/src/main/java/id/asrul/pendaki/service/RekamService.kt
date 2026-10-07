package id.asrul.pendaki.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import dagger.hilt.android.AndroidEntryPoint
import id.asrul.pendaki.MainActivity
import id.asrul.pendaki.PendakiApp
import id.asrul.pendaki.R
import id.asrul.pendaki.data.health.HealthServicesManager
import id.asrul.pendaki.domain.HikeEngine
import id.asrul.pendaki.shared.format.Format
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service yang menjaga [HikeEngine] tetap hidup selama pendakian, bersama
 * sesi Health Services dan OngoingActivity. START_STICKY: bila dimatikan sistem, sesi
 * dipulihkan dari Room saat service dihidupkan ulang.
 */
@AndroidEntryPoint
class RekamService : LifecycleService() {

    @Inject lateinit var engine: HikeEngine
    @Inject lateinit var health: HealthServicesManager

    private var berjalan = false

    override fun onCreate() {
        super.onCreate()
        tampilkanForeground(getString(R.string.app_name), "")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_HENTIKAN) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!berjalan) {
            berjalan = true
            lifecycleScope.launch {
                if (!engine.state.value.sedangAktif && !engine.pulihkan()) {
                    Timber.w("Tidak ada sesi aktif, service berhenti")
                    stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return@launch
                }
                health.mulai()
                engine.jalankanSensor()
                pantauNotifikasi()
            }
            lifecycleScope.launch { pantauKejadian() }
        }
        return START_STICKY
    }

    private suspend fun pantauNotifikasi() {
        while (true) {
            val st = engine.state.value
            if (!st.sedangAktif) break
            tampilkanForeground(st.judul, getString(R.string.notif_teks, Format.durasi(st.durasiMs), Format.ribuan(st.ketinggianM ?: 0.0)))
            delay(60_000)
        }
    }

    private suspend fun pantauKejadian() {
        engine.kejadian.collectLatest { k ->
            when (k) {
                is HikeEngine.Kejadian.PeringatanDetak -> notifPeringatan(2, getString(R.string.hr_tinggi), getString(R.string.hr_di_atas_batas, k.p.batas, k.p.durasiAtasMs / 1000))
                is HikeEngine.Kejadian.PeringatanAmsMuncul -> notifPeringatan(3, getString(R.string.ams_peringatan), getString(R.string.ams_saran))
                is HikeEngine.Kejadian.PuncakTerdeteksi -> notifPeringatan(4, getString(R.string.puncak_terdeteksi), Format.jam(k.info.waktu))
                is HikeEngine.Kejadian.PengingatMinum -> notifPeringatan(5, getString(R.string.minum_pengingat), "")
                is HikeEngine.Kejadian.PengingatSpO2 -> notifPeringatan(6, getString(R.string.spo2_pengingat), "")
                else -> Unit
            }
        }
    }

    private fun bukaApp(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun tampilkanForeground(judul: String, teks: String) {
        val builder = NotificationCompat.Builder(this, PendakiApp.CHANNEL_REKAM)
            .setSmallIcon(R.drawable.ic_mountain)
            .setContentTitle(judul.ifBlank { getString(R.string.notif_judul) })
            .setContentText(teks)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setContentIntent(bukaApp())
        OngoingActivity.Builder(applicationContext, NOTIF_ID, builder)
            .setStaticIcon(R.drawable.ic_mountain)
            .setTouchIntent(bukaApp())
            .setStatus(Status.Builder().addTemplate(teks.ifBlank { getString(R.string.notif_judul) }).build())
            .build()
            .apply(applicationContext)
        val tipe = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        ServiceCompat.startForeground(this, NOTIF_ID, builder.build(), tipe)
    }

    private fun notifPeringatan(id: Int, judul: String, teks: String) {
        val n: Notification = NotificationCompat.Builder(this, PendakiApp.CHANNEL_PERINGATAN)
            .setSmallIcon(R.drawable.ic_mountain)
            .setContentTitle(judul).setContentText(teks)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(bukaApp())
            .build()
        runCatching { NotificationManagerCompat.from(this).notify(id, n) }
    }

    override fun onDestroy() {
        engine.hentikanSensor()
        super.onDestroy()
    }

    companion object {
        const val NOTIF_ID = 1
        const val ACTION_HENTIKAN = "id.asrul.pendaki.HENTIKAN"

        fun mulai(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, RekamService::class.java))
        }

        fun hentikan(ctx: Context) {
            ctx.startService(Intent(ctx, RekamService::class.java).setAction(ACTION_HENTIKAN))
        }
    }
}
