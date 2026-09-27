package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RombelMapelResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nama_kelas") val namaKelas: String,
    @SerializedName("jurusan") val jurusan: String?,
    @SerializedName("wali_kelas_id") val waliKelasId: String?,
    @SerializedName("tahun_ajaran") val tahunAjaran: String?,
    @SerializedName("kapasitas_kuota") val kapasitasKuota: Int? = 36,
    @SerializedName("users") val waliKelas: WalasUserDetail?,
    var hadirCount: Int = 0,
    var izinCount: Int = 0,
    var alpaCount: Int = 0,
    var persentaseHadir: Float = 0f
) : Serializable {
    val kuota: Int
        get() = if (id == 8 || id == 10) 35 else (kapasitasKuota ?: 36)
}

data class WalasUserDetail(
    @SerializedName("nama_lengkap") val namaLengkap: String?
) : Serializable
