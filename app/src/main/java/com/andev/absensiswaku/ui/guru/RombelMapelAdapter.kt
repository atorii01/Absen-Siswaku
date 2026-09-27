package com.andev.absensiswaku.ui.guru

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.RombelMapelResponse
import com.andev.absensiswaku.databinding.ItemRombelMapelBinding
import java.util.Locale

class RombelMapelAdapter(
    private var items: List<RombelMapelResponse> = emptyList(),
    private val onLihatSiswaClick: (RombelMapelResponse) -> Unit
) : RecyclerView.Adapter<RombelMapelAdapter.ViewHolder>() {

    fun submitList(newItems: List<RombelMapelResponse>) {
        this.items = ArrayList(newItems)
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

        fun bind(item: RombelMapelResponse) {
            val ctx = binding.root.context
            val kuota = if (item.id == 8 || item.id == 10) 35 else 36

            // 1. Judul Card: nama_kelas (cth: "XII AKL 1", "XII RPL")
            binding.tvNamaKelas.text = item.namaKelas

            // 2. Wali Kelas: relasi users!wali_kelas_id
            binding.tvWaliKelas.text = "Wali Kelas: ${item.waliKelas?.namaLengkap ?: "-"}"

            // 3. Kuota Siswa: "{hadir} / {kuota} Siswa" (jika 0 hadir: "0 / 35 Siswa" atau "0 / 36 Siswa")
            binding.tvKapasitasSiswa.text = "${item.hadirCount} / $kuota Siswa"

            // 4. Badge Persentase & LinearProgressIndicator
            val percentText = if (item.persentaseHadir >= 100f && item.hadirCount > 0) {
                "100% Lengkap"
            } else {
                String.format(Locale.US, "%.1f%% Hadir", item.persentaseHadir)
            }
            binding.tvBadgePersentase.text = percentText

            when {
                item.persentaseHadir >= 95f && item.hadirCount > 0 -> {
                    binding.tvBadgePersentase.setBackgroundResource(R.drawable.bg_badge_hadir)
                    binding.tvBadgePersentase.setTextColor(ContextCompat.getColor(ctx, R.color.badge_hadir_text))
                }
                item.persentaseHadir >= 80f && item.hadirCount > 0 -> {
                    binding.tvBadgePersentase.setBackgroundResource(R.drawable.bg_badge_terlambat)
                    binding.tvBadgePersentase.setTextColor(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))
                }
                else -> {
                    binding.tvBadgePersentase.setBackgroundResource(R.drawable.bg_badge_hadir)
                    binding.tvBadgePersentase.setTextColor(ContextCompat.getColor(ctx, R.color.badge_hadir_text))
                }
            }

            binding.progressKehadiran.progress = item.persentaseHadir.toInt().coerceIn(0, 100)

            // 5. Teks Rincian Mini
            binding.tvCountHadir.text = "${item.hadirCount} Hadir"
            binding.tvCountIzin.text = "${item.izinCount} Izin/Sakit"
            binding.tvCountAlpa.text = "${item.alpaCount} Alpa"

            // 6. Listener
            binding.btnLihatSiswa.setOnClickListener {
                onLihatSiswaClick(item)
            }

            binding.root.setOnClickListener {
                onLihatSiswaClick(item)
            }
        }
    }
}
