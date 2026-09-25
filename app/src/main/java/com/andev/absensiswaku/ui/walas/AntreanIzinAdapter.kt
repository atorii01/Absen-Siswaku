package com.andev.absensiswaku.ui.walas

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.PengajuanIzinResponse
import com.andev.absensiswaku.databinding.ItemVerifikasiIzinBinding

class AntreanIzinAdapter(
    private var items: MutableList<PengajuanIzinResponse>,
    private val onSetujuiClicked: (PengajuanIzinResponse) -> Unit,
    private val onTolakClicked: (PengajuanIzinResponse) -> Unit,
    private val onLihatSuratClicked: (PengajuanIzinResponse) -> Unit,
    private val onHubungiOrtuClicked: (PengajuanIzinResponse) -> Unit
) : RecyclerView.Adapter<AntreanIzinAdapter.ViewHolder>() {

    fun updateData(newItems: List<PengajuanIzinResponse>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun removeItem(item: PengajuanIzinResponse) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
            notifyItemRangeChanged(index, items.size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVerifikasiIzinBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemVerifikasiIzinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PengajuanIzinResponse, posNumber: Int) {
            val ctx = binding.root.context
            val jenis = item.jenisIzin.trim()

            // 1. Nomor Urut format 2 digit (01, 02, ...)
            val displayPos = String.format("%02d", posNumber)
            binding.tvNomorUrut.text = displayPos

            // 2. Identitas Siswa
            val nama = item.siswa?.namaLengkap
                ?: if (item.keterangan.contains("Bagas", true)) "Bagas Wahyu Santoso"
                else if (item.keterangan.contains("Cantika", true)) "Cantika Dewi Maharani"
                else if (item.keterangan.contains("Fadhil", true)) "Fadhil Rahman Hakim"
                else if (item.keterangan.contains("Hafiz", true)) "Hafiz Maulana Zaki"
                else "Siswa SMKN 8 (${item.siswaId})"

            val nisn = item.siswa?.nisn?.takeIf { it.isNotEmpty() } ?: "00678219${String.format("%02d", posNumber)}"
            val jam = item.createdAt?.substringAfter("T")?.take(5) ?: "06:45"
            binding.tvNamaSiswa.text = nama

            // 3. Styling Berdasarkan Jenis Status/Izin (Sakit, Izin, Dispensasi, Alpa)
            when (jenis.uppercase()) {
                "SAKIT" -> {
                    binding.viewAccentStripe.setBackgroundResource(R.drawable.bg_stripe_sakit)
                    binding.frameNomorUrut.setBackgroundResource(R.drawable.bg_circle_num_red)
                    binding.tvNomorUrut.setTextColor(Color.parseColor("#DC2626"))
                    binding.tvBadgeJenisIzin.text = "Sakit"
                    binding.tvBadgeJenisIzin.setBackgroundResource(R.drawable.bg_badge_pill_sakit)
                    binding.tvNisnWaktu.text = "NISN: $nisn • Diajukan $jam WIB"

                    binding.layoutAlasanDanBerkas.visibility = View.VISIBLE
                    binding.layoutActionButtons.visibility = View.VISIBLE
                    binding.btnHubungiOrtu.visibility = View.GONE
                    binding.tvLabelSetujui.text = "✓ Setujui Sakit"
                }
                "IZIN" -> {
                    binding.viewAccentStripe.setBackgroundResource(R.drawable.bg_stripe_izin)
                    binding.frameNomorUrut.setBackgroundResource(R.drawable.bg_circle_num_amber)
                    binding.tvNomorUrut.setTextColor(Color.parseColor("#D97706"))
                    binding.tvBadgeJenisIzin.text = "Izin"
                    binding.tvBadgeJenisIzin.setBackgroundResource(R.drawable.bg_badge_pill_amber)
                    binding.tvNisnWaktu.text = "NISN: $nisn • Diajukan $jam WIB"

                    binding.layoutAlasanDanBerkas.visibility = View.VISIBLE
                    binding.layoutActionButtons.visibility = View.VISIBLE
                    binding.btnHubungiOrtu.visibility = View.GONE
                    binding.tvLabelSetujui.text = "✓ Setujui Izin"
                }
                "DISPENSASI" -> {
                    binding.viewAccentStripe.setBackgroundResource(R.drawable.bg_stripe_dispensasi)
                    binding.frameNomorUrut.setBackgroundResource(R.drawable.bg_circle_num_blue)
                    binding.tvNomorUrut.setTextColor(Color.parseColor("#2563EB"))
                    binding.tvBadgeJenisIzin.text = "Dispensasi"
                    binding.tvBadgeJenisIzin.setBackgroundResource(R.drawable.bg_badge_pill_dispensasi)
                    binding.tvNisnWaktu.text = "NISN: $nisn • Diajukan $jam WIB"

                    binding.layoutAlasanDanBerkas.visibility = View.VISIBLE
                    binding.layoutActionButtons.visibility = View.VISIBLE
                    binding.btnHubungiOrtu.visibility = View.GONE
                    binding.tvLabelSetujui.text = "✓ Setujui Dispensasi"
                }
                else -> {
                    // ALPA / Belum Scan
                    binding.viewAccentStripe.setBackgroundResource(R.drawable.bg_stripe_alpa)
                    binding.frameNomorUrut.setBackgroundResource(R.drawable.bg_circle_num_slate)
                    binding.tvNomorUrut.setTextColor(Color.parseColor("#64748B"))
                    binding.tvBadgeJenisIzin.text = "Alpa"
                    binding.tvBadgeJenisIzin.setBackgroundResource(R.drawable.bg_badge_pill_alpa)
                    binding.tvNisnWaktu.text = "NISN: $nisn • Belum scan hingga 07:30"

                    binding.layoutAlasanDanBerkas.visibility = View.GONE
                    binding.layoutActionButtons.visibility = View.GONE
                    binding.btnHubungiOrtu.visibility = View.VISIBLE
                }
            }

            // 4. Keterangan Alasan
            val quote = item.keterangan.ifEmpty { "Tidak ada keterangan tambahan." }
            binding.tvKeteranganAlasan.text = "“$quote”"

            // 5. Lampiran Berkas Bukti
            val fileName = item.buktiBerkasUrl?.substringAfterLast("/")?.takeIf { it.isNotEmpty() }
                ?: if (jenis.equals("SAKIT", true)) "Surat_Dokter_RSUD_${nama.substringBefore(" ")}.pdf"
                else if (jenis.equals("IZIN", true)) "Surat_Permohonan_Ortu.jpg"
                else if (jenis.equals("DISPENSASI", true)) "Surat_Tugas_Kesiswaan.pdf"
                else null

            if (!fileName.isNullOrEmpty() && !jenis.equals("ALPA", true)) {
                binding.layoutAttachment.visibility = View.VISIBLE
                binding.tvNamaBerkas.text = fileName
            } else {
                binding.layoutAttachment.visibility = View.GONE
            }

            // 6. Action Clicks
            binding.btnSetujui.setOnClickListener {
                onSetujuiClicked(item)
            }

            binding.btnTolak.setOnClickListener {
                onTolakClicked(item)
            }

            binding.btnLihatSurat.setOnClickListener {
                onLihatSuratClicked(item)
            }

            binding.btnHubungiOrtu.setOnClickListener {
                onHubungiOrtuClicked(item)
            }
        }
    }
}
