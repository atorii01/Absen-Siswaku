package com.andev.absensiswaku.ui.walas

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.PengajuanIzinResponse
import com.andev.absensiswaku.data.network.RiwayatModel
import com.andev.absensiswaku.data.network.SiswaMiniResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.databinding.LayoutBottomSheetDetailPresensiBinding
import com.andev.absensiswaku.util.applyBounceEffect
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class BottomSheetDetailPresensiDialog : BottomSheetDialogFragment() {

    private var _binding: LayoutBottomSheetDetailPresensiBinding? = null
    private val binding get() = _binding!!

    private var tanggalFormatted: String = ""
    private var rawTanggal: String = ""
    private var idKelas: Int = 1
    private var namaKelas: String = "XII RPL"

    private lateinit var adapter: DetailSiswaPresensiAdapter
    private var allSiswaItems: List<SiswaDetailPresensiItem> = emptyList()

    companion object {
        private const val ARG_TANGGAL = "arg_tanggal"
        private const val ARG_RAW_TANGGAL = "arg_raw_tanggal"
        private const val ARG_ID_KELAS = "arg_id_kelas"
        private const val ARG_NAMA_KELAS = "arg_nama_kelas"

        fun newInstance(
            tanggal: String,
            rawTanggal: String,
            idKelas: Int,
            namaKelas: String
        ): BottomSheetDetailPresensiDialog {
            val fragment = BottomSheetDetailPresensiDialog()
            fragment.arguments = Bundle().apply {
                putString(ARG_TANGGAL, tanggal)
                putString(ARG_RAW_TANGGAL, rawTanggal)
                putInt(ARG_ID_KELAS, idKelas)
                putString(ARG_NAMA_KELAS, namaKelas)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tanggalFormatted = it.getString(ARG_TANGGAL).orEmpty()
            rawTanggal = it.getString(ARG_RAW_TANGGAL).orEmpty()
            idKelas = it.getInt(ARG_ID_KELAS, 1)
            namaKelas = it.getString(ARG_NAMA_KELAS).orEmpty().ifEmpty { "Kelas $idKelas" }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutBottomSheetDetailPresensiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupRecyclerView()
        setupFilterChips()
        loadDataFromSupabase()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeader() {
        binding.tvSubjudulDetail.text = if (tanggalFormatted.isNotBlank()) {
            "$namaKelas • $tanggalFormatted"
        } else {
            namaKelas
        }

        binding.btnCloseDialog.applyBounceEffect {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        adapter = DetailSiswaPresensiAdapter { item ->
            // Klik item siswa jika diperlukan info lebih lanjut
        }
        binding.rvDetailSiswaPresensi.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BottomSheetDetailPresensiDialog.adapter
        }
    }

    private fun setupFilterChips() {
        binding.chipFilterSemua.applyBounceEffect {
            switchChipFilter("SEMUA")
        }
        binding.chipFilterHadir.applyBounceEffect {
            switchChipFilter("HADIR")
        }
        binding.chipFilterIzin.applyBounceEffect {
            switchChipFilter("IZIN")
        }
        binding.chipFilterAlpa.applyBounceEffect {
            switchChipFilter("ALPA")
        }
    }

    private fun switchChipFilter(category: String) {
        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_month_active)
        val inactiveBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_month_inactive)
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val textSecondaryColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        // Reset semua chip
        binding.chipFilterSemua.background = inactiveBg
        binding.tvFilterSemua.setTextColor(textSecondaryColor)

        binding.chipFilterHadir.background = inactiveBg
        binding.tvFilterHadir.setTextColor(textSecondaryColor)

        binding.chipFilterIzin.background = inactiveBg
        binding.tvFilterIzin.setTextColor(textSecondaryColor)

        binding.chipFilterAlpa.background = inactiveBg
        binding.tvFilterAlpa.setTextColor(textSecondaryColor)

        // Aktifkan chip terpilih
        when (category.uppercase()) {
            "SEMUA" -> {
                binding.chipFilterSemua.background = activeBg
                binding.tvFilterSemua.setTextColor(whiteColor)
            }
            "HADIR" -> {
                binding.chipFilterHadir.background = activeBg
                binding.tvFilterHadir.setTextColor(whiteColor)
            }
            "IZIN" -> {
                binding.chipFilterIzin.background = activeBg
                binding.tvFilterIzin.setTextColor(whiteColor)
            }
            "ALPA" -> {
                binding.chipFilterAlpa.background = activeBg
                binding.tvFilterAlpa.setTextColor(whiteColor)
            }
        }

        adapter.filter(category)
        binding.rvDetailSiswaPresensi.scheduleLayoutAnimation()
        updateEmptyStateVisibility()
    }

    private fun updateEmptyStateVisibility() {
        if (adapter.itemCount == 0) {
            binding.layoutEmptyDetail.visibility = View.VISIBLE
            binding.rvDetailSiswaPresensi.visibility = View.GONE
        } else {
            binding.layoutEmptyDetail.visibility = View.GONE
            binding.rvDetailSiswaPresensi.visibility = View.VISIBLE
        }
    }

    /**
     * Mengambil data kehadiran kelas secara riil dari Supabase:
     * 1. Daftar seluruh siswa kelas binaan: siswa?id_kelas=eq.$idKelas
     * 2. Catatan presensi pada tanggal tersebut: presensi_harian?id_kelas=eq.$idKelas&tanggal=eq.$rawTanggal
     * 3. Catatan izin sah (disetujui): pengajuan_izin?id_kelas=eq.$idKelas&status_verifikasi=eq.Disetujui
     */
    private fun loadDataFromSupabase() {
        if (!isAdded || _binding == null) return
        binding.progressBarDetail.visibility = View.VISIBLE
        binding.layoutEmptyDetail.visibility = View.GONE
        binding.rvDetailSiswaPresensi.visibility = View.GONE

        // 1. Ambil daftar siswa kelas binaan
        SupabaseClient.instance.getSiswaByKelas(filterKelas = "eq.$idKelas")
            .enqueue(object : Callback<List<SiswaMiniResponse>> {
                override fun onResponse(
                    call: Call<List<SiswaMiniResponse>>,
                    response: Response<List<SiswaMiniResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    val siswaList = response.body().orEmpty()
                    loadPresensiRecords(siswaList)
                }

                override fun onFailure(call: Call<List<SiswaMiniResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    loadPresensiRecords(emptyList())
                }
            })
    }

    private fun loadPresensiRecords(siswaList: List<SiswaMiniResponse>) {
        if (rawTanggal.isBlank()) {
            loadIzinRecords(siswaList, emptyList())
            return
        }

        // 2. Ambil catatan presensi pada tanggal terkait
        SupabaseClient.instance.getPresensiHariIniWalas(
            filterTanggal = "eq.$rawTanggal",
            filterKelas = "eq.$idKelas"
        ).enqueue(object : Callback<List<RiwayatModel>> {
            override fun onResponse(
                call: Call<List<RiwayatModel>>,
                response: Response<List<RiwayatModel>>
            ) {
                if (!isAdded || _binding == null) return
                val presensiList = response.body().orEmpty()
                loadIzinRecords(siswaList, presensiList)
            }

            override fun onFailure(call: Call<List<RiwayatModel>>, t: Throwable) {
                if (!isAdded || _binding == null) return
                // Fallback coba ambil presensi kelas keseluruhan jika filter tanggal spesifik nihil
                fetchPresensiKelasFallback(siswaList)
            }
        })
    }

    private fun fetchPresensiKelasFallback(siswaList: List<SiswaMiniResponse>) {
        SupabaseClient.instance.getPresensiKelas(filterKelas = "eq.$idKelas")
            .enqueue(object : Callback<List<RiwayatModel>> {
                override fun onResponse(
                    call: Call<List<RiwayatModel>>,
                    response: Response<List<RiwayatModel>>
                ) {
                    if (!isAdded || _binding == null) return
                    val allPresensi = response.body().orEmpty()
                    val filteredByDate = allPresensi.filter { it.tanggal?.equals(rawTanggal, ignoreCase = true) == true }
                    loadIzinRecords(siswaList, filteredByDate)
                }

                override fun onFailure(call: Call<List<RiwayatModel>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    loadIzinRecords(siswaList, emptyList())
                }
            })
    }

    private fun loadIzinRecords(
        siswaList: List<SiswaMiniResponse>,
        presensiList: List<RiwayatModel>
    ) {
        // 3. Ambil pengajuan izin sah (disetujui)
        SupabaseClient.instance.getIzinDisetujuiSemuaKelas()
            .enqueue(object : Callback<List<PengajuanIzinResponse>> {
                override fun onResponse(
                    call: Call<List<PengajuanIzinResponse>>,
                    response: Response<List<PengajuanIzinResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    val allIzin = response.body().orEmpty()
                    val validIzin = allIzin.filter { izin ->
                        val isClassMatch = (izin.idKelas == idKelas || izin.siswa?.idKelas == idKelas)
                        val tMulai = izin.tanggalMulai ?: ""
                        val tSelesai = izin.tanggalSelesai ?: tMulai
                        val isDateMatch = if (rawTanggal.isNotBlank() && tMulai.isNotBlank()) {
                            rawTanggal in tMulai..tSelesai || tMulai == rawTanggal
                        } else true
                        isClassMatch && isDateMatch
                    }
                    processAndDisplayData(siswaList, presensiList, validIzin)
                }

                override fun onFailure(call: Call<List<PengajuanIzinResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    processAndDisplayData(siswaList, presensiList, emptyList())
                }
            })
    }

    /**
     * Cocokkan data 36 siswa binaan dengan catatan presensi dan izin pada tanggal ini
     */
    private fun processAndDisplayData(
        siswaList: List<SiswaMiniResponse>,
        presensiList: List<RiwayatModel>,
        izinList: List<PengajuanIzinResponse>
    ) {
        if (!isAdded || _binding == null) return
        binding.progressBarDetail.visibility = View.GONE

        val presensiMap = presensiList.filter { it.siswaId != null }.associateBy { it.siswaId }
        val izinMap = izinList.filter { it.siswaId != null }.associateBy { it.siswaId }

        var countHadir = 0
        var countTerlambat = 0
        var countIzin = 0
        var countAlpa = 0

        val items = siswaList.mapIndexed { index, siswa ->
            val noUrut = String.format(Locale.getDefault(), "%02d", index + 1)
            val nama = siswa.namaLengkap?.takeIf { it.isNotBlank() }
                ?: "Siswa ${siswa.id ?: ""}".trim()
            val nisn = siswa.nisn?.takeIf { it.isNotBlank() } ?: "-"
            val inisial = siswa.initialLetters

            val presensi = siswa.id?.let { presensiMap[it] }
            val izin = siswa.id?.let { izinMap[it] }

            when {
                // Kasus 1: Siswa tercatat di presensi_harian
                presensi != null -> {
                    val statusDb = presensi.status?.trim().orEmpty()
                    val jam = presensi.displayJamMasuk.take(5).ifEmpty { "06:30" }
                    val jarak = presensi.jarakGerbangMeter?.toInt()
                    val jarakText = if (jarak != null && jarak > 0) " • Akurasi GPS ${jarak}m" else ""

                    if (statusDb.equals("Terlambat", true)) {
                        countTerlambat++
                        SiswaDetailPresensiItem(
                            nomorUrut = noUrut,
                            siswaId = siswa.id,
                            namaLengkap = nama,
                            inisial = inisial,
                            nisn = nisn,
                            subteks = "Masuk $jam WIB$jarakText",
                            status = "TERLAMBAT",
                            badgeLabel = "Terlambat",
                            badgeBgResId = R.drawable.bg_badge_terlambat,
                            badgeTextColor = Color.parseColor("#B45309")
                        )
                    } else if (statusDb.equals("Izin", true) || statusDb.equals("Sakit", true)) {
                        countIzin++
                        val jenis = statusDb.replaceFirstChar { it.uppercase() }
                        val ket = presensi.keterangan.takeIf { it.isNotBlank() } ?: "Disetujui Wali Kelas"
                        SiswaDetailPresensiItem(
                            nomorUrut = noUrut,
                            siswaId = siswa.id,
                            namaLengkap = nama,
                            inisial = inisial,
                            nisn = nisn,
                            subteks = "$jenis: $ket",
                            status = "IZIN",
                            badgeLabel = jenis,
                            badgeBgResId = R.drawable.bg_badge_izin,
                            badgeTextColor = Color.parseColor("#1D4ED8")
                        )
                    } else {
                        // Default Hadir / TEPAT_WAKTU
                        countHadir++
                        SiswaDetailPresensiItem(
                            nomorUrut = noUrut,
                            siswaId = siswa.id,
                            namaLengkap = nama,
                            inisial = inisial,
                            nisn = nisn,
                            subteks = "Masuk $jam WIB$jarakText",
                            status = "HADIR",
                            badgeLabel = "Hadir",
                            badgeBgResId = R.drawable.bg_badge_hadir,
                            badgeTextColor = Color.parseColor("#15803D")
                        )
                    }
                }

                // Kasus 2: Siswa tercatat di pengajuan_izin yang disetujui
                izin != null -> {
                    countIzin++
                    val jenis = izin.jenisIzin?.trim().orEmpty().ifEmpty { "Izin" }.replaceFirstChar { it.uppercase() }
                    val ket = izin.keterangan?.trim().orEmpty().ifEmpty { "Surat Disetujui" }
                    SiswaDetailPresensiItem(
                        nomorUrut = noUrut,
                        siswaId = siswa.id,
                        namaLengkap = nama,
                        inisial = inisial,
                        nisn = nisn,
                        subteks = "$jenis: $ket",
                        status = "IZIN",
                        badgeLabel = jenis,
                        badgeBgResId = R.drawable.bg_badge_izin,
                        badgeTextColor = Color.parseColor("#1D4ED8")
                    )
                }

                // Kasus 3: Tidak ada di presensi maupun izin -> Alpa
                else -> {
                    countAlpa++
                    SiswaDetailPresensiItem(
                        nomorUrut = noUrut,
                        siswaId = siswa.id,
                        namaLengkap = nama,
                        inisial = inisial,
                        nisn = nisn,
                        subteks = "NISN: $nisn • Alpa (Tanpa Keterangan)",
                        status = "ALPA",
                        badgeLabel = "Alpa",
                        badgeBgResId = R.drawable.bg_badge_alpa,
                        badgeTextColor = Color.parseColor("#DC2626")
                    )
                }
            }
        }

        allSiswaItems = items

        // Update 4 Mini Card Ringkasan Sejajar
        binding.tvCountHadir.text = countHadir.toString()
        binding.tvCountTerlambat.text = countTerlambat.toString()
        binding.tvCountIzin.text = countIzin.toString()
        binding.tvCountAlpa.text = countAlpa.toString()

        // Update Counter pada Filter Chip
        binding.tvFilterSemua.text = "Semua (${items.size})"
        binding.tvFilterHadir.text = "Hadir (${countHadir + countTerlambat})"
        binding.tvFilterIzin.text = "Izin/Sakit ($countIzin)"
        binding.tvFilterAlpa.text = "Alpa ($countAlpa)"

        // Tampilkan data ke adapter
        adapter.setAllData(items)
        binding.rvDetailSiswaPresensi.scheduleLayoutAnimation()
        updateEmptyStateVisibility()
    }
}
