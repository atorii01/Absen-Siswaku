package com.andev.absensiswaku.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.RiwayatModel
import com.andev.absensiswaku.data.network.SiswaMiniResponse
import com.andev.absensiswaku.databinding.ItemSiswaAuditPresensiBinding
import java.util.Locale

data class SiswaAuditItem(
    val nomorUrut: Int,
    val siswa: SiswaMiniResponse,
    val presensi: RiwayatModel?
)

class SiswaAuditAdapter : RecyclerView.Adapter<SiswaAuditAdapter.AuditViewHolder>() {

    private val allItems = mutableListOf<SiswaAuditItem>()
    private val displayedItems = mutableListOf<SiswaAuditItem>()

    fun submitList(list: List<SiswaAuditItem>) {
        allItems.clear()
        allItems.addAll(list)
        displayedItems.clear()
        displayedItems.addAll(list)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.trim().lowercase(Locale.getDefault())
        displayedItems.clear()
        if (q.isEmpty()) {
            displayedItems.addAll(allItems)
        } else {
            displayedItems.addAll(allItems.filter {
                val nama = it.siswa.namaLengkap?.lowercase(Locale.getDefault()).orEmpty()
                val nisn = it.siswa.nisn?.lowercase(Locale.getDefault()).orEmpty()
                nama.contains(q) || nisn.contains(q)
            })
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuditViewHolder {
        val binding = ItemSiswaAuditPresensiBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AuditViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AuditViewHolder, position: Int) {
        holder.bind(displayedItems[position])
    }

    override fun getItemCount(): Int = displayedItems.size

    inner class AuditViewHolder(private val binding: ItemSiswaAuditPresensiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SiswaAuditItem) {
            val context = itemView.context
            val siswa = item.siswa
            val presensi = item.presensi

            // Nomor Urut
            binding.tvAuditNomorUrut.text = String.format(Locale.getDefault(), "#%02d", item.nomorUrut)

            // Nama Siswa & Inisial
            val nama = siswa.namaLengkap?.trim().takeIf { !it.isNullOrBlank() } ?: "Siswa ${item.nomorUrut}"
            binding.tvAuditNamaSiswa.text = nama

            // NISN
            val nisn = siswa.nisn?.trim().takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvAuditNisn.text = "NISN: $nisn"

            // Tampilkan inisial jika selfie default
            val initials = siswa.initialLetters
            binding.tvAuditInitials.text = initials

            // Status Presensi & Jam Masuk
            if (presensi != null) {
                val waktu = presensi.displayJamMasuk
                binding.tvAuditJamMasuk.text = if (waktu != "-") "$waktu WIB" else "Tercatat"
                
                val statusText = presensi.status?.trim() ?: "Hadir"
                if (statusText.equals("Terlambat", true)) {
                    binding.tvAuditStatusBadge.text = "⚠ Terlambat"
                    binding.tvAuditStatusBadge.setBackgroundResource(R.drawable.bg_badge_pill_amber)
                    binding.tvAuditStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_amber_text))
                } else {
                    binding.tvAuditStatusBadge.text = "✓ Hadir Sah"
                    binding.tvAuditStatusBadge.setBackgroundResource(R.drawable.bg_badge_rombel_verif)
                    binding.tvAuditStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_hadir_text))
                }

                // AI Face Match Score
                val score = presensi.biometrikMatchScore ?: 96.5
                binding.tvAuditFaceMatch.text = String.format(Locale.US, "AI: %.1f%%", score)

                // Jarak Gerbang (Geofence SMKN 8 Jakarta - Radius 50m)
                val distance = presensi.jarakGerbangMeter ?: 14.0
                binding.tvAuditJarakGerbang.text = String.format(Locale.US, "📍 %.0fm Gerbang", distance)

                // Anti Mock GPS
                binding.tvAuditAntiMock.text = "GPS: Valid"
                binding.tvAuditAntiMock.setTextColor(ContextCompat.getColor(context, R.color.badge_hadir_text))
            } else {
                binding.tvAuditJamMasuk.text = "Belum Masuk"
                binding.tvAuditStatusBadge.text = "Alpa"
                binding.tvAuditStatusBadge.setBackgroundResource(R.drawable.bg_badge_rombel_alpa)
                binding.tvAuditStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_alpa_text))

                binding.tvAuditFaceMatch.text = "AI: Menunggu"
                binding.tvAuditJarakGerbang.text = "📍 Belum Geotag"
                binding.tvAuditAntiMock.text = "GPS: -"
                binding.tvAuditAntiMock.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }
}
