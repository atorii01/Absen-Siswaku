package com.andev.absensiswaku.ui.pengajuan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import java.io.ByteArrayOutputStream
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import com.andev.absensiswaku.MainActivity
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.FragmentPengajuanBinding
import com.andev.absensiswaku.util.MotionUtils
import com.andev.absensiswaku.util.applyBounceEffect
import com.andev.absensiswaku.ui.presensi.PresensiFragment
import com.andev.absensiswaku.ui.riwayat.RiwayatFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PengajuanFragment : Fragment() {

    private var _binding: FragmentPengajuanBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager

    private var jenisIzinDipilih: String = "" // SAKIT, IZIN, DISPENSASI
    private var isRangeMode: Boolean = true

    private var startDateMillis: Long = System.currentTimeMillis()
    private var endDateMillis: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000L) // Default +1 day

    private var tanggalMulaiStr: String = ""
    private var tanggalSelesaiStr: String = ""

    private var attachedFileUri: Uri? = null
    private var attachedFileName: String = ""

    private var selectedFileUri: Uri?
        get() = attachedFileUri
        set(value) { attachedFileUri = value }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { validasiDanProsesBerkas(it) }
    }

    private fun validasiDanProsesBerkas(uri: Uri) {
        val contentResolver = requireContext().contentResolver

        // 1. Cek MIME Type
        val mimeType = contentResolver.getType(uri) ?: ""
        val validMimeTypes = listOf("image/jpeg", "image/png", "application/pdf")
        if (!validMimeTypes.contains(mimeType)) {
            Toast.makeText(
                requireContext(),
                "Format berkas tidak valid! Hanya diperbolehkan JPG, PNG, atau PDF.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // 2. Cek Ukuran Berkas via OpenableColumns.SIZE (Maksimal 5 MB = 5 * 1024 * 1024 bytes)
        var fileSize: Long = 0
        var fileName = "Dokumen_Lampiran"

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                if (nameIndex != -1) fileName = cursor.getString(nameIndex)
            }
        }

        val maxSizeBytes = 5 * 1024 * 1024L // 5 MB
        if (fileSize > maxSizeBytes) {
            val sizeInMb = String.format(Locale.getDefault(), "%.2f", fileSize / (1024.0 * 1024.0))
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ukuran Berkas Terlalu Besar")
                .setMessage("Ukuran berkas Anda ($sizeInMb MB) melebihi batas ketentuan 5 MB. Harap kompres atau pilih berkas lain.")
                .setPositiveButton("Mengerti", null)
                .show()
            return
        }

        // 3. Jika Lolos: Simpan URI, tampilkan nama berkas dan badge hijau di form
        selectedFileUri = uri
        attachedFileName = fileName
        binding.layoutPreviewUploaded.visibility = View.VISIBLE
        binding.cardUploadBukti.visibility = View.GONE
        binding.tvNamaFileUploaded.text = fileName
        val sizeText = if (fileSize > 1024 * 1024) {
            "${String.format(Locale.getDefault(), "%.1f", fileSize / (1024.0 * 1024.0))} MB"
        } else {
            "${fileSize / 1024} KB"
        }
        binding.tvUkuranFileUploaded.text = "$sizeText • Siap Diunggah"

        if (fileName.endsWith(".pdf", ignoreCase = true) || mimeType == "application/pdf") {
            binding.imgFileTypeIcon.setImageResource(R.drawable.ic_pdf_doc)
        } else {
            binding.imgFileTypeIcon.setImageResource(R.drawable.ic_medical)
        }
        Toast.makeText(requireContext(), "Berkas $fileName berhasil dipilih", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPengajuanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = context ?: return
        sessionManager = SessionManager(ctx)

        setupHeaderData()
        setupJenisIzinSelection()
        setupPeriodToggleAndPickers()
        setupFileUpload()
        setupActionButtons()
        updateDateDisplays()

        MotionUtils.animateStaggeredEntrance(
            binding.cardHeaderFormulir,
            binding.cardJenisKetidakhadiran,
            binding.cardPeriodeWaktu,
            binding.cardUploadBukti,
            binding.btnKirimPengajuan
        )
    }

    private fun setupHeaderData() {
        if (!isAdded || _binding == null) return

        val nisn = sessionManager.getNisn().ifEmpty { "0068192341" }
        val nama = sessionManager.getNama().ifEmpty { "Muhammad Fadhil" }
        val namaKelas = sessionManager.getNamaKelas().ifEmpty { "XII RPL" }
        val namaWalas = sessionManager.getWaliKelas().ifEmpty { "Farauk Pratama S.Kom" }

        binding.tvTopBarNisn.text = "NISN: $nisn"
        binding.tvProfilSubtext.text = "$nama • Kelas $namaKelas"
        binding.tvWalasNote.text = "Pengajuan akan langsung diteruskan ke $namaWalas. Status otomatis diperbarui di aplikasi dan dikirimkan via WhatsApp orang tua."
    }

    private fun setupJenisIzinSelection() {
        binding.cardOptionSakit.applyBounceEffect {
            selectJenisIzin("Sakit")
        }

        binding.cardOptionIzin.applyBounceEffect {
            selectJenisIzin("Izin")
        }

        binding.cardOptionDispensasi.applyBounceEffect {
            selectJenisIzin("Dispensasi")
        }
    }

    private fun selectJenisIzin(jenis: String) {
        jenisIzinDipilih = jenis
        val ctx = context ?: return

        val textDark = ContextCompat.getColor(ctx, R.color.text_primary)

        // Reset All
        binding.cardOptionSakit.setBackgroundResource(R.drawable.bg_card_option_unselected)
        binding.cardOptionIzin.setBackgroundResource(R.drawable.bg_card_option_unselected)
        binding.cardOptionDispensasi.setBackgroundResource(R.drawable.bg_card_option_unselected)

        binding.imgIconSakit.setColorFilter(textDark)
        binding.imgIconIzin.setColorFilter(textDark)
        binding.imgIconDispensasi.setColorFilter(textDark)

        binding.tvTitleSakit.setTextColor(textDark)
        binding.tvTitleIzin.setTextColor(textDark)
        binding.tvTitleDispensasi.setTextColor(textDark)

        when (jenis.uppercase()) {
            "SAKIT" -> {
                binding.cardOptionSakit.setBackgroundResource(R.drawable.bg_card_sakit_selected)
                binding.imgIconSakit.setColorFilter(ContextCompat.getColor(ctx, R.color.status_unread_text))
                binding.tvTitleSakit.setTextColor(ContextCompat.getColor(ctx, R.color.status_unread_text))
            }
            "IZIN" -> {
                binding.cardOptionIzin.setBackgroundResource(R.drawable.bg_card_izin_selected)
                binding.imgIconIzin.setColorFilter(ContextCompat.getColor(ctx, R.color.badge_izin_text))
                binding.tvTitleIzin.setTextColor(ContextCompat.getColor(ctx, R.color.badge_izin_text))
            }
            "DISPENSASI" -> {
                binding.cardOptionDispensasi.setBackgroundResource(R.drawable.bg_card_dispensasi_selected)
                binding.imgIconDispensasi.setColorFilter(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))
                binding.tvTitleDispensasi.setTextColor(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))
            }
        }
    }

    private fun setupPeriodToggleAndPickers() {
        binding.btnModeSingle.setOnClickListener {
            isRangeMode = false
            endDateMillis = startDateMillis
            binding.btnModeSingle.setBackgroundResource(R.drawable.bg_chip_month_active)
            binding.btnModeSingle.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            binding.btnModeRange.setBackgroundResource(android.R.color.transparent)
            binding.btnModeRange.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

            updateDateDisplays()
        }

        binding.btnModeRange.setOnClickListener {
            isRangeMode = true
            binding.btnModeRange.setBackgroundResource(R.drawable.bg_chip_month_active)
            binding.btnModeRange.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            binding.btnModeSingle.setBackgroundResource(android.R.color.transparent)
            binding.btnModeSingle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

            updateDateDisplays()
        }

        binding.btnTanggalMulai.setOnClickListener { showDatePicker() }
        binding.btnTanggalSelesai.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        if (isRangeMode) {
            val constraintsBuilder = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())

            val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Pilih Rentang Tanggal Izin/Sakit")
                .setSelection(Pair(startDateMillis, endDateMillis))
                .setCalendarConstraints(constraintsBuilder.build())
                .build()

            dateRangePicker.addOnPositiveButtonClickListener { selection ->
                if (selection.first != null && selection.second != null) {
                    startDateMillis = selection.first!!
                    endDateMillis = selection.second!!
                    updateDateDisplays()
                }
            }

            dateRangePicker.show(childFragmentManager, "DATE_RANGE_PICKER")
        } else {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Pilih Tanggal Ketidakhadiran")
                .setSelection(startDateMillis)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                if (selection != null) {
                    startDateMillis = selection
                    endDateMillis = selection
                    updateDateDisplays()
                }
            }

            datePicker.show(childFragmentManager, "DATE_SINGLE_PICKER")
        }
    }

    private fun updateDateDisplays() {
        if (!isAdded || _binding == null) return

        try {
            val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
            val sdfDay = SimpleDateFormat("EEEE", Locale.forLanguageTag("id-ID"))
            val sdfSql = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            sdfDate.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            sdfDay.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            sdfSql.timeZone = TimeZone.getTimeZone("Asia/Jakarta")

            val startDate = Date(startDateMillis)
            val endDate = Date(endDateMillis)

            tanggalMulaiStr = sdfSql.format(startDate)
            tanggalSelesaiStr = sdfSql.format(endDate)

            val startFormatted = sdfDate.format(startDate)
            val startDay = sdfDay.format(startDate)

            val endFormatted = sdfDate.format(endDate)
            val endDay = sdfDay.format(endDate)

            val diffMillis = (endDateMillis - startDateMillis).coerceAtLeast(0)
            val durationDays = ((diffMillis / (1000 * 60 * 60 * 24)) + 1).toInt()

            binding.tvTanggalMulaiVal.text = startFormatted
            binding.tvHariMulaiSub.text = startDay

            binding.tvTanggalSelesaiVal.text = endFormatted
            binding.tvHariSelesaiSub.text = "$endDay ($durationDays Hari)"

            binding.tvDurasiInfo.text = "Total durasi ketidakhadiran: $durationDays Hari Kalender Sekolah"
        } catch (e: Exception) {
            binding.tvTanggalMulaiVal.text = "-"
            binding.tvTanggalSelesaiVal.text = "-"
            binding.tvDurasiInfo.text = "Periode tanggal telah ditentukan."
        }
    }

    private fun setupFileUpload() {
        binding.cardUploadBukti.applyBounceEffect {
            filePickerLauncher.launch(
                arrayOf("image/jpeg", "image/png", "application/pdf")
            )
        }

        binding.btnDeleteFile.applyBounceEffect {
            selectedFileUri = null
            attachedFileName = ""
            binding.layoutPreviewUploaded.visibility = View.GONE
            binding.cardUploadBukti.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Berkas lampiran dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupActionButtons() {
        binding.btnKirimPengajuan.applyBounceEffect {
            submitPengajuanForm()
        }

        binding.btnBatal.applyBounceEffect {
            (activity as? MainActivity)?.loadFragmentFromOutside(PresensiFragment())
        }
    }

    /**
     * Konversi URI gambar ke string Base64 dengan inSampleSize & pengaman OutOfMemoryError
     */
    private fun uriToBase64(uri: Uri): String? {
        val ctx = context ?: return null
        return try {
            val resolver = ctx.contentResolver

            // 1. Ambil dimensi gambar terlebih dahulu tanpa load ke memori
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            // 2. Perkecil sample size jika ukuran gambar lebih dari 1000px
            var inSampleSize = 1
            while ((options.outHeight / inSampleSize) >= 1000 || (options.outWidth / inSampleSize) >= 1000) {
                inSampleSize *= 2
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize

            // 3. Decode dengan inSampleSize aman
            val sampledBitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            // Resize proporsional dengan lebar maksimal 800px
            val targetWidth = 800
            val finalBitmap = if (sampledBitmap.width > targetWidth) {
                val targetHeight = (sampledBitmap.height * (targetWidth.toFloat() / sampledBitmap.width)).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true)
            } else {
                sampledBitmap
            }

            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (oom: OutOfMemoryError) {
            System.gc()
            Toast.makeText(context, "Ukuran foto terlalu besar untuk diproses.", Toast.LENGTH_SHORT).show()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun submitPengajuanForm() {
        val ctx = context ?: return

        if (jenisIzinDipilih.isEmpty()) {
            Toast.makeText(ctx, "Pilih jenis ketidakhadiran terlebih dahulu (Sakit / Izin / Dispensasi).", Toast.LENGTH_LONG).show()
            return
        }

        if (tanggalMulaiStr.isEmpty() || tanggalSelesaiStr.isEmpty()) {
            Toast.makeText(ctx, "Tentukan periode tanggal ketidakhadiran.", Toast.LENGTH_LONG).show()
            return
        }

        val keteranganText = binding.etKeterangan.text.toString().trim()
        if (keteranganText.isEmpty()) {
            Toast.makeText(ctx, "Isi alasan / keterangan lengkap ketidakhadiran.", Toast.LENGTH_LONG).show()
            return
        }

        if (attachedFileName.isEmpty()) {
            Toast.makeText(ctx, "Unggah bukti fisik (Surat Dokter / Surat Ortu / Surat Tugas) terlebih dahulu.", Toast.LENGTH_LONG).show()
            return
        }

        val pref = ctx.getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val siswaIdInt = try {
            pref?.getInt("user_id", 0)?.takeIf { it != 0 }
                ?: pref?.getInt("ID_SISWA", 0)?.takeIf { it != 0 }
                ?: pref?.getString("user_id", "0")?.toIntOrNull()
                ?: pref?.getString("ID_SISWA", "0")?.toIntOrNull()
                ?: sessionManager.getUserId().takeIf { it != 0 }
                ?: 1
        } catch (e: Exception) { 1 }

        val namaWalas = sessionManager.getWaliKelas().ifEmpty { "Farauk Pratama S.Kom" }

        val formattedJenisIzin = when (jenisIzinDipilih.uppercase()) {
            "SAKIT" -> "Sakit"
            "IZIN" -> "Izin"
            "DISPENSASI" -> "Dispensasi"
            else -> jenisIzinDipilih.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        // Konversi berkas bukti foto/PDF ke Base64 agar dapat langsung dipratinjau oleh Wali Kelas
        val base64Bukti = if (attachedFileName.endsWith(".pdf", ignoreCase = true)) {
            try {
                val bytes = context?.contentResolver?.openInputStream(selectedFileUri ?: attachedFileUri!!)?.readBytes()
                if (bytes != null) {
                    "data:application/pdf;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                } else attachedFileName
            } catch (e: Exception) {
                attachedFileName
            }
        } else {
            (selectedFileUri ?: attachedFileUri)?.let { uriToBase64(it) } ?: (attachedFileName.ifEmpty { "surat_keterangan.jpg" })
        }

        val payload: Map<String, Any> = mapOf(
            "id" to "iz-" + System.currentTimeMillis(),
            "siswa_id" to siswaIdInt,
            "jenis_izin" to formattedJenisIzin,
            "tanggal_mulai" to tanggalMulaiStr,
            "tanggal_selesai" to tanggalSelesaiStr,
            "keterangan" to keteranganText,
            "bukti_berkas_url" to base64Bukti, // Kirim data foto asli base64
            "status_verifikasi" to "Pending"
        )

        binding.btnKirimPengajuan.isEnabled = false
        binding.btnKirimPengajuan.text = "Mengirimkan Pengajuan..."

        SupabaseClient.instance.kirimPengajuanIzin(payload).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!isAdded || _binding == null) return

                if (response.isSuccessful || response.code() == 201 || response.code() == 200) {
                    Toast.makeText(requireContext(), "✓ Pengajuan izin berhasil dikirim!", Toast.LENGTH_SHORT).show()

                    binding.etKeterangan.text?.clear()
                    selectedFileUri = null
                    attachedFileName = ""
                    binding.layoutPreviewUploaded.visibility = View.GONE
                    binding.cardUploadBukti.visibility = View.VISIBLE

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("✓ Pengajuan Berhasil Dikirim")
                        .setMessage("Pengajuan $formattedJenisIzin Anda telah berhasil dikirimkan ke Wali Kelas ($namaWalas). Pantau status verifikasi pada Tab Riwayat.")
                        .setPositiveButton("Selesai") { dialog, _ ->
                            dialog.dismiss()
                            (activity as? MainActivity)?.loadFragmentFromOutside(RiwayatFragment(), R.id.nav_riwayat)
                        }
                        .show()
                } else {
                    binding.btnKirimPengajuan.isEnabled = true
                    binding.btnKirimPengajuan.text = "➤ Kirim Pengajuan Izin"
                    val err = response.errorBody()?.string() ?: ""
                    Toast.makeText(requireContext(), "Gagal mengirim pengajuan (${response.code()}): $err", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (!isAdded || _binding == null) return
                binding.btnKirimPengajuan.isEnabled = true
                binding.btnKirimPengajuan.text = "➤ Kirim Pengajuan Izin"
                Toast.makeText(requireContext(), "Koneksi Error: ${t.localizedMessage ?: t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "surat_keterangan_${System.currentTimeMillis()}.pdf"
        try {
            val cursor = context?.contentResolver?.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index) ?: name
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }

    private fun getFileSizeFromUri(uri: Uri): String {
        var sizeBytes = 0L
        try {
            val cursor = context?.contentResolver?.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.SIZE)
                    if (index != -1) {
                        sizeBytes = it.getLong(index)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (sizeBytes > 0) {
            val mb = sizeBytes / (1024f * 1024f)
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            "1.2 MB"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
