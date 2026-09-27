package com.andev.absensiswaku.ui.walas

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.SiswaKelolaResponse
import com.andev.absensiswaku.databinding.ItemSiswaKelolaBinding

class SiswaKelolaAdapter(
    private var listSiswa: List<SiswaKelolaResponse> = emptyList(),
    private val defaultNamaKelas: String = "XII RPL",
    private val onEditClick: (SiswaKelolaResponse) -> Unit,
    private val onMutasiClick: (SiswaKelolaResponse) -> Unit,
    private val onHapusClick: (SiswaKelolaResponse) -> Unit
) : RecyclerView.Adapter<SiswaKelolaAdapter.SiswaViewHolder>() {

    private var fullList: List<SiswaKelolaResponse> = emptyList()
    private var currentGenderFilter: String = "ALL" // "ALL", "L", "P"
    private var currentQuery: String = ""

    private data class AvatarPalette(
        val bgResId: Int,
        val textColor: Int
    )

    private val palettes = listOf(
        AvatarPalette(R.drawable.bg_avatar_circle_mint, Color.parseColor("#00685F")),
        AvatarPalette(R.drawable.bg_avatar_circle_blue, Color.parseColor("#1E40AF")),
        AvatarPalette(R.drawable.bg_avatar_circle_amber, Color.parseColor("#B45309")),
        AvatarPalette(R.drawable.bg_avatar_circle_purple, Color.parseColor("#6D28D9"))
    )

    inner class SiswaViewHolder(val binding: ItemSiswaKelolaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiswaViewHolder {
        val binding = ItemSiswaKelolaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SiswaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SiswaViewHolder, position: Int) {
        val item = listSiswa[position]
        val context = holder.itemView.context

        with(holder.binding) {
            // 1. Inisial Nama & Warna Avatar Pastel
            tvAvatarInitials.text = item.initialLetters

            val paletteIndex = kotlin.math.abs(
                (item.id ?: item.namaLengkap?.hashCode() ?: position)
            ) % palettes.size
            val palette = palettes[paletteIndex]

            layoutAvatarInitials.background = ContextCompat.getDrawable(context, palette.bgResId)
            tvAvatarInitials.setTextColor(palette.textColor)

            // 2. Nama Lengkap & Status Dot
            tvNamaSiswaKelola.text = item.namaLengkap ?: "Siswa"
            dotStatusSiswa.background = ContextCompat.getDrawable(
                context,
                if (item.statusAktif != false) R.drawable.bg_dot_status_online else R.drawable.bg_dot_red
            )

            // 3. NISN & Badge Rombel
            val nisnText = item.nisn?.takeIf { it.isNotEmpty() } ?: "-"
            tvNisnSiswaKelola.text = "NISN: $nisnText"

            val kelasText = item.rombelKelas?.namaKelas?.takeIf { it.isNotEmpty() }
                ?: defaultNamaKelas
            tvBadgeRombelSiswa.text = kelasText

            // 4. Tombol Aksi: Edit, Mutasi Rombel, Hapus Siswa
            btnEditSiswa.setOnClickListener {
                onEditClick(item)
            }

            btnMutasiRombel.setOnClickListener {
                onMutasiClick(item)
            }

            btnHapusSiswa.setOnClickListener {
                onHapusClick(item)
            }
        }
    }

    override fun getItemCount(): Int = listSiswa.size

    fun submitList(newList: List<SiswaKelolaResponse>) {
        this.fullList = newList
        applyFilter()
    }

    fun updateData(newList: List<SiswaKelolaResponse>) {
        submitList(newList)
    }

    fun filter(query: String) {
        this.currentQuery = query.lowercase().trim()
        applyFilter()
    }

    fun filterByGender(gender: String) {
        this.currentGenderFilter = gender.uppercase()
        applyFilter()
    }

    private fun applyFilter() {
        var result = fullList

        if (currentGenderFilter == "L") {
            result = result.filter {
                val jk = it.jenisKelamin?.uppercase().orEmpty()
                jk.contains("L")
            }
        } else if (currentGenderFilter == "P") {
            result = result.filter {
                val jk = it.jenisKelamin?.uppercase().orEmpty()
                jk.contains("P")
            }
        }

        if (currentQuery.isNotEmpty()) {
            result = result.filter {
                it.namaLengkap.orEmpty().lowercase().contains(currentQuery) ||
                it.nisn.orEmpty().lowercase().contains(currentQuery) ||
                it.rombelKelas?.namaKelas.orEmpty().lowercase().contains(currentQuery)
            }
        }

        this.listSiswa = result
        notifyDataSetChanged()
    }
}
