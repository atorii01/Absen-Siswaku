package com.andev.absensiswaku.data.network

import com.google.gson.annotations.SerializedName

data class SiswaResponse(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("nisn")
    val nisn: String? = null,

    @SerializedName("nama_lengkap")
    val namaLengkap: String? = null,

    @SerializedName("id_kelas")
    val idKelas: Int? = null,

    @SerializedName("rombel_kelas")
    val rombelKelas: RombelKelasResponse? = null
)

data class RombelKelasResponse(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("nama_kelas")
    val namaKelas: String? = null,

    @SerializedName("jurusan")
    val jurusan: String? = null,

    @SerializedName("kapasitas_kuota")
    val kapasitasKuota: Int? = 36,

    @SerializedName("users")
    val waliKelas: WalasUserResponse? = null
)

data class WalasUserResponse(
    @SerializedName("nama_lengkap")
    val namaLengkap: String? = null
)

data class UserResponse(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("password_hash")
    val passwordHash: String? = null,

    @SerializedName("nama_lengkap")
    val namaLengkap: String? = null,

    @SerializedName("role")
    val role: String? = "WALI_KELAS",

    @SerializedName("mata_pelajaran")
    val mataPelajaran: String? = null,

    @SerializedName("rombel_kelas")
    val rombelKelas: List<RombelKelasResponse>? = null
)

data class KonfigurasiSistemResponse(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("nama_sekolah")
    val namaSekolah: String? = "SMKN 8 Jakarta",

    @SerializedName("alamat")
    val alamat: String? = "Jl. Raya Pejaten Pasar Minggu",

    @SerializedName("latitude")
    val latitude: Double? = -6.2755200,

    @SerializedName("longitude")
    val longitude: Double? = 106.8378900,

    @SerializedName("radius_meter")
    val radiusMeter: Int? = 50,

    @SerializedName("jam_masuk_mulai")
    val jamMasukMulai: String? = "05:30",

    @SerializedName("jam_masuk_selesai")
    val jamMasukSelesai: String? = "06:40",

    @SerializedName("jam_pulang")
    val jamPulang: String? = "15:00",

    @SerializedName("toleransi_menit")
    val toleransiMenit: Int? = 15,

    @SerializedName("tahun_ajaran")
    val tahunAjaran: String? = "2026/2027",

    @SerializedName("semester")
    val semester: String? = "Ganjil",

    @SerializedName("anti_fake_gps")
    val antiFakeGps: Boolean? = true,

    @SerializedName("biometrik_ai_liveness")
    val biometrikAiLiveness: Boolean? = true
)

data class TambahRombelRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("nama_kelas") val namaKelas: String,
    @SerializedName("jurusan") val jurusan: String,
    @SerializedName("tingkat") val tingkat: String = "12",
    @SerializedName("wali_kelas_id") val waliKelasId: String?,
    @SerializedName("tahun_ajaran") val tahunAjaran: String = "2026/2027",
    @SerializedName("kapasitas_kuota") val kapasitasKuota: Int
)

data class RombelIdResponse(
    @SerializedName("id") val id: Int? = null
)

data class SiswaExportResponse(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("nisn") val nisn: String? = null,
    @SerializedName("nama_lengkap") val namaLengkap: String? = null,
    @SerializedName("id_kelas") val idKelas: Int? = null,
    @SerializedName("rombel_kelas") val rombelKelas: RombelInfoResponse? = null
)

data class PresensiRecordResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("siswa_id") val siswaId: Int? = null,
    @SerializedName("id_kelas") val idKelas: Int? = null,
    @SerializedName("tanggal") val tanggal: String? = null,
    @SerializedName("waktu_masuk") val waktuMasuk: String? = null,
    @SerializedName("status") val status: String? = null
)

data class IzinRecordResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("siswa_id") val siswaId: Int? = null,
    @SerializedName("id_kelas") val idKelas: Int? = null,
    @SerializedName("jenis_izin") val jenisIzin: String? = null,
    @SerializedName("status_verifikasi") val statusVerifikasi: String? = null,
    @SerializedName("tanggal_mulai") val tanggalMulai: String? = null,
    @SerializedName("tanggal_selesai") val tanggalSelesai: String? = null
)


