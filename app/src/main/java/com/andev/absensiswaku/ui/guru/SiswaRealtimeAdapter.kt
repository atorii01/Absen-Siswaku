package com.andev.absensiswaku.ui.guru

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.databinding.ItemSiswaRealtimeBinding

data class SiswaRealtimeItem(
    val nomorUrut: String,
    val siswaId: Int?,
    val namaLengkap: String,
    val nisn: String,
    val subteks: String,
    val status: String, // "HADIR", "TERLAMBAT", "SAKIT", "IZIN", "ALPA"
    val badgeLabel: String,
    val waktuMasuk: String? = null
)

class SiswaRealtimeAdapter(
    private var items: List<SiswaRealtimeItem> = emptyList(),
    private val onItemClick: ((SiswaRealtimeItem) -> Unit)? = null
) : RecyclerView.Adapter<SiswaRealtimeAdapter.ViewHolder>() {

    fun updateData(newItems: List<SiswaRealtimeItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSiswaRealtimeBinding.inflate(
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
        private val binding: ItemSiswaRealtimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SiswaRealtimeItem) {
            val context = itemView.context

            binding.tvNomorAbsen.text = item.nomorUrut
            binding.tvNamaSiswa.text = item.namaLengkap
            binding.tvSubteksSiswa.text = item.subteks
            binding.tvBadgeStatus.text = item.badgeLabel

            when (item.status.uppercase()) {
                "HADIR" -> {
                    binding.tvNomorAbsen.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_avatar_circle_mint)
                    binding.tvNomorAbsen.setTextColor(
                        ContextCompat.getColor(context, R.color.primary_teal)
                    )
                    binding.tvBadgeStatus.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_badge_hadir)
                    binding.tvBadgeStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.badge_hadir_text)
                    )
                }

                "TERLAMBAT" -> {
                    binding.tvNomorAbsen.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_circle_num_amber)
                    binding.tvNomorAbsen.setTextColor(
                        ContextCompat.getColor(context, R.color.badge_terlambat_text)
                    )
                    binding.tvBadgeStatus.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_badge_terlambat)
                    binding.tvBadgeStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.badge_terlambat_text)
                    )
                }

                "SAKIT" -> {
                    binding.tvNomorAbsen.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_circle_num_red)
                    binding.tvNomorAbsen.setTextColor(
                        ContextCompat.getColor(context, R.color.badge_sakit_text)
                    )
                    binding.tvBadgeStatus.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_badge_sakit)
                    binding.tvBadgeStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.badge_sakit_text)
                    )
                }

                "IZIN" -> {
                    binding.tvNomorAbsen.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_avatar_circle_blue)
                    binding.tvNomorAbsen.setTextColor(
                        ContextCompat.getColor(context, R.color.badge_izin_text)
                    )
                    binding.tvBadgeStatus.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_badge_izin)
                    binding.tvBadgeStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.badge_izin_text)
                    )
                }

                else -> { // ALPA
                    binding.tvNomorAbsen.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_circle_num_red)
                    binding.tvNomorAbsen.setTextColor(
                        ContextCompat.getColor(context, R.color.badge_alpa_text)
                    )
                    binding.tvBadgeStatus.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_badge_alpa)
                    binding.tvBadgeStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.badge_alpa_text)
                    )
                }
            }

            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}
