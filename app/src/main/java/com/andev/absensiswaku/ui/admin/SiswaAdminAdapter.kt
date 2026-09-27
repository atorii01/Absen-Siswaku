package com.andev.absensiswaku.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.SiswaAdminResponse
import com.andev.absensiswaku.databinding.ItemSiswaAdminBinding
import java.util.Locale

class SiswaAdminAdapter(
    private val onEditClick: (SiswaAdminResponse) -> Unit,
    private val onMutasiClick: (SiswaAdminResponse) -> Unit,
    private val onDeleteClick: (SiswaAdminResponse) -> Unit
) : RecyclerView.Adapter<SiswaAdminAdapter.SiswaViewHolder>() {

    private val allItems = mutableListOf<SiswaAdminResponse>()
    private val displayedItems = mutableListOf<SiswaAdminResponse>()

    private val avatarDrawables = listOf(
        R.drawable.bg_avatar_circle_mint,
        R.drawable.bg_avatar_circle_blue,
        R.drawable.bg_avatar_circle_amber,
        R.drawable.bg_avatar_circle_purple
    )

    fun submitList(list: List<SiswaAdminResponse>) {
        allItems.clear()
        allItems.addAll(list)
        displayedItems.clear()
        displayedItems.addAll(list)
        notifyDataSetChanged()
    }

    fun filter(query: String, filterKelasId: Int? = null) {
        val q = query.trim().lowercase(Locale.getDefault())
        displayedItems.clear()

        val baseList = if (filterKelasId != null && filterKelasId > 0) {
            allItems.filter { it.idKelas == filterKelasId }
        } else {
            allItems
        }

        if (q.isEmpty()) {
            displayedItems.addAll(baseList)
        } else {
            displayedItems.addAll(baseList.filter {
                val nama = it.namaLengkap.lowercase(Locale.getDefault())
                val nisn = it.nisn.lowercase(Locale.getDefault())
                val rombel = it.rombelKelas?.namaKelas?.lowercase(Locale.getDefault()).orEmpty()
                nama.contains(q) || nisn.contains(q) || rombel.contains(q)
            })
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiswaViewHolder {
        val binding = ItemSiswaAdminBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SiswaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SiswaViewHolder, position: Int) {
        holder.bind(displayedItems[position], position)
    }

    override fun getItemCount(): Int = displayedItems.size

    inner class SiswaViewHolder(private val binding: ItemSiswaAdminBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(siswa: SiswaAdminResponse, position: Int) {
            // Inisial avatar standar aman dari null
            binding.tvAvatarInitials.text = siswa.initialLetters
            val drawableBg = avatarDrawables[position % avatarDrawables.size]
            binding.tvAvatarInitials.setBackgroundResource(drawableBg)

            // Bind nama dan NISN/kelas siswa
            binding.tvNamaSiswa.text = siswa.namaLengkap
            binding.tvNisnKelas.text = "${siswa.rombelKelas?.namaKelas ?: "Kelas XII"} • NISN: ${siswa.nisn}"

            binding.btnEditSiswa.setOnClickListener {
                onEditClick(siswa)
            }

            binding.btnMutasiRombel.setOnClickListener {
                onMutasiClick(siswa)
            }

            binding.btnHapusSiswa.setOnClickListener {
                onDeleteClick(siswa)
            }
        }
    }
}
