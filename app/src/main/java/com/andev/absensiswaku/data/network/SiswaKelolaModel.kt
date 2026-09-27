package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class SiswaKelolaResponse(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("nisn") val nisn: String? = null,
    @SerializedName("nama_lengkap") val namaLengkap: String? = null,
    @SerializedName("id_kelas") val idKelas: Int? = null,
    @SerializedName("jenis_kelamin") val jenisKelamin: String? = null,
    @SerializedName("nik") val nik: String? = null,
    @SerializedName("no_wa_orang_tua") val noWa: String? = null,
    @SerializedName("no_whatsapp_wali") val noWhatsappWali: String? = null,
    @SerializedName("status_aktif") val statusAktif: Boolean? = true,
    @SerializedName("rombel_kelas") val rombelKelas: RombelKelasResponse? = null
) {
    val displayNoWa: String
        get() = noWa?.takeIf { it.isNotEmpty() }
            ?: noWhatsappWali?.takeIf { it.isNotEmpty() }
            ?: "-"

    val initialLetters: String
        get() {
            val name = namaLengkap?.trim().orEmpty()
            if (name.isEmpty()) return "SW"
            val parts = name.split(" ").filter { it.isNotEmpty() }
            return when {
                parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
                parts.size == 1 && parts[0].length >= 2 -> parts[0].substring(0, 2).uppercase()
                parts.size == 1 -> parts[0].uppercase()
                else -> "SW"
            }
        }
}

typealias SiswaKelolaModel = SiswaKelolaResponse
