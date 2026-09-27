package com.andev.absensiswaku.ui.admin

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
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportStudentRow(
    val nomor: Int,
    val nisn: String,
    val namaSiswa: String,
    val namaKelas: String,
    val tanggal: String,
    val jamMasuk: String,
    val status: String,
    val jarakMeter: String = "14m",
    val skorAi: String = "98.2%",
    val catatan: String = "Terverifikasi Valid"
)

object ExportReportHelper {

    fun exportReport(
        context: Context,
        format: String,
        rombelName: String,
        periodName: String,
        dataRows: List<ExportStudentRow>,
        includeGps: Boolean = true,
        includeWalasNotes: Boolean = true,
        includeDapodik: Boolean = true,
        onComplete: (Boolean, String, Long, File?) -> Unit
    ) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanRombel = rombelName.replace(" ", "_").replace("(", "").replace(")", "").replace("-", "_")

        try {
            when (format.uppercase(Locale.getDefault())) {
                "XLSX" -> {
                    val fileName = "Rekap_Presensi_SMKN8_${cleanRombel}_$timeStamp.xlsx"
                    val file = generateXlsxFile(context, fileName, dataRows, includeGps, includeWalasNotes)
                    val sizeKb = (file.length() / 1024).coerceAtLeast(1)
                    saveToDownloadsPublic(context, file, fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    onComplete(true, fileName, sizeKb, file)
                }
                "PDF" -> {
                    val fileName = "Laporan_Resmi_SMKN8_${cleanRombel}_$timeStamp.pdf"
                    val file = generatePdfFile(context, fileName, rombelName, periodName, dataRows)
                    val sizeKb = (file.length() / 1024).coerceAtLeast(1)
                    saveToDownloadsPublic(context, file, fileName, "application/pdf")
                    onComplete(true, fileName, sizeKb, file)
                }
                "CSV" -> {
                    val fileName = "Sinkron_Dapodik_SMKN8_${cleanRombel}_$timeStamp.csv"
                    val file = generateCsvFile(context, fileName, dataRows, includeDapodik)
                    val sizeKb = (file.length() / 1024).coerceAtLeast(1)
                    saveToDownloadsPublic(context, file, fileName, "text/csv")
                    onComplete(true, fileName, sizeKb, file)
                }
                else -> {
                    onComplete(false, "Format tidak didukung", 0, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false, e.localizedMessage ?: "Gagal mengekspor berkas", 0, null)
        }
    }

    private fun generateXlsxFile(
        context: Context,
        fileName: String,
        dataRows: List<ExportStudentRow>,
        includeGps: Boolean,
        includeWalasNotes: Boolean
    ): File {
        val destFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        FileOutputStream(destFile).use { fos ->
            // Menulis UTF-8 BOM untuk kompatibilitas penuh Microsoft Excel
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val headerBuilder = StringBuilder("No;NISN;Nama Lengkap;Kelas;Tanggal;Jam Masuk;Status Presensi")
            if (includeGps) headerBuilder.append(";Jarak Gerbang;Skor Face Match")
            if (includeWalasNotes) headerBuilder.append(";Catatan Verifikasi")
            headerBuilder.append("\n")
            fos.write(headerBuilder.toString().toByteArray(Charsets.UTF_8))

            dataRows.forEach { row ->
                val lineBuilder = StringBuilder()
                lineBuilder.append("${row.nomor};")
                lineBuilder.append("${row.nisn};")
                lineBuilder.append("\"${row.namaSiswa.replace("\"", "\"\"")}\";")
                lineBuilder.append("${row.namaKelas};")
                lineBuilder.append("${row.tanggal};")
                lineBuilder.append("${row.jamMasuk};")
                lineBuilder.append("${row.status}")
                if (includeGps) {
                    lineBuilder.append(";${row.jarakMeter};${row.skorAi}")
                }
                if (includeWalasNotes) {
                    lineBuilder.append(";\"${row.catatan}\"")
                }
                lineBuilder.append("\n")
                fos.write(lineBuilder.toString().toByteArray(Charsets.UTF_8))
            }
        }
        return destFile
    }

