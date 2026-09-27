package com.andev.absensiswaku.ui.guru

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.databinding.ItemSiswaRombelReadonlyBinding

data class SiswaReadOnlyItem(
    val id: Int,
    val namaLengkap: String,
    val nisn: String,
    val waktuMasuk: String,
    val status: String // "HADIR", "TERLAMBAT", "IZIN", "SAKIT", "DISPENSASI", "ALPA"
) {
    val initialLetters: String
        get() {
            val name = namaLengkap.trim()
            if (name.isEmpty()) return "SW"
            val parts = name.split(" ").filter { it.isNotEmpty() }
            return when {
                parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
                parts.size == 1 && parts[0].length >= 2 -> parts[0].substring(0, 2).uppercase()
                parts.size == 1 -> parts[0].uppercase()
                else -> "SW"
            }
        }
}

class SiswaReadOnlyAdapter(
    private var items: List<SiswaReadOnlyItem> = emptyList()
) : RecyclerView.Adapter<SiswaReadOnlyAdapter.ViewHolder>() {

    private var fullList: List<SiswaReadOnlyItem> = emptyList()

    fun updateData(newItems: List<SiswaReadOnlyItem>) {
        this.fullList = newItems
        this.items = newItems
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.lowercase().trim()
        items = if (q.isEmpty()) {
            fullList
        } else {
            fullList.filter {
                it.namaLengkap.lowercase().contains(q) || it.nisn.lowercase().contains(q)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSiswaRombelReadonlyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemSiswaRombelReadonlyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SiswaReadOnlyItem) {
            val ctx = binding.root.context

            binding.tvNamaSiswa.text = item.namaLengkap
            binding.tvAvatarInitials.text = item.initialLetters

            val waktuTeks = if (item.waktuMasuk.isNotEmpty() && item.waktuMasuk != "-") {
                item.waktuMasuk
            } else if (item.status.equals("HADIR", true)) {
                "06:45 WIB"
            } else if (item.status.equals("TERLAMBAT", true)) {
                "07:05 WIB"
            } else {
                "Belum Scan"
            }
            binding.tvNisnWaktu.text = "NISN: ${item.nisn} • $waktuTeks"

            // Styling status badge
            when (item.status.uppercase()) {
                "HADIR", "TEPAT_WAKTU" -> {
                    binding.tvBadgeStatusSiswa.text = "Hadir"
                    binding.tvBadgeStatusSiswa.setBackgroundResource(R.drawable.bg_badge_hadir)
                    binding.tvBadgeStatusSiswa.setTextColor(ContextCompat.getColor(ctx, R.color.badge_hadir_text))
                }
                "TERLAMBAT" -> {
                    binding.tvBadgeStatusSiswa.text = "Terlambat"
                    binding.tvBadgeStatusSiswa.setBackgroundResource(R.drawable.bg_badge_terlambat)
                    binding.tvBadgeStatusSiswa.setTextColor(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))
                }
                "IZIN" -> {
                    binding.tvBadgeStatusSiswa.text = "Izin"
                    binding.tvBadgeStatusSiswa.setBackgroundResource(R.drawable.bg_badge_izin)
                    binding.tvBadgeStatusSiswa.setTextColor(ContextCompat.getColor(ctx, R.color.badge_izin_text))
                }
                "SAKIT" -> {
                    binding.tvBadgeStatusSiswa.text = "Sakit"
                    binding.tvBadgeStatusSiswa.setBackgroundResource(R.drawable.bg_badge_pill_sakit)
                    binding.tvBadgeStatusSiswa.setTextColor(ContextCompat.getColor(ctx, R.color.badge_alpa_text))
                }
                "DISPENSASI" -> {
                    binding.tvBadgeStatusSiswa.text = "Dispensasi"
                    binding.tvBadgeStatusSiswa.setBackgroundResource(R.drawable.bg_badge_izin)
                    binding.tvBadgeStatusSiswa.setTextColor(ContextCompat.getColor(ctx, R.color.badge_izin_text))
                }
                else -> {
                    binding.tvBadgeStatusSiswa.text = "Alpa"
                    binding.tvBadgeStatusSiswa.setBackgroundResource(R.drawable.bg_badge_alpa)
                    binding.tvBadgeStatusSiswa.setTextColor(ContextCompat.getColor(ctx, R.color.badge_alpa_text))
                }
            }
        }
    }
}
