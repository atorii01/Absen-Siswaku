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
import java.util.TimeZone

// ViewBinding aliases agar selaras dengan naming convention
private val FragmentWalasLaporanBinding.tvCountHadir get() = tvTotalHadir
private val FragmentWalasLaporanBinding.tvCountTerlambat get() = tvTotalTerlambat
private val FragmentWalasLaporanBinding.tvCountIzin get() = tvTotalIzinSakit
private val FragmentWalasLaporanBinding.tvCountAlpa get() = tvTotalAlpa
private val FragmentWalasLaporanBinding.tvPersentaseKelas get() = tvDisiplinPercentage
private val FragmentWalasLaporanBinding.progressBarDisiplin get() = progressKehadiranKelas

class WalasLaporanFragment : Fragment() {

    private var _binding: FragmentWalasLaporanBinding? = null
    private val binding get() = _binding!!

    private var namaWalas: String = "Farauk Pratama, S.Kom."
    private var idKelas: Int = 9
    private var namaKelas: String = "XII RPL"
    private var countTotalSiswa: Int = 0

    private var selectedFilterMode: Int = 1 // 1: Bulan Ini, 2: Semester Ganjil, 3: Rentang Kustom
    private var customStartMillis: Long = 0L
    private var customEndMillis: Long = 0L

    private val allPresensiList = mutableListOf<RiwayatModel>()
    private val allIzinList = mutableListOf<PengajuanIzinResponse>()
    private val currentRekapHarianList = mutableListOf<RekapHarianKelasModel>()

    private lateinit var rekapHarianAdapter: RekapHarianWalasAdapter

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

