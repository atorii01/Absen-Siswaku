package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class PresensiRequest(
    @SerializedName("siswa_id")
    val siswaId: Int,
    @SerializedName("id_kelas")
    val idKelas: Int,
    @SerializedName("jarak_meter")
    val jarakMeter: Int,
    @SerializedName("biometrik_match_score")
    val biometrikMatchScore: Double = 95.0,
    @SerializedName("tipe")
    val tipe: String = "MASUK",
    @SerializedName("status")
    val status: String = "HADIR"
)

data class PresensiResponse(
    @SerializedName("success")
    val success: Boolean = true,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("status_presensi")
    val statusPresensi: String? = null
)