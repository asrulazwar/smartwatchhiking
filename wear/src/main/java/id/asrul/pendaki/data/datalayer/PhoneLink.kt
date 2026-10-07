package id.asrul.pendaki.data.datalayer

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import id.asrul.pendaki.shared.datalayer.DataLayerContract
import id.asrul.pendaki.shared.datalayer.HcSnapshot
import id.asrul.pendaki.shared.datalayer.SesiCodec
import id.asrul.pendaki.shared.model.SesiPendakian
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Komunikasi jam → HP lewat Wearable Data Layer. */
@Singleton
class PhoneLink @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val channelClient by lazy { Wearable.getChannelClient(ctx) }
    private val dataClient by lazy { Wearable.getDataClient(ctx) }
    private val messageClient by lazy { Wearable.getMessageClient(ctx) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(ctx) }

    private val _hcSnapshot = MutableStateFlow<HcSnapshot?>(null)
    val hcSnapshot: StateFlow<HcSnapshot?> = _hcSnapshot

    private val _sesiDiterima = MutableStateFlow<Set<String>>(emptySet())
    /** Id sesi yang sudah dikonfirmasi diterima HP. */
    val sesiDiterima: StateFlow<Set<String>> = _sesiDiterima

    internal fun terimaSnapshotHc(s: HcSnapshot) { _hcSnapshot.value = s }
    internal fun terimaKonfirmasi(idSesi: String) { _sesiDiterima.value = _sesiDiterima.value + idSesi }

    /** Node HP yang menjalankan aplikasi pendamping, atau null. */
    suspend fun nodeHp(): String? = runCatching {
        val info = capabilityClient.getCapability(DataLayerContract.KAPABILITAS_HP, CapabilityClient.FILTER_REACHABLE).await()
        info.nodes.firstOrNull { it.isNearby }?.id ?: info.nodes.firstOrNull()?.id
    }.getOrElse { Timber.w(it, "Gagal mencari node HP"); null }

    suspend fun namaNodeHp(): String? = runCatching {
        Wearable.getNodeClient(ctx).connectedNodes.await().firstOrNull()?.displayName
    }.getOrNull()

    /**
     * Kirim sesi lengkap: metadata ringan via DataClient, payload gzip via ChannelClient.
     * Mengembalikan true jika payload berhasil ditulis (konfirmasi akhir datang lewat PATH_SESI_DITERIMA).
     */
    suspend fun kirimSesi(s: SesiPendakian): Boolean {
        val node = nodeHp() ?: return false
        return runCatching {
            val bytes = SesiCodec.keGzip(s)
            val meta = SesiCodec.meta(s, bytes.size.toLong())
            val req = PutDataMapRequest.create(DataLayerContract.PATH_META_PREFIX + s.id).apply {
                dataMap.putString(DataLayerContract.KEY_JSON, SesiCodec.metaKeJson(meta))
                dataMap.putLong(DataLayerContract.KEY_WAKTU, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(req).await()

            val channel = channelClient.openChannel(node, DataLayerContract.PATH_SESI_CHANNEL + "/" + s.id).await()
            channelClient.getOutputStream(channel).await().use { it.write(bytes); it.flush() }
            channelClient.close(channel).await()
            Timber.i("Sesi ${s.id} terkirim (${bytes.size} byte)")
            true
        }.getOrElse { Timber.w(it, "Gagal mengirim sesi"); false }
    }

    suspend fun mintaSnapshotHc() {
        val node = nodeHp() ?: return
        runCatching { messageClient.sendMessage(node, DataLayerContract.PATH_MINTA_HC, ByteArray(0)).await() }
            .onFailure { Timber.w(it, "Gagal meminta snapshot HC") }
    }
}
