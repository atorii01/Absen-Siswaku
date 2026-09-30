package com.andev.absensiswaku.data.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseService {

    @GET("siswa")
    fun loginSiswa(
        @Query("nisn") nisnFilter: String,
        @Query("pin_presensi") pinFilter: String,
        @Query("select") select: String = "id,nisn,nama_lengkap,id_kelas,rombel_kelas(id,nama_kelas,jurusan,users(nama_lengkap))"
    ): Call<List<SiswaResponse>>

    @Headers("Prefer: return=representation")
    @POST("presensi_harian")
    fun simpanPresensi(
        @Body payload: Map<String, @JvmSuppressWildcards Any>
    ): Call<ResponseBody>

    @Headers("Prefer: return=representation")
    @POST("presensi_harian")
    fun simpanPresensiSupabase(
        @Body request: PresensiRequest
    ): Call<List<PresensiResponse>>

    @Headers("Prefer: return=representation")
    @POST("pengajuan_izin")
    fun kirimPengajuanIzin(
        @Body payload: Map<String, @JvmSuppressWildcards Any>
    ): Call<ResponseBody>

    @GET("presensi_harian")
    fun checkPresensiHariIni(
        @Query("siswa_id") filterSiswa: String,
        @Query("tanggal") filterTanggal: String,
        @Query("select") select: String = "id,status,waktu_masuk"
    ): Call<List<Map<String, Any>>>

    @GET("presensi_harian")
    fun getRiwayatPresensiSupabase(
        @Query("siswa_id") siswaIdFilter: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "tanggal.desc,created_at.desc"
    ): Call<List<RiwayatModel>>

    @GET("pengajuan_izin")
    fun getRiwayatIzinSupabase(
        @Query("siswa_id") siswaIdFilter: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "tanggal_mulai.desc"
    ): Call<List<PengajuanIzinModel>>

    // ================= WALAS DASHBOARD ENDPOINTS =================
    @GET("pengajuan_izin")
    fun getAntreanIzinWalas(
        @Query("status_verifikasi") status: String = "eq.Pending",
        @Query("select") select: String = "*,siswa(*,rombel_kelas(*))",
        @Query("siswa.id_kelas") filterKelas: String,
        @Query("order") order: String = "created_at.desc,tanggal_mulai.desc"
    ): Call<List<PengajuanIzinResponse>>

    @Headers("Prefer: return=representation")
    @PATCH("pengajuan_izin")
    fun updateStatusIzin(
        @Query("id") filterId: String,
        @Body payload: Map<String, @JvmSuppressWildcards Any>
    ): Call<ResponseBody>

    @GET("presensi_harian")
    fun getPresensiHariIniWalas(
        @Query("tanggal") filterTanggal: String,
        @Query("id_kelas") filterKelas: String,
        @Query("select") select: String = "*,siswa(*,rombel_kelas(*))",
        @Query("order") order: String = "waktu_masuk.asc"
    ): Call<List<RiwayatModel>>

    @GET("presensi_harian")
    fun getPresensiKelas(
        @Query("id_kelas") filterKelas: String,
        @Query("select") select: String = "*,siswa(*,rombel_kelas(*))",
        @Query("order") order: String = "tanggal.desc,waktu_masuk.asc"
    ): Call<List<RiwayatModel>>

    @GET("pengajuan_izin")
    fun getIzinKelas(
        @Query("siswa.id_kelas") filterKelas: String,
        @Query("status_verifikasi") status: String = "eq.Disetujui",
        @Query("select") select: String = "*,siswa!inner(*,rombel_kelas(*))",
        @Query("order") order: String = "tanggal_mulai.desc"
    ): Call<List<PengajuanIzinResponse>>

    @GET("rombel_kelas")
    fun getRombelByWalas(
        @Query("wali_kelas_id") filterWalas: String,
        @Query("select") select: String = "id,nama_kelas,jurusan,wali_kelas_id,tahun_ajaran,kapasitas_kuota,users!wali_kelas_id(nama_lengkap)"
    ): Call<List<RombelMapelResponse>>

    @GET("siswa")
    fun getAllSiswa(
        @Query("select") select: String = "*,rombel_kelas(*)",
        @Query("order") order: String = "nama_lengkap.asc"
    ): Call<List<SiswaMiniResponse>>

    @GET("siswa")
    fun getSiswaByKelas(
        @Query("id_kelas") filterKelas: String,
        @Query("select") select: String = "*,rombel_kelas(*)",
        @Query("order") order: String = "nama_lengkap.asc"
    ): Call<List<SiswaMiniResponse>>

    @GET("siswa")
    fun getSiswaKelola(
        @Query("id_kelas") filterKelas: String,
        @Query("order") order: String = "nama_lengkap.asc"
    ): Call<List<SiswaKelolaResponse>>

    @Headers("Prefer: return=representation")
    @POST("siswa")
    fun tambahSiswa(
        @Body payload: Map<String, @JvmSuppressWildcards Any>
    ): Call<ResponseBody>

    @Headers("Prefer: return=representation")
    @POST("siswa")
    fun tambahSiswaBatch(
        @Body payload: List<Map<String, @JvmSuppressWildcards Any>>
    ): Call<ResponseBody>

    @Headers("Prefer: return=representation")
    @PATCH("siswa")
    fun updateSiswa(
        @Query("id") filterId: String,
        @Body payload: Map<String, @JvmSuppressWildcards Any>
    ): Call<ResponseBody>

    @DELETE("siswa")
    fun deleteSiswa(
        @Query("id") filterId: String
    ): Call<ResponseBody>

    @Headers("Prefer: return=representation")
    @PATCH("rombel_kelas")
    fun updateRombelKelas(
        @Query("id") filterId: String,
        @Body payload: Map<String, @JvmSuppressWildcards Any>
    ): Call<ResponseBody>

    @GET("users")
    fun loginGuru(
        @Query("or", encoded = true) orFilter: String,
        @Query("select") select: String = "id,username,password_hash,nama_lengkap,role,mata_pelajaran,rombel_kelas(id,nama_kelas,jurusan)"
    ): Call<List<UserResponse>>

    @GET("konfigurasi_sistem")
    fun getKonfigurasiSistem(
        @Query("id") idFilter: String? = null,
        @Query("select") select: String = "*"
    ): Call<List<KonfigurasiSistemResponse>>

    @Headers("Prefer: return=representation")
    @PATCH("konfigurasi_sistem")
    fun updateKonfigurasiSistem(
        @Query("id") filterId: String = "eq.1",
        @Body payload: Map<String, @JvmSuppressWildcards Any>
    ): Call<ResponseBody>

    // ================= GURU MAPEL READ-ONLY ENDPOINTS =================
    @GET("rombel_kelas")
    fun getRombelMapel(
        @Query("select") select: String = "id,nama_kelas,jurusan,wali_kelas_id,tahun_ajaran,kapasitas_kuota,users!wali_kelas_id(nama_lengkap)",
        @Query("order") order: String = "id.asc"
    ): Call<List<RombelMapelResponse>>

    @GET("rombel_kelas")
    fun getAllRombelKelas(
        @Query("select") select: String = "id,nama_kelas,jurusan,kapasitas_kuota,users(nama_lengkap)",
        @Query("order") order: String = "nama_kelas.asc"
    ): Call<List<RombelKelasResponse>>

    @GET("presensi_harian")
    fun getPresensiHariIniSemuaKelas(
        @Query("tanggal") filterTanggal: String,
        @Query("select") select: String = "id,siswa_id,id_kelas,tanggal,waktu_masuk,status",
        @Query("order") order: String = "waktu_masuk.asc"
    ): Call<List<PresensiHarianResponse>>

    @GET("pengajuan_izin")
    fun getIzinDisetujuiSemuaKelas(
        @Query("status_verifikasi") status: String = "eq.Disetujui",
        @Query("select") select: String = "id,siswa_id,jenis_izin,tanggal_mulai,tanggal_selesai,status_verifikasi,siswa(id,nama_lengkap,id_kelas)"
    ): Call<List<PengajuanIzinResponse>>

    @GET("siswa")
    fun getSiswaAdmin(
        @Query("select") select: String = "id,nisn,nama_lengkap,id_kelas,nik,jenis_kelamin,no_wa_orang_tua,rombel_kelas!id_kelas(nama_kelas)",
        @Query("order") order: String = "nama_lengkap.asc",
        @Query("limit") limit: Int = 500
    ): Call<List<SiswaAdminResponse>>

    @POST("rombel_kelas")
    fun tambahRombel(
        @Body request: TambahRombelRequest
    ): Call<Void>

    @GET("rombel_kelas")
    fun getMaxRombelId(
        @Query("select") select: String = "id",
        @Query("order") order: String = "id.desc",
        @Query("limit") limit: Int = 1
    ): Call<List<RombelIdResponse>>

    @GET("users")
    fun getWaliKelasList(
        @Query("role") roleFilter: String = "eq.WALI_KELAS",
        @Query("select") select: String = "id,nama_lengkap",
        @Query("order") order: String = "nama_lengkap.asc"
    ): Call<List<UserResponse>>

    // Ambil 357 data siswa asli lengkap dengan rombelnya
    @GET("siswa")
    fun getAllSiswaForExport(
        @Query("select") select: String = "id,nisn,nama_lengkap,id_kelas,rombel_kelas(nama_kelas)",
        @Query("order") order: String = "id_kelas.asc,nama_lengkap.asc",
        @Query("limit") limit: Int = 500
    ): Call<List<SiswaExportResponse>>

    // Ambil catatan presensi riil sesuai tanggal/periode
    @GET("presensi_harian")
    fun getPresensiByDate(
        @Query("tanggal") tanggal: String,
        @Query("limit") limit: Int = 500
    ): Call<List<PresensiRecordResponse>>

    // Ambil izin yang sudah disetujui walas
    @GET("pengajuan_izin")
    fun getIzinSah(
        @Query("status_verifikasi") status: String = "eq.Disetujui",
        @Query("limit") limit: Int = 500
    ): Call<List<IzinRecordResponse>>
}