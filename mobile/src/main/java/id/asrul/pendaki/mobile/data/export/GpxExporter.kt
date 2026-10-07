package id.asrul.pendaki.mobile.data.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import id.asrul.pendaki.shared.format.Format
import id.asrul.pendaki.shared.gpx.GpxOptions
import id.asrul.pendaki.shared.gpx.GpxWriter
import id.asrul.pendaki.shared.model.SesiPendakian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class HasilGpx(val namaFile: String, val isi: String, val ukuranByte: Int, val jumlahTitik: Int)

@Singleton
class GpxExporter @Inject constructor(@ApplicationContext private val ctx: Context) {

    fun buat(s: SesiPendakian, opsi: GpxOptions): HasilGpx {
        val isi = GpxWriter.tulis(s, opsi)
        return HasilGpx(Format.namaFileGpx(s.namaGunung, s.namaJalur, s.mulai), isi, isi.toByteArray().size, s.titik.size)
    }

    /** Bagikan lewat ACTION_SEND dengan FileProvider. */
    suspend fun bagikan(h: HasilGpx) = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "gpx").apply { mkdirs() }
        val f = File(dir, h.namaFile).apply { writeText(h.isi) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, h.namaFile)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, h.namaFile).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Simpan ke folder Download. Mengembalikan lokasi yang bisa ditampilkan. */
    suspend fun simpanKeDownload(h: HasilGpx): String = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, h.namaFile)
                put(MediaStore.Downloads.MIME_TYPE, "application/gpx+xml")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Gagal membuat file")
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(h.isi.toByteArray()) } ?: error("Gagal menulis file")
            "Download/${h.namaFile}"
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).apply { mkdirs() }
            File(dir, h.namaFile).writeText(h.isi)
            "Download/${h.namaFile}"
        }
    }
}
