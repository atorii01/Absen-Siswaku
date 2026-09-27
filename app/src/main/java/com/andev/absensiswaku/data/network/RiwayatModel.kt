package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class RiwayatModel(
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
    @SerializedName("jam_masuk")
    val jamMasukField: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("jarak_gerbang_meter")
    val jarakGerbangMeter: Double? = null,
    @SerializedName("biometrik_match_score")
    val biometrikMatchScore: Double? = null,
    @SerializedName("verifikator")
    val verifikator: String? = null,
    @SerializedName("jam_verifikasi")
    val jamVerifikasi: String? = null,
    @SerializedName("keterangan_status")
    val keteranganStatus: String? = null,
    @SerializedName("siswa")
    val siswa: SiswaMiniResponse? = null,
    @SerializedName("status_verifikasi")
    val statusVerifikasi: String? = null,
    @SerializedName("jenis_izin")
    val jenisIzin: String? = null
) {
    val displayJamMasuk: String
        get() = when {
            !waktuMasuk.isNullOrEmpty() -> waktuMasuk
            !jamMasukField.isNullOrEmpty() -> jamMasukField
            else -> "-"
        }

    val namaWalas: String
        get() = if (!verifikator.isNullOrEmpty()) verifikator else "Wali Kelas"

    val waktu: String
        get() = when {
            displayJamMasuk == "-" -> "-"
            displayJamMasuk.endsWith("WIB") -> displayJamMasuk
            else -> "$displayJamMasuk WIB"
        }

    val keterangan: String
        get() = keteranganStatus.orEmpty()
}