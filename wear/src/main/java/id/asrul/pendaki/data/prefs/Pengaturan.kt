package id.asrul.pendaki.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import id.asrul.pendaki.shared.heart.HrThresholdMonitor
import id.asrul.pendaki.shared.ams.AmsRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pengaturan")

data class PengaturanData(
    val batasDetak: Int = HrThresholdMonitor.DEFAULT_BATAS,
    val usia: Int = 35,
    val ambangSpo2: Int = AmsRules.DEFAULT_AMBANG_SPO2,
    val gpsHemat: Boolean = false,
    /** 0 = mati. */
    val pengingatMinumMenit: Int = 30,
    val getar: Boolean = true,
    val suara: Boolean = true,
    val gunungTerakhirId: String? = null,
    val jalurTerakhir: String? = null,
) {
    val intervalGpsBergerakMs: Long get() = if (gpsHemat) 30_000L else 15_000L
    val intervalGpsDiamMs: Long get() = if (gpsHemat) 120_000L else 60_000L
}

/** Snapshot ringkas untuk Tile & komplikasi (ditulis engine, dibaca proses tile). */
data class SnapshotTile(
    val aktif: Boolean = false,
    val namaGunung: String? = null,
    val mulai: Long = 0,
    val ketinggianM: Double? = null,
    val sisaNaikM: Double? = null,
    val naikTotalM: Double = 0.0,
    val jarakM: Double = 0.0,
    val langkah: Int = 0,
    val hr: Int? = null,
    val hrRata: Int? = null,
    val spo2: Int? = null,
    val spo2Waktu: Long? = null,
    val posTerakhir: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val puncakElev: Int? = null,
    val basecampElev: Int? = null,
    val diperbarui: Long = 0,
)

@Singleton
class Pengaturan @Inject constructor(@ApplicationContext private val ctx: Context) {
    private object K {
        val BATAS = intPreferencesKey("batas_detak")
        val USIA = intPreferencesKey("usia")
        val AMBANG_SPO2 = intPreferencesKey("ambang_spo2")
        val GPS_HEMAT = booleanPreferencesKey("gps_hemat")
        val MINUM = intPreferencesKey("minum_menit")
        val GETAR = booleanPreferencesKey("getar")
        val SUARA = booleanPreferencesKey("suara")
        val GUNUNG_TERAKHIR = stringPreferencesKey("gunung_terakhir")
        val JALUR_TERAKHIR = stringPreferencesKey("jalur_terakhir")

        val T_AKTIF = booleanPreferencesKey("t_aktif")
        val T_GUNUNG = stringPreferencesKey("t_gunung")
        val T_MULAI = longPreferencesKey("t_mulai")
        val T_ALT = doublePreferencesKey("t_alt")
        val T_SISA = doublePreferencesKey("t_sisa")
        val T_NAIK = doublePreferencesKey("t_naik")
        val T_JARAK = doublePreferencesKey("t_jarak")
        val T_LANGKAH = intPreferencesKey("t_langkah")
        val T_HR = intPreferencesKey("t_hr")
        val T_HR_RATA = intPreferencesKey("t_hr_rata")
        val T_SPO2 = intPreferencesKey("t_spo2")
        val T_SPO2_WAKTU = longPreferencesKey("t_spo2_waktu")
        val T_POS = stringPreferencesKey("t_pos")
        val T_LAT = doublePreferencesKey("t_lat")
        val T_LON = doublePreferencesKey("t_lon")
        val T_PUNCAK = intPreferencesKey("t_puncak")
        val T_BASECAMP = intPreferencesKey("t_basecamp")
        val T_WAKTU = longPreferencesKey("t_waktu")
    }

    val data: Flow<PengaturanData> = ctx.dataStore.data.map { p ->
        PengaturanData(
            batasDetak = p[K.BATAS] ?: HrThresholdMonitor.DEFAULT_BATAS,
            usia = p[K.USIA] ?: 35,
            ambangSpo2 = p[K.AMBANG_SPO2] ?: AmsRules.DEFAULT_AMBANG_SPO2,
            gpsHemat = p[K.GPS_HEMAT] ?: false,
            pengingatMinumMenit = p[K.MINUM] ?: 30,
            getar = p[K.GETAR] ?: true,
            suara = p[K.SUARA] ?: true,
            gunungTerakhirId = p[K.GUNUNG_TERAKHIR],
            jalurTerakhir = p[K.JALUR_TERAKHIR],
        )
    }