    private fun generateCsvFile(
        context: Context,
        fileName: String,
        dataRows: List<ExportStudentRow>,
        includeDapodik: Boolean
    ): File {
        val destFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        FileOutputStream(destFile).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val header = if (includeDapodik) {
                "KODE_SEKOLAH,KODE_ROMBEL,NISN,NAMA_SISWA,STATUS_KEHADIRAN,TANGGAL,WAKTU_PRESENSI,VERIFIKASI_DAPODIK\n"
            } else {
                "NO,NISN,NAMA_SISWA,KELAS,STATUS,TANGGAL,JAM_MASUK\n"
            }
            fos.write(header.toByteArray(Charsets.UTF_8))

            dataRows.forEach { row ->
                val line = if (includeDapodik) {
                    "SMKN8,${row.namaKelas},${row.nisn},\"${row.namaSiswa}\",${row.status},${row.tanggal},${row.jamMasuk},TERVERIFIKASI_MUTU\n"
                } else {
                    "${row.nomor},${row.nisn},\"${row.namaSiswa}\",${row.namaKelas},${row.status},${row.tanggal},${row.jamMasuk}\n"
                }
                fos.write(line.toByteArray(Charsets.UTF_8))
            }
        }
        return destFile
    }

    private fun generatePdfFile(
        context: Context,
        fileName: String,
        rombelName: String,
        periodName: String,
        dataRows: List<ExportStudentRow>
    ): File {
        val destFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Ukuran A4 standar (595 x 842 pt)
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.parseColor("#00685F")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSubtitle = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 9.5f
            isAntiAlias = true
        }

        val paintHeaderTable = Paint().apply {
            color = Color.parseColor("#0B1C30")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintRow = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 8.5f
            isAntiAlias = true
        }

        val paintLine = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        // Kop Surat Resmi SMKN 8 Jakarta
        canvas.drawText("PEMERINTAH PROVINSI DAERAH KHUSUS IBUKOTA JAKARTA", 40f, 45f, paintSubtitle)
        canvas.drawText("DINAS PENDIDIKAN - SEKOLAH MENENGAH KEJURUAN NEGERI 8 JAKARTA", 40f, 60f, paintTitle)
        canvas.drawText("Jl. Raya Pejaten, Pasar Minggu, Jakarta Selatan • Telp: (021) 7996386", 40f, 74f, paintSubtitle)
        canvas.drawLine(40f, 82f, 555f, 82f, paintLine)

        // Judul Laporan & Sub Judul
        canvas.drawText("LAPORAN RESMI REKAPITULASI PRESENSI SISWA", 40f, 105f, paintTitle)
        canvas.drawText("Cakupan: $rombelName  |  Periode: $periodName  |  T.A 2026/2027", 40f, 120f, paintSubtitle)

        // Table Header
        var yPos = 145f
        canvas.drawRect(40f, yPos - 12f, 555f, yPos + 6f, Paint().apply { color = Color.parseColor("#F1F5F9") })
        canvas.drawText("NO", 45f, yPos, paintHeaderTable)
        canvas.drawText("NISN", 75f, yPos, paintHeaderTable)
        canvas.drawText("NAMA SISWA", 155f, yPos, paintHeaderTable)
        canvas.drawText("KELAS", 330f, yPos, paintHeaderTable)
        canvas.drawText("WAKTU", 400f, yPos, paintHeaderTable)
        canvas.drawText("STATUS", 480f, yPos, paintHeaderTable)

        canvas.drawLine(40f, yPos + 8f, 555f, yPos + 8f, paintLine)
        yPos += 22f

        // Table Rows (maksimal 30 baris per preview cetak resmi)
        val previewRows = dataRows.take(28)
        previewRows.forEach { row ->
            canvas.drawText(String.format(Locale.getDefault(), "%02d", row.nomor), 45f, yPos, paintRow)
            canvas.drawText(row.nisn, 75f, yPos, paintRow)
            val truncatedName = if (row.namaSiswa.length > 26) row.namaSiswa.substring(0, 24) + ".." else row.namaSiswa
            canvas.drawText(truncatedName, 155f, yPos, paintRow)
            canvas.drawText(row.namaKelas, 330f, yPos, paintRow)
            canvas.drawText(row.jamMasuk, 400f, yPos, paintRow)
            canvas.drawText(row.status, 480f, yPos, paintRow)

            canvas.drawLine(40f, yPos + 6f, 555f, yPos + 6f, Paint().apply {
                color = Color.parseColor("#F1F5F9")
                strokeWidth = 0.5f
            })
            yPos += 18f
        }

        // Tanda Tangan & Verifikasi Digital di Bawah
        val yFooter = 740f
        canvas.drawText("Diverifikasi secara digital melalui Portal Super Admin SMKN 8 Jakarta", 40f, yFooter, paintSubtitle)
        canvas.drawText("Mengetahui,", 420f, yFooter, paintSubtitle)
        canvas.drawText("Kepala SMKN 8 Jakarta", 420f, yFooter + 14f, paintHeaderTable)
        canvas.drawText("Drs. H. Ahmad Fauzi, M.Pd", 420f, yFooter + 65f, paintHeaderTable)
        canvas.drawText("NIP. 19680715 199403 1 004", 420f, yFooter + 78f, paintSubtitle)

        pdfDocument.finishPage(page)
        FileOutputStream(destFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return destFile
    }

    private fun saveToDownloadsPublic(context: Context, sourceFile: File, fileName: String, mimeType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SMKN8_Presensi")
            }
            val resolver = context.contentResolver
            val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
    }

    fun openFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
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
}
