package com.andev.absensiswaku.ui.riwayat

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.RiwayatModel
import com.andev.absensiswaku.databinding.ItemRiwayatPresensiBinding

// ViewBinding aliases agar selaras dengan naming convention
private val ItemRiwayatPresensiBinding.badgeStatus: TextView get() = tvStatusBadgeItem
private val ItemRiwayatPresensiBinding.tvKeteranganVerifikator: TextView get() = tvVerifyDetail
private val ItemRiwayatPresensiBinding.tvWaktuDanKeterangan: TextView get() = tvJamItem

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

        private fun setBadgePill(
            view: TextView,
            @androidx.annotation.DrawableRes bgDrawableRes: Int,
            @androidx.annotation.ColorRes textRes: Int
        ) {
            val ctx = view.context
            view.setBackgroundResource(bgDrawableRes)
            view.setTextColor(ContextCompat.getColor(ctx, textRes))
        }

        fun bind(item: RiwayatModel) {
            val ctx = binding.root.context
            val walasName = if (!item.verifikator.isNullOrEmpty()) item.verifikator else defaultWalas

            binding.tvTanggalItem.text = item.tanggal ?: "-"
            val jamTeks = item.waktu // Berisi jam WIB hasil format dari created_at
            val subKeterangan = if (item.keterangan.isNotEmpty()) " • ${item.keterangan}" else ""
            binding.tvWaktuDanKeterangan.text = "$jamTeks$subKeterangan"

            val statusVerif = item.statusVerifikasi?.lowercase()

            if (!statusVerif.isNullOrEmpty()) {
                // 3 Kondisi Eksplisit Status Pengajuan Izin/Sakit/Dispensasi
                when (statusVerif) {
                    "pending" -> {
                        // Badge Kuning / Oranye
                        binding.badgeStatus.text = "Menunggu Verifikasi"
                        setBadgePill(binding.badgeStatus, R.drawable.bg_badge_pending, R.color.badge_pending_text)
                        binding.tvKeteranganVerifikator.text = "Diserahkan ke ${item.namaWalas} • Pending"

                        binding.imgSelfieThumbnail.setImageResource(R.drawable.ic_medical)
                        binding.imgShieldVerify.setImageResource(R.drawable.ic_pending_clock)
                        binding.imgShieldVerify.setColorFilter(ContextCompat.getColor(ctx, R.color.badge_pending_text))
                        binding.tvVerifyTag.text = "Pending"
                        binding.tvVerifyTag.setTextColor(ContextCompat.getColor(ctx, R.color.badge_pending_text))
                    }
                    "disetujui" -> {
                        // Badge Hijau Terverifikasi
                        val labelJenis = item.jenisIzin?.ifEmpty { "Izin" } ?: "Izin"
                        binding.badgeStatus.text = "✓ $labelJenis Disetujui"
                        setBadgePill(binding.badgeStatus, R.drawable.bg_badge_hadir, R.color.badge_hadir_text)
                        binding.tvKeteranganVerifikator.text = "Diverifikasi oleh ${item.namaWalas}"

                        binding.imgSelfieThumbnail.setImageResource(R.drawable.ic_medical)
                        binding.imgShieldVerify.setImageResource(R.drawable.ic_shield_check)
                        binding.imgShieldVerify.setColorFilter(ContextCompat.getColor(ctx, R.color.badge_hadir_text))
                        binding.tvVerifyTag.text = "Disetujui"
                        binding.tvVerifyTag.setTextColor(ContextCompat.getColor(ctx, R.color.badge_hadir_text))
                    }
                    "ditolak" -> {
                        // Badge Merah Ditolak
                        binding.badgeStatus.text = "✕ Pengajuan Ditolak"
                        setBadgePill(binding.badgeStatus, R.drawable.bg_badge_alpa, R.color.badge_alpa_text)
                        binding.tvKeteranganVerifikator.text = "Ditolak oleh ${item.namaWalas} (Tidak Sah)"

                        binding.imgSelfieThumbnail.setImageResource(R.drawable.ic_medical)
                        binding.imgShieldVerify.setImageResource(R.drawable.ic_warning)
                        binding.imgShieldVerify.setColorFilter(ContextCompat.getColor(ctx, R.color.badge_alpa_text))
                        binding.tvVerifyTag.text = "Ditolak"
                        binding.tvVerifyTag.setTextColor(ContextCompat.getColor(ctx, R.color.badge_alpa_text))
                    }
                    else -> {
                        binding.badgeStatus.text = item.status ?: "-"
                        binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_pill_blue)
                        binding.badgeStatus.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))

                        binding.imgSelfieThumbnail.setImageResource(R.drawable.ic_avatar)
                        binding.tvKeteranganVerifikator.text = "Diverifikasi oleh ${item.namaWalas}"
                        binding.tvVerifyTag.text = ""
                    }
                }
            } else {
                // Presensi Mandiri Harian Siswa (AI + GPS)
                val statusUpper = item.status?.uppercase().orEmpty()
                when (statusUpper) {
                    "HADIR", "TEPAT_WAKTU" -> {
                        binding.badgeStatus.text = "✓ Terverifikasi AI & GPS (Hadir)"
                        binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_hadir)
                        binding.badgeStatus.setTextColor(ContextCompat.getColor(ctx, R.color.badge_hadir_text))

                        binding.imgSelfieThumbnail.setImageResource(R.drawable.ic_avatar)
                        binding.imgShieldVerify.setImageResource(R.drawable.ic_shield_check)
                        binding.imgShieldVerify.setColorFilter(ContextCompat.getColor(ctx, R.color.badge_hadir_text))

                        binding.tvKeteranganVerifikator.text = "Diverifikasi otomatis oleh Sistem AI SMKN 8 Jakarta"
                        binding.tvVerifyTag.text = "Otomatis"
                        binding.tvVerifyTag.setTextColor(ContextCompat.getColor(ctx, R.color.badge_hadir_text))
                    }
                    "TERLAMBAT" -> {
                        binding.badgeStatus.text = "⚠ Terverifikasi AI (Terlambat)"
                        binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_terlambat)
                        binding.badgeStatus.setTextColor(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))

                        binding.imgSelfieThumbnail.setImageResource(R.drawable.ic_avatar)
                        binding.imgShieldVerify.setImageResource(R.drawable.ic_warning)
                        binding.imgShieldVerify.setColorFilter(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))

                        binding.tvKeteranganVerifikator.text = "Diverifikasi otomatis oleh Sistem AI SMKN 8 Jakarta"
                        binding.tvVerifyTag.text = "Toleransi"
                        binding.tvVerifyTag.setTextColor(ContextCompat.getColor(ctx, R.color.badge_terlambat_text))
                    }
                    else -> {
                        binding.badgeStatus.text = item.status ?: "-"
                        binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_pill_blue)
                        binding.badgeStatus.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))

                        binding.imgSelfieThumbnail.setImageResource(R.drawable.ic_avatar)
                        binding.tvKeteranganVerifikator.text = "Diverifikasi oleh ${item.namaWalas}"
                        binding.tvVerifyTag.text = ""
                    }
                }
            }

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}
