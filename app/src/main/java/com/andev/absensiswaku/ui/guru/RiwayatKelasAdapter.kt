package com.andev.absensiswaku.ui.guru

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.databinding.ItemRiwayatKelasBinding
import java.util.Locale

data class RiwayatKelasItem(
    val tanggalDisplay: String,
    val tanggalIso: String,
    val hadirCount: Int,
    val totalCount: Int,
    val persentase: Float,
    val sakitCount: Int,
    val izinCount: Int,
    val alpaCount: Int,
    val statusValidasi: String = "✓ Tervalidasi Walas"
)

class RiwayatKelasAdapter(
    private var rombelId: Int = 9,
    private var namaKelas: String = "XII RPL",
    private var items: List<RiwayatKelasItem> = emptyList(),
    private val onItemClick: ((RiwayatKelasItem) -> Unit)? = null
) : RecyclerView.Adapter<RiwayatKelasAdapter.ViewHolder>() {

    constructor(
        items: List<RiwayatKelasItem> = emptyList(),
        onItemClick: ((RiwayatKelasItem) -> Unit)? = null
    ) : this(9, "XII RPL", items, onItemClick)

    fun setRombelInfo(id: Int, name: String) {
        this.rombelId = id
        this.namaKelas = name
    }

    fun updateData(newItems: List<RiwayatKelasItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatKelasBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemRiwayatKelasBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RiwayatKelasItem) {
            binding.tvTanggalRiwayat.text = item.tanggalDisplay
            binding.tvBadgeValidasi.text = item.statusValidasi
            binding.tvRasioHadir.text = "${item.hadirCount}/${item.totalCount}"

            val persenStr = if (item.persentase == 100f) {
                "100%"
            } else {
                "${String.format(Locale.US, "%.1f", item.persentase)}%"
            }
            binding.tvPersenHadir.text = "($persenStr)"

            val rincianText = if (item.sakitCount == 0 && item.izinCount == 0 && item.alpaCount == 0) {
                "Lengkap • 0 Sakit • 0 Alpa"
            } else {
                "${item.sakitCount} Sakit • ${item.izinCount} Izin • ${item.alpaCount} Alpa"
            }
            binding.tvRincianRiwayat.text = rincianText

            val openDetailAction = {
                if (onItemClick != null) {
                    onItemClick.invoke(item)
                } else {
                    val activity = itemView.context as? FragmentActivity
                    activity?.let { act ->
                        val dialog = BottomSheetRiwayatDialog.newInstance(
                            rombelId = rombelId,
                            namaKelas = namaKelas,
                            tanggalIso = item.tanggalIso,
                            tanggalDisplay = item.tanggalDisplay
                        )
                        dialog.show(act.supportFragmentManager, "BottomSheetRiwayatDialog")
                    }
                }
            }

            binding.btnLihatDetailRiwayat.setOnClickListener {
                openDetailAction.invoke()
            }

            itemView.setOnClickListener {
                openDetailAction.invoke()
            }
        }
    }
}
