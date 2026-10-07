package id.asrul.pendaki.shared.model

import kotlinx.serialization.json.Json

/** Pemuat `gunung.json` / `jalur.json` (format bersama untuk assets jam dan resource :shared). */
object DataGunung {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseGunung(text: String): List<Gunung> = json.decodeFromString(text)
    fun parseJalur(text: String): List<Jalur> = json.decodeFromString(text)

    /** Data bawaan yang dibundel di :shared (dipakai untuk unit test dan sebagai fallback). */
    fun gunungBawaan(): List<Gunung> = parseGunung(baca("/gunung.json"))
    fun jalurBawaan(): List<Jalur> = parseJalur(baca("/jalur.json"))

    private fun baca(path: String): String =
        DataGunung::class.java.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: error("Resource $path tidak ditemukan")
}
