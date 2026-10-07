package id.asrul.pendaki.mobile.data.datalayer

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import id.asrul.pendaki.shared.datalayer.DataLayerContract
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** HP → jam: meminta jam mengirim ulang sesi. */
@Singleton
class JamLink @Inject constructor(@ApplicationContext private val ctx: Context) {
    suspend fun nodeJam(): String? = runCatching {
        val info = Wearable.getCapabilityClient(ctx).getCapability(DataLayerContract.KAPABILITAS_JAM, CapabilityClient.FILTER_REACHABLE).await()
        info.nodes.firstOrNull { it.isNearby }?.id ?: info.nodes.firstOrNull()?.id
    }.getOrElse { Timber.w(it, "Gagal mencari node jam"); null }

    /** Minta semua sesi (true) atau hanya yang belum terkirim (false). Mengembalikan false bila jam tidak terhubung. */
    suspend fun mintaSesi(semua: Boolean): Boolean {
        val node = nodeJam() ?: return false
        return runCatching {
            Wearable.getMessageClient(ctx).sendMessage(node, DataLayerContract.PATH_MINTA_SESI, (if (semua) "semua" else "").toByteArray(Charsets.UTF_8)).await()
            true
        }.getOrElse { Timber.w(it, "Gagal meminta sesi dari jam"); false }
    }
}
