package id.asrul.pendaki.data.datalayer

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import id.asrul.pendaki.shared.datalayer.DataLayerContract
import id.asrul.pendaki.shared.datalayer.SesiCodec
import timber.log.Timber
import javax.inject.Inject

/** Menerima snapshot Health Connect dan konfirmasi sesi dari HP. */
@AndroidEntryPoint
class JamListenerService : WearableListenerService() {
    @Inject lateinit var phone: PhoneLink
    @Inject lateinit var repo: id.asrul.pendaki.data.db.SesiRepository

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (ev in dataEvents) {
            if (ev.type != DataEvent.TYPE_CHANGED) continue
            val path = ev.dataItem.uri.path ?: continue
            if (path == DataLayerContract.PATH_HC_SNAPSHOT) {
                val json = DataMapItem.fromDataItem(ev.dataItem).dataMap.getString(DataLayerContract.KEY_JSON) ?: continue
                runCatching { phone.terimaSnapshotHc(SesiCodec.hcDariJson(json)) }
                    .onFailure { Timber.w(it, "Snapshot HC tidak valid") }
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            DataLayerContract.PATH_MINTA_SESI -> {
                val semua = String(event.data, Charsets.UTF_8) == "semua"
                kotlinx.coroutines.runBlocking { if (semua) repo.tandaiSemuaBelumTerkirim() }
                id.asrul.pendaki.data.sync.SyncWorker.jadwalkan(this)
                Timber.i("HP meminta sesi (semua=%s)", semua)
            }
            DataLayerContract.PATH_SESI_DITERIMA -> {
                val id = String(event.data, Charsets.UTF_8)
                phone.terimaKonfirmasi(id)
                Timber.i("HP mengonfirmasi sesi $id")
            }
        }
    }
}
