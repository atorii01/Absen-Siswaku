package com.andev.absensiswaku.ui.pengajuan

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import com.andev.absensiswaku.MainActivity
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.FragmentPengajuanBinding
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

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (!isAdded || _binding == null) return@registerForActivityResult

            if (uri != null) {
                attachedFileUri = uri
                attachedFileName = getFileNameFromUri(uri)
                val fileSizeStr = getFileSizeFromUri(uri)

                binding.tvFileName.text = attachedFileName
                binding.tvFileSize.text = fileSizeStr

                if (attachedFileName.endsWith(".pdf", ignoreCase = true)) {
                    binding.imgFileTypeIcon.setImageResource(R.drawable.ic_pdf_doc)
                } else {
                    binding.imgFileTypeIcon.setImageResource(R.drawable.ic_medical)
                }

                binding.cardUploadedFile.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Berkas $attachedFileName berhasil dipilih", Toast.LENGTH_SHORT).show()
            }
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
        binding.cardOptionSakit.setOnClickListener {
            selectJenisIzin("Sakit")
        }

        binding.cardOptionIzin.setOnClickListener {
            selectJenisIzin("Izin")
        }

        binding.cardOptionDispensasi.setOnClickListener {
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
    }

    private fun setupFileUpload() {
        binding.btnUploadBox.setOnClickListener {
            pickFileLauncher.launch("*/*")
        }

        binding.btnDeleteFile.setOnClickListener {
            attachedFileUri = null
            attachedFileName = ""
            binding.cardUploadedFile.visibility = View.GONE
            binding.btnUploadBox.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Berkas lampiran dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupActionButtons() {
        binding.btnKirimPengajuan.setOnClickListener {
            submitPengajuanForm()
        }

        binding.btnBatal.setOnClickListener {
            (activity as? MainActivity)?.loadFragmentFromOutside(PresensiFragment())
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

        val payload: Map<String, Any> = mapOf(
            "id" to "iz-" + System.currentTimeMillis(),
            "siswa_id" to siswaIdInt,
            "jenis_izin" to formattedJenisIzin,
            "tanggal_mulai" to tanggalMulaiStr,
            "tanggal_selesai" to tanggalSelesaiStr,
            "keterangan" to keteranganText,
            "bukti_berkas_url" to (attachedFileName.ifEmpty { "surat_keterangan.jpg" }),
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
                    attachedFileUri = null
                    attachedFileName = ""
                    binding.cardUploadedFile.visibility = View.GONE
                    binding.btnUploadBox.visibility = View.VISIBLE

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
        val cursor = context?.contentResolver?.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    private fun getFileSizeFromUri(uri: Uri): String {
        var sizeBytes = 0L
        val cursor = context?.contentResolver?.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.SIZE)
                if (index != -1) {
                    sizeBytes = it.getLong(index)
                }
            }
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
