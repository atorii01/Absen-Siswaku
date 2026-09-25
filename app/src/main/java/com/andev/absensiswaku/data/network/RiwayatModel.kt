package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class RiwayatModel(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("siswa_id")
    val siswaId: Int = 0,
    @SerializedName("id_kelas")
    val idKelas: Int = 0,
    @SerializedName("tanggal")
    val tanggal: String = "",
    @SerializedName("waktu_masuk")
    val waktuMasuk: String? = null,
    @SerializedName("jam_masuk")
    val jamMasukField: String? = null,
    @SerializedName("status")
    val status: String = "Hadir",
    @SerializedName("jarak_gerbang_meter")
    val jarakGerbangMeter: Double = 0.0,
    @SerializedName("biometrik_match_score")
    val biometrikMatchScore: Double = 0.0,
    @SerializedName("verifikator")
    val verifikator: String? = null,
    @SerializedName("jam_verifikasi")
    val jamVerifikasi: String? = null,
    @SerializedName("keterangan_status")
    val keteranganStatus: String? = null,
    @SerializedName("siswa")
    val siswa: SiswaMiniResponse? = null
) {
    val displayJamMasuk: String
        get() = when {
            !waktuMasuk.isNullOrEmpty() -> waktuMasuk
            !jamMasukField.isNullOrEmpty() -> jamMasukField
            else -> "-"
        }
}