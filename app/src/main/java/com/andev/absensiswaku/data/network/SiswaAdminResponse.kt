package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName
import java.util.Locale

data class SiswaAdminResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nisn") val nisn: String,
    @SerializedName("nama_lengkap") val namaLengkap: String,
    @SerializedName("id_kelas") val idKelas: Int,
    @SerializedName("nik") val nik: String? = null,
    @SerializedName("jenis_kelamin") val jenisKelamin: String? = null,
    @SerializedName("no_wa_orang_tua") val noWaOrangTua: String? = null,
    @SerializedName("rombel_kelas") val rombelKelas: RombelInfoResponse? = null
) {
    val initialLetters: String
        get() {
            val parts = namaLengkap.trim().split(" ").filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> "S"
                parts.size == 1 -> parts[0].take(2).uppercase(Locale.getDefault())
                else -> "${parts[0].first()}${parts[1].first()}".uppercase(Locale.getDefault())
            }
        }
}

data class RombelInfoResponse(
    @SerializedName("nama_kelas") val namaKelas: String? = null
)
