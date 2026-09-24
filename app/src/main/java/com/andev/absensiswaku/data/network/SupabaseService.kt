package com.andev.absensiswaku.data.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseService {

    @GET("siswa")
    fun loginSiswa(
        @Query("nisn") nisnFilter: String,
        @Query("pin_presensi") pinFilter: String,
        @Query("select") select: String = "id,nisn,nama_lengkap,id_kelas,rombel_kelas(id,nama_kelas,jurusan,users(nama_lengkap))"
    ): Call<List<SiswaResponse>>

    @POST("presensi_harian")
    fun simpanPresensiSupabase(
        @Body request: PresensiRequest
    ): Call<List<PresensiResponse>>
}
