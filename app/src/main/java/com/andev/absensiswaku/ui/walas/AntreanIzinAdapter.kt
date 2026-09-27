package com.andev.absensiswaku.ui.walas

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.PengajuanIzinResponse
import com.andev.absensiswaku.databinding.ItemVerifikasiIzinBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AntreanIzinAdapter(
    private var items: MutableList<PengajuanIzinResponse>,
    private val onSetujuiClicked: (PengajuanIzinResponse) -> Unit,
    private val onTolakClicked: (PengajuanIzinResponse) -> Unit,
    private val onLihatSuratClicked: (PengajuanIzinResponse) -> Unit,
    private val onHubungiOrtuClicked: (PengajuanIzinResponse) -> Unit
) : RecyclerView.Adapter<AntreanIzinAdapter.ViewHolder>() {

    companion object {
        fun formatJamWib(isoString: String?): String {
            if (isoString.isNullOrEmpty()) return "Diajukan baru saja"
            return try {
                // Tangani format ISO 8601 dari Supabase (contoh: 2026-09-25T05:30:00+00:00)
                val cleanIso = isoString.substringBefore(".").substringBefore("+")
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val formatter = SimpleDateFormat("HH:mm 'WIB'", Locale.forLanguageTag("id-ID")).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                }
                val date = parser.parse(cleanIso) ?: Date()
                "Diajukan " + formatter.format(date)
            } catch (e: Exception) {
                "Diajukan hari ini"
            }
        }
    }

    fun updateData(newItems: List<PengajuanIzinResponse>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun removeItem(item: PengajuanIzinResponse) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
            notifyItemRangeChanged(index, items.size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVerifikasiIzinBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemVerifikasiIzinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PengajuanIzinResponse, posNumber: Int) {
            val ctx = binding.root.context
            val jenis = item.jenisIzin?.trim().orEmpty().ifEmpty { "Izin" }

            // 1. Nomor Urut format 2 digit (01, 02, ...)
            val displayPos = String.format(java.util.Locale.getDefault(), "%02d", posNumber)
            binding.tvNomorUrut.text = displayPos

            // 2. Identitas Siswa
            val nama = item.siswa?.namaLengkap?.takeIf { it.isNotEmpty() }
                ?: "Siswa ${item.siswaId}"

            val nisn = item.siswa?.nisn?.takeIf { it.isNotEmpty() } ?: "-"
            val waktuDiajukan = formatJamWib(item.createdAt)
            binding.tvNamaSiswa.text = nama

            // 3. Styling Berdasarkan Jenis Status/Izin (Sakit, Izin, Dispensasi, Alpa)
            when (jenis.uppercase()) {
                "SAKIT" -> {
                    binding.viewAccentStripe.setBackgroundResource(R.drawable.bg_stripe_sakit)
                    binding.frameNomorUrut.setBackgroundResource(R.drawable.bg_circle_num_red)
                    binding.tvNomorUrut.setTextColor(ContextCompat.getColor(ctx, R.color.badge_alpa_text))
                    binding.tvBadgeJenisIzin.text = "Sakit"
                    binding.tvBadgeJenisIzin.setBackgroundResource(R.drawable.bg_badge_pill_sakit)
                    binding.tvNisnWaktu.text = "NISN: $nisn • $waktuDiajukan"

                    binding.layoutAlasanDanBerkas.visibility = View.VISIBLE
                    binding.layoutActionButtons.visibility = View.VISIBLE
                    binding.btnHubungiOrtu.visibility = View.GONE
                    binding.tvLabelSetujui.text = "✓ Setujui Sakit"
                }
                "IZIN" -> {
                    binding.viewAccentStripe.setBackgroundResource(R.drawable.bg_stripe_izin)
                    binding.frameNomorUrut.setBackgroundResource(R.drawable.bg_circle_num_amber)
                    binding.tvNomorUrut.setTextColor(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))
                    binding.tvBadgeJenisIzin.text = "Izin"
                    binding.tvBadgeJenisIzin.setBackgroundResource(R.drawable.bg_badge_pill_amber)
                    binding.tvNisnWaktu.text = "NISN: $nisn • $waktuDiajukan"

                    binding.layoutAlasanDanBerkas.visibility = View.VISIBLE
                    binding.layoutActionButtons.visibility = View.VISIBLE
                    binding.btnHubungiOrtu.visibility = View.GONE
                    binding.tvLabelSetujui.text = "✓ Setujui Izin"
                }
                "DISPENSASI" -> {
                    binding.viewAccentStripe.setBackgroundResource(R.drawable.bg_stripe_dispensasi)
                    binding.frameNomorUrut.setBackgroundResource(R.drawable.bg_circle_num_blue)
                    binding.tvNomorUrut.setTextColor(ContextCompat.getColor(ctx, R.color.badge_izin_text))
                    binding.tvBadgeJenisIzin.text = "Dispensasi"
                    binding.tvBadgeJenisIzin.setBackgroundResource(R.drawable.bg_badge_pill_dispensasi)
                    binding.tvNisnWaktu.text = "NISN: $nisn • $waktuDiajukan"

                    binding.layoutAlasanDanBerkas.visibility = View.VISIBLE
                    binding.layoutActionButtons.visibility = View.VISIBLE
                    binding.btnHubungiOrtu.visibility = View.GONE
                    binding.tvLabelSetujui.text = "✓ Setujui Dispensasi"
                }
                else -> {
                    // ALPA / Belum Scan
                    binding.viewAccentStripe.setBackgroundResource(R.drawable.bg_stripe_alpa)
                    binding.frameNomorUrut.setBackgroundResource(R.drawable.bg_circle_num_slate)
                    binding.tvNomorUrut.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                    binding.tvBadgeJenisIzin.text = "Alpa"
                    binding.tvBadgeJenisIzin.setBackgroundResource(R.drawable.bg_badge_pill_alpa)
                    binding.tvNisnWaktu.text = "NISN: $nisn • Belum scan hingga 07:30"

                    binding.layoutAlasanDanBerkas.visibility = View.GONE
                    binding.layoutActionButtons.visibility = View.GONE
                    binding.btnHubungiOrtu.visibility = View.VISIBLE
                }
            }

            // 4. Keterangan Alasan
            val quote = item.keterangan.orEmpty().ifEmpty { "Tidak ada keterangan tambahan." }
            binding.tvKeteranganAlasan.text = "“$quote”"

            // 5. Lampiran Berkas Bukti
            val berkasRaw = item.buktiBerkasUrl.orEmpty()
            val fileName = if (berkasRaw.startsWith("data:image") || berkasRaw.length > 500) {
                "Foto_Bukti_Fisik_${jenis.lowercase().replaceFirstChar { it.uppercase() }}.jpg"
            } else {
                berkasRaw.substringAfterLast("/").takeIf { it.isNotEmpty() }
                    ?: if (jenis.equals("SAKIT", true)) "Surat_Keterangan_Sakit.pdf"
                    else if (jenis.equals("IZIN", true)) "Surat_Permohonan_Izin.jpg"
                    else if (jenis.equals("DISPENSASI", true)) "Surat_Tugas_Kesiswaan.pdf"
                    else null
            }

            if (!fileName.isNullOrEmpty() && !jenis.equals("ALPA", true)) {
                binding.layoutAttachment.visibility = View.VISIBLE
                binding.tvNamaBerkas.text = fileName
            } else {
                binding.layoutAttachment.visibility = View.GONE
            }

            // 6. Action Clicks
            binding.btnSetujui.setOnClickListener {
                onSetujuiClicked(item)
            }

            binding.btnTolak.setOnClickListener {
                onTolakClicked(item)
            }

            binding.btnLihatSurat.setOnClickListener {
                onLihatSuratClicked(item)
            }

            binding.layoutAttachment.setOnClickListener {
                onLihatSuratClicked(item)
            }

            binding.btnHubungiOrtu.setOnClickListener {
                onHubungiOrtuClicked(item)
            }
        }
    }
}
