package com.andev.absensiswaku.data.network

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {

    @FormUrlEncoded
    @POST("login.php")
    fun loginSiswa(
        @Field("role_tab") roleTab: String = "SISWA",
        @Field("nisn") nisn: String,
        @Field("pin") pin: String
    ): Call<LoginResponse>

    @FormUrlEncoded
    @POST("login.php")
    fun loginGuru(
        @Field("role_tab") roleTab: String = "GURU",
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    @FormUrlEncoded
    @POST("login.php")
    fun loginFlex(
        @FieldMap fields: Map<String, String>
    ): Call<LoginResponse>

    @FormUrlEncoded
    @POST("simpan_presensi.php")
    fun simpanPresensi(
        @Field("siswa_id") siswaId: Int,
        @Field("id_kelas") idKelas: Int,
        @Field("jarak_meter") jarakMeter: Int,
        @Field("biometrik_match_score") matchScore: Double = 95.0,
        @Field("tipe") tipe: String = "MASUK",
        @Field("status") status: String = "HADIR"
    ): Call<PresensiResponse>
}
