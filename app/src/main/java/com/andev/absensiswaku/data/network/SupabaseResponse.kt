package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class SiswaResponse(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("nisn")
    val nisn: String?,

    @SerializedName("nama_lengkap")
    val namaLengkap: String?,

    @SerializedName("id_kelas")
    val idKelas: Int?,

    @SerializedName("rombel_kelas")
    val rombelKelas: RombelKelasResponse?
)

data class RombelKelasResponse(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("nama_kelas")
    val namaKelas: String?,

    @SerializedName("jurusan")
    val jurusan: String?,

    @SerializedName("users")
    val waliKelas: WalasUserResponse?
)

data class WalasUserResponse(
    @SerializedName("nama_lengkap")
    val namaLengkap: String?
)

data class UserResponse(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("nama_lengkap")
    val namaLengkap: String? = null,

    @SerializedName("role")
    val role: String? = "WALI_KELAS",

    @SerializedName("rombel_kelas")
    val rombelKelas: List<RombelKelasResponse>? = null
)
