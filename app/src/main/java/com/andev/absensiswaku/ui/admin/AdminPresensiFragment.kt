package com.andev.absensiswaku.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.PengajuanIzinResponse
import com.andev.absensiswaku.data.network.PresensiHarianResponse
import com.andev.absensiswaku.data.network.RombelMapelResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.databinding.FragmentAdminPresensiBinding
import com.andev.absensiswaku.ui.guru.BottomSheetRiwayatDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminPresensiFragment : Fragment() {

    private var _binding: FragmentAdminPresensiBinding? = null
    private val binding get() = _binding!!

    private lateinit var rombelAdapter: RombelAdminAdapter

    private var allRombelList: List<RombelMapelResponse> = emptyList()
    private var filteredRombelList: List<RombelMapelResponse> = emptyList()
    private var presensiHariIniList: List<PresensiHarianResponse> = emptyList()
    private var izinDisetujuiList: List<PengajuanIzinResponse> = emptyList()

    private var selectedCategory: String = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPresensiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDateHeader()
        setupRecyclerView()
        setupFilterChips()
        setupBroadcastButton()
        loadDataFromSupabase()
    }

    private fun setupDateHeader() {
        val localeId = Locale.forLanguageTag("id-ID")
        val sdfHariTanggal = SimpleDateFormat("EEEE, dd MMM yyyy", localeId)
        val todayStr = sdfHariTanggal.format(Date())
        binding.tvTanggalHariIni.text = todayStr
    }

    private fun setupRecyclerView() {
        rombelAdapter = RombelAdminAdapter(
            onAuditClick = { rombel ->
                val auditDialog = BottomSheetAuditPresensiDialog.newInstance(rombel)
                auditDialog.show(childFragmentManager, "BottomSheetAuditPresensiDialog")
            },
            onRincianClick = { rombel ->
                val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val todayDisplay = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.forLanguageTag("id-ID")).format(Date())
                val detailDialog = BottomSheetRiwayatDialog.newInstance(
                    rombelId = rombel.id,
                    namaKelas = rombel.namaKelas,
                    tanggalIso = todayIso,
                    tanggalDisplay = todayDisplay
                )
                detailDialog.show(childFragmentManager, "BottomSheetRiwayatDialog")
            }
        )

        binding.rvRombelAdmin.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rombelAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupFilterChips() {
        val chips = listOf(
            Triple(binding.chipSemuaTingkat, "ALL", "Semua Tingkat (10)"),
            Triple(binding.chipAkl, "AKL", "AKL (3)"),
            Triple(binding.chipMp, "MP", "MP (2)"),
            Triple(binding.chipBr, "BR", "BR (2)"),
            Triple(binding.chipBd, "BD", "BD (1)"),
            Triple(binding.chipRpl, "RPL", "RPL (1)"),
            Triple(binding.chipUpw, "UPW", "UPW (1)")
        )

        chips.forEach { (chipView, categoryKey, _) ->
            chipView.setOnClickListener {
                selectedCategory = categoryKey
                updateChipStates(chips, chipView)
                applyFilter()
            }
        }
    }

    private fun updateChipStates(
        chips: List<Triple<TextView, String, String>>,
        activeChip: TextView
    ) {
        val context = requireContext()
        chips.forEach { (chipView, _, _) ->
            if (chipView == activeChip) {
                chipView.setBackgroundResource(R.drawable.bg_chip_filter_active)
                chipView.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            } else {
                chipView.setBackgroundResource(R.drawable.bg_chip_filter_inactive)
                chipView.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }

    private fun applyFilter() {
        filteredRombelList = when (selectedCategory) {
            "AKL" -> allRombelList.filter { it.namaKelas.contains("AKL", true) || it.jurusan?.contains("AKL", true) == true }
            "MP" -> allRombelList.filter { it.namaKelas.contains("MP", true) || it.jurusan?.contains("MP", true) == true }
            "BR" -> allRombelList.filter { it.namaKelas.contains("BR", true) || it.jurusan?.contains("BR", true) == true }
            "BD" -> allRombelList.filter { it.namaKelas.contains("BD", true) || it.jurusan?.contains("BD", true) == true }
            "RPL" -> allRombelList.filter { it.namaKelas.contains("RPL", true) || it.jurusan?.contains("RPL", true) == true }
            "UPW" -> allRombelList.filter { it.namaKelas.contains("UPW", true) || it.jurusan?.contains("UPW", true) == true }
            else -> allRombelList
        }

        if (filteredRombelList.isEmpty()) {
            binding.layoutEmptyRombelAdmin.visibility = View.VISIBLE
            binding.rvRombelAdmin.visibility = View.GONE
        } else {
            binding.layoutEmptyRombelAdmin.visibility = View.GONE
            binding.rvRombelAdmin.visibility = View.VISIBLE
            rombelAdapter.submitList(filteredRombelList)
        }
    }

    private fun setupBroadcastButton() {
        binding.btnBroadcastWalas.setOnClickListener {
            val rombelWithAlpa = allRombelList.filter { it.alpaCount > 0 }
            if (rombelWithAlpa.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("📢 Status Presensi Sempurna")
                    .setMessage("Luar biasa! Seluruh rombel (10 kelas) sudah 100% lengkap terverifikasi. Tidak ada siswa alpa yang memerlukan konfirmasi.")
                    .setPositiveButton("Mengerti", null)
                    .show()
            } else {
                val walasList = rombelWithAlpa.joinToString("\n") {
                    val walas = it.waliKelas?.namaLengkap ?: "Wali Kelas"
                    "• ${it.namaKelas}: $walas (${it.alpaCount} Alpa)"
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("📢 Broadcast Peringatan Presensi")
                    .setMessage(
                        "Peringatan verifikasi akan dikirimkan kepada ${rombelWithAlpa.size} Wali Kelas yang rombelnya masih memiliki siswa Alpa:\n\n" +
                                "$walasList\n\n" +
                                "Kirimkan sinyal verifikasi sekarang?"
                    )
                    .setPositiveButton("Kirim Broadcast") { _, _ ->
                        Toast.makeText(
                            requireContext(),
                            "Peringatan dikirim ke ${rombelWithAlpa.size} Wali Kelas terindikasi alpa",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }
    }

    private fun loadDataFromSupabase() {
        binding.progressBarAdmin.visibility = View.VISIBLE

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 1. Ambil 10 Rombel Aktif dari Supabase
        SupabaseClient.instance.getRombelMapel()
            .enqueue(object : Callback<List<RombelMapelResponse>> {
                override fun onResponse(
                    call: Call<List<RombelMapelResponse>>,
                    response: Response<List<RombelMapelResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    allRombelList = response.body().orEmpty()

                    // 2. Ambil Presensi Hari Ini
                    loadPresensiHariIni(todayDate)
                }

                override fun onFailure(call: Call<List<RombelMapelResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    binding.progressBarAdmin.visibility = View.GONE
                    Toast.makeText(requireContext(), "Gagal memuat rombel: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadPresensiHariIni(todayDate: String) {
        SupabaseClient.instance.getPresensiHariIniSemuaKelas(filterTanggal = "eq.$todayDate")
            .enqueue(object : Callback<List<PresensiHarianResponse>> {
                override fun onResponse(
                    call: Call<List<PresensiHarianResponse>>,
                    response: Response<List<PresensiHarianResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    presensiHariIniList = response.body().orEmpty()

                    // 3. Ambil Pengajuan Izin Disetujui
                    loadIzinDisetujui(todayDate)
                }

                override fun onFailure(call: Call<List<PresensiHarianResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    presensiHariIniList = emptyList()
                    loadIzinDisetujui(todayDate)
                }
            })
    }

    private fun loadIzinDisetujui(todayDate: String) {
        SupabaseClient.instance.getIzinDisetujuiSemuaKelas()
            .enqueue(object : Callback<List<PengajuanIzinResponse>> {
                override fun onResponse(
                    call: Call<List<PengajuanIzinResponse>>,
                    response: Response<List<PengajuanIzinResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    binding.progressBarAdmin.visibility = View.GONE
                    izinDisetujuiList = response.body().orEmpty()
                    calculateAccumulation(todayDate)
                }

                override fun onFailure(call: Call<List<PengajuanIzinResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    binding.progressBarAdmin.visibility = View.GONE
                    izinDisetujuiList = emptyList()
                    calculateAccumulation(todayDate)
                }
            })
    }

    private fun calculateAccumulation(todayDate: String) {
        if (_binding == null) return

        // Presensi unik untuk mencegah duplikasi data
        val presensiUnik = presensiHariIniList
            .filter { it.siswaId != null }
            .distinctBy { it.siswaId }

        // Filter izin sah aktif hari ini
        val izinHariIni = izinDisetujuiList.filter { izin ->
            if (izin.siswaId == null) return@filter false
            val tglMulai = izin.tanggalMulai ?: ""
            val tglSelesai = izin.tanggalSelesai ?: tglMulai
            if (tglMulai.isBlank()) false else (todayDate in tglMulai..tglSelesai || tglMulai == todayDate)
        }.distinctBy { it.siswaId }

        var totalSekolahHadir = 0
        var totalSekolahIzin = 0
        var totalSekolahAlpa = 0
        val totalKapasitasSekolah = 357 // Standar total siswa SMKN 8 Jakarta (10 Rombel Aktif)
        var countRombelAlpa = 0

        allRombelList.forEach { rombel ->
            // Kuota kelas: ID 8 (XII BD) dan ID 10 (XII UPW) = 35, rombel lainnya = 36
            val kuota = rombel.kuota

            val hadir = presensiUnik.count {
                it.idKelas == rombel.id && (it.status.equals("Hadir", true) || it.status.equals("Terlambat", true))
            }

            val izin = izinHariIni.count {
                it.idKelas == rombel.id || it.siswa?.idKelas == rombel.id
            }

            val alpa = (kuota - (hadir + izin)).coerceAtLeast(0)

            rombel.hadirCount = hadir
            rombel.izinCount = izin
            rombel.alpaCount = alpa
            rombel.persentaseHadir = if (kuota > 0) (hadir.toFloat() / kuota.toFloat()) * 100f else 0f

            totalSekolahHadir += hadir
            totalSekolahIzin += izin
            totalSekolahAlpa += alpa

            if (alpa > 0) {
                countRombelAlpa++
            }
        }

        // Hitung Persentase Akumulasi Murni Sekolah
        val persenSekolah = if (totalKapasitasSekolah > 0) {
            (totalSekolahHadir.toDouble() / totalKapasitasSekolah.toDouble()) * 100.0
        } else {
            0.0
        }

        // Update Hero Card Utama
        binding.tvPersenKehadiranSekolah.text = String.format(Locale.US, "%.1f%%", persenSekolah)
        binding.tvRasioHadirSekolah.text = "($totalSekolahHadir dari $totalKapasitasSekolah Siswa)"
        binding.progressKehadiranSekolah.progress = persenSekolah.toInt()

        // Update Sub-Box Hero Card
        binding.tvCountIzinSekolah.text = "$totalSekolahIzin Siswa"
        binding.tvCountAlpaSekolah.text = "$totalSekolahAlpa Siswa"

        // Update Early Warning Alert Card
        if (totalSekolahAlpa > 0) {
            binding.cardPeringatanDini.visibility = View.VISIBLE
            binding.tvAlertPeringatanDesc.text =
                "$totalSekolahAlpa siswa belum terekam absen atau terindikasi Alpa. Segera kirim sinyal verifikasi kepada wali kelas terkait."
            binding.tvBadgeRombelAlpa.text = "$countRombelAlpa Rombel"
        } else {
            binding.cardPeringatanDini.visibility = View.VISIBLE
            binding.tvAlertPeringatanDesc.text =
                "Seluruh rombel telah lengkap terverifikasi. Tidak ada siswa yang terindikasi Alpa hari ini."
            binding.tvBadgeRombelAlpa.text = "0 Rombel"
        }

        // Update Subtitle Header Rombel
        binding.tvCountRombelAktif.text = "${allRombelList.size} Rombel Aktif"

        // Refresh List Rombel
        applyFilter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