    suspend fun sekarang(): PengaturanData = data.first()

    suspend fun setBatasDetak(v: Int) = ctx.dataStore.edit { it[K.BATAS] = v.coerceIn(HrThresholdMonitor.MIN_BATAS, HrThresholdMonitor.MAX_BATAS) }
    suspend fun setUsia(v: Int) = ctx.dataStore.edit { it[K.USIA] = v.coerceIn(10, 100) }
    suspend fun setAmbangSpo2(v: Int) = ctx.dataStore.edit { it[K.AMBANG_SPO2] = v.coerceIn(70, 95) }
    suspend fun setGpsHemat(v: Boolean) = ctx.dataStore.edit { it[K.GPS_HEMAT] = v }
    suspend fun setPengingatMinum(menit: Int) = ctx.dataStore.edit { it[K.MINUM] = menit.coerceIn(0, 120) }
    suspend fun setGetar(v: Boolean) = ctx.dataStore.edit { it[K.GETAR] = v }
    suspend fun setSuara(v: Boolean) = ctx.dataStore.edit { it[K.SUARA] = v }
    suspend fun setGunungTerakhir(gunungId: String, jalur: String?) = ctx.dataStore.edit {
        it[K.GUNUNG_TERAKHIR] = gunungId
        if (jalur == null) it.remove(K.JALUR_TERAKHIR) else it[K.JALUR_TERAKHIR] = jalur
    }

    val snapshotTile: Flow<SnapshotTile> = ctx.dataStore.data.map { p ->
        SnapshotTile(
            aktif = p[K.T_AKTIF] ?: false,
            namaGunung = p[K.T_GUNUNG],
            mulai = p[K.T_MULAI] ?: 0,
            ketinggianM = p[K.T_ALT],
            sisaNaikM = p[K.T_SISA],
            naikTotalM = p[K.T_NAIK] ?: 0.0,
            jarakM = p[K.T_JARAK] ?: 0.0,
            langkah = p[K.T_LANGKAH] ?: 0,
            hr = p[K.T_HR],
            hrRata = p[K.T_HR_RATA],
            spo2 = p[K.T_SPO2],
            spo2Waktu = p[K.T_SPO2_WAKTU],
            posTerakhir = p[K.T_POS],
            lat = p[K.T_LAT],
            lon = p[K.T_LON],
            puncakElev = p[K.T_PUNCAK],
            basecampElev = p[K.T_BASECAMP],
            diperbarui = p[K.T_WAKTU] ?: 0,
        )
    }

    suspend fun simpanSnapshotTile(s: SnapshotTile) = ctx.dataStore.edit { p ->
        p[K.T_AKTIF] = s.aktif
        s.namaGunung?.let { p[K.T_GUNUNG] = it } ?: p.remove(K.T_GUNUNG)
        p[K.T_MULAI] = s.mulai
        s.ketinggianM?.let { p[K.T_ALT] = it } ?: p.remove(K.T_ALT)
        s.sisaNaikM?.let { p[K.T_SISA] = it } ?: p.remove(K.T_SISA)
        p[K.T_NAIK] = s.naikTotalM
        p[K.T_JARAK] = s.jarakM
        p[K.T_LANGKAH] = s.langkah
        s.hr?.let { p[K.T_HR] = it } ?: p.remove(K.T_HR)
        s.hrRata?.let { p[K.T_HR_RATA] = it } ?: p.remove(K.T_HR_RATA)
        s.spo2?.let { p[K.T_SPO2] = it } ?: p.remove(K.T_SPO2)
        s.spo2Waktu?.let { p[K.T_SPO2_WAKTU] = it } ?: p.remove(K.T_SPO2_WAKTU)
        s.posTerakhir?.let { p[K.T_POS] = it } ?: p.remove(K.T_POS)
        s.lat?.let { p[K.T_LAT] = it } ?: p.remove(K.T_LAT)
        s.lon?.let { p[K.T_LON] = it } ?: p.remove(K.T_LON)
        s.puncakElev?.let { p[K.T_PUNCAK] = it } ?: p.remove(K.T_PUNCAK)
        s.basecampElev?.let { p[K.T_BASECAMP] = it } ?: p.remove(K.T_BASECAMP)
        p[K.T_WAKTU] = s.diperbarui
    }
}
