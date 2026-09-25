package com.andev.absensiswaku.ui.riwayat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.RiwayatModel
import com.andev.absensiswaku.databinding.ItemRiwayatPresensiBinding

class RiwayatAdapter(
    private var listRiwayat: List<RiwayatModel>,
    private val defaultWalas: String = "Farauk Pratama S.Kom",
    private val onItemClick: ((RiwayatModel) -> Unit)? = null
) : RecyclerView.Adapter<RiwayatAdapter.RiwayatViewHolder>() {

    fun updateData(newList: List<RiwayatModel>) {
        listRiwayat = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiwayatViewHolder {
        val binding = ItemRiwayatPresensiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RiwayatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RiwayatViewHolder, position: Int) {
        holder.bind(listRiwayat[position])
    }

    override fun getItemCount(): Int = listRiwayat.size

    inner class RiwayatViewHolder(private val binding: ItemRiwayatPresensiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RiwayatModel) {
            val ctx = binding.root.context
            val walasName = if (!item.verifikator.isNullOrEmpty()) item.verifikator else defaultWalas

            binding.tvTanggalItem.text = item.tanggal
            val jamStr = item.displayJamMasuk
            val ketText = if (!item.keteranganStatus.isNullOrEmpty()) " • ${item.keteranganStatus}" else ""
            binding.tvJamItem.text = "$jamStr WIB$ketText"

            val statusUpper = item.status.uppercase()

            when {
                // 1. Presensi Mandiri Siswa (Zero-Touch Validation AI + GPS) - Tepat Waktu / Hadir
                statusUpper == "HADIR" || statusUpper == "TEPAT_WAKTU" -> {
                    binding.tvStatusBadgeItem.text = "✓ Terverifikasi AI & GPS (Hadir)"
                    binding.tvStatusBadgeItem.setBackgroundResource(R.drawable.bg_badge_hadir)
                    binding.tvStatusBadgeItem.setTextColor(ContextCompat.getColor(ctx, R.color.badge_hadir_text))

                    binding.imgSelfieThumbnail.setImageResource(R.drawable.ic_avatar)
                    binding.imgShieldVerify.setImageResource(R.drawable.ic_shield_check)
                    binding.imgShieldVerify.setColorFilter(ContextCompat.getColor(ctx, R.color.badge_hadir_text))

                    binding.tvVerifyDetail.text = "Diverifikasi otomatis oleh Sistem AI SMKN 8 Jakarta"
                    binding.tvVerifyTag.text = "Otomatis"
                    binding.tvVerifyTag.setTextColor(ContextCompat.getColor(ctx, R.color.badge_hadir_text))
                }

                // 2. Presensi Mandiri Siswa (Zero-Touch Validation AI + GPS) - Terlambat
                statusUpper == "TERLAMBAT" -> {
                    binding.tvStatusBadgeItem.text = "⚠ Terverifikasi AI (Terlambat)"
                    binding.tvStatusBadgeItem.setBackgroundResource(R.drawable.bg_badge_terlambat)
                    binding.tvStatusBadgeItem.setTextColor(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))

                    binding.imgSelfieThumbnail.setImageResource(R.drawable.ic_avatar)
                    binding.imgShieldVerify.setImageResource(R.drawable.ic_warning)
                    binding.imgShieldVerify.setColorFilter(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))

                    binding.tvVerifyDetail.text = "Diverifikasi otomatis oleh Sistem AI SMKN 8 Jakarta"
                    binding.tvVerifyTag.text = "Toleransi"
                    binding.tvVerifyTag.setTextColor(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))
                }

                // 3. Pengajuan Ketidakhadiran - Pending Menunggu Verifikasi Walas
                statusUpper == "PENDING" -> {
                    binding.tvStatusBadgeItem.text = "Menunggu Verifikasi"
                    binding.tvStatusBadgeItem.setBackgroundResource(R.drawable.bg_badge_pending)
                    binding.tvStatusBadgeItem.setTextColor(ContextCompat.getColor(ctx, R.color.badge_pending_text))

                    binding.imgSelfieThumbnail.setImageResource(R.drawable.ic_medical)
                    binding.imgShieldVerify.setImageResource(R.drawable.ic_pending_clock)
                    binding.imgShieldVerify.setColorFilter(ContextCompat.getColor(ctx, R.color.badge_pending_text))

                    binding.tvVerifyDetail.text = "Diserahkan ke $walasName • Pending"
                    binding.tvVerifyTag.text = "Pending"
                    binding.tvVerifyTag.setTextColor(ContextCompat.getColor(ctx, R.color.badge_pending_text))
                }

                // 4. Pengajuan Ketidakhadiran (Izin, Sakit, Dispensasi) - Disetujui Walas
                statusUpper in listOf("IZIN", "SAKIT", "DISPENSASI") -> {
                    val labelStatus = when (statusUpper) {
                        "IZIN" -> "Izin"
                        "SAKIT" -> "Sakit"
                        else -> "Dispensasi"
                    }
                    binding.tvStatusBadgeItem.text = "✓ $labelStatus Disetujui"
                    binding.tvStatusBadgeItem.setBackgroundResource(R.drawable.bg_badge_izin)
                    binding.tvStatusBadgeItem.setTextColor(ContextCompat.getColor(ctx, R.color.badge_izin_text))

                    binding.imgSelfieThumbnail.setImageResource(R.drawable.ic_medical)
                    binding.imgShieldVerify.setImageResource(R.drawable.ic_shield_check)
                    binding.imgShieldVerify.setColorFilter(ContextCompat.getColor(ctx, R.color.badge_izin_text))

                    val verifyTimeText = if (!item.jamVerifikasi.isNullOrEmpty()) " pada ${item.jamVerifikasi}" else ""
                    binding.tvVerifyDetail.text = "Diverifikasi oleh $walasName$verifyTimeText"
                    binding.tvVerifyTag.text = "Disetujui"
                    binding.tvVerifyTag.setTextColor(ContextCompat.getColor(ctx, R.color.badge_izin_text))
                }

                else -> {
                    binding.tvStatusBadgeItem.text = item.status
                    binding.tvStatusBadgeItem.setBackgroundResource(R.drawable.bg_badge_pill_blue)
                    binding.tvStatusBadgeItem.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))

                    binding.imgSelfieThumbnail.setImageResource(R.drawable.ic_avatar)
                    binding.tvVerifyDetail.text = "Diverifikasi oleh $walasName"
                    binding.tvVerifyTag.text = ""
                }
            }

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}
