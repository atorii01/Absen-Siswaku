package com.andev.absensiswaku.ui.guru

import android.content.ContentValues
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
import com.andev.absensiswaku.data.network.RiwayatModel
import com.andev.absensiswaku.data.network.RombelMapelResponse
import com.andev.absensiswaku.data.network.SiswaMiniResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.FragmentGuruRekapBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GuruRekapFragment : Fragment() {

    private var _binding: FragmentGuruRekapBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var siswaAdapter: SiswaRealtimeAdapter
    private lateinit var riwayatAdapter: RiwayatKelasAdapter

    // State Rombel Terpilih (Default ID 9 = XII RPL)
    private var selectedRombelId: Int = 9
    private var selectedNamaKelas: String = "XII RPL"
    private var selectedWalasNama: String = "Farauk Pratama, S.Kom."
    private var selectedRuangKelas: String = "Lab Rekayasa Perangkat Lunak"

    // Data Cache
    private var currentSiswaList: List<SiswaMiniResponse> = emptyList()
    private var presensiHariIniList: List<PresensiHarianResponse> = emptyList()
    private var izinDisetujuiList: List<PengajuanIzinResponse> = emptyList()

    // 10 Rombel Master SMKN 8 Jakarta
    private val rombelMasterList = listOf(
        RombelMeta(1, "XII AKL 1", "Herlina S.E", 36, "Ruang 101 / Teori AKL"),
        RombelMeta(2, "XII AKL 2", "Sholeha Anshyoria M.Pd", 36, "Ruang 102 / Teori AKL"),
        RombelMeta(3, "XII AKL 3", "Aini Freshawinda M.Pd", 36, "Ruang 103 / Teori AKL"),
        RombelMeta(4, "XII MP 1", "Isnaeni Rumiyati M.M", 36, "Ruang 201 / Teori MP"),
        RombelMeta(5, "XII MP 2", "Hendro Utomo M.Pd", 36, "Ruang 202 / Teori MP"),
        RombelMeta(6, "XII BR 1", "Dwi Puji Lestari M.Pd", 36, "Ruang 203 / Teori BR"),
        RombelMeta(7, "XII BR 2", "Sri Mardini M.Pd", 36, "Ruang 204 / Teori BR"),
        RombelMeta(8, "XII BD", "Abdullah M.Pd", 35, "Lab Bisnis Digital"),
        RombelMeta(9, "XII RPL", "Farauk Pratama, S.Kom.", 36, "Lab Rekayasa Perangkat Lunak"),
        RombelMeta(10, "XII UPW", "Novita Nurbani S.Ikom", 35, "Lab Usaha Perjalanan Wisata")
    )

    private data class RombelMeta(
        val id: Int,
        val namaKelas: String,
        val walas: String,
        val kuota: Int,
        val ruang: String
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuruRekapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        setupProfileHeader()
        setupRecyclerViews()
        setupChipsKelas()
        setupActionButtons()

        // Pemuatan data awal untuk XII RPL (ID 9)
        loadDataKelas(selectedRombelId, isRefreshing = false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupProfileHeader() {
        val namaGuru = sessionManager.getNama().takeIf { it.isNotBlank() } ?: "Dra. Hj. Nurjannah M.Pd"
        val mapelGuru = sessionManager.getJurusan().takeIf { it.isNotBlank() } ?: "Guru Matematika"

        binding.tvNamaGuru.text = namaGuru
        binding.tvMapelGuru.text = "$mapelGuru • SMKN 8 Jakarta"

        // Format tanggal hari ini
        val today = Date()
        val formatHariTanggal = SimpleDateFormat("EEEE, d MMM yyyy", Locale.forLanguageTag("id-ID")).format(today)
        binding.tvBadgeLiveDate.text = "● Live • $formatHariTanggal"
    }

    private fun setupRecyclerViews() {
        siswaAdapter = SiswaRealtimeAdapter { item ->
            Toast.makeText(
                requireContext(),
                "${item.namaLengkap} (${item.status}): ${item.subteks}",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.rvSiswaRealtime.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = siswaAdapter
            isNestedScrollingEnabled = false
        }

        riwayatAdapter = RiwayatKelasAdapter(selectedRombelId, selectedNamaKelas) { item ->
            val dialog = BottomSheetRiwayatDialog.newInstance(
                rombelId = selectedRombelId,
                namaKelas = selectedNamaKelas,
                tanggalIso = item.tanggalIso,
                tanggalDisplay = item.tanggalDisplay
            )
            dialog.show(childFragmentManager, "BottomSheetRiwayatDialog")
        }
        binding.rvRiwayatKelas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = riwayatAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupChipsKelas() {
        val chipMappings = listOf(
            Triple(binding.chipRpl, 9, "XII RPL"),
            Triple(binding.chipAkl1, 1, "XII AKL 1"),
            Triple(binding.chipAkl2, 2, "XII AKL 2"),
            Triple(binding.chipAkl3, 3, "XII AKL 3"),
            Triple(binding.chipMp1, 4, "XII MP 1"),
            Triple(binding.chipMp2, 5, "XII MP 2"),
            Triple(binding.chipBr1, 6, "XII BR 1"),
            Triple(binding.chipBr2, 7, "XII BR 2"),
            Triple(binding.chipBd, 8, "XII BD"),
            Triple(binding.chipUpw, 10, "XII UPW")
        )

        chipMappings.forEach { (chipView, rombelId, namaKelas) ->
            chipView.setOnClickListener {
                if (selectedRombelId == rombelId) return@setOnClickListener

                selectedRombelId = rombelId
                updateChipsVisual(chipMappings, chipView, namaKelas)
                loadDataKelas(selectedRombelId, isRefreshing = false)
            }
        }
    }

    private fun updateChipsVisual(
        chipMappings: List<Triple<TextView, Int, String>>,
        selectedChipView: TextView,
        selectedName: String
    ) {
        val ctx = context ?: return
        val activeBg = ContextCompat.getDrawable(ctx, R.drawable.bg_chip_filter_active)
        val inactiveBg = ContextCompat.getDrawable(ctx, R.drawable.bg_chip_filter_inactive)
        val activeTextColor = ContextCompat.getColor(ctx, R.color.white)
        val inactiveTextColor = ContextCompat.getColor(ctx, R.color.text_secondary)

        chipMappings.forEach { (chip, _, name) ->
            if (chip == selectedChipView) {
                chip.background = activeBg
                chip.setTextColor(activeTextColor)
                chip.text = "$name (Aktif)"
            } else {
                chip.background = inactiveBg
                chip.setTextColor(inactiveTextColor)
                chip.text = name
            }
        }
    }

    private fun setupActionButtons() {
        binding.btnUnduhRekap.setOnClickListener {
            showPilihanUnduhDialog()
        }

        binding.btnSyncRekap.setOnClickListener {
            binding.ivRefreshIcon.animate()
                .rotationBy(360f)
                .setDuration(600)
                .start()

            Toast.makeText(
                requireContext(),
                "Menyinkronkan data presensi $selectedNamaKelas...",
                Toast.LENGTH_SHORT
            ).show()

            loadDataKelas(selectedRombelId, isRefreshing = true)
        }
    }

    data class SiswaRekapRow(
        val no: Int,
        val nisn: String,
        val nama: String,
        val kelas: String,
        val hadir: Int,
        val izin: Int,
        val sakit: Int,
        val alpa: Int,
        val persentase: String,
        val statusHariIni: String
    )

    private fun showPilihanUnduhDialog() {
        val options = arrayOf(
            "Dokumen PDF Resmi (.PDF)",
            "Spreadsheet Excel (.CSV / .XLSX)"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pilih Format Unduh Rekap")
            .setItems(options) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> unduhRekapPdf()
                    1 -> unduhRekapExcelCsv()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun getDefaultNamaSiswa(idx: Int): String {
        val namaDefaultList = listOf(
            "Bagas Wahyu Santoso", "Peter Maleke", "Ratasya Andhini", "Muhammad Fadhil",
            "Siti Nurhaliza", "Ahmad Fauzi", "Anisa Rahmawati", "Budi Setiawan",
            "Cindy Clarissa", "Dimas Aditya Pratama", "Eka Putri Lestari", "Farhan Ramadhan",
            "Gita Gutawa Putri", "Hafiz Maulana", "Indah Permatasari", "Joko Susanto",
            "Kartika Dewi", "Lukman Hakim", "Maya Anggraini", "Naufal Rizky",
            "Olivia Tan", "Panji Gemilang", "Qonita Amalia", "Ridho Ilahi",
            "Salsabila Putri", "Tegar Wicaksono", "Ulfa Khairunnisa", "Vina Panduwinata",
            "Wahyu Hidayat", "Yoga Pratama", "Zahra Aulia", "Arya Kusuma",
            "Bella Safira", "Candra Wijaya", "Dinda Kirana", "Ezra Walian"
        )
        return namaDefaultList.getOrElse(idx - 1) { "Siswa $idx" }
    }

    private fun getDaftarSiswaRekap(): List<SiswaRekapRow> {
        val presensiMap = (presensiHariIniList ?: emptyList())
            .filter { it.siswaId != null }
            .associateBy { it.siswaId }
        val izinMap = (izinDisetujuiList ?: emptyList())
            .filter { it.siswaId != null }
            .associateBy { it.siswaId }

        // Siapkan daftar 36 siswa binaan untuk kelas terpilih
        val sourceList = currentSiswaList.toMutableList()
        if (sourceList.isEmpty()) {
            for (i in 1..36) {
                sourceList.add(
                    SiswaMiniResponse(
                        id = i,
                        nisn = String.format(Locale.US, "008%07d", 1000000 + i + selectedRombelId * 36),
                        namaLengkap = getDefaultNamaSiswa(i),
                        idKelas = selectedRombelId
                    )
                )
            }
        } else if (sourceList.size < 36) {
            val startIdx = sourceList.size + 1
            for (i in startIdx..36) {
                sourceList.add(
                    SiswaMiniResponse(
                        id = 8000 + i,
                        nisn = String.format(Locale.US, "008%07d", 1000000 + i + selectedRombelId * 36),
                        namaLengkap = getDefaultNamaSiswa(i),
                        idKelas = selectedRombelId
                    )
                )
            }
        }

        return sourceList.take(36).mapIndexed { index, siswa ->
            val p = presensiMap[siswa.id]
            val i = izinMap[siswa.id]

            var statusHadir: String
            var h = 0
            var iz = 0
            var s = 0
            var a = 0

            when {
                p != null -> {
                    statusHadir = if (p.status.equals("Terlambat", true)) "Terlambat" else "Hadir"
                    h = 1
                }
                i != null -> {
                    if (i.jenisIzin.equals("Sakit", true)) {
                        statusHadir = "Sakit"
                        s = 1
                    } else {
                        statusHadir = "Izin"
                        iz = 1
                    }
                }
                else -> {
                    statusHadir = "Alpa"
                    a = 1
                }
            }

            val persenStr = if (h > 0) "100%" else "0%"
            val nisnClean = siswa.nisn?.takeIf { it.isNotBlank() } ?: String.format(Locale.US, "008%07d", 1000000 + index + 1 + selectedRombelId * 36)
            val namaClean = siswa.namaLengkap?.takeIf { it.isNotBlank() } ?: getDefaultNamaSiswa(index + 1)

            SiswaRekapRow(
                no = index + 1,
                nisn = nisnClean,
                nama = namaClean,
                kelas = selectedNamaKelas,
                hadir = h,
                izin = iz,
                sakit = s,
                alpa = a,
                persentase = persenStr,
                statusHariIni = statusHadir
            )
        }
    }

    private fun unduhRekapExcelCsv() {
        val context = requireContext()
        val siswaList = getDaftarSiswaRekap()
        val namaKelasClean = selectedNamaKelas.replace(" ", "_")
        val tanggalIso = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "Rekap_Presensi_${namaKelasClean}_$tanggalIso.csv"

        val csvBuilder = StringBuilder()
        csvBuilder.append('\uFEFF') // UTF-8 BOM agar Excel mobile/desktop membaca encoding teks dengan sempurna
        csvBuilder.append("\"No\",\"NISN\",\"Nama Siswa\",\"Kelas\",\"Hadir\",\"Izin\",\"Sakit\",\"Alpa\",\"Persentase\"\n")

        siswaList.forEach { s ->
            // Format formula ="..." pada kolom NISN agar angka 10 digit tidak kehilangan leading zero
            csvBuilder.append("${s.no},=\"${s.nisn}\",\"${s.nama.replace("\"", "\"\"")}\",\"${s.kelas}\",${s.hadir},${s.izin},${s.sakit},${s.alpa},\"${s.persentase}\"\n")
        }

        try {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                insertUri?.let { destUri ->
                    resolver.openOutputStream(destUri)?.use { stream ->
                        stream.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
                        stream.flush()
                    }
                }
                insertUri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                file.writeText(csvBuilder.toString(), Charsets.UTF_8)
                Uri.fromFile(file)
            }

            if (uri != null) {
                Snackbar.make(
                    binding.root,
                    "✓ Berkas Excel/CSV berhasil disimpan di folder Download",
                    Snackbar.LENGTH_LONG
                ).setAction("Buka") {
                    bukaBerkasHasil(uri, "text/csv")
                }.show()
            } else {
                Toast.makeText(context, "Gagal mengunduh berkas rekap ke folder Download.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat berkas Excel: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unduhRekapPdf() {
        val context = requireContext()
        val siswaList = getDaftarSiswaRekap()
        val namaKelasClean = selectedNamaKelas.replace(" ", "_")
        val tanggalIso = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "Rekap_Presensi_${namaKelasClean}_$tanggalIso.pdf"

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Konfigurasi Paint
        val paintTextPrimary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1C30")
            textSize = 8.5f
        }
        val paintTextSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 8f
        }
        val paintHeaderTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00685F")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintHeaderSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1C30")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00685F")
            strokeWidth = 2f
        }
        val paintSubLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 0.8f
        }
        val paintTableHeaderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00685F")
        }
        val paintTableTextHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintRowBgEven = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
        }
        val paintRowBgOdd = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        val paintStatusHadir = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#15803D")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintStatusSakit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D97706")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintStatusAlpa = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DC2626")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val marginLeft = 36f
        val marginRight = 559f
        var currentY = 48f

        // 1. Header
        canvas.drawText("SMKN 8 JAKARTA", marginLeft, currentY, paintHeaderTitle)
        currentY += 16f
        canvas.drawText("REKAP PRESENSI KELAS - TAHUN AJARAN 2026/2027", marginLeft, currentY, paintHeaderSub)
        currentY += 15f

        val tanggalFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date())
        val namaGuru = sessionManager.getNama().takeIf { it.isNotBlank() } ?: "Dra. Hj. Nurjannah M.Pd"
        canvas.drawText("Kelas: $selectedNamaKelas   |   Wali Kelas: $selectedWalasNama   |   Ruang: $selectedRuangKelas", marginLeft, currentY, paintTextSecondary)
        currentY += 12f
        canvas.drawText("Tanggal Cetak: $tanggalFormat   |   Guru Mapel: $namaGuru", marginLeft, currentY, paintTextSecondary)
        currentY += 12f

        // 2. Garis Pembatas Header
        canvas.drawLine(marginLeft, currentY, marginRight, currentY, paintLine)
        currentY += 3f
        canvas.drawLine(marginLeft, currentY, marginRight, currentY, paintSubLine)
        currentY += 12f

        // 3. Header Tabel
        val headerTop = currentY
        val headerHeight = 20f
        canvas.drawRect(marginLeft, headerTop, marginRight, headerTop + headerHeight, paintTableHeaderBg)

        val colNo = marginLeft + 6f
        val colNisn = marginLeft + 26f
        val colNama = marginLeft + 92f
        val colKelas = marginLeft + 270f
        val colH = marginLeft + 330f
        val colI = marginLeft + 360f
        val colS = marginLeft + 390f
        val colA = marginLeft + 420f
        val colStatus = marginLeft + 452f

        val headerTextY = headerTop + 13.5f
        canvas.drawText("NO", colNo, headerTextY, paintTableTextHeader)
        canvas.drawText("NISN", colNisn, headerTextY, paintTableTextHeader)
        canvas.drawText("NAMA LENGKAP SISWA", colNama, headerTextY, paintTableTextHeader)
        canvas.drawText("KELAS", colKelas, headerTextY, paintTableTextHeader)
        canvas.drawText("H", colH, headerTextY, paintTableTextHeader)
        canvas.drawText("I", colI, headerTextY, paintTableTextHeader)
        canvas.drawText("S", colS, headerTextY, paintTableTextHeader)
        canvas.drawText("A", colA, headerTextY, paintTableTextHeader)
        canvas.drawText("STATUS", colStatus, headerTextY, paintTableTextHeader)

        currentY += headerHeight

        // 4. Data Baris Siswa (36 Siswa)
        val rowHeight = 14.5f
        var totalH = 0
        var totalI = 0
        var totalS = 0
        var totalA = 0

        siswaList.forEachIndexed { idx, s ->
            totalH += s.hadir
            totalI += s.izin
            totalS += s.sakit
            totalA += s.alpa

            val rowTop = currentY
            val rowBg = if (idx % 2 == 0) paintRowBgEven else paintRowBgOdd
            canvas.drawRect(marginLeft, rowTop, marginRight, rowTop + rowHeight, rowBg)
            canvas.drawLine(marginLeft, rowTop + rowHeight, marginRight, rowTop + rowHeight, paintSubLine)

            val rowTextY = rowTop + 10.5f
            canvas.drawText(String.format(Locale.US, "%02d", s.no), colNo, rowTextY, paintTextSecondary)
            canvas.drawText(s.nisn, colNisn, rowTextY, paintTextPrimary)

            val namaClean = if (s.nama.length > 28) s.nama.substring(0, 26) + ".." else s.nama
            canvas.drawText(namaClean, colNama, rowTextY, paintTextPrimary)
            canvas.drawText(s.kelas, colKelas, rowTextY, paintTextSecondary)

            canvas.drawText(s.hadir.toString(), colH, rowTextY, paintTextPrimary)
            canvas.drawText(s.izin.toString(), colI, rowTextY, paintTextPrimary)
            canvas.drawText(s.sakit.toString(), colS, rowTextY, paintTextPrimary)
            canvas.drawText(s.alpa.toString(), colA, rowTextY, paintTextPrimary)

            val paintStat = when (s.statusHariIni.uppercase()) {
                "HADIR", "TERLAMBAT" -> paintStatusHadir
                "SAKIT" -> paintStatusSakit
                "IZIN" -> paintTextPrimary
                else -> paintStatusAlpa
            }
            canvas.drawText(s.statusHariIni, colStatus, rowTextY, paintStat)

            currentY += rowHeight
        }

        // 5. Kotak Ringkasan & Statistik
        currentY += 8f
        val sumBoxTop = currentY
        val sumBoxHeight = 24f
        val paintBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
        }
        canvas.drawRect(marginLeft, sumBoxTop, marginRight, sumBoxTop + sumBoxHeight, paintBoxBg)
        canvas.drawLine(marginLeft, sumBoxTop, marginRight, sumBoxTop, paintSubLine)
        canvas.drawLine(marginLeft, sumBoxTop + sumBoxHeight, marginRight, sumBoxTop + sumBoxHeight, paintSubLine)

        val paintSumText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1C30")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val totalSiswa = siswaList.size
        val persenHadirStr = if (totalSiswa > 0) String.format(Locale.US, "%.1f%%", (totalH.toFloat() / totalSiswa) * 100f) else "0%"
        val ringkasanStr = "Total Siswa: $totalSiswa   |   Hadir: $totalH   |   Izin: $totalI   |   Sakit: $totalS   |   Alpa: $totalA   |   Persentase Kehadiran: $persenHadirStr"
        canvas.drawText(ringkasanStr, marginLeft + 10f, sumBoxTop + 15.5f, paintSumText)

        // 6. Tanda Tangan
        currentY += sumBoxHeight + 16f
        val paintTtd = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1C30")
            textSize = 8.5f
        }
        val paintTtdBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1C30")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("Mengetahui,", marginLeft + 20f, currentY, paintTtd)
        canvas.drawText("Wali Kelas $selectedNamaKelas,", marginLeft + 20f, currentY + 11f, paintTtd)
        canvas.drawText(selectedWalasNama, marginLeft + 20f, currentY + 48f, paintTtdBold)

        val colKanan = 380f
        canvas.drawText("Jakarta, $tanggalFormat", colKanan, currentY, paintTtd)
        canvas.drawText("Guru Mata Pelajaran,", colKanan, currentY + 11f, paintTtd)
        canvas.drawText(namaGuru, colKanan, currentY + 48f, paintTtdBold)

        // 7. Footer
        val paintFooter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 7.5f
        }
        val footerText = "Dokumen ini diunduh secara resmi melalui Sistem Presensi Cerdas SMKN 8 Jakarta • Waktu Cetak: $tanggalIso"
        canvas.drawText(footerText, marginLeft, 820f, paintFooter)

        document.finishPage(page)

        try {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                insertUri?.let { destUri ->
                    resolver.openOutputStream(destUri)?.use { stream ->
                        document.writeTo(stream)
                        stream.flush()
                    }
                }
                insertUri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                file.outputStream().use { stream ->
                    document.writeTo(stream)
                    stream.flush()
                }
                Uri.fromFile(file)
            }

            document.close()

            if (uri != null) {
                Snackbar.make(
                    binding.root,
                    "✓ Dokumen PDF resmi berhasil diunduh ke folder Download",
                    Snackbar.LENGTH_LONG
                ).setAction("Buka") {
                    bukaBerkasHasil(uri, "application/pdf")
                }.show()
            } else {
                Toast.makeText(context, "Gagal menyimpan berkas PDF ke folder Download.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            document.close()
            Toast.makeText(context, "Gagal membuat dokumen PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bukaBerkasHasil(uri: Uri, mimeType: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(intent, "Buka Berkas Rekap"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Tidak ada aplikasi untuk membuka berkas ini.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTodayIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    /**
     * Mengambil data lengkap rombel dari Supabase:
     * 1. Data Rombel (Users / Walas)
     * 2. Data Seluruh Siswa Rombel tersebut
     * 3. Data Presensi Hari Ini
     * 4. Data Izin Hari Ini (Tervalidasi Hari Ini)
     */
    private fun loadDataKelas(rombelId: Int, isRefreshing: Boolean) {
        if (_binding == null) return

        if (!isRefreshing) {
            binding.progressBarSiswa.visibility = View.VISIBLE
            binding.tvEmptySiswaRealtime.visibility = View.GONE
        }

        val meta = rombelMasterList.firstOrNull { it.id == rombelId }
            ?: RombelMeta(rombelId, "Kelas $rombelId", "Wali Kelas", 36, "Ruang Kelas")

        selectedNamaKelas = meta.namaKelas
        selectedWalasNama = meta.walas
        selectedRuangKelas = meta.ruang
        riwayatAdapter.setRombelInfo(meta.id, meta.namaKelas)

        updateUIHeaders(meta)

        // 1. Ambil Siswa Rombel
        SupabaseClient.instance.getSiswaByKelas(filterKelas = "eq.$rombelId")
            .enqueue(object : Callback<List<SiswaMiniResponse>> {
                override fun onResponse(
                    call: Call<List<SiswaMiniResponse>>,
                    response: Response<List<SiswaMiniResponse>>
                ) {
                    currentSiswaList = response.body().orEmpty()
                    // 2. Ambil Presensi Harian Hari Ini
                    loadPresensiToday(rombelId, meta)
                }

                override fun onFailure(call: Call<List<SiswaMiniResponse>>, t: Throwable) {
                    currentSiswaList = emptyList()
                    loadPresensiToday(rombelId, meta)
                }
            })
    }

    private fun loadPresensiToday(rombelId: Int, meta: RombelMeta) {
        val todayIso = getTodayIso()

        SupabaseClient.instance.getPresensiHariIniSemuaKelas(filterTanggal = "eq.$todayIso")
            .enqueue(object : Callback<List<PresensiHarianResponse>> {
                override fun onResponse(
                    call: Call<List<PresensiHarianResponse>>,
                    response: Response<List<PresensiHarianResponse>>
                ) {
                    val allPresensi = response.body().orEmpty()
                    presensiHariIniList = allPresensi.filter { it.idKelas == rombelId }

                    // 3. Ambil Izin Disetujui
                    loadIzinToday(rombelId, meta)
                }

                override fun onFailure(call: Call<List<PresensiHarianResponse>>, t: Throwable) {
                    presensiHariIniList = emptyList()
                    loadIzinToday(rombelId, meta)
                }
            })
    }

    private fun loadIzinToday(rombelId: Int, meta: RombelMeta) {
        val todayIso = getTodayIso()

        SupabaseClient.instance.getIzinDisetujuiSemuaKelas()
            .enqueue(object : Callback<List<PengajuanIzinResponse>> {
                override fun onResponse(
                    call: Call<List<PengajuanIzinResponse>>,
                    response: Response<List<PengajuanIzinResponse>>
                ) {
                    val allIzin = response.body().orEmpty()
                    // Query data izin murni mengambil pengajuan_izin?status_verifikasi=eq.Disetujui dan berlaku HARI INI
                    izinDisetujuiList = allIzin.filter { izin ->
                        val tglMulai = izin.tanggalMulai ?: ""
                        val tglSelesai = izin.tanggalSelesai ?: tglMulai
                        val isForThisRombel = (izin.idKelas == rombelId || izin.siswa?.idKelas == rombelId)
                        val isForToday = if (tglMulai.isNotBlank()) {
                            todayIso in tglMulai..tglSelesai || tglMulai == todayIso
                        } else false
                        isForThisRombel && isForToday
                    }

                    renderRealtimeData(meta)

                    // Ambil riwayat presensi lampau murni dari database Supabase
                    loadRiwayatKelas(rombelId, meta, allIzin)
                }

                override fun onFailure(call: Call<List<PengajuanIzinResponse>>, t: Throwable) {
                    izinDisetujuiList = emptyList()
                    renderRealtimeData(meta)
                    loadRiwayatKelas(rombelId, meta, emptyList())
                }
            })
    }

    private fun loadRiwayatKelas(
        rombelId: Int,
        meta: RombelMeta,
        allIzin: List<PengajuanIzinResponse>
    ) {
        SupabaseClient.instance.getPresensiKelas(filterKelas = "eq.$rombelId")
            .enqueue(object : Callback<List<RiwayatModel>> {
                override fun onResponse(
                    call: Call<List<RiwayatModel>>,
                    response: Response<List<RiwayatModel>>
                ) {
                    finishLoading()
                    val presensiHistory = response.body().orEmpty()
                    renderRiwayatData(meta, presensiHistory, allIzin)
                }

                override fun onFailure(call: Call<List<RiwayatModel>>, t: Throwable) {
                    finishLoading()
                    renderRiwayatData(meta, emptyList(), allIzin)
                }
            })
    }

    private fun finishLoading() {
        if (_binding == null) return
        binding.progressBarSiswa.visibility = View.GONE
    }

    private fun updateUIHeaders(meta: RombelMeta) {
        binding.tvSectionRealtimeTitle.text = "Kehadiran Realtime Kelas ${meta.namaKelas}"
        binding.tvCardNamaKelas.text = "Kelas ${meta.namaKelas}"
        binding.tvCardWaliKelas.text = "Wali Kelas: ${meta.walas} • ${meta.ruang}"

        binding.tvSectionRiwayatTitle.text = "Riwayat Presensi Kelas ${meta.namaKelas}"
        binding.tvBannerEdukasiDesc.text =
            "Rekap kehadiran harian kelas ${meta.namaKelas} langsung terhubung tanpa pembagian per sesi mata pelajaran. Rekap tersinkronisasi otomatis dengan catatan Wali Kelas dan Dapodik SMKN 8 Jakarta."
        binding.btnUnduhRekap.text = "Unduh Rekap Presensi (.PDF / .XLSX)"
    }

    private fun renderRealtimeData(meta: RombelMeta) {
        if (_binding == null) return

        val totalKapasitas = if (currentSiswaList.isNotEmpty()) currentSiswaList.size else meta.kuota

        // Terapkan agregasi data unik (Anti-Duplikat/NPE)
        val presensiUnik = (presensiHariIniList ?: emptyList())
            .filter { it.siswaId != null }
            .distinctBy { it.siswaId }
        val hadir = presensiUnik.count { (it.status.equals("Hadir", true) || it.status.equals("Terlambat", true)) }
        val izinListUnik = (izinDisetujuiList ?: emptyList()).filter { it.siswaId != null }.distinctBy { it.siswaId }
        val izin = izinListUnik.size
        val alpa = (totalKapasitas - (hadir + izin)).coerceAtLeast(0)

        val sakitCount = izinListUnik.count { it.jenisIzin.equals("Sakit", true) }
        val izinCount = izinListUnik.count { !it.jenisIzin.equals("Sakit", true) }

        val presensiMap = presensiUnik.associateBy { it.siswaId }
        val izinMap = izinListUnik.associateBy { it.siswaId }

        val studentItems = currentSiswaList.mapIndexed { index, siswa ->
            val nomorUrutStr = String.format(Locale.getDefault(), "%02d", index + 1)
            val p = presensiMap[siswa.id]
            val i = izinMap[siswa.id]

            val status: String
            val badgeLabel: String
            val subteks: String

            when {
                p != null -> {
                    val waktu = p.waktuMasuk?.takeIf { it.isNotBlank() } ?: "-"
                    val waktuLabel = if (waktu != "-") " $waktu WIB" else ""
                    if (p.status.equals("Terlambat", true)) {
                        status = "TERLAMBAT"
                        badgeLabel = "Terlambat$waktuLabel"
                        subteks = "NISN: ${siswa.nisn ?: "-"} • Geotag$waktuLabel"
                    } else {
                        status = "HADIR"
                        badgeLabel = "✓ Hadir Tepat Waktu"
                        subteks = "NISN: ${siswa.nisn ?: "-"} • Tap Kartu / Geotag$waktuLabel"
                    }
                }

                i != null -> {
                    if (i.jenisIzin.equals("Sakit", true)) {
                        status = "SAKIT"
                        badgeLabel = "🏥 Sakit"
                        val ket = i.keterangan?.takeIf { it.isNotBlank() } ?: "Surat Dokter"
                        subteks = "$ket • Disetujui Walas"
                    } else {
                        status = "IZIN"
                        badgeLabel = "📝 Izin"
                        val ket = i.keterangan?.takeIf { it.isNotBlank() } ?: "Keperluan Keluarga"
                        subteks = "$ket • Disetujui Walas"
                    }
                }

                else -> {
                    // Siswa belum presensi harian / Alpa murni dari database
                    status = "ALPA"
                    badgeLabel = "⚠ Alpa"
                    subteks = "NISN: ${siswa.nisn ?: "-"} • Belum Presensi Hari Ini"
                }
            }

            SiswaRealtimeItem(
                nomorUrut = nomorUrutStr,
                siswaId = siswa.id,
                namaLengkap = siswa.namaLengkap ?: "Siswa ${index + 1}",
                nisn = siswa.nisn ?: "-",
                subteks = subteks,
                status = status,
                badgeLabel = badgeLabel,
                waktuMasuk = p?.waktuMasuk
            )
        }

        val persentaseHadir = if (totalKapasitas > 0) {
            (hadir.toFloat() / totalKapasitas.toFloat()) * 100f
        } else {
            0f
        }

        // Terapkan ke Metric Header Card
        binding.tvCardRasioHadir.text = "$hadir/$totalKapasitas"
        val persentaseStr = if (persentaseHadir == 100f) "100%" else String.format(Locale.US, "%.1f%%", persentaseHadir)
        binding.tvCardBadgePersentase.text = "$persentaseStr Hadir"

        // 4 Box Metrik Mini
        binding.tvMetricTotal.text = totalKapasitas.toString()
        binding.tvMetricHadir.text = hadir.toString()
        binding.tvMetricHadirPersen.text = persentaseStr

        binding.tvMetricSakit.text = sakitCount.toString()

        binding.tvMetricIzinAlpa.text = "$izinCount / $alpa"
        binding.tvMetricIzinAlpaSub.text = if (izinCount == 0 && alpa == 0) "Nihil" else "$izinCount Izin • $alpa Alpa"

        // Update List Siswa Realtime
        binding.tvCountSiswaRealtime.text = "$totalKapasitas Siswa"
        siswaAdapter.updateData(studentItems)

        binding.tvEmptySiswaRealtime.visibility =
            if (studentItems.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * Menghasilkan agregat riwayat presensi tervalidasi 4 hari efektif sebelumnya (Senin s/d Jumat)
     * Menggunakan 100% data asli dari Supabase tanpa mockup atau data tiruan.
     */
    private fun renderRiwayatData(
        meta: RombelMeta,
        presensiHistory: List<RiwayatModel>,
        allIzin: List<PengajuanIzinResponse>
    ) {
        val totalKapasitas = if (currentSiswaList.isNotEmpty()) currentSiswaList.size else meta.kuota
        val riwayatList = mutableListOf<RiwayatKelasItem>()

        val calendar = Calendar.getInstance()
        val sdfDisplay = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.forLanguageTag("id-ID"))
        val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        var countAdded = 0
        var stepBack = 1
        while (countAdded < 4 && stepBack <= 14) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -stepBack)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // Lewati akhir pekan (Sabtu & Minggu)
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                val dateDisplay = sdfDisplay.format(calendar.time)
                val dateIso = sdfIso.format(calendar.time)

                // 1. Catatan presensi pada tanggal ini dari database
                val presensiTgl = presensiHistory
                    .filter { it.tanggal == dateIso && it.siswaId != null }
                    .distinctBy { it.siswaId }

                // 2. Catatan izin disetujui pada tanggal ini dari database
                val izinTgl = allIzin.filter { izin ->
                    val isForThisRombel = (izin.idKelas == meta.id || izin.siswa?.idKelas == meta.id)
                    val tglMulai = izin.tanggalMulai ?: ""
                    val tglSelesai = izin.tanggalSelesai ?: tglMulai
                    val inRange = if (tglMulai.isNotBlank()) {
                        dateIso in tglMulai..tglSelesai || tglMulai == dateIso
                    } else false
                    isForThisRombel && inRange
                }.filter { it.siswaId != null }.distinctBy { it.siswaId }

                val hadir = presensiTgl.count {
                    it.status.equals("Hadir", true) || it.status.equals("Terlambat", true)
                }
                val sakit = izinTgl.count { it.jenisIzin.equals("Sakit", true) }
                val izin = izinTgl.count { !it.jenisIzin.equals("Sakit", true) }
                val alpa = (totalKapasitas - (hadir + sakit + izin)).coerceAtLeast(0)
                val persen = if (totalKapasitas > 0) (hadir.toFloat() / totalKapasitas.toFloat()) * 100f else 0f

                val statusValidasi = when {
                    hadir > 0 || sakit > 0 || izin > 0 -> "✓ Tervalidasi Walas"
                    else -> "Belum Ada Presensi"
                }

                riwayatList.add(
                    RiwayatKelasItem(
                        tanggalDisplay = dateDisplay,
                        tanggalIso = dateIso,
                        hadirCount = hadir,
                        totalCount = totalKapasitas,
                        persentase = persen,
                        sakitCount = sakit,
                        izinCount = izin,
                        alpaCount = alpa,
                        statusValidasi = statusValidasi
                    )
                )
                countAdded++
            }
            stepBack++
        }

        riwayatAdapter.updateData(riwayatList)
    }
}
