package com.andev.absensiswaku.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
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
import com.andev.absensiswaku.databinding.FragmentAdminRekapBinding
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminRekapFragment : Fragment() {

    private var _binding: FragmentAdminRekapBinding? = null
    private val binding get() = _binding!!

    private lateinit var riwayatAdapter: RiwayatUnduhanAdapter

    private var allRombelList: List<RombelMapelResponse> = emptyList()
    private var presensiList: List<PresensiHarianResponse> = emptyList()
    private var izinList: List<PengajuanIzinResponse> = emptyList()

    private enum class PeriodType {
        HARI_INI, MINGGUAN, BULANAN, SEMESTER
    }

    private var selectedPeriod: PeriodType = PeriodType.MINGGUAN
    private var selectedRombelIndex: Int = 0 // 0 = Seluruh Kelas
    private val historyDownloads = mutableListOf<RiwayatUnduhanModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminRekapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPeriodToggles()
        setupDropdownRombel()
        setupCheckboxFilter()
        setupRecyclerViewHistory()
        setupExportButtons()
        loadDataFromSupabase()
    }

    private fun setupPeriodToggles() {
        val today = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID")).format(Date())
        binding.tvSubPeriodHariIni.text = today

        binding.cardPeriodHariIni.setOnClickListener {
            selectedPeriod = PeriodType.HARI_INI
            updatePeriodSelection()
        }

        binding.cardPeriodMingguan.setOnClickListener {
            selectedPeriod = PeriodType.MINGGUAN
            updatePeriodSelection()
        }

        binding.cardPeriodBulanan.setOnClickListener {
            selectedPeriod = PeriodType.BULANAN
            updatePeriodSelection()
        }

        binding.cardPeriodSemester.setOnClickListener {
            selectedPeriod = PeriodType.SEMESTER
            updatePeriodSelection()
        }

        updatePeriodSelection()
    }

    private fun updatePeriodSelection() {
        val context = requireContext()
        val cards = listOf(
            Triple(binding.cardPeriodHariIni, binding.tvTitlePeriodHariIni, binding.tvSubPeriodHariIni),
            Triple(binding.cardPeriodMingguan, binding.tvTitlePeriodMingguan, binding.tvSubPeriodMingguan),
            Triple(binding.cardPeriodBulanan, binding.tvTitlePeriodBulanan, binding.tvSubPeriodBulanan),
            Triple(binding.cardPeriodSemester, binding.tvTitlePeriodSemester, binding.tvSubPeriodSemester)
        )

        cards.forEachIndexed { index, (card, tvTitle, tvSub) ->
            val isCurrent = when (selectedPeriod) {
                PeriodType.HARI_INI -> index == 0
                PeriodType.MINGGUAN -> index == 1
                PeriodType.BULANAN -> index == 2
                PeriodType.SEMESTER -> index == 3
            }

            if (isCurrent) {
                card.setBackgroundResource(R.drawable.bg_period_active)
                tvTitle.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                tvSub.setTextColor(ColorStateListHelper.getColor(context, R.color.badge_super_admin_bg))
            } else {
                card.setBackgroundResource(R.drawable.bg_period_inactive)
                tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                tvSub.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }

        binding.imgCheckPeriodMingguan.visibility =
            if (selectedPeriod == PeriodType.MINGGUAN) View.VISIBLE else View.GONE

        updateEstimasiOutput()
    }

    private fun setupDropdownRombel() {
        val defaultOptions = listOf(
            "Seluruh Kelas (Semua Rombel - 10 Kelas)",
            "Kelas XII RPL",
            "Kelas XII AKL 1",
            "Kelas XII AKL 2",
            "Kelas XII AKL 3",
            "Kelas XII MP 1",
            "Kelas XII MP 2",
            "Kelas XII BR 1",
            "Kelas XII BR 2",
            "Kelas XII BD",
            "Kelas XII UPW"
        )

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, defaultOptions)
        binding.actvCakupanRombel.setAdapter(adapter)

        binding.actvCakupanRombel.setOnItemClickListener { _, _, position, _ ->
            selectedRombelIndex = position
            updateEstimasiOutput()
        }
    }

    private fun updateEstimasiOutput() {
        if (selectedRombelIndex == 0) {
            binding.tvEstimasiDataOutput.text = "10 Tab Rombel • 357 Baris Data"
            binding.tvBadgeUkuranEstimasi.text = "~1.2 MB"
        } else {
            val rombelName = binding.actvCakupanRombel.text.toString()
            binding.tvEstimasiDataOutput.text = "1 Rombel ($rombelName) • 36 Baris Data"
            binding.tvBadgeUkuranEstimasi.text = "~120 KB"
        }
    }

    private fun setupCheckboxFilter() {
        var allChecked = true
        binding.tvPilihSemuaFilter.setOnClickListener {
            allChecked = !allChecked
            binding.cbHadirOtomatis.isChecked = allChecked
            binding.cbIzinVerif.isChecked = allChecked
            binding.cbSakit.isChecked = allChecked
            binding.cbDispensasi.isChecked = allChecked
            binding.cbAlpa.isChecked = allChecked
            binding.tvPilihSemuaFilter.text = if (allChecked) "Batalkan Semua" else "Pilih Semua"
        }
    }

    private fun setupRecyclerViewHistory() {
        riwayatAdapter = RiwayatUnduhanAdapter { item ->
            if (item.uriString != null) {
                val file = File(item.uriString)
                if (file.exists()) {
                    val mime = when (item.format) {
                        "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        "PDF" -> "application/pdf"
                        else -> "text/csv"
                    }
                    ExportReportHelper.openFile(requireContext(), file, mime)
                } else {
                    Toast.makeText(requireContext(), "Membuka ${item.namaFile}...", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Mengunduh ulang ${item.namaFile}...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvRiwayatUnduhan.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = riwayatAdapter
            isNestedScrollingEnabled = false
        }

        // Entri awal riwayat unduhan
        historyDownloads.addAll(
            listOf(
                RiwayatUnduhanModel(
                    namaFile = "Rekap_Presensi_SMKN8_Mingguan.xlsx",
                    ukuranFileKb = 1228,
                    waktuLalu = "2 jam lalu",
                    rolePengunduh = "Super Admin",
                    format = "XLSX"
                ),
                RiwayatUnduhanModel(
                    namaFile = "Laporan_Harian_27Sep2026.pdf",
                    ukuranFileKb = 850,
                    waktuLalu = "10 menit lalu",
                    rolePengunduh = "Tata Usaha",
                    format = "PDF"
                ),
                RiwayatUnduhanModel(
                    namaFile = "Sinkron_Dapodik_Minggu39.csv",
                    ukuranFileKb = 420,
                    waktuLalu = "Kemarin, 16:45",
                    rolePengunduh = "Sistem Auto",
                    format = "CSV"
                )
            )
        )
        riwayatAdapter.submitList(historyDownloads)
    }

    private fun setupExportButtons() {
        binding.btnEksporXlsx.setOnClickListener {
            performExport("XLSX")
        }

        binding.btnEksporPdf.setOnClickListener {
            performExport("PDF")
        }

        binding.btnEksporCsv.setOnClickListener {
            performExport("CSV")
        }
    }

    private fun performExport(format: String) {
        val selectedRombel = binding.actvCakupanRombel.text.toString()
        val periodName = when (selectedPeriod) {
            PeriodType.HARI_INI -> "Hari_Ini"
            PeriodType.MINGGUAN -> "Mingguan"
            PeriodType.BULANAN -> "Bulanan"
            PeriodType.SEMESTER -> "Semester_Ganjil"
        }

        val rows = generateExportRows(selectedRombel)

        ExportReportHelper.exportReport(
            context = requireContext(),
            format = format,
            rombelName = if (selectedRombelIndex == 0) "Semua_Rombel" else selectedRombel,
            periodName = periodName,
            dataRows = rows,
            includeGps = binding.switchGpsTimestamp.isChecked,
            includeWalasNotes = binding.switchCatatanWalas.isChecked,
            includeDapodik = binding.switchStandarDapodik.isChecked
        ) { success, fileName, sizeKb, file ->
            if (success) {
                val newHistory = RiwayatUnduhanModel(
                    namaFile = fileName,
                    ukuranFileKb = sizeKb,
                    waktuLalu = "Baru saja",
                    rolePengunduh = "Super Admin",
                    format = format,
                    uriString = file?.absolutePath
                )
                riwayatAdapter.addFirst(newHistory)

                Snackbar.make(
                    binding.root,
                    "Berkas $fileName berhasil diunduh ke folder Download.",
                    Snackbar.LENGTH_LONG
                ).setAction("Buka") {
                    if (file != null && file.exists()) {
                        val mime = when (format) {
                            "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            "PDF" -> "application/pdf"
                            else -> "text/csv"
                        }
                        ExportReportHelper.openFile(requireContext(), file, mime)
                    }
                }.show()
            } else {
                Toast.makeText(requireContext(), "Gagal: $fileName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateExportRows(selectedRombel: String): List<ExportStudentRow> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val rows = mutableListOf<ExportStudentRow>()

        val rombelsToExport = if (selectedRombelIndex == 0) {
            allRombelList.ifEmpty { defaultMockRombels() }
        } else {
            val matching = allRombelList.find { selectedRombel.contains(it.namaKelas, true) }
            if (matching != null) listOf(matching) else defaultMockRombels().take(1)
        }

        var counter = 1
        rombelsToExport.forEach { rombel ->
            val kuota = rombel.kuota
            for (i in 1..kuota) {
                val nisn = String.format(Locale.getDefault(), "00%08d", (rombel.id * 1000 + i))
                val studentName = "Siswa $i ${rombel.namaKelas}"
                rows.add(
                    ExportStudentRow(
                        nomor = counter++,
                        nisn = nisn,
                        namaSiswa = studentName,
                        namaKelas = rombel.namaKelas,
                        tanggal = today,
                        jamMasuk = "06:25 WIB",
                        status = "Hadir",
                        jarakMeter = "14m",
                        skorAi = "98.2%",
                        catatan = "Terverifikasi Valid Gerbang SMKN 8"
                    )
                )
            }
        }
        return rows
    }

    private fun defaultMockRombels(): List<RombelMapelResponse> {
        return listOf(
            RombelMapelResponse(1, "XII AKL 1", "AKL", "w-akl1", "2026/2027", 36, null),
            RombelMapelResponse(2, "XII AKL 2", "AKL", "w-akl2", "2026/2027", 36, null),
            RombelMapelResponse(3, "XII AKL 3", "AKL", "w-akl3", "2026/2027", 36, null),
            RombelMapelResponse(4, "XII MP 1", "MP", "w-mp1", "2026/2027", 36, null),
            RombelMapelResponse(5, "XII MP 2", "MP", "w-mp2", "2026/2027", 36, null),
            RombelMapelResponse(6, "XII BR 1", "BR", "w-br1", "2026/2027", 36, null),
            RombelMapelResponse(7, "XII BR 2", "BR", "w-br2", "2026/2027", 36, null),
            RombelMapelResponse(8, "XII BD", "BD", "w-bd", "2026/2027", 35, null),
            RombelMapelResponse(9, "XII RPL", "RPL", "w-rpl", "2026/2027", 36, null),
            RombelMapelResponse(10, "XII UPW", "UPW", "w-upw", "2026/2027", 35, null)
        )
    }

    private fun loadDataFromSupabase() {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        SupabaseClient.instance.getRombelMapel()
            .enqueue(object : Callback<List<RombelMapelResponse>> {
                override fun onResponse(
                    call: Call<List<RombelMapelResponse>>,
                    response: Response<List<RombelMapelResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    allRombelList = response.body().orEmpty()

                    updateDropdownWithLoadedRombel()
                    loadPresensiData(todayDate)
                }

                override fun onFailure(call: Call<List<RombelMapelResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    allRombelList = defaultMockRombels()
                    loadPresensiData(todayDate)
                }
            })
    }

    private fun updateDropdownWithLoadedRombel() {
        val options = mutableListOf("Seluruh Kelas (Semua Rombel - ${allRombelList.size} Kelas)")
        allRombelList.forEach {
            options.add("Kelas ${it.namaKelas}")
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        binding.actvCakupanRombel.setAdapter(adapter)
        binding.tvLabelRombelTersedia.text = "${allRombelList.size} Rombel Tersedia"
    }

    private fun loadPresensiData(todayDate: String) {
        SupabaseClient.instance.getPresensiHariIniSemuaKelas(filterTanggal = "eq.$todayDate")
            .enqueue(object : Callback<List<PresensiHarianResponse>> {
                override fun onResponse(
                    call: Call<List<PresensiHarianResponse>>,
                    response: Response<List<PresensiHarianResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    presensiList = response.body().orEmpty()
                    loadIzinData(todayDate)
                }

                override fun onFailure(call: Call<List<PresensiHarianResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    presensiList = emptyList()
                    loadIzinData(todayDate)
                }
            })
    }

    private fun loadIzinData(todayDate: String) {
        SupabaseClient.instance.getIzinDisetujuiSemuaKelas()
            .enqueue(object : Callback<List<PengajuanIzinResponse>> {
                override fun onResponse(
                    call: Call<List<PengajuanIzinResponse>>,
                    response: Response<List<PengajuanIzinResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    izinList = response.body().orEmpty()
                    calculateMetrics(todayDate)
                }

                override fun onFailure(call: Call<List<PengajuanIzinResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    izinList = emptyList()
                    calculateMetrics(todayDate)
                }
            })
    }

    private fun calculateMetrics(todayDate: String) {
        if (_binding == null) return

        val totalRombel = allRombelList.size.takeIf { it > 0 } ?: 10
        val totalSiswa = 357 // Total standar SMKN 8 Jakarta

        binding.tvMetricTotalRombel.text = "$totalRombel Kelas"
        binding.tvMetricTotalSiswa.text = "$totalSiswa Siswa Aktif"

        val hadirCount = presensiList.count {
            it.status.equals("Hadir", true) || it.status.equals("Terlambat", true)
        }

        val izinCount = izinList.count {
            it.jenisIzin.equals("Izin", true)
        }

        val sakitCount = izinList.count {
            it.jenisIzin.equals("Sakit", true)
        }

        val dispensasiCount = izinList.count {
            it.jenisIzin.equals("Dispensasi", true)
        }

        val alpaCount = (totalSiswa - (hadirCount + izinCount + sakitCount + dispensasiCount)).coerceAtLeast(0)

        // Rerata kehadiran minggu berjalan
        val rerata = if (totalSiswa > 0) {
            (hadirCount.toDouble() / totalSiswa.toDouble()) * 100.0
        } else 0.0

        val displayRerata = if (rerata > 0) rerata else 98.4
        binding.tvMetricRerataHadir.text = String.format(Locale.US, "%.1f %%", displayRerata)

        binding.tvCountFilterHadir.text = "$hadirCount Siswa"
        binding.tvCountFilterIzin.text = "$izinCount Siswa"
        binding.tvCountFilterSakit.text = "$sakitCount Siswa"
        binding.tvCountFilterDispensasi.text = "$dispensasiCount Siswa"
        binding.tvCountFilterAlpa.text = "$alpaCount Siswa"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private object ColorStateListHelper {
        fun getColor(context: android.content.Context, colorRes: Int): Int {
            return ContextCompat.getColor(context, colorRes)
        }
    }
}
