package com.andev.absensiswaku.ui.admin

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class SiswaItem(
    val id: Int,
    val nisn: String,
    val namaLengkap: String,
    val namaKelas: String,
    val jamMasuk: String,
    val status: String,
    val idKelas: Int = 0
)

object ExportReportHelper {

    fun exportToCsv(context: Context, listSiswa: List<SiswaItem>, tanggal: String): Uri? {
        val fileName = "Rekap_Presensi_SMKN8_${tanggal.replace("-", "")}.csv"
        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM agar rapi di Microsoft Excel
        sb.append("\"No\",\"NISN\",\"Nama Siswa\",\"Kelas\",\"Tanggal\",\"Jam Masuk\",\"Status Kehadiran\"\n")

        listSiswa.forEachIndexed { index, s ->
            val namaUpper = s.namaLengkap.uppercase(Locale.getDefault()).replace("\"", "\"\"")
            sb.append("\"${index + 1}\",=\"${s.nisn}\",\"$namaUpper\",\"${s.namaKelas}\",\"$tanggal\",\"${s.jamMasuk}\",\"${s.status}\"\n")
        }

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
                    stream.write(sb.toString().toByteArray(Charsets.UTF_8))
                    stream.flush()
                }
            }
            insertUri
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            file.writeText(sb.toString(), Charsets.UTF_8)
            Uri.fromFile(file)
        }
        return uri
    }

    fun exportToXlsx(context: Context, listSiswa: List<SiswaItem>, tanggal: String): Uri? {
        val fileName = "Rekap_Presensi_SMKN8_${tanggal.replace("-", "")}.xlsx"
        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM agar rapi di Microsoft Excel
        sb.append("\"No\",\"NISN\",\"Nama Siswa\",\"Kelas\",\"Tanggal\",\"Jam Masuk\",\"Status Kehadiran\"\n")

        listSiswa.forEachIndexed { index, s ->
            val namaUpper = s.namaLengkap.uppercase(Locale.getDefault()).replace("\"", "\"\"")
            sb.append("\"${index + 1}\",=\"${s.nisn}\",\"$namaUpper\",\"${s.namaKelas}\",\"$tanggal\",\"${s.jamMasuk}\",\"${s.status}\"\n")
        }

        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            insertUri?.let { destUri ->
                resolver.openOutputStream(destUri)?.use { stream ->
                    stream.write(sb.toString().toByteArray(Charsets.UTF_8))
                    stream.flush()
                }
            }
            insertUri
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            file.writeText(sb.toString(), Charsets.UTF_8)
            Uri.fromFile(file)
        }
        return uri
    }

    fun exportToPdfMultiPage(context: Context, listSiswa: List<SiswaItem>, tanggalFormatted: String): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Ukuran A4 (Lebar: 595, Tinggi: 842)

        val paint = Paint()
        val paintText = Paint().apply {
            textSize = 9.5f
            color = Color.BLACK
            isAntiAlias = true
        }

        // Kelompokkan 357 siswa berdasarkan 10 Rombel Kelas
        val groupedByClass = listSiswa.groupBy { it.namaKelas }
        var pageNumber = 1

        for ((namaKelas, siswaDiKelas) in groupedByClass) {
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Header Dokumen SMKN 8 Jakarta
            paint.color = Color.parseColor("#00685F")
            paint.textSize = 15f
            paint.isFakeBoldText = true
            canvas.drawText("SMKN 8 JAKARTA - REKAPITULASI PRESENSI HARIAN", 40f, 45f, paint)

            paint.color = Color.DKGRAY
            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText("Cakupan: $namaKelas | Tanggal: $tanggalFormatted | Halaman: $pageNumber dari ${groupedByClass.size}", 40f, 65f, paint)

            // Garis Pembatas Header
            paint.strokeWidth = 1.2f
            paint.color = Color.parseColor("#00685F")
            canvas.drawLine(40f, 75f, 555f, 75f, paint)

            // Header Kolom Tabel
            var yPos = 95f
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 9f
            paint.isFakeBoldText = true
            canvas.drawText("NO", 40f, yPos, paint)
            canvas.drawText("NISN", 65f, yPos, paint)
            canvas.drawText("NAMA LENGKAP SISWA", 150f, yPos, paint)
            canvas.drawText("STATUS", 420f, yPos, paint)
            canvas.drawText("WAKTU", 495f, yPos, paint)

            yPos += 8f
            paint.strokeWidth = 0.5f
            paint.color = Color.LTGRAY
            canvas.drawLine(40f, yPos, 555f, yPos, paint)
            yPos += 14f

            // Cetak Siswa Rombel Ini
            siswaDiKelas.forEachIndexed { idx, s ->
                paintText.isFakeBoldText = false
                canvas.drawText("${idx + 1}.", 40f, yPos, paintText)
                canvas.drawText(s.nisn, 65f, yPos, paintText)

                val namaUpper = s.namaLengkap.uppercase(Locale.getDefault())
                val nama = if (namaUpper.length > 32) namaUpper.take(30) + ".." else namaUpper
                canvas.drawText(nama, 150f, yPos, paintText)

                // Warna Status
                paintText.isFakeBoldText = true
                when (s.status) {
                    "Hadir" -> paintText.color = Color.parseColor("#15803D")
                    "Terlambat" -> paintText.color = Color.parseColor("#B45309")
                    "Izin", "Sakit" -> paintText.color = Color.parseColor("#1D4ED8")
                    else -> paintText.color = Color.parseColor("#DC2626") // Alpa
                }
                canvas.drawText(s.status, 420f, yPos, paintText)

                paintText.color = Color.DKGRAY
                paintText.isFakeBoldText = false
                canvas.drawText(s.jamMasuk, 495f, yPos, paintText)

                yPos += 16.5f
            }

            // Tanda Tangan Wali Kelas di Bawah
            paint.color = Color.BLACK
            paint.textSize = 9.5f
            paint.isFakeBoldText = false
            canvas.drawText("Mengetahui,", 410f, 735f, paint)
            canvas.drawText("Wali Kelas $namaKelas", 410f, 750f, paint)
            canvas.drawText("(..........................................)", 410f, 800f, paint)

            pdfDocument.finishPage(page)
            pageNumber++
        }

        val fileName = "Laporan_Presensi_Resmi_SMKN8_${System.currentTimeMillis()}.pdf"
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
                    pdfDocument.writeTo(stream)
                }
            }
            insertUri
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { stream ->
                pdfDocument.writeTo(stream)
            }
            Uri.fromFile(file)
        }
        pdfDocument.close()
        return uri
    }

    fun openUri(context: Context, uri: Uri, mimeType: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Buka Berkas Laporan"))
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi pembuka berkas: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            openUri(context, uri, mimeType)
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka berkas: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