        loadSessionData()
        setupRecyclerView()
        setupFilterChips()
        setupExportPdfButton()
        loadDataFromSupabase()
    }

    override fun onResume() {
        super.onResume()
        loadSessionData()
    }

    private fun loadSessionData() {
        val pref = requireContext().getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        namaWalas = pref.getString("NAMA_WALAS", "Farauk Pratama, S.Kom.") ?: "Farauk Pratama, S.Kom."
        idKelas = pref.getInt("ID_KELAS", 9)
        namaKelas = pref.getString("NAMA_KELAS", "XII RPL") ?: "XII RPL"

        binding.tvSubHeaderWalas.text = "Kelas binaan: $namaKelas • Wali Kelas: $namaWalas"
        binding.btnUnduhPdfKelas.text = "📥 Unduh Laporan PDF Kelas $namaKelas"
    }

    private fun setupRecyclerView() {
        rekapHarianAdapter = RekapHarianWalasAdapter(emptyList()) { item ->
            Toast.makeText(
                requireContext(),
                "${item.tanggalFormatted}: ${item.countHadir} Hadir, ${item.countTerlambat} Terlambat, ${item.countIzin} Izin, ${item.countAlpa} Alpa",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.rvRiwayatHarianKelas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rekapHarianAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupFilterChips() {
        // Label Bulan Ini dinamis berdasarkan tanggal sekarang
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

        // 1. Ambil jumlah siswa terdaftar di rombel binaan
        SupabaseClient.instance.getSiswaByKelas(filterKelas = "eq.$idKelas")
            .enqueue(object : Callback<List<SiswaMiniResponse>> {
                override fun onResponse(
                    call: Call<List<SiswaMiniResponse>>,
                    response: Response<List<SiswaMiniResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    val body = response.body().orEmpty()
                    countTotalSiswa = if (body.isNotEmpty()) body.size else 0
                    loadPresensiDanIzinSupabase()
                }

                override fun onFailure(call: Call<List<SiswaMiniResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    countTotalSiswa = 0
                    loadPresensiDanIzinSupabase()
                }
            })
    }

    private fun loadPresensiDanIzinSupabase() {
        if (!isAdded || _binding == null) return
        // 2. Ambil data presensi harian kelas
        SupabaseClient.instance.getPresensiKelas(filterKelas = "eq.$idKelas")
            .enqueue(object : Callback<List<RiwayatModel>> {
                override fun onResponse(
                    call: Call<List<RiwayatModel>>,
                    response: Response<List<RiwayatModel>>
                ) {
                    if (!isAdded || _binding == null) return
                    allPresensiList.clear()
                    if (response.isSuccessful && response.body() != null) {
                        allPresensiList.addAll(response.body()!!)
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
        // 3. Ambil data pengajuan izin berstatus Disetujui
        SupabaseClient.instance.getIzinKelas(status = "eq.Disetujui", filterKelas = "eq.$idKelas")
            .enqueue(object : Callback<List<PengajuanIzinResponse>> {
                override fun onResponse(
                    call: Call<List<PengajuanIzinResponse>>,
                    response: Response<List<PengajuanIzinResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    allIzinList.clear()
                    if (response.isSuccessful && response.body() != null) {
                        allIzinList.addAll(response.body()!!)
                    }
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

        // 1. Ambil presensi unik per siswa (dengan safe filter siswaId != null)
        val presensiUnikPerSiswa = (filteredPresensi ?: emptyList())
            .filter { it.siswaId != null }
            .sortedByDescending { it.waktuMasuk }
            .distinctBy { it.siswaId }

        // 2. Hitung jumlah siswa per kategori status (aman dari null)
        val countHadir = presensiUnikPerSiswa.count { 
            it.status?.equals("Hadir", ignoreCase = true) == true || 
            it.status?.equals("TEPAT_WAKTU", ignoreCase = true) == true 
        }
        val countTerlambat = presensiUnikPerSiswa.count { 
            it.status?.equals("Terlambat", ignoreCase = true) == true 
        }

        // 3. Ambil pengajuan izin unik per siswa yang disetujui (aman dari null)
        val izinUnikPerSiswa = (filteredIzin ?: emptyList())
            .filter { it.siswaId != null && it.statusVerifikasi?.equals("Disetujui", ignoreCase = true) == true }
            .distinctBy { it.siswaId }
        val countIzin = izinUnikPerSiswa.size

        // 4. Hitung Alpa secara proporsional dari total kapasitas rombel (hindari division by zero dan negatif)
        val totalSiswaKelas = countTotalSiswa.takeIf { it > 0 } ?: 36
        val totalMasukDanIzin = countHadir + countTerlambat + countIzin
        val countAlpa = (totalSiswaKelas - totalMasukDanIzin).coerceAtLeast(0)

        // 5. Hitung Persentase Disiplin Kehadiran Kelas (hindari pembagian dengan nol)
        val totalSiswaHadir = countHadir + countTerlambat
        val persentaseDisiplin = if (totalSiswaKelas > 0) {
            (totalSiswaHadir.toDouble() / totalSiswaKelas.toDouble()) * 100.0
        } else {
            0.0
        }

        // Terapkan ke Komponen UI Header & Mini Card
        binding.tvCountHadir.text = "$countHadir Siswa"
        binding.tvCountTerlambat.text = "$countTerlambat Siswa"
        binding.tvCountIzin.text = "$countIzin Siswa"
        binding.tvCountAlpa.text = "$countAlpa Siswa"
        binding.tvPersentaseKelas.text = String.format(Locale.getDefault(), "%.1f%% Disiplin", persentaseDisiplin)
        binding.progressBarDisiplin.progress = persentaseDisiplin.toInt().coerceIn(0, 100)

        // 2. Kelompokkan Data Berdasarkan Tanggal (groupBy { it.tanggal }) untuk Histori Harian
        val distinctDates = (filteredPresensi.mapNotNull { it.tanggal?.takeIf { s -> s.isNotBlank() } } +
                filteredIzin.mapNotNull { it.tanggalMulai?.takeIf { s -> s.isNotBlank() } }).distinct()
        val sdfDisplay = SimpleDateFormat("EEEE, d MMM yyyy", Locale.forLanguageTag("id-ID"))
        val sortedDates = distinctDates.sortedDescending()

        currentRekapHarianList.clear()

        sortedDates.forEach { tgl ->
            val presensiHariIni = filteredPresensi.filter { it.tanggal?.equals(tgl, ignoreCase = true) == true }
            val izinHariIni = filteredIzin.filter { it.tanggalMulai?.equals(tgl, ignoreCase = true) == true }

            // 1. Ambil presensi unik per siswa hari ini (safe filter)
            val presensiUnikHariIni = presensiHariIni
                .filter { it.siswaId != null }
                .sortedByDescending { it.waktuMasuk }
                .distinctBy { it.siswaId }

            // 2. Hitung jumlah siswa per kategori status
            val h = presensiUnikHariIni.count { 
                it.status?.equals("Hadir", ignoreCase = true) == true || 
                it.status?.equals("TEPAT_WAKTU", ignoreCase = true) == true 
            }
            val t = presensiUnikHariIni.count { 
                it.status?.equals("Terlambat", ignoreCase = true) == true 
            }

            // 3. Ambil pengajuan izin unik per siswa yang disetujui hari ini
            val izinUnikHariIni = izinHariIni
                .filter { it.siswaId != null && it.statusVerifikasi?.equals("Disetujui", ignoreCase = true) == true }
                .distinctBy { it.siswaId }
            val i = izinUnikHariIni.size

            // 4. Hitung Alpa secara proporsional dari kapasitas rombel
            val a = (totalSiswaKelas - (h + t + i)).coerceAtLeast(0)

            // 5. Hitung Persentase Disiplin Kehadiran Kelas (hindari pembagian dengan nol)
            val totalHadirHariIni = h + t
            val pct = if (totalSiswaKelas > 0) {
                (totalHadirHariIni.toDouble() / totalSiswaKelas.toDouble()) * 100.0
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
                RekapHarianKelasModel(
                    tanggal = tgl,
                    tanggalFormatted = formattedDate,
                    countHadir = h,
                    countTerlambat = t,
                    countIzin = i,
                    countAlpa = a,
                    totalSiswa = totalSiswaKelas,
                    persentase = pct,
                    isLengkap = totalSiswaKelas > 0 && (h + t + i) >= (totalSiswaKelas * 0.90)
                )
            )
        }

        // Tampilkan ke Adapter
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
                Toast.makeText(requireContext(), "Tidak ada data rekap presensi untuk diekspor pada periode ini.", Toast.LENGTH_SHORT).show()
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

                // Otomatis buka preview PDF
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
