package id.asrul.pendaki.mobile.data.datalayer

import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import id.asrul.pendaki.mobile.data.db.SesiHpDao
import id.asrul.pendaki.mobile.data.db.SesiHpEntity
import id.asrul.pendaki.mobile.data.health.HealthConnectManager
import id.asrul.pendaki.shared.datalayer.DataLayerContract
import id.asrul.pendaki.shared.datalayer.SesiCodec
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

/** Menerima sesi dari jam (ChannelClient) dan melayani permintaan snapshot Health Connect. */
@AndroidEntryPoint
class HpListenerService : WearableListenerService() {

    @Inject lateinit var dao: SesiHpDao
    @Inject lateinit var healthConnect: HealthConnectManager

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        val path = channel.path
        if (!path.startsWith(DataLayerContract.PATH_SESI_CHANNEL)) return
        val client = Wearable.getChannelClient(this)
        runCatching {
            val bytes = Tasks.await(client.getInputStream(channel)).use { it.readBytes() }
            val sesi = SesiCodec.dariGzip(bytes)
            runBlocking { dao.simpan(SesiHpEntity.dari(sesi, bytes)) }
            Timber.i("Sesi %s diterima (%d byte, %d titik)", sesi.id, bytes.size, sesi.titik.size)
            Tasks.await(Wearable.getMessageClient(this).sendMessage(channel.nodeId, DataLayerContract.PATH_SESI_DITERIMA, sesi.id.toByteArray(Charsets.UTF_8)))
        }.onFailure { Timber.w(it, "Gagal menerima sesi dari jam") }
        runCatching { Tasks.await(client.close(channel)) }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != DataLayerContract.PATH_MINTA_HC) return
        runCatching {
            val snap = runBlocking { healthConnect.bacaSnapshot() }
            val req = PutDataMapRequest.create(DataLayerContract.PATH_HC_SNAPSHOT).apply {
                dataMap.putString(DataLayerContract.KEY_JSON, SesiCodec.hcKeJson(snap))
                dataMap.putLong(DataLayerContract.KEY_WAKTU, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            Tasks.await(Wearable.getDataClient(this).putDataItem(req))
        }.onFailure { Timber.w(it, "Gagal mengirim snapshot Health Connect") }
    }
}
