package com.andev.absensiswaku.ui.walas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.databinding.ItemRekapHarianWalasBinding
import java.util.Locale

data class HistoriKehadiranModel(
    val tanggal: String = "",
    val rawTanggal: String = tanggal,
    val tanggalFormatted: String = "",
    val idKelas: Int = 1,
    val namaKelas: String = "XII RPL",
    val countHadir: Int = 0,
    val countTerlambat: Int = 0,
    val countIzin: Int = 0,
    val countAlpa: Int = 0,
    val totalSiswa: Int = 0,
    val persentase: Double = 0.0,
    val isLengkap: Boolean = false,
    val countSakit: Int = 0
)

typealias RekapHarianKelasModel = HistoriKehadiranModel

class HistoriKehadiranAdapter(
    private var listData: List<HistoriKehadiranModel> = emptyList(),
    private val fragmentManager: FragmentManager? = null,
    private val onItemClick: ((HistoriKehadiranModel) -> Unit)? = null
) : RecyclerView.Adapter<HistoriKehadiranAdapter.ViewHolder>() {

    fun updateData(newList: List<HistoriKehadiranModel>) {
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

        fun bind(item: HistoriKehadiranModel) {
            binding.tvTanggalHarian.text = item.tanggalFormatted

            val persenFormatted = String.format(Locale.getDefault(), "%.1f%%", item.persentase)
            val totalSiswaHadir = item.countHadir + item.countTerlambat
            binding.tvRingkasanKehadiran.text =
                "$totalSiswaHadir/${item.totalSiswa} Siswa Hadir ($persenFormatted)"

            binding.tvRincianStatus.text =
                "${item.countIzin} Izin • ${item.countAlpa} Alpa"

            binding.tvBadgeKelengkapan.text =
                if (item.isLengkap) "✓ Terverifikasi Lengkap" else "Dalam Verifikasi"

            // Buka BottomSheetDialog dan kirim tanggal serta id_kelas
            val openDialog = {
                if (fragmentManager != null) {
                    val dialog = BottomSheetDetailPresensiDialog.newInstance(
                        tanggal = item.tanggalFormatted,
                        rawTanggal = item.rawTanggal,
                        idKelas = item.idKelas,
                        namaKelas = item.namaKelas
                    )
                    dialog.show(fragmentManager, "DetailPresensiDialog")
                } else {
                    onItemClick?.invoke(item)
                }
            }

            binding.btnRincian.setOnClickListener {
                openDialog()
            }

            binding.root.setOnClickListener {
                openDialog()
            }
        }
    }
}

typealias RekapHarianWalasAdapter = HistoriKehadiranAdapter
