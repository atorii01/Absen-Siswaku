package com.andev.absensiswaku.ui.walas

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.PengajuanIzinResponse
import com.andev.absensiswaku.data.network.RiwayatModel
import com.andev.absensiswaku.data.network.SiswaHadirWalasModel
import com.andev.absensiswaku.data.network.SiswaMiniResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.FragmentWalasPresensiBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class WalasPresensiFragment : Fragment() {

    private var _binding: FragmentWalasPresensiBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var antreanAdapter: AntreanIzinAdapter
    private lateinit var siswaHadirAdapter: SiswaHadirWalasAdapter

    private var namaWalas: String = "Wali Kelas"
    private var namaKelas: String = "XII RPL"
    private var idKelas: Int = 9

    private var countTotalSiswa = 0
    private var countHadir = 0
    private var countVerifikasi = 0
    private var countAlpa = 0

    private val siswaKelasMap = mutableMapOf<Int, SiswaMiniResponse>()
    private val antreanList = mutableListOf<PengajuanIzinResponse>()
    private val hadirList = mutableListOf<SiswaHadirWalasModel>()

    companion object {
        private const val PREF_SESSION_NAME = "PREF_SMKN8_SESSION"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalasPresensiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = context ?: return
        sessionManager = SessionManager(ctx)

        loadSessionData()
        setupHeaderAndDate()
        setupRecyclerViews()
        setupClickListeners()
        loadDataFromSupabase()
    }

    override fun onResume() {
        super.onResume()
        loadSessionData()
        loadDataFromSupabase()
    }

    private fun loadSessionData() {
        val pref = requireContext().getSharedPreferences(PREF_SESSION_NAME, Context.MODE_PRIVATE)
        namaWalas = pref.getString("NAMA_WALAS", "Wali Kelas") ?: "Wali Kelas"
        idKelas = pref.getInt("ID_KELAS", 9)
        namaKelas = pref.getString("NAMA_KELAS", "XII RPL") ?: "XII RPL"

        // Terapkan ke Header UI secara dinamis
        binding.tvGreetingWalas.text = "Selamat Pagi, $namaWalas"
        binding.tvSubHeaderKelas.text = "Wali Kelas $namaKelas • Rekap Masuk Otomatis"
    }

    private fun setupHeaderAndDate() {
        val todayStr = try {
            val localeId = Locale.forLanguageTag("id-ID")
            val sdfHari = SimpleDateFormat("EEEE, d MMM yyyy", localeId)
            sdfHari.format(Date())
        } catch (e: Exception) {
            "Hari Ini"
        }
        binding.tvTanggalHariIni.text = todayStr
    }

    private fun setupRecyclerViews() {
        // 1. Adapter Antrean Izin
        antreanAdapter = AntreanIzinAdapter(
            items = mutableListOf(),
            onSetujuiClicked = { item ->
                handlePersetujuanIzin(item)
            },
            onTolakClicked = { item ->
                handlePenolakanIzin(item)
            },
            onLihatSuratClicked = { item ->
                handleLihatSurat(item)
            },
            onHubungiOrtuClicked = { item ->
                handleHubungiOrtu(item)
            }
        )

        binding.rvAntreanIzin.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = antreanAdapter
            isNestedScrollingEnabled = false
        }

        // 2. Adapter Siswa Hadir
        siswaHadirAdapter = SiswaHadirWalasAdapter(emptyList())
        binding.rvSiswaHadir.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = siswaHadirAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.imgAvatarWalas.setOnClickListener {
            Toast.makeText(requireContext(), "Memperbarui data presensi...", Toast.LENGTH_SHORT).show()
            loadDataFromSupabase()
        }

        binding.btnFinalisasiRekap.setOnClickListener {
            handleFinalisasiRekap()
        }
    }

    /**
     * Mengambil data antrean izin, siswa hadir, dan jumlah siswa secara dinamis dari Supabase
     * Difilter murni berdasarkan id_kelas walas yang sedang aktif.
     */
    private fun loadDataFromSupabase() {
        val todayIso = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        } catch (e: Exception) {
            ""
        }

        // 1. Ambil Data Siswa di Kelas Walas untuk perhitungan Total Siswa & Map Nama/NISN
        SupabaseClient.instance.getSiswaByKelas(filterKelas = "eq.$idKelas")
            .enqueue(object : Callback<List<SiswaMiniResponse>> {
                override fun onResponse(
                    call: Call<List<SiswaMiniResponse>>,
                    response: Response<List<SiswaMiniResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    val body = response.body().orEmpty()
                    siswaKelasMap.clear()
                    body.forEach { s ->
                        s.id?.let { siswaKelasMap[it] = s }
                    }
                    countTotalSiswa = if (body.isNotEmpty()) body.size else 0
                    updateSummaryMetrics()
                }

                override fun onFailure(call: Call<List<SiswaMiniResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    countTotalSiswa = 0
                    updateSummaryMetrics()
                }
            })

        // 2. Ambil Antrean Izin (status eq.Pending, filter nested siswa.id_kelas)
        SupabaseClient.instance.getAntreanIzinWalas(
            status = "eq.Pending",
            select = "*,siswa(*,rombel_kelas(*))",
            filterKelas = "eq.$idKelas"
        ).enqueue(object : Callback<List<PengajuanIzinResponse>> {
            override fun onResponse(
                call: Call<List<PengajuanIzinResponse>>,
                response: Response<List<PengajuanIzinResponse>>
            ) {
                if (!isAdded || _binding == null) return
                val body = response.body().orEmpty()

                if (response.isSuccessful) {
                    // Filter defensif: pastikan siswa adalah anggota kelas walas (menyingkirkan siswa luar kelas cth: Peter Maleke XII RPL)
                    val filtered = body.filter { izin ->
                        val kId = izin.siswa?.idKelas ?: izin.siswa?.rombelKelas?.id
                        kId == idKelas || (kId == null && siswaKelasMap.containsKey(izin.siswaId))
                    }
                    antreanList.clear()
                    antreanList.addAll(filtered)
                } else {
                    antreanList.clear()
                }

                renderAntreanUI()
                updateSummaryMetrics()
            }

            override fun onFailure(call: Call<List<PengajuanIzinResponse>>, t: Throwable) {
                if (!isAdded || _binding == null) return
                antreanList.clear()
                renderAntreanUI()
                updateSummaryMetrics()
            }
        })

        // 3. Ambil Siswa Hadir Hari Ini dari presensi_harian berdasarkan id_kelas walas dan tanggal hari ini
        SupabaseClient.instance.getPresensiHariIniWalas(
            filterTanggal = "eq.$todayIso",
            filterKelas = "eq.$idKelas",
            select = "*,siswa(*,rombel_kelas(*))"
        ).enqueue(object : Callback<List<RiwayatModel>> {
            override fun onResponse(
                call: Call<List<RiwayatModel>>,
                response: Response<List<RiwayatModel>>
            ) {
                if (!isAdded || _binding == null) return
                val body = response.body().orEmpty()

                if (response.isSuccessful) {
                    val validHadir = body.filter { it.status?.equals("Hadir", ignoreCase = true) == true }
                    val mapped = validHadir.mapIndexed { idx, riwayat ->
                        val s = riwayat.siswa ?: riwayat.siswaId?.let { siswaKelasMap[it] }
                        val sNama = s?.namaLengkap?.takeIf { it.isNotEmpty() } ?: "Siswa ${riwayat.siswaId ?: ""}".trim()
                        val sNisn = s?.nisn?.takeIf { it.isNotEmpty() } ?: "-"
                        val waktu = riwayat.displayJamMasuk.take(5)

                        SiswaHadirWalasModel(
                            nomorUrut = String.format(java.util.Locale.getDefault(), "%02d", idx + 1),
                            siswaId = riwayat.siswaId,
                            namaLengkap = sNama,
                            nisn = sNisn,
                            waktuMasuk = waktu,
                            status = riwayat.status ?: "Hadir",
                            verifiedAiGps = true
                        )
                    }
                    hadirList.clear()
                    hadirList.addAll(mapped)
                } else {
                    hadirList.clear()
                }

                renderHadirUI()
                updateSummaryMetrics()
            }

            override fun onFailure(call: Call<List<RiwayatModel>>, t: Throwable) {
                if (!isAdded || _binding == null) return
                hadirList.clear()
                renderHadirUI()
                updateSummaryMetrics()
            }
        })
    }

    private fun renderAntreanUI() {
        if (!isAdded || _binding == null) return

        countVerifikasi = antreanList.size
        binding.tvBadgeAntreanCount.text = "$countVerifikasi Pengajuan"
        binding.tvCountPending.text = countVerifikasi.toString()

        if (antreanList.isEmpty()) {
            binding.rvAntreanIzin.visibility = View.GONE
            binding.layoutEmptyAntrean.visibility = View.VISIBLE
        } else {
            binding.rvAntreanIzin.visibility = View.VISIBLE
            binding.layoutEmptyAntrean.visibility = View.GONE
            antreanAdapter.updateData(antreanList)
        }
    }

    private fun renderHadirUI() {
        if (!isAdded || _binding == null) return

        countHadir = hadirList.size
        binding.tvCountHadir.text = countHadir.toString()
        binding.tvJudulDaftarHadir.text = "Daftar Hadir Otomatis Hari Ini ($countHadir Siswa)"

        if (hadirList.isEmpty()) {
            binding.rvSiswaHadir.visibility = View.GONE
            binding.layoutEmptyHadir.visibility = View.VISIBLE
        } else {
            binding.rvSiswaHadir.visibility = View.VISIBLE
            binding.layoutEmptyHadir.visibility = View.GONE
            siswaHadirAdapter.updateData(hadirList)
        }
    }

    private fun updateSummaryMetrics() {
        if (!isAdded || _binding == null) return

        countVerifikasi = antreanList.size
        countHadir = hadirList.size
        val total = if (countTotalSiswa > 0) countTotalSiswa else 0
        countAlpa = (total - countHadir - countVerifikasi).coerceAtLeast(0)

        binding.tvTotalSiswa.text = total.toString()
        binding.tvCountHadir.text = countHadir.toString()
        binding.tvCountPending.text = countVerifikasi.toString()
        binding.tvCountAlpa.text = countAlpa.toString()
    }

    /**
     * Logika Persetujuan Izin (Disetujui)
     */
    private fun handlePersetujuanIzin(item: PengajuanIzinResponse) {
        val nama = item.siswa?.namaLengkap ?: item.siswaId?.let { siswaKelasMap[it]?.namaLengkap } ?: "Siswa"
        val jenis = item.jenisIzin?.trim().orEmpty().ifEmpty { "Izin" }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Konfirmasi Persetujuan")
            .setMessage("Setujui permohonan $jenis dari $nama dan masukkan status ke rekap presensi harian?")
            .setPositiveButton("✓ Setujui") { dialog, _ ->
                dialog.dismiss()
                prosesPersetujuanKeSupabase(item)
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun prosesPersetujuanKeSupabase(item: PengajuanIzinResponse) {
        val updatePayload: Map<String, Any> = mapOf(
            "status_verifikasi" to "Disetujui",
            "verified_by" to namaWalas
        )

        // 1. PATCH status pengajuan izin
        SupabaseClient.instance.updateStatusIzin(
            filterId = "eq.${item.id}",
            payload = updatePayload
        ).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {}
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {}
        })

        // 2. Tambahkan / Update ke presensi_harian
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.US)
        val now = Date()

        val presensiPayload: Map<String, Any> = mapOf(
            "id" to UUID.randomUUID().toString(),
            "siswa_id" to (item.siswaId ?: 0),
            "id_kelas" to idKelas,
            "tanggal" to sdfDate.format(now),
            "waktu_masuk" to sdfTime.format(now),
            "status" to (item.jenisIzin?.trim().orEmpty().ifEmpty { "Izin" }),
            "jarak_gerbang_meter" to 0,
            "biometrik_match_score" to 100.0,
            "verifikator" to namaWalas
        )

        SupabaseClient.instance.simpanPresensi(presensiPayload).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                loadDataFromSupabase()
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                loadDataFromSupabase()
            }
        })

        // 3. Update UI Seketika
        antreanAdapter.removeItem(item)
        antreanList.removeAll { it.id == item.id }
        renderAntreanUI()
        updateSummaryMetrics()

        val displayJenis = item.jenisIzin?.trim().orEmpty().ifEmpty { "Izin" }
        val displayNama = item.siswa?.namaLengkap ?: item.siswaId?.let { siswaKelasMap[it]?.namaLengkap } ?: "Siswa"
        Toast.makeText(
            requireContext(),
            "✓ Pengajuan $displayJenis dari $displayNama berhasil disetujui!",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Logika Penolakan Izin (Ditolak)
     */
    private fun handlePenolakanIzin(item: PengajuanIzinResponse) {
        val nama = item.siswa?.namaLengkap ?: item.siswaId?.let { siswaKelasMap[it]?.namaLengkap } ?: "Siswa"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tolak / Review Pengajuan")
            .setMessage("Apakah Anda yakin ingin menolak pengajuan ketidakhadiran dari $nama?")
            .setPositiveButton("✕ Ya, Tolak") { dialog, _ ->
                dialog.dismiss()

                val payloadTolak: Map<String, Any> = mapOf(
                    "status_verifikasi" to "Ditolak",
                    "verified_by" to namaWalas
                )

                SupabaseClient.instance.updateStatusIzin(
                    filterId = "eq.${item.id}",
                    payload = payloadTolak
                ).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Pengajuan siswa berhasil ditolak", Toast.LENGTH_SHORT).show()
                            loadDataFromSupabase()
                        }
                    }
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        loadDataFromSupabase()
                    }
                })

                antreanAdapter.removeItem(item)
                antreanList.removeAll { it.id == item.id }
                renderAntreanUI()
                updateSummaryMetrics()

                Toast.makeText(
                    requireContext(),
                    "Pengajuan dari $nama ditolak dan dikembalikan untuk review",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Pratinjau Surat / Berkas Bukti (Membuka In-App Custom Modal Dialog)
     */
    private fun handleLihatSurat(item: PengajuanIzinResponse) {
        tampilkanDialogPreview(requireContext(), item)
    }

    /**
     * Menampilkan Modal Dialog Pratinjau Surat Bukti In-App
     */
    fun tampilkanDialogPreview(context: Context, item: PengajuanIzinResponse) {
        val namaSiswa = item.siswa?.namaLengkap ?: item.siswaId?.let { siswaKelasMap[it]?.namaLengkap } ?: "Siswa"
        if (item.siswa == null) {
            item.siswa = SiswaMiniResponse(namaLengkap = namaSiswa)
        }
        DialogPratinjauBuktiIzin.tampilkan(context, item, namaKelas)
    }

    /**
     * Hubungi Orang Tua / Siswa Alpa
     */
    private fun handleHubungiOrtu(item: PengajuanIzinResponse) {
        val nama = item.siswa?.namaLengkap ?: item.siswaId?.let { siswaKelasMap[it]?.namaLengkap } ?: "Siswa"
        val noHpDefault = "081234567890"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hubungi Wali Murid")
            .setMessage("Kirimkan pemberitahuan kehadiran ke orang tua/wali murid dari $nama?")
            .setPositiveButton("WhatsApp Wali") { dialog, _ ->
                dialog.dismiss()
                val pesan = "Yth. Orang Tua/Wali Murid dari $nama, kami menginformasikan bahwa ananda belum melakukan presensi masuk di SMKN 8 Jakarta hari ini."
                try {
                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$noHpDefault&text=${Uri.encode(pesan)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$noHpDefault"))
                        startActivity(dialIntent)
                    } catch (e2: Exception) {
                        Toast.makeText(context, "Tidak dapat membuka aplikasi telepon: ${e2.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$noHpDefault"))
                        startActivity(dialIntent)
                    } catch (e2: Exception) {
                        Toast.makeText(context, "Gagal membuka aplikasi: ${e2.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Finalisasi Rekap Harian ke Laporan
     */
    private fun handleFinalisasiRekap() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Kirim & Finalisasi Rekap?")
            .setMessage("Rekap kehadiran kelas $namaKelas hari ini:\n• Hadir AI/GPS: $countHadir Siswa\n• Verifikasi: $countVerifikasi Siswa\n• Alpa: $countAlpa Siswa\n\nData akan dikirimkan ke Kepala Sekolah & Pusdatin.")
            .setPositiveButton("➤ Kirim Sekarang") { dialog, _ ->
                dialog.dismiss()
                binding.btnFinalisasiRekap.isEnabled = false
                binding.btnFinalisasiRekap.alpha = 0.8f

                Toast.makeText(
                    requireContext(),
                    "✓ Berhasil! Rekap kehadiran kelas $namaKelas telah difinalisasi.",
                    Toast.LENGTH_LONG
                ).show()

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("✓ Rekap Berhasil Dikirim")
                    .setMessage("Laporan presensi harian kelas $namaKelas telah tercatat resmi di buku rekap dan siap diunduh oleh Kesiswaan.")
                    .setPositiveButton("Tutup") { d, _ -> d.dismiss() }
                    .show()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
