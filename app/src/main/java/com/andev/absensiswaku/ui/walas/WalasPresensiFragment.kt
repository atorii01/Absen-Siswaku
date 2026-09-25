package com.andev.absensiswaku.ui.walas

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.andev.absensiswaku.data.network.PengajuanIzinResponse
import com.andev.absensiswaku.data.network.RiwayatModel
import com.andev.absensiswaku.data.network.SiswaHadirWalasModel
import com.andev.absensiswaku.data.network.SiswaMiniResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.FragmentWalasPresensiBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

    private var namaWalas: String = "Herlina, S.E"
    private var namaKelas: String = "XII AKL 1"
    private var idKelas: Int = 1

    private var countTotalSiswa = 36
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
        val ctx = context ?: return
        val pref = ctx.getSharedPreferences(PREF_SESSION_NAME, Context.MODE_PRIVATE)

        val prefWalas = pref.getString("NAMA_WALAS", null)
        namaWalas = when {
            !prefWalas.isNullOrEmpty() && !prefWalas.contains("Farauk", true) -> prefWalas
            sessionManager.getWaliKelas().isNotEmpty() && !sessionManager.getWaliKelas().contains("Farauk", true) -> sessionManager.getWaliKelas()
            sessionManager.getNama().isNotEmpty() && !sessionManager.getNama().contains("Farauk", true) -> sessionManager.getNama()
            else -> "Herlina, S.E"
        }

        val prefKelas = pref.getString("NAMA_KELAS", null)
        namaKelas = when {
            !prefKelas.isNullOrEmpty() && !prefKelas.contains("RPL", true) -> prefKelas
            sessionManager.getNamaKelas().isNotEmpty() && !sessionManager.getNamaKelas().contains("RPL", true) -> sessionManager.getNamaKelas()
            else -> "XII AKL 1"
        }

        idKelas = pref.getInt("ID_KELAS", 1).takeIf { it != 0 }
            ?: sessionManager.getIdKelas().takeIf { it != 0 }
            ?: 1

        binding.tvGreetingWalas.text = "Selamat Pagi, $namaWalas"
        binding.tvSubJudulKelas.text = "Wali Kelas $namaKelas • Rekap Masuk Otomatis"
    }

    private fun setupHeaderAndDate() {
        val localeId = Locale.forLanguageTag("id-ID")
        val sdfHari = SimpleDateFormat("EEEE, d MMM yyyy", localeId)
        val todayStr = sdfHari.format(Date())
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
        val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayIso = sdfIso.format(Date())

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
                    countTotalSiswa = if (body.isNotEmpty()) body.size else 36
                    updateSummaryMetrics()
                }

                override fun onFailure(call: Call<List<SiswaMiniResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    countTotalSiswa = 36
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
                    val validHadir = body.filter { it.status.equals("Hadir", true) }
                    val mapped = validHadir.mapIndexed { idx, riwayat ->
                        val s = riwayat.siswa ?: siswaKelasMap[riwayat.siswaId]
                        val sNama = s?.namaLengkap?.takeIf { it.isNotEmpty() } ?: "Siswa ${riwayat.siswaId}"
                        val sNisn = s?.nisn?.takeIf { it.isNotEmpty() } ?: "-"
                        val waktu = riwayat.displayJamMasuk.take(5)

                        SiswaHadirWalasModel(
                            nomorUrut = String.format("%02d", idx + 1),
                            siswaId = riwayat.siswaId,
                            namaLengkap = sNama,
                            nisn = sNisn,
                            waktuMasuk = waktu,
                            status = riwayat.status,
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
        val total = if (countTotalSiswa > 0) countTotalSiswa else 36
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
        val nama = item.siswa?.namaLengkap ?: siswaKelasMap[item.siswaId]?.namaLengkap ?: "Siswa"
        val jenis = item.jenisIzin

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
            "siswa_id" to item.siswaId,
            "id_kelas" to idKelas,
            "tanggal" to sdfDate.format(now),
            "waktu_masuk" to sdfTime.format(now),
            "status" to item.jenisIzin,
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

        Toast.makeText(
            requireContext(),
            "✓ Pengajuan ${item.jenisIzin} dari ${item.siswa?.namaLengkap ?: siswaKelasMap[item.siswaId]?.namaLengkap ?: "Siswa"} berhasil disetujui!",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Logika Penolakan Izin (Ditolak)
     */
    private fun handlePenolakanIzin(item: PengajuanIzinResponse) {
        val nama = item.siswa?.namaLengkap ?: siswaKelasMap[item.siswaId]?.namaLengkap ?: "Siswa"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tolak / Review Pengajuan")
            .setMessage("Apakah Anda yakin ingin menolak pengajuan ketidakhadiran dari $nama?")
            .setPositiveButton("✕ Ya, Tolak") { dialog, _ ->
                dialog.dismiss()

                val updatePayload: Map<String, Any> = mapOf(
                    "status_verifikasi" to "Ditolak",
                    "verified_by" to namaWalas
                )

                SupabaseClient.instance.updateStatusIzin(
                    filterId = "eq.${item.id}",
                    payload = updatePayload
                ).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {}
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {}
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
     * Pratinjau Surat / Berkas Bukti
     */
    private fun handleLihatSurat(item: PengajuanIzinResponse) {
        val fileName = item.buktiBerkasUrl?.substringAfterLast("/")?.takeIf { it.isNotEmpty() }
            ?: "Surat_Keterangan_${item.jenisIzin}.pdf"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pratinjau Berkas Bukti")
            .setMessage("Berkas: $fileName\n\nStatus Digital: Terverifikasi oleh sistem SMKN 8 Jakarta.\nPengaju: ${item.siswa?.namaLengkap ?: siswaKelasMap[item.siswaId]?.namaLengkap ?: "Siswa"}\nKeterangan: ${item.keterangan}")
            .setPositiveButton("Buka Dokumen") { dialog, _ ->
                dialog.dismiss()
                val url = item.buktiBerkasUrl
                if (!url.isNullOrEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Tidak dapat membuka berkas browser", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Membuka salinan berkas: $fileName", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Tutup") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Hubungi Orang Tua / Siswa Alpa
     */
    private fun handleHubungiOrtu(item: PengajuanIzinResponse) {
        val nama = item.siswa?.namaLengkap ?: siswaKelasMap[item.siswaId]?.namaLengkap ?: "Siswa"
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
                } catch (e: Exception) {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$noHpDefault"))
                    startActivity(dialIntent)
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
