package com.andev.absensiswaku.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.data.network.RombelMapelResponse
import com.andev.absensiswaku.databinding.ItemRombelKelolaBinding

class RombelKelolaAdapter(
    private val onSettingsClick: (RombelMapelResponse) -> Unit
) : RecyclerView.Adapter<RombelKelolaAdapter.RombelKelolaViewHolder>() {

    private val items = mutableListOf<RombelMapelResponse>()
    private var studentCountMap: Map<Int, Int> = emptyMap()

    fun submitData(list: List<RombelMapelResponse>, counts: Map<Int, Int>) {
        items.clear()
        items.addAll(list)
        studentCountMap = counts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RombelKelolaViewHolder {
        val binding = ItemRombelKelolaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RombelKelolaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RombelKelolaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class RombelKelolaViewHolder(private val binding: ItemRombelKelolaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RombelMapelResponse) {
            val code = formatShortCode(item.namaKelas)
            binding.tvKodeRombelKelola.text = code

            val prefix = if (item.namaKelas.startsWith("Kelas", ignoreCase = true)) "" else "Kelas "
            binding.tvNamaRombelKelola.text = "$prefix${item.namaKelas}"

            val walas = item.waliKelas?.namaLengkap?.trim().takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvWaliKelasKelola.text = "Wali Kelas: $walas"

            val kuota = item.kuota
            val countSiswa = studentCountMap[item.id] ?: kuota
            val percent = if (kuota > 0) ((countSiswa.toFloat() / kuota.toFloat()) * 100).toInt() else 100
            binding.progressKapasitasKursi.progress = percent

            val sisa = kuota - countSiswa
            val statusDesc = if (sisa <= 0) "Penuh" else "Sisa $sisa"
            binding.tvStatusKapasitasKursi.text = "$countSiswa / $kuota Siswa ($statusDesc)"

            binding.btnSetelanRombel.setOnClickListener {
                onSettingsClick(item)
            }
        }

        private fun formatShortCode(namaKelas: String): String {
            val clean = namaKelas.trim()
            return when {
                clean.contains("RPL", true) -> "12.RPL"
                clean.contains("AKL 1", true) -> "12.AK1"
                clean.contains("AKL 2", true) -> "12.AK2"
                clean.contains("AKL 3", true) -> "12.AK3"
                clean.contains("MP 1", true) -> "12.MP1"
                clean.contains("MP 2", true) -> "12.MP2"
                clean.contains("BR 1", true) -> "12.BR1"
                clean.contains("BR 2", true) -> "12.BR2"
                clean.contains("BD", true) -> "12.BD"
                clean.contains("UPW", true) -> "12.UPW"
                else -> {
                    clean.replace("Kelas", "", ignoreCase = true)
                        .replace("XII", "12.")
                        .replace(" ", "")
                        .trim()
                }
            }
        }
    }
}
