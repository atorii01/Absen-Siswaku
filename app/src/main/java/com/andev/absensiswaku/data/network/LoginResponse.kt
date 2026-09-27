package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success")
    val success: Boolean? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("role")
    val role: String? = null,

    @SerializedName("data")
    val data: UserData? = null
)

data class UserData(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("nisn")
    val nisn: String? = null,

    @SerializedName("nama_lengkap")
    val namaLengkap: String? = null,

    @SerializedName("kelas_id")
    val kelasId: Int? = null,

    @SerializedName("nama_kelas")
    val namaKelas: String? = null,

    @SerializedName("jurusan")
    val jurusan: String? = null,

    @SerializedName("wali_kelas")
    val waliKelas: String? = null,

    @SerializedName("username")
    val username: String? = null
)
