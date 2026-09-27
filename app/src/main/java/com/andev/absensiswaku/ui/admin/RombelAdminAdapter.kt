package com.andev.absensiswaku.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.RombelMapelResponse
import com.andev.absensiswaku.databinding.ItemRombelAdminBinding
import java.util.Locale

class RombelAdminAdapter(
    private val onAuditClick: (RombelMapelResponse) -> Unit,
    private val onRincianClick: (RombelMapelResponse) -> Unit
) : RecyclerView.Adapter<RombelAdminAdapter.RombelAdminViewHolder>() {

    private val rombelList = mutableListOf<RombelMapelResponse>()

    fun submitList(list: List<RombelMapelResponse>) {
        rombelList.clear()
        rombelList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RombelAdminViewHolder {
        val binding = ItemRombelAdminBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RombelAdminViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RombelAdminViewHolder, position: Int) {
        holder.bind(rombelList[position])
    }

    override fun getItemCount(): Int = rombelList.size

    inner class RombelAdminViewHolder(private val binding: ItemRombelAdminBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RombelMapelResponse) {
            val context = itemView.context

            // Format Kode Rombel Singkat (cth: "12-RPL", "12-AK1", "12-MP1")
            val code = formatShortRombelCode(item.namaKelas)
            binding.tvKodeRombel.text = code

            // Nama Kelas & Wali Kelas
            val prefix = if (item.namaKelas.startsWith("Kelas", ignoreCase = true)) "" else "Kelas "
            binding.tvNamaKelas.text = "$prefix${item.namaKelas}"
            
            val walasName = item.waliKelas?.namaLengkap?.trim().takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvWaliKelas.text = "Wali: $walasName"

            // Badge Status Rombel: Lengkap vs Perlu Konfirmasi Alpa
            if (item.alpaCount == 0) {
                binding.tvStatusBadge.text = "✓ Lengkap Terverifikasi"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_rombel_verif)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_hadir_text))
            } else {
                binding.tvStatusBadge.text = "▲ Perlu Konfirmasi Alpa"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_rombel_alpa)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_amber_text))
            }

            // Progres Kehadiran & Persentase
            val kuota = item.kuota
            binding.tvProgresKehadiran.text = "${item.hadirCount} / $kuota Siswa Hadir"
            binding.tvPersenKehadiran.text = String.format(Locale.US, "%.1f%%", item.persentaseHadir)
            binding.progressRombel.progress = item.persentaseHadir.toInt()

            // Rincian Mini (Hadir, Sakit/Izin, Alpa)
            binding.tvCountHadir.text = "● ${item.hadirCount} Hadir"
            binding.tvCountIzin.text = "● ${item.izinCount} Sakit/Izin"
            binding.tvCountAlpa.text = "● ${item.alpaCount} Alpa"

            // Aksi Tombol
            binding.btnAuditGpsFoto.setOnClickListener {
                onAuditClick(item)
            }

            binding.btnRincianKelas.setOnClickListener {
                onRincianClick(item)
            }
        }

        private fun formatShortRombelCode(namaKelas: String): String {
            val clean = namaKelas.trim()
            return when {
                clean.contains("RPL", true) -> "12-RPL"
                clean.contains("AKL 1", true) -> "12-AK1"
                clean.contains("AKL 2", true) -> "12-AK2"
                clean.contains("AKL 3", true) -> "12-AK3"
                clean.contains("MP 1", true) -> "12-MP1"
                clean.contains("MP 2", true) -> "12-MP2"
                clean.contains("BR 1", true) -> "12-BR1"
                clean.contains("BR 2", true) -> "12-BR2"
                clean.contains("BD", true) -> "12-BD"
                clean.contains("UPW", true) -> "12-UPW"
                else -> {
                    clean.replace("Kelas", "", ignoreCase = true)
                        .replace("XII", "12-", ignoreCase = true)
                        .replace(" ", "")
                        .trim()
                }
            }
        }
    }
}
