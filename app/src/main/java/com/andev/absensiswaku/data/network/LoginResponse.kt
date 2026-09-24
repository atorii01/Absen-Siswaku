package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("message")
    val message: String?,

    @SerializedName("role")
    val role: String?,

    @SerializedName("data")
    val data: UserData?
)

data class UserData(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("nisn")
    val nisn: String?,

    @SerializedName("nama_lengkap")
    val namaLengkap: String?,

    @SerializedName("kelas_id")
    val kelasId: Int?,

    @SerializedName("nama_kelas")
    val namaKelas: String?,

    @SerializedName("jurusan")
    val jurusan: String?,

    @SerializedName("wali_kelas")
    val waliKelas: String?,

    @SerializedName("username")
    val username: String?
)
