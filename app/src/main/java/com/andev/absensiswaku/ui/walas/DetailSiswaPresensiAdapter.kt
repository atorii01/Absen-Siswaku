package com.andev.absensiswaku.ui.walas

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.databinding.ItemDetailSiswaPresensiBinding

data class SiswaDetailPresensiItem(
    val nomorUrut: String = "",
    val siswaId: Int? = null,
    val namaLengkap: String = "",
    val inisial: String = "",
    val nisn: String = "-",
    val subteks: String = "",
    val status: String = "HADIR", // "HADIR", "TERLAMBAT", "IZIN", "SAKIT", "ALPA"
    val badgeLabel: String = "Hadir",
    val badgeBgResId: Int = R.drawable.bg_badge_hadir,
    val badgeTextColor: Int = Color.parseColor("#15803D")
)

class DetailSiswaPresensiAdapter(
    private var allItems: List<SiswaDetailPresensiItem> = emptyList(),
    private val onItemClick: ((SiswaDetailPresensiItem) -> Unit)? = null
) : RecyclerView.Adapter<DetailSiswaPresensiAdapter.ViewHolder>() {

    private var displayedItems: List<SiswaDetailPresensiItem> = allItems
    private var currentFilterCategory: String = "SEMUA"

    fun setAllData(newList: List<SiswaDetailPresensiItem>) {
        allItems = newList
        applyFilter()
    }

    fun filter(statusCategory: String) {
        currentFilterCategory = statusCategory
        applyFilter()
    }

    private fun applyFilter() {
        displayedItems = when (currentFilterCategory.uppercase()) {
            "HADIR" -> allItems.filter {
                it.status.equals("HADIR", true) ||
                it.status.equals("TERLAMBAT", true) ||
                it.status.equals("TEPAT_WAKTU", true)
            }
            "IZIN", "SAKIT", "IZIN/SAKIT" -> allItems.filter {
                it.status.equals("IZIN", true) || it.status.equals("SAKIT", true)
            }
            "ALPA" -> allItems.filter {
                it.status.equals("ALPA", true)
            }
            else -> allItems
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetailSiswaPresensiBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(displayedItems[position])
    }

    override fun getItemCount(): Int = displayedItems.size

    inner class ViewHolder(private val binding: ItemDetailSiswaPresensiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SiswaDetailPresensiItem) {
            val ctx = binding.root.context

            binding.tvInisialSiswa.text = item.inisial.ifBlank { item.nomorUrut }
            binding.tvNamaSiswa.text = item.namaLengkap
            binding.tvSubteksSiswa.text = item.subteks

            binding.tvBadgeStatus.text = item.badgeLabel
            binding.tvBadgeStatus.setBackgroundResource(item.badgeBgResId)
            binding.tvBadgeStatus.setTextColor(item.badgeTextColor)

            // Atur warna lingkaran avatar sesuai status
            when (item.status.uppercase()) {
                "HADIR" -> {
                    binding.layoutAvatarSiswa.setBackgroundResource(R.drawable.bg_avatar_circle_mint)
                    binding.tvInisialSiswa.setTextColor(ContextCompat.getColor(ctx, R.color.primary_teal))
                }
                "TERLAMBAT" -> {
                    binding.layoutAvatarSiswa.setBackgroundResource(R.drawable.bg_avatar_circle_amber)
                    binding.tvInisialSiswa.setTextColor(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))
                }
                "IZIN", "SAKIT" -> {
                    binding.layoutAvatarSiswa.setBackgroundResource(R.drawable.bg_avatar_circle_blue)
                    binding.tvInisialSiswa.setTextColor(ContextCompat.getColor(ctx, R.color.badge_izin_text))
                }
                else -> {
                    binding.layoutAvatarSiswa.setBackgroundResource(R.drawable.bg_avatar_circle_purple)
                    binding.tvInisialSiswa.setTextColor(ContextCompat.getColor(ctx, R.color.badge_alpa_text))
                }
            }

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}
