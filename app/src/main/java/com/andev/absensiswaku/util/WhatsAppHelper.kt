package com.andev.absensiswaku.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URLEncoder

/**
 * Helper komunikasi WhatsApp resmi untuk Wali Kelas dan Pengajar SMKN 8 Jakarta.
 * Mempermudah pelaporan status kehadiran (Terlambat, Alpa, Izin) langsung ke wali murid.
 */
object WhatsAppHelper {

    /**
     * Menormalisasi nomor telepon lokal Indonesia ke format internasional E.164 (tanpa tanda '+')
     */
    fun formatToInternational(rawPhone: String): String {
        val digitsOnly = rawPhone.replace(Regex("[^0-9]"), "")
        return when {
            digitsOnly.startsWith("0") -> "62" + digitsOnly.substring(1)
            digitsOnly.startsWith("62") -> digitsOnly
            digitsOnly.startsWith("8") -> "62$digitsOnly"
            else -> digitsOnly
        }
    }

    /**
     * Membuka aplikasi WhatsApp dengan nomor dan draf pesan terstandar
     */
    fun kirimPesanWhatsApp(context: Context, rawPhone: String, pesan: String): Boolean {
        val targetPhone = formatToInternational(rawPhone)
        if (targetPhone.length < 9) {
            Toast.makeText(context, "Nomor WhatsApp tidak valid ($rawPhone)", Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val encodedMessage = URLEncoder.encode(pesan, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$targetPhone&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            // Prioritaskan membuka via aplikasi resmi WhatsApp
            intent.setPackage("com.whatsapp")
            try {
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                // Fallback: Buka melalui peramban atau klien web
                val webIntent = Intent(Intent.ACTION_VIEW, uri)
                webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(webIntent)
                true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka WhatsApp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Menampilkan dialog interaktif bagi Wali Kelas untuk menghubungi wali murid
     */
    fun tampilkanDialogHubungiOrtu(
        context: Context,
        namaSiswa: String,
        namaKelas: String,
        statusKehadiran: String,
        keteranganWaktu: String,
        noWaOrtu: String?
    ) {
        val waNumber = noWaOrtu?.trim().orEmpty()
        val formattedWa = if (waNumber.isNotBlank()) waNumber else "Belum didaftarkan"

        val pesanDraft = when (statusKehadiran.uppercase()) {
            "TERLAMBAT" -> "Yth. Bapak/Ibu Wali Murid dari $namaSiswa ($namaKelas),\n\nKami menginformasikan bahwa siswa hadir terlambat di SMKN 8 Jakarta pada hari ini ($keteranganWaktu).\n\nMohon perhatian dan kerjasamanya agar siswa dapat hadir tepat waktu sebelum pukul 06:40 WIB.\n\nTerima kasih.\n- Wali Kelas SMKN 8 Jakarta"
            "ALPA" -> "Yth. Bapak/Ibu Wali Murid dari $namaSiswa ($namaKelas),\n\nKami menginformasikan bahwa hingga saat ini siswa belum tercatat hadir di SMKN 8 Jakarta (Alpa/Tanpa Keterangan).\n\nMohon konfirmasi terkait keberadaan dan kondisi siswa. Terima kasih.\n- Wali Kelas SMKN 8 Jakarta"
            "IZIN", "SAKIT" -> "Yth. Bapak/Ibu Wali Murid dari $namaSiswa ($namaKelas),\n\nKami mengonfirmasi bahwa pengajuan $statusKehadiran ($keteranganWaktu) untuk siswa telah tercatat di sistem presensi SMKN 8 Jakarta.\n\nTerima kasih.\n- Wali Kelas SMKN 8 Jakarta"
            else -> "Yth. Bapak/Ibu Wali Murid dari $namaSiswa ($namaKelas),\n\nKami menginformasikan catatan kehadiran siswa di SMKN 8 Jakarta hari ini berstatus: $statusKehadiran ($keteranganWaktu).\n\nTerima kasih.\n- Wali Kelas SMKN 8 Jakarta"
        }

        val dialogBuilder = MaterialAlertDialogBuilder(context)
            .setTitle("Hubungi Wali Murid")
            .setMessage("Siswa: $namaSiswa\nKelas: $namaKelas\nStatus: $statusKehadiran\nNo. WhatsApp: $formattedWa\n\nKirimkan konfirmasi laporan presensi langsung ke WhatsApp orang tua?")
            .setNegativeButton("Batal") { d, _ -> d.dismiss() }

        if (waNumber.isNotBlank()) {
            dialogBuilder.setPositiveButton("Kirim WhatsApp") { d, _ ->
                d.dismiss()
                kirimPesanWhatsApp(context, waNumber, pesanDraft)
            }
        } else {
            dialogBuilder.setPositiveButton("Tutup") { d, _ -> d.dismiss() }
        }

        dialogBuilder.show()
    }
}
