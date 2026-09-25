package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class PengajuanIzinModel(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("siswa_id")
    val siswaId: Int = 0,
    @SerializedName("jenis_izin")
    val jenisIzin: String = "",
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String = "",
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String = "",
    @SerializedName("bukti_berkas_url")
    val buktiBerkasUrl: String? = null,
    @SerializedName("keterangan")
    val keterangan: String = "",
    @SerializedName("status_verifikasi")
    val statusVerifikasi: String = "Pending"
)