package com.andev.absensiswaku.ui.walas

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.PengajuanIzinResponse
import com.andev.absensiswaku.data.network.RiwayatModel
import com.andev.absensiswaku.data.network.RombelMapelResponse
import com.andev.absensiswaku.data.network.SiswaMiniResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.databinding.FragmentWalasLaporanBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalasLaporanFragment : Fragment() {

    private var _binding: FragmentWalasLaporanBinding? = null
    private val binding get() = _binding!!

    private var namaWalas: String = "Herlina S.E"
    private var idKelas: Int = 1
    private var namaKelas: String = "XII AKL 1"
    private var countTotalSiswa: Int = 0

    private var selectedFilterMode: Int = 1 // 1: Bulan Ini, 2: Semester Ganjil, 3: Rentang Kustom
    private var customStartMillis: Long = 0L
    private var customEndMillis: Long = 0L

    private val listSiswaKelas = mutableListOf<SiswaMiniResponse>()
    private val allPresensiList = mutableListOf<RiwayatModel>()
    private val allIzinList = mutableListOf<PengajuanIzinResponse>()
    private val currentRekapHarianList = mutableListOf<HistoriKehadiranModel>()

    private lateinit var rekapHarianAdapter: HistoriKehadiranAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalasLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterChips()
        setupExportPdfButton()
        loadSessionData()
    }

    override fun onResume() {
        super.onResume()
        loadSessionData()
    }

    private fun loadSessionData() {
        val pref = requireContext().getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val idWalas = pref.getString("ID_WALAS", "")?.takeIf { it.isNotBlank() }
            ?: pref.getString("user_id", "")?.takeIf { it.isNotBlank() }
            ?: ""

        namaWalas = pref.getString("NAMA_WALAS", "")?.takeIf { it.isNotBlank() }
            ?: pref.getString("NAMA_LENGKAP", "")?.takeIf { it.isNotBlank() }
            ?: pref.getString("nama", "")?.takeIf { it.isNotBlank() }
            ?: "Herlina S.E"

        val savedIdKelas = pref.getInt("ID_KELAS", 0)
        val savedNamaKelas = pref.getString("NAMA_KELAS", "")?.takeIf { it.isNotBlank() }
            ?: pref.getString("nama_kelas", "")?.takeIf { it.isNotBlank() }
            ?: ""

        if (savedIdKelas > 0 && savedNamaKelas.isNotBlank()) {
            idKelas = savedIdKelas
            namaKelas = savedNamaKelas
            updateHeaderUi()
            loadDataFromSupabase()
        } else if (idWalas.isNotBlank()) {
            // Ambil data kelas binaan asli berdasarkan wali_kelas_id milik user yang login
            SupabaseClient.instance.getRombelByWalas(filterWalas = "eq.$idWalas")
                .enqueue(object : Callback<List<RombelMapelResponse>> {
                    override fun onResponse(
                        call: Call<List<RombelMapelResponse>>,
                        response: Response<List<RombelMapelResponse>>
                    ) {
                        if (!isAdded || _binding == null) return
                        val rombel = response.body()?.firstOrNull()
                        if (rombel != null) {
                            idKelas = rombel.id ?: 1
                            namaKelas = rombel.namaKelas ?: "XII AKL 1"
                            pref.edit()
                                .putInt("ID_KELAS", idKelas)
                                .putString("NAMA_KELAS", namaKelas)
                                .commit()
                        } else {
                            idKelas = 1
                            namaKelas = "XII AKL 1"
                        }
                        updateHeaderUi()
                        loadDataFromSupabase()
                    }

                    override fun onFailure(call: Call<List<RombelMapelResponse>>, t: Throwable) {
                        if (!isAdded || _binding == null) return
                        idKelas = 1
                        namaKelas = "XII AKL 1"
                        updateHeaderUi()
                        loadDataFromSupabase()
                    }
                })
        } else {
            idKelas = 1
            namaKelas = "XII AKL 1"
            updateHeaderUi()
            loadDataFromSupabase()
        }
    }

    private fun updateHeaderUi() {
        binding.tvSubHeaderWalas.text = "Kelas binaan: $namaKelas • Wali Kelas: $namaWalas"
        binding.btnUnduhPdfKelas.text = "📥 Unduh Laporan PDF Kelas $namaKelas"
    }

    private fun setupRecyclerView() {
        rekapHarianAdapter = HistoriKehadiranAdapter(
            listData = emptyList(),
            fragmentManager = childFragmentManager
        )

        binding.rvRiwayatHarianKelas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rekapHarianAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupFilterChips() {
        val namaBulanIni = try {
            val sdfBulanIni = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
            sdfBulanIni.format(Date())
        } catch (e: Exception) {
            "Bulan Ini"
        }
        binding.tvChipBulanIni.text = "Bulan Ini ($namaBulanIni)"

        binding.chipBulanIni.setOnClickListener {
            switchFilterSelection(1)
        }
        binding.chipSemester.setOnClickListener {
            switchFilterSelection(2)
        }
        binding.chipRentangKustom.setOnClickListener {
            showCustomDateRangePicker()
        }
    }

    private fun switchFilterSelection(mode: Int) {
        selectedFilterMode = mode

        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_month_active)
        val inactiveBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_month_inactive)
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val textSecondaryColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        // Reset All
        binding.chipBulanIni.background = inactiveBg
        binding.tvChipBulanIni.setTextColor(textSecondaryColor)

        binding.chipSemester.background = inactiveBg
        binding.tvChipSemester.setTextColor(textSecondaryColor)

        binding.chipRentangKustom.background = inactiveBg
        binding.tvChipRentangKustom.setTextColor(textSecondaryColor)

        // Set Active
        when (mode) {
            1 -> {
                binding.chipBulanIni.background = activeBg
                binding.tvChipBulanIni.setTextColor(whiteColor)
            }
            2 -> {
                binding.chipSemester.background = activeBg
                binding.tvChipSemester.setTextColor(whiteColor)
            }
            3 -> {
                binding.chipRentangKustom.background = activeBg
                binding.tvChipRentangKustom.setTextColor(whiteColor)
            }
        }

        processAndFilterAttendanceData()
    }

    private fun showCustomDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Pilih Rentang Tanggal Rekap")
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            if (!isAdded || _binding == null) return@addOnPositiveButtonClickListener
            if (selection.first != null && selection.second != null) {
                customStartMillis = selection.first!!
                customEndMillis = selection.second!!

                val rangeText = try {
                    val sdfRange = SimpleDateFormat("dd MMM", Locale.forLanguageTag("id-ID"))
                    val startStr = sdfRange.format(Date(customStartMillis))
                    val endStr = sdfRange.format(Date(customEndMillis))
                    "$startStr - $endStr"
                } catch (e: Exception) {
                    "Rentang Kustom"
                }
                binding.tvChipRentangKustom.text = rangeText

                switchFilterSelection(3)
            }
        }

        picker.show(childFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun loadDataFromSupabase() {
        if (!isAdded || _binding == null) return
        binding.progressBarLoading.visibility = View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE

        // 1. Ambil jumlah siswa riil di kelas binaan dari Supabase:
        //    siswa?id_kelas=eq.$idKelasBinaan&select=id
        SupabaseClient.instance.getSiswaByKelas(filterKelas = "eq.$idKelas")
            .enqueue(object : Callback<List<SiswaMiniResponse>> {
                override fun onResponse(
                    call: Call<List<SiswaMiniResponse>>,
                    response: Response<List<SiswaMiniResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    listSiswaKelas.clear()
                    val body = response.body().orEmpty()
                    listSiswaKelas.addAll(body)
                    countTotalSiswa = listSiswaKelas.size
                    loadPresensiDanIzinSupabase()
                }

                override fun onFailure(call: Call<List<SiswaMiniResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    listSiswaKelas.clear()
                    countTotalSiswa = 0
                    loadPresensiDanIzinSupabase()
                }
            })
    }

    private fun loadPresensiDanIzinSupabase() {
        if (!isAdded || _binding == null) return
        // 2. Ambil catatan presensi dari Supabase:
        //    presensi_harian?id_kelas=eq.$idKelasBinaan
        val siswaIds = listSiswaKelas.mapNotNull { it.id }.toSet()

        SupabaseClient.instance.getPresensiKelas(filterKelas = "eq.$idKelas")
            .enqueue(object : Callback<List<RiwayatModel>> {
                override fun onResponse(
                    call: Call<List<RiwayatModel>>,
                    response: Response<List<RiwayatModel>>
                ) {
                    if (!isAdded || _binding == null) return
                    allPresensiList.clear()
                    val records = response.body().orEmpty()
                    // Pastikan hanya catatan siswa kelas binaan yang disimpan
                    if (siswaIds.isNotEmpty()) {
                        allPresensiList.addAll(records.filter { it.siswaId in siswaIds })
                    } else {
                        allPresensiList.addAll(records)
                    }
                    fetchIzinDisetujuiSupabase()
                }

                override fun onFailure(call: Call<List<RiwayatModel>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    allPresensiList.clear()
                    fetchIzinDisetujuiSupabase()
                }
            })
    }

    private fun fetchIzinDisetujuiSupabase() {
        if (!isAdded || _binding == null) return
        // 3. Ambil pengajuan izin sah dari Supabase:
        //    pengajuan_izin?id_kelas=eq.$idKelasBinaan&status_verifikasi=eq.Disetujui
        val siswaIds = listSiswaKelas.mapNotNull { it.id }.toSet()

        SupabaseClient.instance.getIzinKelas(status = "eq.Disetujui", filterKelas = "eq.$idKelas")
            .enqueue(object : Callback<List<PengajuanIzinResponse>> {
                override fun onResponse(
                    call: Call<List<PengajuanIzinResponse>>,
                    response: Response<List<PengajuanIzinResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    allIzinList.clear()
                    val records = response.body().orEmpty()
                    // Filter sah: hanya izin disetujui milik siswa rombel binaan ini
                    val validIzin = records.filter { izin ->
                        val isClassMatch = if (siswaIds.isNotEmpty()) (izin.siswaId in siswaIds) else true
                        isClassMatch && izin.statusVerifikasi?.equals("Disetujui", ignoreCase = true) == true
                    }
                    allIzinList.addAll(validIzin)

                    binding.progressBarLoading.visibility = View.GONE
                    processAndFilterAttendanceData()
                }

                override fun onFailure(call: Call<List<PengajuanIzinResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    allIzinList.clear()
                    binding.progressBarLoading.visibility = View.GONE
                    processAndFilterAttendanceData()
                }
            })
    }

    private fun processAndFilterAttendanceData() {
        if (!isAdded || _binding == null) return

        val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Date()
        val currentMonthPattern = try {
            SimpleDateFormat("yyyy-MM", Locale.US).format(today)
        } catch (e: Exception) {
            ""
        }

        // Filter presensi sesuai periode
        val filteredPresensi = allPresensiList.filter { item ->
            val tgl = item.tanggal ?: ""
            when (selectedFilterMode) {
                1 -> if (currentMonthPattern.isNotEmpty()) tgl.startsWith(currentMonthPattern) else true
                2 -> {
                    // Semester Ganjil: Juli s/d Desember
                    val month = tgl.substringOrNull(5, 7)?.toIntOrNull() ?: 9
                    month in 7..12
                }
                3 -> {
                    if (customStartMillis != 0L && customEndMillis != 0L) {
                        try {
                            val itemTime = sdfIso.parse(tgl)?.time ?: 0L
                            itemTime in customStartMillis..customEndMillis
                        } catch (e: Exception) { true }
                    } else true
                }
                else -> true
            }
        }

        // Filter izin sesuai periode
        val filteredIzin = allIzinList.filter { item ->
            val tglMulai = item.tanggalMulai ?: ""
            when (selectedFilterMode) {
                1 -> if (currentMonthPattern.isNotEmpty()) tglMulai.startsWith(currentMonthPattern) else true
                2 -> {
                    val month = tglMulai.substringOrNull(5, 7)?.toIntOrNull() ?: 9
                    month in 7..12
                }
                3 -> {
                    if (customStartMillis != 0L && customEndMillis != 0L) {
                        try {
                            val itemTime = sdfIso.parse(tglMulai)?.time ?: 0L
                            itemTime in customStartMillis..customEndMillis
                        } catch (e: Exception) { true }
                    } else true
                }
                else -> true
            }
        }

        // ==========================================================
        // PERHITUNGAN MURNI DATABASE:
        // ==========================================================
        val totalSiswa = listSiswaKelas.size // cth: 36
        val hadir = filteredPresensi.distinctBy { it.siswaId }.count {
            it.status.equals("Hadir", true) ||
            it.status.equals("Terlambat", true) ||
            it.status.equals("TEPAT_WAKTU", true)
        }
        val terlambat = filteredPresensi.distinctBy { it.siswaId }.count {
            it.status.equals("Terlambat", true)
        }
        val izinSakit = filteredIzin.distinctBy { it.siswaId }.size
        val alpa = (totalSiswa - (hadir + izinSakit)).coerceAtLeast(0)

        // Pasang ke UI (JIKA DI DATABASE TIDAK ADA IZIN: WAJIB "0 Siswa", Hadir "0 Siswa", Alpa "$totalSiswa Siswa")
        binding.tvStatHadir.text = "$hadir Siswa"
        binding.tvStatTerlambat.text = "$terlambat Siswa"
        binding.tvStatIzin.text = "$izinSakit Siswa"
        binding.tvStatAlpa.text = "$alpa Siswa"

        // Hitung Persentase Disiplin
        val persentaseDisiplin = if (totalSiswa > 0) {
            (hadir.toDouble() / totalSiswa.toDouble()) * 100.0
        } else {
            0.0
        }
        binding.tvDisiplinPercentage.text = String.format(Locale.getDefault(), "%.1f%% Disiplin", persentaseDisiplin)
        binding.progressKehadiranKelas.progress = persentaseDisiplin.toInt().coerceIn(0, 100)

        // ==========================================================
        // HISTORI KEHADIRAN HARIAN ROMBEL:
        // Ambil rekaman asli dari database tanpa data dummy/mock!
        // ==========================================================
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val allDates = (
            filteredPresensi.mapNotNull { it.tanggal?.takeIf { s -> s.isNotBlank() } } +
            filteredIzin.mapNotNull { it.tanggalMulai?.takeIf { s -> s.isNotBlank() } }
        ).toMutableSet()

        // Pastikan tanggal hari ini otomatis tercatat dalam histori harian pada mode filter aktif
        if (selectedFilterMode == 1) {
            allDates.add(todayIso)
        } else if (selectedFilterMode == 2) {
            val cal = java.util.Calendar.getInstance()
            val m = cal.get(java.util.Calendar.MONTH) + 1
            if (m in 7..12) allDates.add(todayIso)
        } else if (selectedFilterMode == 3) {
            if (customStartMillis != 0L && customEndMillis != 0L) {
                val nowTime = System.currentTimeMillis()
                if (nowTime in customStartMillis..customEndMillis) {
                    allDates.add(todayIso)
                }
            }
        }

        val distinctDates = allDates.distinct().sortedDescending()

        val sdfDisplay = SimpleDateFormat("EEEE, d MMM yyyy", Locale.forLanguageTag("id-ID"))
        currentRekapHarianList.clear()

        distinctDates.forEach { tgl ->
            val presensiHariIni = filteredPresensi.filter { it.tanggal?.equals(tgl, ignoreCase = true) == true }
            val izinHariIni = filteredIzin.filter { izin ->
                val tMulai = izin.tanggalMulai ?: ""
                val tSelesai = izin.tanggalSelesai ?: tMulai
                tgl in tMulai..tSelesai || tMulai == tgl
            }

            val h = presensiHariIni.distinctBy { it.siswaId }.count {
                it.status.equals("Hadir", true) ||
                it.status.equals("Terlambat", true) ||
                it.status.equals("TEPAT_WAKTU", true)
            }
            val t = presensiHariIni.distinctBy { it.siswaId }.count {
                it.status.equals("Terlambat", true)
            }
            val i = izinHariIni.distinctBy { it.siswaId }.size
            val a = (totalSiswa - (h + i)).coerceAtLeast(0)

            val totalHadirHariIni = h
            val pct = if (totalSiswa > 0) {
                (totalHadirHariIni.toDouble() / totalSiswa.toDouble()) * 100.0
            } else {
                0.0
            }

            val formattedDate = try {
                val d = sdfIso.parse(tgl)
                if (d != null) sdfDisplay.format(d) else tgl
            } catch (e: Exception) {
                tgl
            }

            currentRekapHarianList.add(
                HistoriKehadiranModel(
                    tanggal = tgl,
                    rawTanggal = tgl,
                    tanggalFormatted = formattedDate,
                    idKelas = idKelas,
                    namaKelas = namaKelas,
                    countHadir = (h - t).coerceAtLeast(0),
                    countTerlambat = t,
                    countIzin = i,
                    countAlpa = a,
                    totalSiswa = totalSiswa,
                    persentase = pct,
                    isLengkap = totalSiswa > 0 && (h + i) >= (totalSiswa * 0.90)
                )
            )
        }

        // Tampilkan Empty State jika tidak ada rekaman riwayat di database
        if (currentRekapHarianList.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvRiwayatHarianKelas.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvRiwayatHarianKelas.visibility = View.VISIBLE
            rekapHarianAdapter.updateData(currentRekapHarianList)
        }
    }

    private fun setupExportPdfButton() {
        binding.btnUnduhPdfKelas.setOnClickListener {
            if (!isAdded || _binding == null) return@setOnClickListener
            if (currentRekapHarianList.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Tidak ada data rekap presensi untuk diekspor pada periode ini.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            generatePdfRekapKelas()
        }
    }

    /**
     * Membuat berkas PDF Resmi A4 Rekapitulasi Kehadiran Kelas Binaan
     */
    private fun generatePdfRekapKelas() {
        if (!isAdded || _binding == null) return
        val ctx = context ?: return

        val periodeText = when (selectedFilterMode) {
            1 -> {
                val bulanStr = try {
                    val sdfBulan = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
                    sdfBulan.format(Date())
                } catch (e: Exception) { "" }
                "Bulan $bulanStr"
            }
            2 -> "Semester Ganjil 2026/2027"
            else -> "Rentang Kustom (${binding.tvChipRentangKustom.text})"
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4: 595 x 842 pt
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Setup Paints
        val paintPrimaryTeal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00685F")
            textSize = 15f
            isFakeBoldText = true
        }

        val paintTextDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1C30")
            textSize = 10f
        }

        val paintTextMuted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
        }

        val paintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1C30")
            textSize = 10f
            isFakeBoldText = true
        }

        val paintLineTeal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00685F")
            strokeWidth = 2f
        }

        val paintDivider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        val paintTableHeaderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EFF4FF")
            style = Paint.Style.FILL
        }

        var y = 40f

        // --- 1. KOP SURAT RESMI ---
        canvas.drawText("PEMERINTAH PROVINSI DKI JAKARTA", 40f, y, paintBold)
        y += 16f
        canvas.drawText("DINAS PENDIDIKAN", 40f, y, paintBold)
        y += 18f
        canvas.drawText("SMK NEGERI 8 JAKARTA", 40f, y, paintPrimaryTeal)
        y += 14f
        canvas.drawText("Alamat: Jl. Raya Pejaten Pasar Minggu, Jakarta Selatan • SISTEM PRESENSI RESMI", 40f, y, paintTextMuted)
        y += 12f
        canvas.drawLine(40f, y, 555f, y, paintLineTeal)
        y += 24f

        // --- 2. METADATA DOKUMEN ---
        canvas.drawText("LAPORAN REKAPITULASI PRESENSI KELAS $namaKelas", 40f, y, paintBold)
        y += 18f
        canvas.drawText("Kelas Binaan           : $namaKelas", 40f, y, paintTextDark)
        canvas.drawText("Periode Rekap          : $periodeText", 310f, y, paintTextDark)
        y += 14f
        canvas.drawText("Wali Kelas Pembina     : $namaWalas", 40f, y, paintTextDark)
        canvas.drawText("Jumlah Siswa Terdaftar : $countTotalSiswa Siswa", 310f, y, paintTextDark)
        y += 22f

        // --- 3. TABEL REKAP KEHADIRAN (HEADERS) ---
        val tableTop = y
        val tableBottom = tableTop + 24f
        canvas.drawRect(40f, tableTop, 555f, tableBottom, paintTableHeaderBg)

        y += 16f
        canvas.drawText("No", 46f, y, paintBold)
        canvas.drawText("Tanggal", 75f, y, paintBold)
        canvas.drawText("Hadir", 200f, y, paintBold)
        canvas.drawText("Terlambat", 260f, y, paintBold)
        canvas.drawText("Izin/Sakit", 335f, y, paintBold)
        canvas.drawText("Alpa", 410f, y, paintBold)
        canvas.drawText("% Kehadiran", 475f, y, paintBold)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, paintDivider)

        // --- 4. ROWS DATA LOOP ---
        currentRekapHarianList.forEachIndexed { index, item ->
            y += 18f
            if (y > 700f) return@forEachIndexed // Batasi satu halaman A4

            val persenText = String.format(Locale.US, "%.1f%%", item.persentase)
            val izinSakitTotal = item.countSakit + item.countIzin

            canvas.drawText("${index + 1}", 46f, y, paintTextDark)
            canvas.drawText(item.tanggal, 75f, y, paintTextDark)
            canvas.drawText("${item.countHadir}", 200f, y, paintTextDark)
            canvas.drawText("${item.countTerlambat}", 260f, y, paintTextDark)
            canvas.drawText("$izinSakitTotal", 335f, y, paintTextDark)
            canvas.drawText("${item.countAlpa}", 410f, y, paintTextDark)
            canvas.drawText(persenText, 475f, y, paintBold)

            y += 6f
            canvas.drawLine(40f, y, 555f, y, paintDivider)
        }

        // --- 5. TANDA TANGAN PENGESAHAN DOKUMEN ---
        y = 730f
        val todayStr = try {
            SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date())
        } catch (e: Exception) {
            ""
        }

        // Kolom Kiri: Kepala Sekolah
        canvas.drawText("Mengetahui,", 40f, y, paintTextDark)
        canvas.drawText("Jakarta, $todayStr", 360f, y, paintTextDark)
        y += 14f
        canvas.drawText("Kepala SMKN 8 Jakarta,", 40f, y, paintTextDark)
        canvas.drawText("Wali Kelas $namaKelas,", 360f, y, paintTextDark)
        y += 45f
        canvas.drawText("Dra. Hj. Nurjanah, M.Pd", 40f, y, paintBold)
        canvas.drawText(namaWalas, 360f, y, paintBold)
        y += 12f
        canvas.drawText("NIP. 196805121994122001", 40f, y, paintTextMuted)
        canvas.drawText("NIP/NIK. Pembina Rombel", 360f, y, paintTextMuted)

        pdfDocument.finishPage(page)

        // --- 6. SIMPAN FILE KE MEDIASTORE (DOWNLOADS) ---
        val cleanKelas = namaKelas.replace(" ", "_").replace("/", "-")
        val fileName = "Rekap_Presensi_${cleanKelas}_${System.currentTimeMillis()}.pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val resolver = ctx.contentResolver
        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val pdfUri: Uri? = resolver.insert(collectionUri, contentValues)

        if (pdfUri != null) {
            try {
                resolver.openOutputStream(pdfUri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }

                val rootLayout = _binding?.root ?: return
                Snackbar.make(rootLayout, "✓ Berhasil Diekspor: $fileName", Snackbar.LENGTH_LONG)
                    .setAction("Buka") {
                        openPdfFile(pdfUri)
                    }
                    .show()

                openPdfFile(pdfUri)

            } catch (e: Exception) {
                Toast.makeText(ctx, "Gagal menulis file PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                try {
                    pdfDocument.close()
                } catch (ignored: Exception) {}
            }
        } else {
            try {
                pdfDocument.close()
            } catch (ignored: Exception) {}
            Toast.makeText(ctx, "Gagal membuat entri PDF di direktori Downloads", Toast.LENGTH_LONG).show()
        }
    }

    private fun openPdfFile(pdfUri: Uri) {
        val ctx = context ?: return
        try {
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(openIntent, "Buka Berkas PDF"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "Tidak ditemukan aplikasi pembaca PDF di perangkat ini.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(ctx, "Gagal membuka PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun String.substringOrNull(startIndex: Int, endIndex: Int): String? {
        return try {
            if (this.length >= endIndex) this.substring(startIndex, endIndex) else null
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
