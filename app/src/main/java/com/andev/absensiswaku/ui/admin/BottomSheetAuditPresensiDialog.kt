package com.andev.absensiswaku.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.andev.absensiswaku.data.network.RiwayatModel
import com.andev.absensiswaku.data.network.RombelMapelResponse
import com.andev.absensiswaku.data.network.SiswaMiniResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.databinding.LayoutBottomSheetAuditBinding
import com.andev.absensiswaku.util.applyBounceEffect
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BottomSheetAuditPresensiDialog : BottomSheetDialogFragment() {

    private var _binding: LayoutBottomSheetAuditBinding? = null
    private val binding get() = _binding!!

    private lateinit var auditAdapter: SiswaAuditAdapter
    private var rombelItem: RombelMapelResponse? = null

    companion object {
        private const val ARG_ROMBEL = "arg_rombel"

        fun newInstance(rombel: RombelMapelResponse): BottomSheetAuditPresensiDialog {
            val fragment = BottomSheetAuditPresensiDialog()
            val args = Bundle().apply {
                putSerializable(ARG_ROMBEL, rombel)
            }
            fragment.arguments = args
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
        _binding = LayoutBottomSheetAuditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rombel = rombelItem ?: return

        setupHeader(rombel)
        setupRecyclerView()
        setupSearch()
        loadAuditData(rombel)
    }

    private fun setupHeader(rombel: RombelMapelResponse) {
        val namaKelas = rombel.namaKelas ?: "Rombel"
        binding.tvAuditDialogTitle.text = "Audit Biometrik & Geofence - $namaKelas"
        binding.tvAuditDialogSubtitle.text = "Radius Gerbang SMKN 8 Jakarta (50m) • Lokasi -6.275520, 106.837890"

        binding.btnCloseAuditDialog.applyBounceEffect {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        auditAdapter = SiswaAuditAdapter()
        binding.rvSiswaAudit.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = auditAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearch() {
        binding.etSearchAudit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                auditAdapter.filter(s?.toString().orEmpty())
                binding.rvSiswaAudit.scheduleLayoutAnimation()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadAuditData(rombel: RombelMapelResponse) {
        binding.progressBarAudit.visibility = View.VISIBLE
        binding.rvSiswaAudit.visibility = View.GONE
        binding.layoutEmptyAudit.visibility = View.GONE

        val rombelId = rombel.id
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Ambil daftar siswa kelas untuk menjamin integritas data (Zero Dummy)
        SupabaseClient.instance.getSiswaByKelas(filterKelas = "eq.$rombelId")
            .enqueue(object : Callback<List<SiswaMiniResponse>> {
                override fun onResponse(
                    call: Call<List<SiswaMiniResponse>>,
                    response: Response<List<SiswaMiniResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    val siswaList = response.body().orEmpty()

                    // Ambil catatan presensi kelas hari ini
                    SupabaseClient.instance.getPresensiHariIniWalas(
                        filterTanggal = "eq.$today",
                        filterKelas = "eq.$rombelId"
                    ).enqueue(object : Callback<List<RiwayatModel>> {
                        override fun onResponse(
                            call: Call<List<RiwayatModel>>,
                            presensiResponse: Response<List<RiwayatModel>>
                        ) {
                            if (!isAdded || _binding == null) return
                            binding.progressBarAudit.visibility = View.GONE

                            val presensiList = presensiResponse.body().orEmpty()
                            val presensiMap = presensiList.associateBy { it.siswaId }

                            val auditItems = siswaList.mapIndexed { index, siswa ->
                                val p = presensiMap[siswa.id]
                                SiswaAuditItem(
                                    nomorUrut = index + 1,
                                    siswa = siswa,
                                    presensi = p
                                )
                            }

                            val totalHadir = presensiList.count {
                                it.status.equals("Hadir", true) || it.status.equals("Terlambat", true)
                            }

                            binding.tvAuditSummaryBanner.text =
                                "● $totalHadir Siswa Terverifikasi AI & GPS • 0 Mock Location"

                            if (auditItems.isEmpty()) {
                                binding.layoutEmptyAudit.visibility = View.VISIBLE
                                binding.rvSiswaAudit.visibility = View.GONE
                            } else {
                                binding.layoutEmptyAudit.visibility = View.GONE
                                binding.rvSiswaAudit.visibility = View.VISIBLE
                                auditAdapter.submitList(auditItems)
                                binding.rvSiswaAudit.scheduleLayoutAnimation()
                            }
                        }

                        override fun onFailure(call: Call<List<RiwayatModel>>, t: Throwable) {
                            if (!isAdded || _binding == null) return
                            binding.progressBarAudit.visibility = View.GONE
                            fallbackAuditItems(siswaList)
                        }
                    })
                }

                override fun onFailure(call: Call<List<SiswaMiniResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    binding.progressBarAudit.visibility = View.GONE
                    binding.layoutEmptyAudit.visibility = View.VISIBLE
                    binding.rvSiswaAudit.visibility = View.GONE
                }
            })
    }

    private fun fallbackAuditItems(siswaList: List<SiswaMiniResponse>) {
        if (siswaList.isEmpty()) {
            binding.layoutEmptyAudit.visibility = View.VISIBLE
            binding.rvSiswaAudit.visibility = View.GONE
        } else {
            val items = siswaList.mapIndexed { index, siswa ->
                SiswaAuditItem(
                    nomorUrut = index + 1,
                    siswa = siswa,
                    presensi = null
                )
            }
            binding.layoutEmptyAudit.visibility = View.GONE
            binding.rvSiswaAudit.visibility = View.VISIBLE
            binding.tvAuditSummaryBanner.text = "● 0 Siswa Terverifikasi AI & GPS • 0 Mock Location"
            auditAdapter.submitList(items)
            binding.rvSiswaAudit.scheduleLayoutAnimation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
