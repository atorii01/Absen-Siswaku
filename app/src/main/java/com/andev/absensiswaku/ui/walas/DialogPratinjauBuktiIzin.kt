package com.andev.absensiswaku.ui.walas

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.PengajuanIzinResponse
import com.andev.absensiswaku.databinding.LayoutDialogPratinjauBinding
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

/**
 * Dialog Pratinjau Bukti Lampiran Ketidakhadiran Siswa untuk Wali Kelas.
 * Mendukung pratinjau adaptif foto surat fisik (JPG/JPEG/PNG) dan pembuka berkas dokumen/PDF
 * melalui aplikasi viewer di perangkat pengguna menggunakan Intent ACTION_VIEW dan FileProvider.
 */
class DialogPratinjauBuktiIzin : DialogFragment() {

    private var _binding: LayoutDialogPratinjauBinding? = null
    private val binding get() = _binding!!

    var pengajuan: PengajuanIzinResponse? = null
    var namaKelas: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        _binding = LayoutDialogPratinjauBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val displayWidth = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(displayWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        val item = pengajuan ?: PengajuanIzinResponse()
        setupDialogContent(requireContext(), binding, item, namaKelas, dialog)

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DialogPratinjauBuktiIzin"

        fun newInstance(pengajuan: PengajuanIzinResponse, namaKelas: String = ""): DialogPratinjauBuktiIzin {
            return DialogPratinjauBuktiIzin().apply {
                this.pengajuan = pengajuan
                this.namaKelas = namaKelas
            }
        }

        /**
         * Menampilkan modal dialog secara langsung menggunakan Context
         */
        fun tampilkan(context: Context, pengajuan: PengajuanIzinResponse, namaKelas: String = ""): Dialog {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            val binding = LayoutDialogPratinjauBinding.inflate(LayoutInflater.from(context))
            dialog.setContentView(binding.root)

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val displayWidth = (context.resources.displayMetrics.widthPixels * 0.90).toInt()
            dialog.window?.setLayout(displayWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

            setupDialogContent(context, binding, pengajuan, namaKelas, dialog)
            dialog.show()
            return dialog
        }

        /**
         * Konfigurasi elemen tampilan dan logika verifikasi berkas
         */
        fun setupDialogContent(
            context: Context,
            binding: LayoutDialogPratinjauBinding,
            pengajuan: PengajuanIzinResponse,
            namaKelas: String,
            dialog: Dialog
        ) {
            // 1. Data Siswa & Kelas
            val namaSiswa = pengajuan.siswa?.namaLengkap ?: "Siswa"
            val kelasSuffix = if (namaKelas.isNotBlank()) " • Kelas $namaKelas" else ""
            binding.tvNamaSiswa.text = "$namaSiswa$kelasSuffix"

            // 2. Badge Jenis Izin
            val jenis = pengajuan.jenisIzin?.trim().orEmpty().ifEmpty { "Izin" }
            binding.tvJenisIzin.text = jenis
            when (jenis.uppercase()) {
                "SAKIT" -> binding.tvJenisIzin.setBackgroundResource(R.drawable.bg_badge_pill_sakit)
                "IZIN" -> binding.tvJenisIzin.setBackgroundResource(R.drawable.bg_badge_pill_amber)
                "DISPENSASI" -> binding.tvJenisIzin.setBackgroundResource(R.drawable.bg_badge_pill_dispensasi)
                else -> binding.tvJenisIzin.setBackgroundResource(R.drawable.bg_badge_pill_gray)
            }

            // 3. Periksa ekstensi atau tipe berkas lampiran (misal dari bukti_berkas_url Supabase)
            val url = pengajuan.buktiBerkasUrl ?: ""
            val isImage = url.endsWith(".jpg", true) ||
                    url.endsWith(".jpeg", true) ||
                    url.endsWith(".png", true) ||
                    url.startsWith("data:image")

            val isBase64 = url.startsWith("data:") || url.length > 500
            val rawFileName = if (isBase64) {
                if (isImage) "Foto_Bukti_Fisik_${jenis}.jpg" else "Dokumen_Lampiran_${jenis}.pdf"
            } else {
                url.substringAfterLast("/").takeIf { it.isNotEmpty() } ?: "Surat_Keterangan_${jenis}.pdf"
            }
            binding.tvNamaFile.text = "📄 $rawFileName"

            if (isImage) {
                binding.ivPratinjauFoto.visibility = View.VISIBLE
                binding.layoutDokumenPlaceholder.visibility = View.GONE
                binding.btnBukaDokumen.visibility = View.GONE

                // Tampilkan gambar menggunakan Bitmap decode / Network stream
                if (url.startsWith("data:image")) {
                    try {
                        val cleanBase64 = url.substringAfter("base64,")
                        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)

                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)

                        var inSampleSize = 1
                        while ((options.outHeight / inSampleSize) >= 1200 || (options.outWidth / inSampleSize) >= 1200) {
                            inSampleSize *= 2
                        }

                        options.inJustDecodeBounds = false
                        options.inSampleSize = inSampleSize
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)
                        if (bitmap != null) {
                            binding.ivPratinjauFoto.setImageBitmap(bitmap)
                            binding.ivPratinjauFoto.scaleType = ImageView.ScaleType.FIT_CENTER
                            binding.ivPratinjauFoto.setBackgroundColor(Color.TRANSPARENT)
                        } else {
                            binding.ivPratinjauFoto.setImageResource(R.drawable.ic_surat_dokter_preview)
                        }
                    } catch (oom: OutOfMemoryError) {
                        System.gc()
                        binding.ivPratinjauFoto.setImageResource(R.drawable.ic_surat_dokter_preview)
                        Toast.makeText(context, "Ukuran foto terlalu besar untuk ditampilkan.", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        binding.ivPratinjauFoto.setImageResource(R.drawable.ic_surat_dokter_preview)
                    }
                } else if (url.startsWith("http://") || url.startsWith("https://")) {
                    thread {
                        try {
                            val input = URL(url).openStream()
                            val bitmap = BitmapFactory.decodeStream(input)
                            binding.ivPratinjauFoto.post {
                                if (bitmap != null) {
                                    binding.ivPratinjauFoto.setImageBitmap(bitmap)
                                    binding.ivPratinjauFoto.scaleType = ImageView.ScaleType.FIT_CENTER
                                    binding.ivPratinjauFoto.setBackgroundColor(Color.TRANSPARENT)
                                } else {
                                    binding.ivPratinjauFoto.setImageResource(R.drawable.ic_surat_dokter_preview)
                                }
                            }
                        } catch (e: Exception) {
                            binding.ivPratinjauFoto.post {
                                binding.ivPratinjauFoto.setImageResource(R.drawable.ic_surat_dokter_preview)
                            }
                        }
                    }
                } else if (url.startsWith("content://") || url.startsWith("file://")) {
                    try {
                        binding.ivPratinjauFoto.setImageURI(Uri.parse(url))
                        binding.ivPratinjauFoto.scaleType = ImageView.ScaleType.FIT_CENTER
                        binding.ivPratinjauFoto.setBackgroundColor(Color.TRANSPARENT)
                    } catch (e: Exception) {
                        binding.ivPratinjauFoto.setImageResource(R.drawable.ic_surat_dokter_preview)
                    }
                } else {
                    binding.ivPratinjauFoto.setImageResource(R.drawable.ic_surat_dokter_preview)
                }
            } else {
                // Jika berkas adalah PDF atau Dokumen Spreadsheet/Word
                binding.ivPratinjauFoto.visibility = View.GONE
                binding.layoutDokumenPlaceholder.visibility = View.VISIBLE
                binding.btnBukaDokumen.visibility = View.VISIBLE

                binding.btnBukaDokumen.setOnClickListener {
                    try {
                        val targetUri: Uri = if (url.startsWith("data:")) {
                            val cleanBase64 = url.substringAfter("base64,")
                            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                            val cacheFile = File(context.cacheDir, "dokumen_surat_${pengajuan.id ?: System.currentTimeMillis()}.pdf")
                            cacheFile.writeBytes(decodedBytes)
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                cacheFile
                            )
                        } else {
                            Uri.parse(url)
                        }

                        val mimeType = if (url.endsWith(".pdf", true) || url.startsWith("data:application/pdf")) {
                            "application/pdf"
                        } else {
                            "*/*"
                        }

                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(targetUri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(intent, "Buka Berkas Dengan"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Tidak ada aplikasi untuk membuka format berkas ini", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // 4. Keterangan / Alasan Siswa
            val quote = pengajuan.keterangan?.takeIf { it.isNotBlank() } ?: "Tidak ada keterangan tambahan."
            binding.tvKeterangan.text = "\"$quote\""

            // 5. Tombol Tutup Dialog
            binding.btnTutup.setOnClickListener { dialog.dismiss() }
            binding.btnCloseHeader.setOnClickListener { dialog.dismiss() }
        }
    }
}
