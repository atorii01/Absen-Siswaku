package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class PresensiHarianResponse(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("siswa_id")
    val siswaId: Int? = null,
    @SerializedName("id_kelas")
    val idKelas: Int? = null,
    @SerializedName("tanggal")
    val tanggal: String? = null,
    @SerializedName("waktu_masuk")
    val waktuMasuk: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("biometrik_match_score")
    val biometrikScore: Double? = null,
    @SerializedName("jarak_gerbang_meter")
    val jarakGerbang: Double? = null
)
