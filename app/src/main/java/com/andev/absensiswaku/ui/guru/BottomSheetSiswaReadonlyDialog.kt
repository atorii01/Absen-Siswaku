package com.andev.absensiswaku.ui.guru

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.andev.absensiswaku.data.network.PengajuanIzinResponse
import com.andev.absensiswaku.data.network.PresensiHarianResponse
import com.andev.absensiswaku.data.network.RombelMapelResponse
import com.andev.absensiswaku.data.network.SiswaMiniResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.databinding.LayoutBottomSheetSiswaReadonlyBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BottomSheetSiswaReadonlyDialog : BottomSheetDialogFragment() {

    private var _binding: LayoutBottomSheetSiswaReadonlyBinding? = null
    private val binding get() = _binding!!

    private lateinit var studentAdapter: SiswaReadOnlyAdapter
    private var rombelItem: RombelMapelResponse? = null
    private var presensiList: List<PresensiHarianResponse> = emptyList()
    private var izinList: List<PengajuanIzinResponse> = emptyList()

    companion object {
        private const val ARG_ROMBEL = "arg_rombel"

        fun newInstance(
            rombel: RombelMapelResponse,
            presensiList: List<PresensiHarianResponse> = emptyList(),
            izinList: List<PengajuanIzinResponse> = emptyList()
        ): BottomSheetSiswaReadonlyDialog {
            val fragment = BottomSheetSiswaReadonlyDialog()
            val args = Bundle().apply {
                putSerializable(ARG_ROMBEL, rombel)
            }
            fragment.arguments = args
            fragment.presensiList = presensiList
            fragment.izinList = izinList
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        rombelItem = arguments?.getSerializable(ARG_ROMBEL) as? RombelMapelResponse
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutBottomSheetSiswaReadonlyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rombel = rombelItem ?: return

        setupHeader(rombel)
        setupRecyclerView()
        setupSearch()
        loadStudents(rombel)
    }

    private fun setupHeader(rombel: RombelMapelResponse) {
        val kuota = if (rombel.id == 8 || rombel.id == 10) 35 else 36

        // Header: Nama Kelas + Subteks "Status Kehadiran Hari Ini (Read-Only)"
        binding.tvDialogTitle.text = "Daftar Presensi ${rombel.namaKelas}"
        binding.tvDialogSubtitle.text = "Status Kehadiran Hari Ini (Read-Only)"

        // Summary Banner
        binding.tvSummaryDialog.text = "● ${rombel.hadirCount} Hadir  •  ${rombel.izinCount} Izin/Sakit  •  ${rombel.alpaCount} Alpa (Total $kuota Siswa)"

        binding.btnCloseDialog.setOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        studentAdapter = SiswaReadOnlyAdapter()
        binding.rvSiswaDialog.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = studentAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearchSiswaDialog.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty().trim()
                binding.btnClearSearchSiswa.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                studentAdapter.filter(query)
                binding.tvEmptySiswaDialog.visibility =
                    if (studentAdapter.itemCount == 0) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearchSiswa.setOnClickListener {
            binding.etSearchSiswaDialog.text?.clear()
        }
    }

    private fun loadStudents(rombel: RombelMapelResponse) {
        binding.progressBarSiswaDialog.visibility = View.VISIBLE

        SupabaseClient.instance.getSiswaByKelas(filterKelas = "eq.${rombel.id}")
            .enqueue(object : Callback<List<SiswaMiniResponse>> {
                override fun onResponse(
                    call: Call<List<SiswaMiniResponse>>,
                    response: Response<List<SiswaMiniResponse>>
                ) {
                    if (_binding == null) return
                    binding.progressBarSiswaDialog.visibility = View.GONE
                    val siswaList = response.body().orEmpty()

                    val mappedItems = mapStudentsToAttendance(siswaList)
                    studentAdapter.updateData(mappedItems)

                    binding.tvEmptySiswaDialog.visibility =
                        if (mappedItems.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onFailure(call: Call<List<SiswaMiniResponse>>, t: Throwable) {
                    if (_binding == null) return
                    binding.progressBarSiswaDialog.visibility = View.GONE
                    studentAdapter.updateData(emptyList())
                    binding.tvEmptySiswaDialog.visibility = View.VISIBLE
                }
            })
    }

    private fun mapStudentsToAttendance(
        siswaList: List<SiswaMiniResponse>
    ): List<SiswaReadOnlyItem> {
        val presensiMap = presensiList.associateBy { it.siswaId }
        val izinMap = izinList.associateBy { it.siswaId }

        return siswaList.mapIndexed { idx, s ->
            val presensi = presensiMap[s.id]
            val izin = izinMap[s.id]

            val status: String
            val waktu: String

            if (presensi != null) {
                val isLate = presensi.status.equals("Terlambat", true)
                status = if (isLate) "TERLAMBAT" else "HADIR"
                waktu = presensi.waktuMasuk?.takeIf { it.isNotBlank() } ?: "-"
            } else if (izin != null) {
                val isSakit = izin.jenisIzin.equals("Sakit", true)
                status = if (isSakit) "SAKIT" else "IZIN"
                waktu = izin.keterangan?.takeIf { it.isNotBlank() } ?: "Disetujui Walas"
            } else {
                status = "ALPA"
                waktu = "-"
            }

            SiswaReadOnlyItem(
                id = s.id ?: (idx + 1),
                namaLengkap = s.namaLengkap.orEmpty().ifEmpty { "Siswa ${idx + 1}" },
                nisn = s.nisn.orEmpty().ifEmpty { "-" },
                waktuMasuk = waktu,
                status = status
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
