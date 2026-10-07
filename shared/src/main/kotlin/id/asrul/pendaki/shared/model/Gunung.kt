package id.asrul.pendaki.shared.model

import kotlinx.serialization.Serializable

/** Satu gunung dari `assets/gunung.json`. Koordinat = puncak tertinggi, elevasi dalam mdpl. */
@Serializable
data class Gunung(
    val id: String,
    val nama: String,
    val lat: Double,
    val lon: Double,
    val elevasi: Int,
    val provinsi: String,
)

/** Pos pada sebuah jalur. Elevasi perkiraan boleh kosong jika tidak diketahui. */
@Serializable
data class PosJalur(
    val nama: String,
    val elevasiPerkiraan: Int? = null,
)

/** Jalur pendakian dari `assets/jalur.json`. */
@Serializable
data class Jalur(
    val gunungId: String,
    val namaJalur: String,
    val basecampElevasi: Int,
    val basecampLat: Double? = null,
    val basecampLon: Double? = null,
    val pos: List<PosJalur> = emptyList(),
) {
    val id: String get() = "$gunungId/$namaJalur"
}
