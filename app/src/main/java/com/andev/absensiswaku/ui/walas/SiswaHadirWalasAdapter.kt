package com.andev.absensiswaku.ui.walas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.data.network.SiswaHadirWalasModel
import com.andev.absensiswaku.databinding.ItemSiswaHadirWalasBinding

class SiswaHadirWalasAdapter(
    private var items: List<SiswaHadirWalasModel>
) : RecyclerView.Adapter<SiswaHadirWalasAdapter.ViewHolder>() {

    fun updateData(newItems: List<SiswaHadirWalasModel>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSiswaHadirWalasBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemSiswaHadirWalasBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SiswaHadirWalasModel) {
            binding.tvNomorUrutHadir.text = item.nomorUrut
            binding.tvNamaSiswaHadir.text = item.namaLengkap
            binding.tvNisnJamMasuk.text = "NISN: ${item.nisn} • Masuk ${item.waktuMasuk} WIB"
        }
    }
}
