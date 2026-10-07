package id.asrul.pendaki.shared.datalayer

/**
 * Kontrak Wearable Data Layer antara :wear dan :mobile. Kedua modul memakai applicationId yang sama
 * (id.asrul.pendaki) agar bisa berkomunikasi.
 */
object DataLayerContract {
    /** ChannelClient: payload JSON [id.asrul.pendaki.shared.model.SesiPendakian] (gzip). */
    const val PATH_SESI_CHANNEL = "/pendaki/sesi"

    /** DataClient: metadata ringan sesi (JSON [id.asrul.pendaki.shared.model.MetaSesi]). */
    const val PATH_META_PREFIX = "/pendaki/meta/"

    /** MessageClient: HP meminta jam mengirim ulang sesi tertentu (payload = id sesi). */
    const val PATH_MINTA_SESI = "/pendaki/minta-sesi"

    /** MessageClient: HP mengonfirmasi sesi diterima (payload = id sesi). */
    const val PATH_SESI_DITERIMA = "/pendaki/sesi-diterima"

    /** MessageClient: jam meminta data Health Connect (SpO2/HRV) dari HP. */
    const val PATH_MINTA_HC = "/pendaki/minta-hc"

    /** DataClient: HP menaruh pembacaan Health Connect terbaru (JSON [HcSnapshot]). */
    const val PATH_HC_SNAPSHOT = "/pendaki/hc-snapshot"

    const val KEY_JSON = "json"
    const val KEY_WAKTU = "waktu"

    /** Kapabilitas yang diiklankan HP agar jam bisa menemukan node yang menjalankan aplikasi pendamping. */
    const val KAPABILITAS_HP = "pendaki_hp"
    const val KAPABILITAS_JAM = "pendaki_jam"
}

@kotlinx.serialization.Serializable
data class HcSnapshot(
    val spo2Persen: Int? = null,
    val spo2Waktu: Long? = null,
    val hrvRmssdMs: Double? = null,
    val hrvWaktu: Long? = null,
    val dibuat: Long,
)
