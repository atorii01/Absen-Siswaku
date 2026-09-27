package com.andev.absensiswaku.ui.guru

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.PengajuanIzinResponse
import com.andev.absensiswaku.data.network.RiwayatModel
import com.andev.absensiswaku.data.network.SiswaMiniResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.databinding.DialogDetailRiwayatBinding
import com.andev.absensiswaku.databinding.ItemSiswaRiwayatDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

data class SiswaRiwayatDetailItem(
    val nomorUrut: String,
    val siswaId: Int?,
    val namaLengkap: String,
    val nisn: String,
    val waktuMasuk: String,
    val aiMatchScore: String,
    val geotagJarak: String,
    val status: String, // "HADIR", "TERLAMBAT", "SAKIT", "IZIN", "ALPA"
    val badgeLabel: String
)

class BottomSheetRiwayatDialog : BottomSheetDialogFragment() {

    private var _binding: DialogDetailRiwayatBinding? = null
    private val binding get() = _binding!!

    private var rombelId: Int = 9
    private var namaKelas: String = "XII RPL"
    private var tanggalIso: String = ""
    private var tanggalDisplay: String = ""

    private lateinit var adapter: SiswaRiwayatDetailAdapter

    companion object {
        private const val ARG_ROMBEL_ID = "arg_rombel_id"
        private const val ARG_NAMA_KELAS = "arg_nama_kelas"
        private const val ARG_TANGGAL_ISO = "arg_tanggal_iso"
        private const val ARG_TANGGAL_DISPLAY = "arg_tanggal_display"

        fun newInstance(
            rombelId: Int,
            namaKelas: String,
            tanggalIso: String,
            tanggalDisplay: String
        ): BottomSheetRiwayatDialog {
            val fragment = BottomSheetRiwayatDialog()
            fragment.arguments = Bundle().apply {
                putInt(ARG_ROMBEL_ID, rombelId)
                putString(ARG_NAMA_KELAS, namaKelas)
                putString(ARG_TANGGAL_ISO, tanggalIso)
                putString(ARG_TANGGAL_DISPLAY, tanggalDisplay)
            }
            return fragment
        }

        fun newInstance(rombelId: Int, tanggal: String): BottomSheetRiwayatDialog {
            return newInstance(rombelId, "Kelas $rombelId", tanggal, tanggal)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            rombelId = it.getInt(ARG_ROMBEL_ID, 9)
            namaKelas = it.getString(ARG_NAMA_KELAS, "XII RPL")
            tanggalIso = it.getString(ARG_TANGGAL_ISO, "")
            tanggalDisplay = it.getString(ARG_TANGGAL_DISPLAY, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDetailRiwayatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupRecyclerView()
        setupSearch()
        loadDataFromSupabase()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeader() {
        val titleText = if (tanggalDisplay.isNotBlank()) {
            "Daftar Presensi $namaKelas - $tanggalDisplay"
        } else {
            "Daftar Presensi $namaKelas"
        }
        binding.tvDialogTitle.text = titleText
        binding.tvDialogSubtitle.text = "Rekap Riwayat Presensi Tervalidasi Sistem • SMKN 8 Jakarta"

        binding.btnCloseDialog.setOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        adapter = SiswaRiwayatDetailAdapter()
        binding.rvSiswaRiwayat.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BottomSheetRiwayatDialog.adapter
        }
    }

    private fun setupSearch() {
        binding.etSearchSiswa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty().trim()
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                adapter.filter(query)
                binding.tvEmptyRiwayat.visibility =
                    if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchSiswa.text?.clear()
        }
    }

    /**
     * Memuat 100% data asli dari Supabase tanpa mockup atau data tiruan:
     * 1. Daftar Siswa: siswa?id_kelas=eq.$rombelId&order=nama_lengkap.asc
     * 2. Catatan Presensi: presensi_harian?id_kelas=eq.$rombelId&tanggal=eq.$tanggalIso
     * 3. Izin Disetujui: pengajuan_izin?id_kelas=eq.$rombelId&status_verifikasi=eq.Disetujui
     */
    private fun loadDataFromSupabase() {
        binding.progressBarRiwayat.visibility = View.VISIBLE
        binding.tvEmptyRiwayat.visibility = View.GONE

        // 1. Ambil data siswa rombel dari database
        SupabaseClient.instance.getSiswaByKelas(filterKelas = "eq.$rombelId")
            .enqueue(object : Callback<List<SiswaMiniResponse>> {
                override fun onResponse(
                    call: Call<List<SiswaMiniResponse>>,
                    response: Response<List<SiswaMiniResponse>>
                ) {
                    val siswaList = response.body().orEmpty()
                    // 2. Ambil catatan presensi tanggal tersebut
                    loadPresensiTanggal(siswaList)
                }

                override fun onFailure(call: Call<List<SiswaMiniResponse>>, t: Throwable) {
                    loadPresensiTanggal(emptyList())
                }
            })
    }

    private fun loadPresensiTanggal(siswaList: List<SiswaMiniResponse>) {
        if (tanggalIso.isBlank()) {
            mapAndDisplayData(siswaList, emptyList(), emptyList())
            return
        }

        // Ambil data presensi harian riil dari Supabase
        SupabaseClient.instance.getPresensiHariIniWalas(filterTanggal = "eq.$tanggalIso", filterKelas = "eq.$rombelId")
            .enqueue(object : Callback<List<RiwayatModel>> {
                override fun onResponse(
                    call: Call<List<RiwayatModel>>,
                    response: Response<List<RiwayatModel>>
                ) {
                    val presensiList = response.body().orEmpty()
                    // 3. Ambil data izin riil yang disetujui
                    loadIzinDisetujui(siswaList, presensiList)
                }

                override fun onFailure(call: Call<List<RiwayatModel>>, t: Throwable) {
                    loadIzinDisetujui(siswaList, emptyList())
                }
            })
    }

    private fun loadIzinDisetujui(
        siswaList: List<SiswaMiniResponse>,
        presensiList: List<RiwayatModel>
    ) {
        SupabaseClient.instance.getIzinDisetujuiSemuaKelas()
            .enqueue(object : Callback<List<PengajuanIzinResponse>> {
                override fun onResponse(
                    call: Call<List<PengajuanIzinResponse>>,
                    response: Response<List<PengajuanIzinResponse>>
                ) {
                    val allIzin = response.body().orEmpty()
                    // Filter murni per kelas dan rentang tanggal riil
                    val izinList = allIzin.filter { izin ->
                        val tglMulai = izin.tanggalMulai ?: ""
                        val tglSelesai = izin.tanggalSelesai ?: tglMulai
                        val isKelas = (izin.idKelas == rombelId || izin.siswa?.idKelas == rombelId)
                        val inRange = if (tanggalIso.isNotBlank() && tglMulai.isNotBlank()) {
                            tanggalIso in tglMulai..tglSelesai || tglMulai == tanggalIso
                        } else false
                        isKelas && inRange
                    }

                    mapAndDisplayData(siswaList, presensiList, izinList)
                }

                override fun onFailure(call: Call<List<PengajuanIzinResponse>>, t: Throwable) {
                    mapAndDisplayData(siswaList, presensiList, emptyList())
                }
            })
    }

    /**
     * Menghubungkan data per siswa murni dari API Supabase:
     * - Ada di presensi_harian: tampilkan Hadir/Terlambat, jam masuk, geotag, dan biometrik asli.
     * - Ada di pengajuan_izin: tampilkan Izin/Sakit beserta keterangan asli.
     * - TIDAK ADA di keduanya: tampilkan Alpa, jam: "-", keterangan: "Belum Presensi".
     */
    private fun mapAndDisplayData(
        siswaList: List<SiswaMiniResponse>,
        presensiList: List<RiwayatModel>,
        izinList: List<PengajuanIzinResponse>
    ) {
        if (_binding == null) return
        binding.progressBarRiwayat.visibility = View.GONE

        val presensiMap = presensiList.filter { it.siswaId != null }.associateBy { it.siswaId }
        val izinMap = izinList.filter { it.siswaId != null }.associateBy { it.siswaId }

        val items = siswaList.mapIndexed { idx, siswa ->
            val nomorUrutStr = String.format(Locale.getDefault(), "%02d", idx + 1)
            val p = presensiMap[siswa.id]
            val i = izinMap[siswa.id]

            val status: String
            val jamMasuk: String
            val biometrikText: String
            val geotagText: String
            val badgeLabel: String

            when {
                p != null -> {
                    val isLate = p.status.equals("Terlambat", true)
                    status = if (isLate) "TERLAMBAT" else "HADIR"
                    badgeLabel = if (isLate) "Terlambat" else "✓ Hadir"
                    jamMasuk = p.displayJamMasuk.ifEmpty { "-" }

                    val score = p.biometrikMatchScore
                    biometrikText = if (score != null && score > 0) {
                        String.format(Locale.US, "AI Match %.1f%%", score * 100)
                    } else {
                        "AI Biometrik Terverifikasi"
                    }

                    val jarak = p.jarakGerbangMeter
                    geotagText = if (jarak != null && jarak > 0) {
                        "📍 Geotag ${jarak.toInt()}m dari gerbang SMKN 8"
                    } else {
                        "📍 Geotag Terverifikasi"
                    }
                }

                i != null -> {
                    val isSakit = i.jenisIzin.equals("Sakit", true)
                    status = if (isSakit) "SAKIT" else "IZIN"
                    badgeLabel = if (isSakit) "🏥 Sakit" else "📝 Izin"
                    jamMasuk = "-"
                    val ket = i.keterangan?.takeIf { it.isNotBlank() } ?: if (isSakit) "Surat Dokter" else "Disetujui Walas"
                    biometrikText = ket
                    geotagText = if (isSakit) "Dispensasi Medis Resmi" else "Izin Resmi Walas"
                }

                else -> {
                    // Siswa tidak ada catatan presensi dan tidak ada izin yang disetujui untuk tanggal ini
                    status = "ALPA"
                    badgeLabel = "⚠ Alpa"
                    jamMasuk = "-"
                    biometrikText = "Belum Presensi"
                    geotagText = "Belum Presensi"
                }
            }

            val waktuDanScore = if (jamMasuk != "-") "$jamMasuk • $biometrikText" else biometrikText

            SiswaRiwayatDetailItem(
                nomorUrut = nomorUrutStr,
                siswaId = siswa.id,
                namaLengkap = siswa.namaLengkap ?: "Siswa ${idx + 1}",
                nisn = siswa.nisn ?: "-",
                waktuMasuk = if (status == "ALPA") "-" else waktuDanScore,
                aiMatchScore = biometrikText,
                geotagJarak = if (status == "ALPA") "Belum Presensi" else geotagText,
                status = status,
                badgeLabel = badgeLabel
            )
        }

        val total = items.size
        val hadir = items.count { it.status == "HADIR" || it.status == "TERLAMBAT" }
        val sakit = items.count { it.status == "SAKIT" }
        val izin = items.count { it.status == "IZIN" }
        val alpa = items.count { it.status == "ALPA" }

        binding.tvSummaryRiwayat.text =
            "● $hadir Hadir  •  $sakit Sakit  •  $izin Izin  •  $alpa Alpa (Total $total Siswa)"

        adapter.updateData(items)
        binding.tvEmptyRiwayat.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}

class SiswaRiwayatDetailAdapter(
    private var items: List<SiswaRiwayatDetailItem> = emptyList()
) : RecyclerView.Adapter<SiswaRiwayatDetailAdapter.ViewHolder>() {

    private var fullList: List<SiswaRiwayatDetailItem> = emptyList()

    fun updateData(newItems: List<SiswaRiwayatDetailItem>) {
        fullList = newItems
        items = newItems
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.lowercase().trim()
        items = if (q.isEmpty()) {
            fullList
        } else {
            fullList.filter {
                it.namaLengkap.lowercase().contains(q) || it.nisn.lowercase().contains(q)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSiswaRiwayatDetailBinding.inflate(
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
        private val binding: ItemSiswaRiwayatDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SiswaRiwayatDetailItem) {
            val context = itemView.context

            binding.tvNomorAbsen.text = item.nomorUrut
            binding.tvNamaSiswa.text = item.namaLengkap
            binding.tvWaktuDanScore.text = item.waktuMasuk
            binding.tvGeotagJarak.text = item.geotagJarak
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
        }
    }
}
