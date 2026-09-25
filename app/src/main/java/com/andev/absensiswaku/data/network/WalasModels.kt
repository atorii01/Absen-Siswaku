package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class PengajuanIzinResponse(
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
    var statusVerifikasi: String = "Pending",
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("verified_by")
    var verifiedBy: String? = null,
    @SerializedName("siswa")
    var siswa: SiswaMiniResponse? = null
)

data class SiswaMiniResponse(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("nama_lengkap")
    val namaLengkap: String? = null,
    @SerializedName("nisn")
    val nisn: String? = null,
    @SerializedName("id_kelas")
    val idKelas: Int? = null,
    @SerializedName("rombel_kelas")
    val rombelKelas: RombelKelasResponse? = null
)

data class SiswaHadirWalasModel(
    val nomorUrut: String,
    val siswaId: Int,
    val namaLengkap: String,
    val nisn: String,
    val waktuMasuk: String,
    val status: String = "Hadir",
    val verifiedAiGps: Boolean = true
)
