package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class SiswaMiniResponse(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("nama_lengkap")
    val namaLengkap: String? = null,
    @SerializedName("nisn")
    val nisn: String? = null,
    @SerializedName("id_kelas")
    val idKelas: Int? = null,
    @SerializedName("rombel_kelas")
    val rombelKelas: RombelKelasResponse? = null,
    @SerializedName("nik")
    val nik: String? = null,
    @SerializedName("jenis_kelamin")
    val jenisKelamin: String? = null,
    @SerializedName("no_whatsapp_wali")
    val noWhatsappWali: String? = null,
    @SerializedName("status_aktif")
    val statusAktif: Boolean? = true
) {
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

data class SiswaHadirWalasModel(
    val nomorUrut: String = "",
    val siswaId: Int? = null,
    val namaLengkap: String = "Siswa",
    val nisn: String = "-",
    val waktuMasuk: String = "-",
    val status: String? = "Hadir",
    val verifiedAiGps: Boolean = true
)
