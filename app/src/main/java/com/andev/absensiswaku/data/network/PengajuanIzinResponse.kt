package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class PengajuanIzinResponse(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("siswa_id")
    val siswaId: Int? = null,
    @SerializedName("jenis_izin")
    val jenisIzin: String? = null,
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String? = null,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String? = null,
    @SerializedName("keterangan")
    val keterangan: String? = null,
    @SerializedName("bukti_berkas_url")
    val buktiBerkasUrl: String? = null,
    @SerializedName("status_verifikasi")
    var statusVerifikasi: String? = null,
    @SerializedName("verified_by")
    var verifiedBy: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null, // Timestamp UTC dari Supabase
    @SerializedName("siswa")
    var siswa: SiswaMiniResponse? = null
)
