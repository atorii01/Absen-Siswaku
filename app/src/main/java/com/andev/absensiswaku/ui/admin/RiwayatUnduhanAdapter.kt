package com.andev.absensiswaku.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.databinding.ItemRiwayatUnduhanBinding
import java.util.Locale
import java.util.UUID

data class RiwayatUnduhanModel(
    val id: String = UUID.randomUUID().toString(),
    val namaFile: String,
    val ukuranFileKb: Long,
    val waktuLalu: String,
    val rolePengunduh: String = "Super Admin",
    val format: String, // "XLSX", "PDF", "CSV"
    val uriString: String? = null
)

class RiwayatUnduhanAdapter(
    private val onDownloadClick: (RiwayatUnduhanModel) -> Unit
) : RecyclerView.Adapter<RiwayatUnduhanAdapter.RiwayatViewHolder>() {

    private val items = mutableListOf<RiwayatUnduhanModel>()

    fun submitList(list: List<RiwayatUnduhanModel>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun addFirst(item: RiwayatUnduhanModel) {
        items.add(0, item)
        notifyItemInserted(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiwayatViewHolder {
        val binding = ItemRiwayatUnduhanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RiwayatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RiwayatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class RiwayatViewHolder(private val binding: ItemRiwayatUnduhanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RiwayatUnduhanModel) {
            val context = itemView.context

            binding.tvNamaFile.text = item.namaFile

            val sizeStr = if (item.ukuranFileKb >= 1024) {
                String.format(Locale.US, "%.1f MB", item.ukuranFileKb / 1024.0)
            } else {
                "${item.ukuranFileKb} KB"
            }
            binding.tvMetadataFile.text = "$sizeStr • ${item.waktuLalu}"

            binding.tvRolePengunduh.text = item.rolePengunduh

            when (item.format.uppercase(Locale.getDefault())) {
                "XLSX" -> {
                    binding.frameIconFile.setBackgroundResource(R.drawable.bg_badge_hadir)
                    binding.imgIconFile.setImageResource(R.drawable.ic_excel)
                    binding.imgIconFile.imageTintList =
                        ContextCompat.getColorStateList(context, R.color.primary_teal)
                }
                "PDF" -> {
                    binding.frameIconFile.setBackgroundResource(R.drawable.bg_badge_alpa)
                    binding.imgIconFile.setImageResource(R.drawable.ic_pdf_doc)
                    binding.imgIconFile.imageTintList =
                        ContextCompat.getColorStateList(context, R.color.alert_coral_text)
                }
                "CSV" -> {
                    binding.frameIconFile.setBackgroundResource(R.drawable.bg_pill_arsip)
                    binding.imgIconFile.setImageResource(R.drawable.ic_csv_file)
                    binding.imgIconFile.imageTintList =
                        ContextCompat.getColorStateList(context, R.color.secondary_blue)
                }
                else -> {
                    binding.frameIconFile.setBackgroundResource(R.drawable.bg_badge_hadir)
                    binding.imgIconFile.setImageResource(R.drawable.ic_excel)
                    binding.imgIconFile.imageTintList =
                        ContextCompat.getColorStateList(context, R.color.primary_teal)
                }
            }

            binding.btnUnduhUlang.setOnClickListener {
                onDownloadClick(item)
            }
        }
    }
}
