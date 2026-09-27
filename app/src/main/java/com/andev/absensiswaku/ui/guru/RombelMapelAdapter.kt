package com.andev.absensiswaku.ui.guru

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.databinding.ItemRombelMapelBinding
import java.util.Locale

data class RombelMapelItem(
    val id: Int,
    val namaKelas: String,
    val jurusan: String,
    val namaWalas: String,
    val kapasitas: Int = 36,
    val hadirCount: Int = 0,
    val izinCount: Int = 0,
    val alpaCount: Int = 0,
    val persentaseHadir: Float = 0f
)

class RombelMapelAdapter(
    private var items: List<RombelMapelItem> = emptyList(),
    private val onLihatSiswaClick: (RombelMapelItem) -> Unit
) : RecyclerView.Adapter<RombelMapelAdapter.ViewHolder>() {

    fun updateData(newItems: List<RombelMapelItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRombelMapelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemRombelMapelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RombelMapelItem) {
            val ctx = binding.root.context

            binding.tvNamaKelas.text = item.namaKelas
            binding.tvNamaWalas.text = "Wali Kelas: ${item.namaWalas}"
            binding.tvKuotaHadir.text = "${item.hadirCount} / ${item.kapasitas} Siswa"

            // Persentase badge
            binding.tvBadgePersentase.text = String.format(Locale.US, "%.1f%% Hadir", item.persentaseHadir)
            when {
                item.persentaseHadir >= 95f -> {
                    binding.tvBadgePersentase.setBackgroundResource(R.drawable.bg_badge_hadir)
                    binding.tvBadgePersentase.setTextColor(ContextCompat.getColor(ctx, R.color.badge_hadir_text))
                }
                item.persentaseHadir >= 80f -> {
                    binding.tvBadgePersentase.setBackgroundResource(R.drawable.bg_badge_terlambat)
                    binding.tvBadgePersentase.setTextColor(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))
                }
                else -> {
                    binding.tvBadgePersentase.setBackgroundResource(R.drawable.bg_badge_alpa)
                    binding.tvBadgePersentase.setTextColor(ContextCompat.getColor(ctx, R.color.badge_alpa_text))
                }
            }

            // Progress bar
            binding.progressKehadiran.progress = item.persentaseHadir.toInt().coerceIn(0, 100)

            // Rincian mini
            binding.tvCountHadir.text = "${item.hadirCount} Hadir"
            binding.tvCountIzin.text = "${item.izinCount} Sakit/Izin"
            binding.tvCountAlpa.text = "${item.alpaCount} Alpa"

            binding.btnLihatSiswa.setOnClickListener {
                onLihatSiswaClick(item)
            }

            binding.root.setOnClickListener {
                onLihatSiswaClick(item)
            }
        }
    }
}
