package com.andev.absensiswaku.ui.guru

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.PengajuanIzinResponse
import com.andev.absensiswaku.data.network.PresensiHarianResponse
import com.andev.absensiswaku.data.network.RombelMapelResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.databinding.FragmentGuruPresensiBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GuruPresensiFragment : Fragment() {

    private var _binding: FragmentGuruPresensiBinding? = null
    private val binding get() = _binding!!

    private lateinit var rombelAdapter: RombelMapelAdapter
    private var allRombelList: List<RombelMapelResponse> = emptyList()
    private var presensiHariIniList: List<PresensiHarianResponse> = emptyList()
    private var izinDisetujuiList: List<PengajuanIzinResponse> = emptyList()

    private var selectedMajorFilter: String = "ALL" // "ALL", "AKL", "MP", "BR", "BD", "RPL", "UPW"
    private var currentSearchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuruPresensiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndDate()
        setupBlinkingLiveDot()
        setupRecyclerView()
        setupSearchAndFilterChips()
        setupLiveSyncButton()

        loadDataPresensiGlobal()
    }

    private fun setupHeaderAndDate() {
        val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.forLanguageTag("id-ID"))
        binding.tvTanggalHariIni.text = dateFormat.format(Date())
    }

    private fun setupBlinkingLiveDot() {
        val blinkAnimation = AlphaAnimation(1.0f, 0.2f).apply {
            duration = 750
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        binding.dotLiveStatus.startAnimation(blinkAnimation)
    }

    private fun setupRecyclerView() {
        rombelAdapter = RombelMapelAdapter(emptyList()) { rombelItem ->
            showDetailSiswaBottomSheet(rombelItem)
        }

        binding.rvRombelPresensi.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rombelAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSearchAndFilterChips() {
        // Search Input
        binding.etSearchRombel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim().orEmpty()
                binding.btnClearSearch.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchRombel.text?.clear()
        }

        // Horizontal Chip Filters: "Semua Rombel", "AKL", "MP", "BR", "BD", "RPL", "UPW"
        val chips = listOf(
            binding.chipSemuaRombel to "ALL",
            binding.chipAkl to "AKL",
            binding.chipMp to "MP",
            binding.chipBr to "BR",
            binding.chipBd to "BD",
            binding.chipRpl to "RPL",
            binding.chipUpw to "UPW"
        )

        chips.forEach { (chipView, majorKey) ->
            chipView.setOnClickListener {
                selectedMajorFilter = majorKey
                updateChipStates(chips, chipView)
                applyFilters()
            }
        }
    }

    private fun updateChipStates(
        allChips: List<Pair<TextView, String>>,
        selectedChip: TextView
    ) {
        val ctx = context ?: return
        val activeBg = ContextCompat.getDrawable(ctx, R.drawable.bg_chip_filter_active)
        val inactiveBg = ContextCompat.getDrawable(ctx, R.drawable.bg_chip_filter_inactive)
        val activeColor = ContextCompat.getColor(ctx, R.color.white)
        val inactiveColor = ContextCompat.getColor(ctx, R.color.text_secondary)

        allChips.forEach { (chip, _) ->
            if (chip == selectedChip) {
                chip.background = activeBg
                chip.setTextColor(activeColor)
            } else {
                chip.background = inactiveBg
                chip.setTextColor(inactiveColor)
            }
        }
    }

    private fun setupLiveSyncButton() {
        binding.tvStatusLiveSync.setOnClickListener {
            Toast.makeText(requireContext(), "Menyinkronkan data presensi SMKN 8 Jakarta...", Toast.LENGTH_SHORT).show()
            loadDataPresensiGlobal(isRefreshing = true)
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * Memanggil data 10 rombel asli dari Supabase (tabel rombel_kelas & users)
     */
    private fun loadDataPresensiGlobal(isRefreshing: Boolean = false) {
        if (!isRefreshing) {
            binding.progressBarLoading.visibility = View.VISIBLE
        }

        val todayDate = getTodayDateString()

        // 1. Ambil 10 Rombel Asli dari Supabase (relasi users!wali_kelas_id)
        SupabaseClient.instance.getRombelMapel()
            .enqueue(object : Callback<List<RombelMapelResponse>> {
                override fun onResponse(
                    call: Call<List<RombelMapelResponse>>,
                    response: Response<List<RombelMapelResponse>>
                ) {
                    allRombelList = response.body().orEmpty()

                    // 2. Ambil Presensi Harian Hari Ini untuk Semua Rombel
                    loadPresensiAndIzin(todayDate)
                }

                override fun onFailure(call: Call<List<RombelMapelResponse>>, t: Throwable) {
                    allRombelList = emptyList()
                    finishLoading()
                    Toast.makeText(context, "Gagal memuat rombel: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    calculateAggregation()
                }
            })
    }

    private fun loadPresensiAndIzin(todayDate: String) {
        SupabaseClient.instance.getPresensiHariIniSemuaKelas(filterTanggal = "eq.$todayDate")
            .enqueue(object : Callback<List<PresensiHarianResponse>> {
                override fun onResponse(
                    call: Call<List<PresensiHarianResponse>>,
                    response: Response<List<PresensiHarianResponse>>
                ) {
                    presensiHariIniList = response.body().orEmpty()

                    // 3. Ambil Pengajuan Izin yang Disetujui
                    loadIzinDisetujui()
                }

                override fun onFailure(call: Call<List<PresensiHarianResponse>>, t: Throwable) {
                    presensiHariIniList = emptyList()
                    loadIzinDisetujui()
                }
            })
    }

    private fun loadIzinDisetujui() {
        SupabaseClient.instance.getIzinDisetujuiSemuaKelas()
            .enqueue(object : Callback<List<PengajuanIzinResponse>> {
                override fun onResponse(
                    call: Call<List<PengajuanIzinResponse>>,
                    response: Response<List<PengajuanIzinResponse>>
                ) {
                    finishLoading()
                    izinDisetujuiList = response.body().orEmpty()
                    calculateAggregation()
                }

                override fun onFailure(call: Call<List<PengajuanIzinResponse>>, t: Throwable) {
                    finishLoading()
                    izinDisetujuiList = emptyList()
                    calculateAggregation()
                }
            })
    }

    private fun finishLoading() {
        if (_binding == null) return
        binding.progressBarLoading.visibility = View.GONE
    }

    private fun calculateAggregation() {
        if (_binding == null) return

        val todayDate = getTodayDateString()

        // 1. Agregasi Anti-Duplikat dari transaksi presensi nyata
        val presensiUnik = presensiHariIniList
            .filter { it.siswaId != null }
            .distinctBy { it.siswaId }

        // 2. Filter izin disetujui yang AKTIF HARI INI secara ketat
        val izinHariIni = izinDisetujuiList
            .filter { izin ->
                if (izin.siswaId == null) return@filter false
                val tglMulai = izin.tanggalMulai ?: ""
                val tglSelesai = izin.tanggalSelesai ?: tglMulai
                if (tglMulai.isBlank()) false else (todayDate in tglMulai..tglSelesai || tglMulai == todayDate)
            }
            .distinctBy { it.siswaId }

        var globalHadir = 0
        var globalIzin = 0
        var globalAlpa = 0
        var globalKapasitas = 0

        allRombelList.forEach { rombel ->
            // Kuota otomatis: BD (ID 8) dan UPW (ID 10) kuota 35, rombel lainnya 36 (Total 358 siswa)
            val kuota = rombel.kapasitasKuota ?: if (rombel.id == 8 || rombel.id == 10) 35 else 36

            val hadirRombel = presensiUnik.count {
                it.idKelas == rombel.id && (it.status.equals("Hadir", true) || it.status.equals("Terlambat", true))
            }
            val izinRombel = izinHariIni.count {
                it.idKelas == rombel.id || it.siswa?.idKelas == rombel.id
            }
            val alpaRombel = (kuota - (hadirRombel + izinRombel)).coerceAtLeast(0)

            // Akumulasi murni ke metrik global
            globalHadir += hadirRombel
            globalIzin += izinRombel
            globalAlpa += alpaRombel
            globalKapasitas += kuota

            rombel.hadirCount = hadirRombel
            rombel.izinCount = izinRombel
            rombel.alpaCount = alpaRombel
            rombel.persentaseHadir = if (kuota > 0) {
                (hadirRombel.toFloat() / kuota.toFloat()) * 100f
            } else 0f
        }

        // Pasang ke UI Global ATAS murni dari hasil penjumlahan 10 kartu rombel
        binding.tvTotalHadirGlobal.text = "$globalHadir / $globalKapasitas"
        val globalPercent = if (globalKapasitas > 0) {
            (globalHadir.toDouble() / globalKapasitas.toDouble()) * 100.0
        } else 0.0
        binding.tvPersenHadirGlobal.text = String.format(Locale.US, "%.1f%% Total", globalPercent)
        binding.tvTotalIzinGlobal.text = "$globalIzin Siswa"
        binding.tvSubteksIzinGlobal.text = "Disetujui Walas"
        binding.tvTotalAlpaGlobal.text = "$globalAlpa Siswa"
        binding.tvSubteksAlpaGlobal.text = "Belum Hadir"

        // Update teks chip "Semua Rombel (10)"
        binding.chipSemuaRombel.text = "Semua Rombel (${allRombelList.size})"

        applyFilters()
    }

    private fun applyFilters() {
        if (_binding == null) return

        var filtered = allRombelList

        // 1. Filter Chip: "AKL", "MP", "BR", "BD", "RPL", "UPW" berdasarkan namaKelas
        if (selectedMajorFilter != "ALL") {
            filtered = filtered.filter {
                it.namaKelas.contains(selectedMajorFilter, ignoreCase = true)
            }
        }

        // 2. Filter Search Query
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.namaKelas.contains(currentSearchQuery, ignoreCase = true) ||
                (it.waliKelas?.namaLengkap ?: "").contains(currentSearchQuery, ignoreCase = true) ||
                (it.jurusan ?: "").contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // Masukkan daftar rombel langsung ke RombelMapelAdapter.submitList(listRombel)
        rombelAdapter.submitList(filtered)

        // Update Header Counter: Rombongan Belajar (10 Kelas Aktif)
        binding.tvHeaderDaftarRombel.text = "Rombongan Belajar (${filtered.size} Kelas Aktif)"

        // Empty state visibility
        if (filtered.isEmpty()) {
            binding.layoutEmptyRombel.visibility = View.VISIBLE
            binding.rvRombelPresensi.visibility = View.GONE
        } else {
            binding.layoutEmptyRombel.visibility = View.GONE
            binding.rvRombelPresensi.visibility = View.VISIBLE
        }
    }

    /**
     * Menampilkan BottomSheet Dialog Daftar Siswa Read-Only
     */
    private fun showDetailSiswaBottomSheet(rombel: RombelMapelResponse) {
        val todayDate = getTodayDateString()
        val izinHariIni = izinDisetujuiList.filter { izin ->
            val tglMulai = izin.tanggalMulai ?: ""
            val tglSelesai = izin.tanggalSelesai ?: tglMulai
            if (tglMulai.isBlank()) false else (todayDate in tglMulai..tglSelesai || tglMulai == todayDate)
        }
        val bottomSheet = BottomSheetSiswaReadonlyDialog.newInstance(
            rombel = rombel,
            presensiList = presensiHariIniList,
            izinList = izinHariIni
        )
        bottomSheet.show(childFragmentManager, "BottomSheetSiswaReadonlyDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
