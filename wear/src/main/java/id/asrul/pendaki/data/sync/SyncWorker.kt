package id.asrul.pendaki.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import id.asrul.pendaki.data.datalayer.PhoneLink
import id.asrul.pendaki.data.db.SesiRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Mengirim ulang sesi yang belum dikonfirmasi HP. Dijadwalkan saat selesai dan dicoba lagi
 * dengan backoff sampai HP terhubung.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: SesiRepository,
    private val phone: PhoneLink,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val antre = repo.belumTerkirim()
        if (antre.isEmpty()) return Result.success()
        if (phone.nodeHp() == null) return Result.retry()
        var gagal = false
        for (e in antre) {
            val sesi = repo.muatLengkap(e.id) ?: continue
            val ok = phone.kirimSesi(sesi)
            if (!ok) { gagal = true; continue }
            // tunggu konfirmasi HP sebentar; kalau tidak ada, tetap tandai (payload sudah ditulis)
            withTimeoutOrNull(15_000) { phone.sesiDiterima.first { e.id in it } }
            repo.tandaiTerkirim(e.id)
            delay(500)
        }
        return if (gagal) Result.retry() else Result.success()
    }

    companion object {
        const val NAMA = "sync-sesi"
        fun jadwalkan(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(NAMA, ExistingWorkPolicy.KEEP, req)
            Timber.d("SyncWorker dijadwalkan")
        }
    }
}
