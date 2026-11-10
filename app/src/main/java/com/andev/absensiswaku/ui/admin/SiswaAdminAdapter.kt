package com.andev.absensiswaku.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.SiswaAdminResponse
import com.andev.absensiswaku.databinding.ItemSiswaAdminBinding
import java.util.Locale
import kotlin.math.ceil

class SiswaAdminAdapter(
    private val onEditClick: (SiswaAdminResponse) -> Unit,
    private val onMutasiClick: (SiswaAdminResponse) -> Unit,
    private val onDeleteClick: (SiswaAdminResponse) -> Unit
) : RecyclerView.Adapter<SiswaAdminAdapter.SiswaViewHolder>() {

    private val allItems = mutableListOf<SiswaAdminResponse>()
    private val filteredItems = mutableListOf<SiswaAdminResponse>()
    private val displayedItems = mutableListOf<SiswaAdminResponse>()

    var currentPage: Int = 1
        private set
    var totalPages: Int = 1
        private set
    val pageSize: Int = 20

    var onPageChangedListener: ((page: Int, totalPages: Int, totalFiltered: Int, startItem: Int, endItem: Int) -> Unit)? = null

    private val avatarDrawables = listOf(
        R.drawable.bg_avatar_circle_mint,
        R.drawable.bg_avatar_circle_blue,
        R.drawable.bg_avatar_circle_amber,
        R.drawable.bg_avatar_circle_purple
    )

    fun submitList(list: List<SiswaAdminResponse>) {
        allItems.clear()
        allItems.addAll(list)
        filter("", null, targetPage = 1)
    }

    fun filter(query: String, filterKelasId: Int? = null, targetPage: Int = 1) {
        val q = query.trim().lowercase(Locale.getDefault())
        filteredItems.clear()

        val baseList = if (filterKelasId != null && filterKelasId > 0) {
            allItems.filter { it.idKelas == filterKelasId }
        } else {
            allItems
        }

        if (q.isEmpty()) {
            filteredItems.addAll(baseList)
        } else {
            filteredItems.addAll(baseList.filter {
                val nama = it.namaLengkap.lowercase(Locale.getDefault())
                val nisn = it.nisn.lowercase(Locale.getDefault())
                val rombel = it.rombelKelas?.namaKelas?.lowercase(Locale.getDefault()).orEmpty()
                nama.contains(q) || nisn.contains(q) || rombel.contains(q)
            })
        }

        totalPages = if (filteredItems.isEmpty()) 1 else ceil(filteredItems.size / pageSize.toDouble()).toInt().coerceAtLeast(1)
        goToPage(targetPage)
    }

    fun goToPage(page: Int) {
        currentPage = page.coerceIn(1, totalPages)
        displayedItems.clear()

        if (filteredItems.isNotEmpty()) {
            val startIndex = (currentPage - 1) * pageSize
            val endIndex = (startIndex + pageSize).coerceAtMost(filteredItems.size)
            if (startIndex < filteredItems.size) {
                displayedItems.addAll(filteredItems.subList(startIndex, endIndex))
            }
            val startDisplay = if (displayedItems.isEmpty()) 0 else startIndex + 1
            val endDisplay = endIndex
            notifyDataSetChanged()
            onPageChangedListener?.invoke(currentPage, totalPages, filteredItems.size, startDisplay, endDisplay)
        } else {
            notifyDataSetChanged()
            onPageChangedListener?.invoke(1, 1, 0, 0, 0)
        }
    }

    fun nextPage(): Boolean {
        if (currentPage < totalPages) {
            goToPage(currentPage + 1)
            return true
        }
        return false
    }

    fun prevPage(): Boolean {
        if (currentPage > 1) {
            goToPage(currentPage - 1)
            return true
        }
        return false
    }

    fun getTotalFilteredCount(): Int = filteredItems.size

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
