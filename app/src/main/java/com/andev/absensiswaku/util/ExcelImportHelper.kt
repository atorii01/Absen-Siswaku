package com.andev.absensiswaku.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.andev.absensiswaku.data.network.SupabaseClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class SiswaImportItem(
    val nisn: String,
    val nik: String? = null,
    val namaLengkap: String,
    val jenisKelamin: String? = "L",
    val noWaOrtu: String? = null
)

object ExcelImportHelper {

    const val TEMPLATE_FILE_NAME = "Template_Impor_Siswa_SMKN8.csv"

    /**
     * Membuat berkas template CSV terstandar dengan UTF-8 BOM (\uFEFF) dan format formula teks (="nilai")
     * agar aplikasi spreadsheet mobile tidak menghapus leading zero pada NISN/WA dan tidak
     * mengubah NIK 16 digit menjadi notasi ilmiah (scientific notation).
     */
    fun buatTemplateCsv(context: Context): Uri? {
        val fileName = TEMPLATE_FILE_NAME

        // Gunakan UTF-8 BOM (\uFEFF) di awal agar Excel otomatis mengenali encoding dan kolom
        val csvBuilder = StringBuilder()
        csvBuilder.append('\uFEFF')

        // Header Kolom
        csvBuilder.append("\"NISN\",\"NIK\",\"NAMA_LENGKAP\",\"JENIS_KELAMIN\",\"NO_WA_ORTU\"\n")

        // Baris Contoh 1 (Laki-laki) - Gunakan format formula ="..." agar dibaca sebagai teks utuh
        csvBuilder.append("=\"0081234567\",=\"3174010101080001\",\"PETER MALEKE\",\"L\",=\"081234567890\"\n")

        // Baris Contoh 2 (Perempuan)
        csvBuilder.append("=\"0087654321\",=\"3174010202080002\",\"RATASYA ANDHINI\",\"P\",=\"085712345678\"\n")

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    }
                    Toast.makeText(context, "Template berhasil disimpan di folder Download!", Toast.LENGTH_LONG).show()
                } ?: run {
                    Toast.makeText(context, "Gagal membuat berkas template di folder Download.", Toast.LENGTH_SHORT).show()
                }
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                file.writeText(csvBuilder.toString(), Charsets.UTF_8)
                Toast.makeText(context, "Template berhasil disimpan di folder Download!", Toast.LENGTH_LONG).show()
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal menyimpan template: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Alias fungsi pengunduhan template Excel/CSV.
     */
    fun unduhTemplateExcelCsv(context: Context): Uri? = buatTemplateCsv(context)

    /**
     * Kompatibilitas Boolean helper untuk pemanggilan legacy.
     */
    fun downloadTemplateCsv(context: Context): Boolean = unduhTemplateExcelCsv(context) != null

    /**
     * Membersihkan karakter format teks formula spreadsheet: =", ", ', dan spasi liar
     * agar data bersih saat dikirim ke Supabase.
     */
    fun bersihkanNilaiCsv(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("=")) {
            text = text.removePrefix("=").trim()
        }
        while ((text.startsWith("\"") && text.endsWith("\"")) ||
            (text.startsWith("'") && text.endsWith("'"))
        ) {
            if (text.length >= 2) {
                text = text.substring(1, text.length - 1).trim()
                if (text.startsWith("=")) {
                    text = text.removePrefix("=").trim()
                }
            } else {
                break
            }
        }
        return text
            .removePrefix("=")
            .removePrefix("\"")
            .removeSuffix("\"")
            .removePrefix("'")
            .removeSuffix("'")
            .trim()
    }

    /**
     * Membaca dan mem-parsing isi berkas CSV dari Uri.
     * Menggunakan regex delimiter lookahead untuk memisahkan kolom secara tepat tanpa merusak koma dalam tanda kutip.
     */
    fun parseCsv(context: Context, uri: Uri): List<SiswaImportItem> {
        val result = mutableListOf<SiswaImportItem>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val lines = reader.readLines()
            reader.close()
            inputStream.close()

            if (lines.isEmpty()) return emptyList()

            // Deteksi pemisah default (koma atau titik-koma untuk format Excel Indonesia)
            val firstLineClean = lines[0].trim().replace("\uFEFF", "")
            val useSemicolon = firstLineClean.count { it == ';' } > firstLineClean.count { it == ',' }
            val delimiterRegex = if (useSemicolon) {
                ";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()
            } else {
                ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()
            }

            var startIndex = 0
            var nisnCol = 0
            var nikCol = 1
            var namaCol = 2
            var jkCol = 3
            var waCol = 4

            if (firstLineClean.contains("NISN", ignoreCase = true) || firstLineClean.contains("NAMA", ignoreCase = true)) {
                startIndex = 1
                val headers = firstLineClean.split(delimiterRegex).map { bersihkanNilaiCsv(it).uppercase() }
                val foundNisn = headers.indexOfFirst { it.contains("NISN") }
                val foundNik = headers.indexOfFirst { it.contains("NIK") }
                val foundNama = headers.indexOfFirst { it.contains("NAMA") }
                val foundJk = headers.indexOfFirst { it.contains("KELAMIN") || it == "JK" }
                val foundWa = headers.indexOfFirst { it.contains("WA") || it.contains("HP") || it.contains("TELEPON") || it.contains("ORTU") }

                if (foundNisn != -1) nisnCol = foundNisn
                if (foundNik != -1) nikCol = foundNik
                if (foundNama != -1) namaCol = foundNama
                if (foundJk != -1) jkCol = foundJk
                if (foundWa != -1) waCol = foundWa
            }

            for (i in startIndex until lines.size) {
                val baris = lines[i].trim().replace("\uFEFF", "")
                if (baris.isBlank()) continue

                val kolom = baris.split(delimiterRegex)
                if (kolom.size >= 3) {
                    val nisn = if (kolom.size > nisnCol) bersihkanNilaiCsv(kolom[nisnCol]) else bersihkanNilaiCsv(kolom[0])
                    val nik = if (kolom.size > nikCol && nikCol != -1) bersihkanNilaiCsv(kolom[nikCol]).ifEmpty { null } else null
                    val nama = if (kolom.size > namaCol && namaCol != -1) bersihkanNilaiCsv(kolom[namaCol]) else ""
                    val rawJk = if (kolom.size > jkCol && jkCol != -1) bersihkanNilaiCsv(kolom[jkCol]) else "L"
                    val jenisKelamin = if (rawJk.startsWith("P", ignoreCase = true)) "P" else "L"
                    val noWa = if (kolom.size > waCol && waCol != -1) bersihkanNilaiCsv(kolom[waCol]).ifEmpty { null } else null

                    if (nisn.isNotEmpty() && nama.isNotEmpty()) {
                        result.add(
                            SiswaImportItem(
                                nisn = nisn,
                                nik = nik,
                                namaLengkap = nama,
                                jenisKelamin = jenisKelamin,
                                noWaOrtu = noWa
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * Alias proses parsing berkas Excel/CSV.
     */
    fun prosesFileExcelCsv(context: Context, uri: Uri): List<SiswaImportItem> = parseCsv(context, uri)

    /**
     * Mengirim payload siswa ke Supabase /siswa dengan id_kelas rombel aktif.
     * Menggunakan batch POST dengan fallback sequential jika batch terkendala.
     */
    fun uploadSiswaBatch(
        idKelas: Int,
        siswaList: List<SiswaImportItem>,
        onSuccess: (count: Int) -> Unit,
        onError: (message: String) -> Unit
    ) {
        if (siswaList.isEmpty()) {
            onError("Tidak ada data siswa untuk diunggah.")
            return
        }

        val batchPayload = siswaList.map { item ->
            val map = mutableMapOf<String, Any>(
                "nisn" to item.nisn,
                "nama_lengkap" to item.namaLengkap,
                "id_kelas" to idKelas,
                "pin_presensi" to "123456"
            )
            item.nik?.let { map["nik"] = it }
            item.jenisKelamin?.let { map["jenis_kelamin"] = it }
            item.noWaOrtu?.let {
                map["no_wa_orang_tua"] = it
                map["no_whatsapp_wali"] = it
            }
            map
        }

        // Coba kirim batch POST langsung
        SupabaseClient.instance.tambahSiswaBatch(batchPayload)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        onSuccess(siswaList.size)
                    } else {
                        // Jika batch gagal (misal duplikasi satu NISN), lakukan upload baris per baris
                        uploadSequentialFallback(idKelas, siswaList, onSuccess, onError)
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    uploadSequentialFallback(idKelas, siswaList, onSuccess, onError)
                }
            })
    }

    private fun uploadSequentialFallback(
        idKelas: Int,
        siswaList: List<SiswaImportItem>,
        onSuccess: (count: Int) -> Unit,
        onError: (message: String) -> Unit
    ) {
        var successCount = 0
        var processedCount = 0
        val total = siswaList.size

        for (item in siswaList) {
            val singlePayload = mutableMapOf<String, Any>(
                "nisn" to item.nisn,
                "nama_lengkap" to item.namaLengkap,
                "id_kelas" to idKelas,
                "pin_presensi" to "123456"
            )
            item.nik?.let { singlePayload["nik"] = it }
            item.jenisKelamin?.let { singlePayload["jenis_kelamin"] = it }
            item.noWaOrtu?.let {
                singlePayload["no_wa_orang_tua"] = it
                singlePayload["no_whatsapp_wali"] = it
            }

            SupabaseClient.instance.tambahSiswa(singlePayload)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) successCount++
                        processedCount++
                        if (processedCount == total) {
                            if (successCount > 0) onSuccess(successCount)
                            else onError("Gagal mengimpor siswa. Periksa apakah NISN sudah terdaftar.")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        processedCount++
                        if (processedCount == total) {
                            if (successCount > 0) onSuccess(successCount)
                            else onError(t.localizedMessage ?: "Koneksi gagal.")
                        }
                    }
                })
        }
    }
}
