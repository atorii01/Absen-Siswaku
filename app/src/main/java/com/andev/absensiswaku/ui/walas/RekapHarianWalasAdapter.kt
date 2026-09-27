package com.andev.absensiswaku.ui.walas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.databinding.ItemRekapHarianWalasBinding
import java.util.Locale

data class RekapHarianKelasModel(
    val tanggal: String = "",
    val tanggalFormatted: String = "",
    val countHadir: Int = 0,
    val countTerlambat: Int = 0,
    val countIzin: Int = 0,
    val countAlpa: Int = 0,
    val totalSiswa: Int = 0,
    val persentase: Double = 0.0,
    val isLengkap: Boolean = false,
    val countSakit: Int = 0
)

class RekapHarianWalasAdapter(
    private var listData: List<RekapHarianKelasModel>,
    private val onItemClick: ((RekapHarianKelasModel) -> Unit)? = null
) : RecyclerView.Adapter<RekapHarianWalasAdapter.ViewHolder>() {

    fun updateData(newList: List<RekapHarianKelasModel>) {
        listData = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRekapHarianWalasBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listData[position])
    }

    override fun getItemCount(): Int = listData.size

    inner class ViewHolder(private val binding: ItemRekapHarianWalasBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RekapHarianKelasModel) {
            binding.tvTanggalHarian.text = item.tanggalFormatted

            val persenFormatted = String.format(Locale.getDefault(), "%.1f%%", item.persentase)
            val totalSiswaHadir = item.countHadir + item.countTerlambat
            binding.tvRingkasanKehadiran.text =
                "$totalSiswaHadir/${item.totalSiswa} Siswa Hadir ($persenFormatted)"

            binding.tvRincianStatus.text =
                "${item.countIzin} Izin • ${item.countAlpa} Alpa"

            binding.tvBadgeKelengkapan.text =
                if (item.isLengkap) "✓ Terverifikasi Lengkap" else "Dalam Verifikasi"

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
            binding.btnRincianHarian.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}
