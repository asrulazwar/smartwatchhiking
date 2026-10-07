package id.asrul.pendaki.shared.datalayer

import id.asrul.pendaki.shared.model.MetaSesi
import id.asrul.pendaki.shared.model.SesiPendakian
import id.asrul.pendaki.shared.stats.SessionStats
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Serialisasi sesi untuk Data Layer dan penyimpanan: JSON + gzip. */
object SesiCodec {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun keJson(s: SesiPendakian): String = json.encodeToString(SesiPendakian.serializer(), s)
    fun dariJson(text: String): SesiPendakian = json.decodeFromString(SesiPendakian.serializer(), text)

    fun keGzip(s: SesiPendakian): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(keJson(s).toByteArray(Charsets.UTF_8)) }
        return bos.toByteArray()
    }

    fun dariGzip(bytes: ByteArray): SesiPendakian =
        GZIPInputStream(ByteArrayInputStream(bytes)).use { dariJson(it.readBytes().toString(Charsets.UTF_8)) }

    fun meta(s: SesiPendakian, ukuranByte: Long): MetaSesi {
        val r = SessionStats.ringkasan(s)
        return MetaSesi(
            id = s.id, judul = s.judul, mulai = s.mulai, selesai = s.selesai,
            durasiMs = r.durasiMs, jarakM = r.jarakM, naikTotalM = r.naikTotalM, hrRata = r.hrRata,
            jumlahPos = r.jumlahPos, jumlahTitik = r.jumlahTitik, ukuranByte = ukuranByte,
        )
    }

    fun metaKeJson(m: MetaSesi): String = json.encodeToString(MetaSesi.serializer(), m)
    fun metaDariJson(t: String): MetaSesi = json.decodeFromString(MetaSesi.serializer(), t)
    fun hcKeJson(h: HcSnapshot): String = json.encodeToString(HcSnapshot.serializer(), h)
    fun hcDariJson(t: String): HcSnapshot = json.decodeFromString(HcSnapshot.serializer(), t)
}
