package com.andev.absensiswaku.ui.riwayat

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.ApiClient
import com.andev.absensiswaku.data.network.PengajuanIzinModel
import com.andev.absensiswaku.data.network.RiwayatModel
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.FragmentRiwayatBinding
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RiwayatFragment : Fragment() {

    private var _binding: FragmentRiwayatBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var riwayatAdapter: RiwayatAdapter

    private var allRiwayatList: List<RiwayatModel> = emptyList()

    private var month1Pattern = ""
    private var month2Pattern = ""
    private var month3Pattern = ""

    private var month1Name = ""
    private var month2Name = ""
    private var month3Name = ""

    private var selectedMonthIndex = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = context ?: return
        sessionManager = SessionManager(ctx)

        setupHeaderData()
        setupDynamicMonthChips()
        setupRecyclerView()
        setupExportPdfButton()
    }

    override fun onResume() {
        super.onResume()
        loadSemuaRiwayat()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            loadSemuaRiwayat()
        }
    }

    private fun setupHeaderData() {
        if (!isAdded || _binding == null) return

        val nisn = sessionManager.getNisn().ifEmpty { "0068192341" }
        binding.tvTopBarNisn.text = "NISN: $nisn"
    }

    private fun setupDynamicMonthChips() {
        if (!isAdded || _binding == null) return

        try {
            val sdfMonthFull = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
            val sdfPattern = SimpleDateFormat("yyyy-MM", Locale.getDefault())

            val cal = Calendar.getInstance()

            // Month 1: Bulan Ini
            month1Name = sdfMonthFull.format(cal.time)
            month1Pattern = sdfPattern.format(cal.time)
            binding.tvChipMonth1.text = "$month1Name (Bulan Ini)"

            // Month 2: Bulan Lalu
            cal.add(Calendar.MONTH, -1)
            month2Name = sdfMonthFull.format(cal.time)
            month2Pattern = sdfPattern.format(cal.time)
            binding.tvChipMonth2.text = month2Name

            // Month 3: 2 Bulan Lalu
            cal.add(Calendar.MONTH, -1)
            month3Name = sdfMonthFull.format(cal.time)
            month3Pattern = sdfPattern.format(cal.time)
            binding.tvChipMonth3.text = month3Name
        } catch (e: Exception) {
            month1Name = "Bulan Ini"
            month2Name = "Bulan Lalu"
            month3Name = "2 Bulan Lalu"
            binding.tvChipMonth1.text = month1Name
            binding.tvChipMonth2.text = month2Name
            binding.tvChipMonth3.text = month3Name
        }

        binding.chipMonth1.setOnClickListener { switchMonthSelection(1) }
        binding.chipMonth2.setOnClickListener { switchMonthSelection(2) }
        binding.chipMonth3.setOnClickListener { switchMonthSelection(3) }
    }

    private fun switchMonthSelection(monthIndex: Int) {
        if (!isAdded || _binding == null) return

        selectedMonthIndex = monthIndex

        setChipState(binding.chipMonth1, binding.tvChipMonth1, binding.icChipMonth1, monthIndex == 1)
        setChipState(binding.chipMonth2, binding.tvChipMonth2, binding.icChipMonth2, monthIndex == 2)
        setChipState(binding.chipMonth3, binding.tvChipMonth3, binding.icChipMonth3, monthIndex == 3)

        applyMonthFilter(monthIndex)
    }

    private fun setChipState(
        chipLayout: View,
        textView: TextView,
        iconView: ImageView?,
        isActive: Boolean
    ) {
        val ctx = context ?: return
        val whiteColor = ContextCompat.getColor(ctx, R.color.white)
        val secondaryTextColor = ContextCompat.getColor(ctx, R.color.text_secondary)

        if (isActive) {
            chipLayout.setBackgroundResource(R.drawable.bg_chip_month_active)
            textView.setTextColor(whiteColor)
            textView.setTypeface(null, Typeface.BOLD)
            if (iconView != null) {
                iconView.visibility = View.VISIBLE
                iconView.setColorFilter(whiteColor)
            }
        } else {
            chipLayout.setBackgroundResource(R.drawable.bg_chip_month_inactive)
            textView.setTextColor(secondaryTextColor)
            textView.setTypeface(null, Typeface.NORMAL)
            if (iconView != null) {
                iconView.visibility = View.GONE
                iconView.setColorFilter(secondaryTextColor)
            }
        }
    }

    private fun setupRecyclerView() {
        val walasName = sessionManager.getWaliKelas().ifEmpty { "Farauk Pratama S.Kom" }

        riwayatAdapter = RiwayatAdapter(
            listRiwayat = emptyList(),
            defaultWalas = walasName,
            onItemClick = { item ->
                Toast.makeText(requireContext(), "Presensi tanggal ${item.tanggal} (${item.status})", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvRiwayatPresensi.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = riwayatAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun loadSemuaRiwayat() {
        if (!isAdded || _binding == null) return

        val pref = context?.getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)

        val siswaIdInt = try {
            pref?.getInt("user_id", 0)?.takeIf { it != 0 }
                ?: pref?.getInt("ID_SISWA", 0)?.takeIf { it != 0 }
                ?: pref?.getString("user_id", "0")?.toIntOrNull()
                ?: pref?.getString("ID_SISWA", "0")?.toIntOrNull()
                ?: sessionManager.getUserId().takeIf { it != 0 }
                ?: 1
        } catch (e: Exception) { 1 }

        val walasName = sessionManager.getWaliKelas().ifEmpty { "Farauk Pratama S.Kom" }

        var listPresensi = emptyList<RiwayatModel>()

        // 1. Fetch Presensi Harian dari Supabase
        SupabaseClient.instance.getRiwayatPresensiSupabase(siswaIdFilter = "eq.$siswaIdInt")
            .enqueue(object : Callback<List<RiwayatModel>> {
                override fun onResponse(
                    call: Call<List<RiwayatModel>>,
                    response: Response<List<RiwayatModel>>
                ) {
                    if (!isAdded || _binding == null) return
                    if (response.isSuccessful && response.body() != null) {
                        listPresensi = response.body()!!
                    }
                    // 2. Fetch Pengajuan Izin dan Gabungkan
                    fetchIzinAndCombine(siswaIdInt, walasName, listPresensi)
                }

                override fun onFailure(call: Call<List<RiwayatModel>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    fetchIzinAndCombine(siswaIdInt, walasName, listPresensi)
                }
            })
    }

    /**
     * Konversi timestamp UTC ISO 8601 dari Supabase ke format jam WIB dengan parser fallback
     */
    private fun formatIsoKeWib(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return "07:00 WIB"
        return try {
            val cleanIso = isoString.substringBefore(".").substringBefore("+").trim()
            val parser = if (cleanIso.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            } else if (cleanIso.contains(" ")) {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            }
            val formatter = SimpleDateFormat("HH:mm:ss 'WIB'", Locale.forLanguageTag("id-ID")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }
            val date = parser.parse(cleanIso) ?: Date()
            formatter.format(date)
        } catch (e: Exception) {
            "07:00 WIB"
        }
    }

    private fun fetchIzinAndCombine(siswaIdInt: Int, walasName: String, listPresensi: List<RiwayatModel>) {
        SupabaseClient.instance.getRiwayatIzinSupabase(siswaIdFilter = "eq.$siswaIdInt")
            .enqueue(object : Callback<List<PengajuanIzinModel>> {
                override fun onResponse(
                    call: Call<List<PengajuanIzinModel>>,
                    response: Response<List<PengajuanIzinModel>>
                ) {
                    if (!isAdded || _binding == null) return

                    val mappedIzinList = if (response.isSuccessful && response.body() != null) {
                        response.body()!!.map { item ->
                            val statusVerif = item.statusVerifikasi?.trim().orEmpty()
                            val jenis = item.jenisIzin?.trim().orEmpty().ifEmpty { "Izin" }

                            val statusMapped = when (statusVerif.lowercase()) {
                                "pending" -> "PENDING"
                                "disetujui" -> jenis.uppercase()
                                "ditolak" -> "DITOLAK"
                                else -> statusVerif.uppercase().ifEmpty { "PENDING" }
                            }

                            RiwayatModel(
                                id = item.id,
                                siswaId = item.siswaId,
                                tanggal = item.tanggalMulai,
                                waktuMasuk = formatIsoKeWib(item.createdAt),
                                jamMasukField = jenis,
                                status = statusMapped,
                                verifikator = item.verifiedBy?.takeIf { it.isNotEmpty() } ?: walasName,
                                keteranganStatus = item.keterangan,
                                statusVerifikasi = statusVerif,
                                jenisIzin = jenis
                            )
                        }
                    } else {
                        emptyList()
                    }

                    combineAndApplyLists(listPresensi, mappedIzinList)
                }

                override fun onFailure(call: Call<List<PengajuanIzinModel>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    combineAndApplyLists(listPresensi, emptyList())
                }
            })
    }

    private fun combineAndApplyLists(presensiList: List<RiwayatModel>, izinList: List<RiwayatModel>) {
        val combined = (presensiList + izinList).sortedByDescending { it.tanggal }
        allRiwayatList = combined
        applyMonthFilter(selectedMonthIndex)
    }

    private fun getCurrentFilteredList(): List<RiwayatModel> {
        val pattern = when (selectedMonthIndex) {
            1 -> month1Pattern
            2 -> month2Pattern
            else -> month3Pattern
        }
        val monthNameText = when (selectedMonthIndex) {
            1 -> month1Name
            2 -> month2Name
            else -> month3Name
        }

        return if (allRiwayatList.isNotEmpty()) {
            allRiwayatList.filter { item ->
                val tgl = item.tanggal ?: ""
                (pattern.isNotEmpty() && tgl.contains(pattern)) ||
                (monthNameText.isNotEmpty() && tgl.contains(monthNameText, ignoreCase = true))
            }
        } else {
            emptyList()
        }
    }

    private fun applyMonthFilter(monthIndex: Int) {
        if (!isAdded || _binding == null) return

        val monthNameText = when (monthIndex) {
            1 -> month1Name
            2 -> month2Name
            else -> month3Name
        }

        binding.tvStatistikTitle.text = "Statistik Kehadiran $monthNameText"

        val filteredList = getCurrentFilteredList()

        val totalHadir = filteredList.count { 
            it.status?.equals("Hadir", ignoreCase = true) == true || 
            it.status?.equals("TEPAT_WAKTU", ignoreCase = true) == true 
        }
        val totalTerlambat = filteredList.count { 
            it.status?.equals("Terlambat", ignoreCase = true) == true 
        }
        // Hitung izin masuk ke card "Izin / Sakit" HANYA jika statusnya "Disetujui"
        val totalIzin = filteredList.count {
            it.statusVerifikasi?.equals("Disetujui", ignoreCase = true) == true ||
            (it.statusVerifikasi.isNullOrEmpty() && (
                it.status?.equals("Izin", ignoreCase = true) == true || 
                it.status?.equals("Sakit", ignoreCase = true) == true || 
                it.status?.equals("Dispensasi", ignoreCase = true) == true
            ))
        }
        // Jika statusnya "Ditolak", data tersebut TIDAK dimasukkan ke total Izin/Sakit yang sah (tetap dihitung Alpa atau status evaluasi)
        val totalAlpa = filteredList.count {
            it.statusVerifikasi?.equals("Ditolak", ignoreCase = true) == true ||
            it.status?.equals("Alpa", ignoreCase = true) == true || 
            it.status?.equals("Ditolak", ignoreCase = true) == true
        }
        val totalDays = totalHadir + totalTerlambat + totalIzin + totalAlpa

        val disiplinPct = if (totalDays > 0) {
            String.format(Locale.US, "%.1f", ((totalHadir + totalIzin).toDouble() / totalDays) * 100.0)
        } else {
            "0.0"
        }

        binding.tvCountHadir.text = totalHadir.toString()
        binding.tvCountTerlambat.text = totalTerlambat.toString()
        binding.tvCountIzin.text = totalIzin.toString()
        binding.tvCountAlpa.text = totalAlpa.toString()
        binding.tvDisiplinBadge.text = "$disiplinPct% Disiplin"

        if (filteredList.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvRiwayatPresensi.visibility = View.GONE
            binding.tvCountRecordHeader.text = "0 Catatan"
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvRiwayatPresensi.visibility = View.VISIBLE
            binding.tvCountRecordHeader.text = "${filteredList.size} Catatan Terbaru"
            riwayatAdapter.updateData(filteredList)
        }
    }

    private fun setupExportPdfButton() {
        binding.btnExportPdf.setOnClickListener {
            val currentFilteredList = getCurrentFilteredList()

            if (currentFilteredList.isEmpty()) {
                Toast.makeText(requireContext(), "Tidak ada data riwayat presensi untuk diekspor.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            generateAndSavePdf(currentFilteredList)
        }

        binding.btnInfoRiwayat.setOnClickListener {
            Toast.makeText(requireContext(), "Semua rekapan presensi diverifikasi oleh Sistem AI & Wali Kelas SMKN 8 Jakarta.", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateAndSavePdf(records: List<RiwayatModel>) {
        val ctx = context ?: return

        val namaSiswa = sessionManager.getNama().ifEmpty { "Muhammad Fadhil" }
        val nisn = sessionManager.getNisn().ifEmpty { "0068192341" }
        val namaKelas = sessionManager.getNamaKelas().ifEmpty { "XII RPL" }
        val namaWalas = sessionManager.getWaliKelas().ifEmpty { "Farauk Pratama S.Kom" }

        val monthNameText = when (selectedMonthIndex) {
            1 -> month1Name
            2 -> month2Name
            else -> month3Name
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Ukuran A4 (595 x 842 pt)
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Setup Paints
        val paintHeaderTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00685F")
            textSize = 15f
            isFakeBoldText = true
        }

        val paintHeaderSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1C30")
            textSize = 11f
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

        val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

        // --- KOP DOKUMEN ---
        canvas.drawText("PEMERINTAH PROVINSI DKI JAKARTA", 40f, y, paintHeaderSub)
        y += 16f
        canvas.drawText("DINAS PENDIDIKAN", 40f, y, paintHeaderSub)
        y += 18f
        canvas.drawText("SMK NEGERI 8 JAKARTA", 40f, y, paintHeaderTitle)
        y += 14f
        canvas.drawText("Jl. Raya Pejaten Pasar Minggu, Jakarta Selatan • PORTAL PRESENSI DIGITAL", 40f, y, paintTextMuted)
        y += 12f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 24f

        // --- IDENTITAS SISWA ---
        canvas.drawText("LAPORAN REKAPITULASI PRESENSI KEHADIRAN SISWA", 40f, y, paintBold)
        y += 18f
        canvas.drawText("Nama Lengkap  : $namaSiswa", 40f, y, paintTextDark)
        canvas.drawText("NISN / NIS    : $nisn", 320f, y, paintTextDark)
        y += 14f
        canvas.drawText("Kelas / Rombel : $namaKelas", 40f, y, paintTextDark)
        canvas.drawText("Periode Rekap : $monthNameText", 320f, y, paintTextDark)
        y += 20f

        // --- TABEL HEADERS ---
        val tableTop = y
        val tableBottom = tableTop + 24f
        canvas.drawRect(40f, tableTop, 555f, tableBottom, paintTableHeaderBg)

        y += 16f
        canvas.drawText("No", 46f, y, paintBold)
        canvas.drawText("Tanggal", 80f, y, paintBold)
        canvas.drawText("Jam Masuk", 200f, y, paintBold)
        canvas.drawText("Status Kehadiran", 300f, y, paintBold)
        canvas.drawText("Verifikasi Sistem AI", 430f, y, paintBold)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, paintDivider)

        // --- TABEL ROWS LOOP ---
        records.forEachIndexed { index, item ->
            y += 18f
            if (y > 720f) return@forEachIndexed // Page height limit

            canvas.drawText("${index + 1}", 46f, y, paintTextDark)
            canvas.drawText(item.tanggal ?: "-", 80f, y, paintTextDark)
            canvas.drawText(item.displayJamMasuk ?: "-", 200f, y, paintTextDark)
            canvas.drawText(item.status ?: "-", 300f, y, paintBold)

            val statusStr = (item.status ?: "").uppercase()
            val verifikasiStr = if (statusStr == "HADIR" || statusStr == "TEPAT_WAKTU") {
                "Valid AI & GPS (Otomatis)"
            } else if (statusStr == "TERLAMBAT") {
                "Terverifikasi (Terlambat)"
            } else {
                "Diverifikasi Walas"
            }
            canvas.drawText(verifikasiStr, 430f, y, paintTextMuted)

            y += 6f
            canvas.drawLine(40f, y, 555f, y, paintDivider)
        }

        // --- FOOTER & TANDA TANGAN WALAS ---
        y = 740f
        val todayStr = try {
            SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date())
        } catch (e: Exception) {
            ""
        }

        canvas.drawText("Status Dokumen: Terverifikasi Digital oleh Sistem SMKN 8 Jakarta", 40f, y, paintTextMuted)
        canvas.drawText("Jakarta, $todayStr", 380f, y, paintTextDark)
        y += 14f
        canvas.drawText("Wali Kelas Pembina,", 380f, y, paintTextDark)
        y += 45f
        canvas.drawText(namaWalas, 380f, y, paintBold)

        pdfDocument.finishPage(page)

        // --- SIMPAN BERKAS VIA MEDIASTORE (FOLDER DOWNLOADS) ---
        val timeStamp = System.currentTimeMillis()
        val pdfFileName = "Rekap_Presensi_${nisn}_${timeStamp}.pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, pdfFileName)
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

                Snackbar.make(binding.root, "✓ PDF Berhasil Diunduh: $pdfFileName", Snackbar.LENGTH_LONG)
                    .setAction("Buka") {
                        openPdfDocument(pdfUri)
                    }
                    .show()

            } catch (e: Exception) {
                Toast.makeText(ctx, "Gagal menyimpan berkas PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                try {
                    pdfDocument.close()
                } catch (ignored: Exception) {}
            }
        } else {
            try {
                pdfDocument.close()
            } catch (ignored: Exception) {}
            Toast.makeText(ctx, "Gagal membuat entri PDF di direktori Download", Toast.LENGTH_LONG).show()
        }
    }

    private fun openPdfDocument(pdfUri: Uri) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
