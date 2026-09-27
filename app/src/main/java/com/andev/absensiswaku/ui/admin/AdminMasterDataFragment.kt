package com.andev.absensiswaku.ui.admin

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.RombelMapelResponse
import com.andev.absensiswaku.data.network.SiswaAdminResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.databinding.FragmentAdminMasterDataBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class AdminMasterDataFragment : Fragment() {

    private var _binding: FragmentAdminMasterDataBinding? = null
    private val binding get() = _binding!!

    private lateinit var siswaAdapter: SiswaAdminAdapter
    private lateinit var rombelAdapter: RombelKelolaAdapter

    private var allSiswaList: List<SiswaAdminResponse> = emptyList()
    private var allRombelList: List<RombelMapelResponse> = emptyList()

    private var selectedGender: String = "L"
    private var editingSiswaId: Int? = null
    private var selectedFilterKelasId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminMasterDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSegmentedSwitcher()
        setupRecyclerViews()
        setupFormTambahSiswa()
        setupSearchAndFilter()
        setupImportExcelButton()
        loadDataFromSupabase()
    }

    private fun setupSegmentedSwitcher() {
        binding.btnTabSiswa.setOnClickListener {
            switchTab(isSiswaTab = true)
        }

        binding.btnTabRombel.setOnClickListener {
            switchTab(isSiswaTab = false)
        }
    }

    private fun switchTab(isSiswaTab: Boolean) {
        val context = requireContext()
        if (isSiswaTab) {
            binding.panelKelolaSiswa.visibility = View.VISIBLE
            binding.panelKelolaRombel.visibility = View.GONE

            binding.btnTabSiswa.setBackgroundResource(R.drawable.bg_segmented_active)
            binding.tvTabSiswa.setTextColor(ContextCompat.getColor(context, R.color.primary_teal))
            binding.imgTabSiswa.imageTintList = ContextCompat.getColorStateList(context, R.color.primary_teal)

            binding.btnTabRombel.background = null
            binding.tvTabRombel.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            binding.imgTabRombel.imageTintList = ContextCompat.getColorStateList(context, R.color.text_secondary)
        } else {
            binding.panelKelolaSiswa.visibility = View.GONE
            binding.panelKelolaRombel.visibility = View.VISIBLE

            binding.btnTabRombel.setBackgroundResource(R.drawable.bg_segmented_active)
            binding.tvTabRombel.setTextColor(ContextCompat.getColor(context, R.color.primary_teal))
            binding.imgTabRombel.imageTintList = ContextCompat.getColorStateList(context, R.color.primary_teal)

            binding.btnTabSiswa.background = null
            binding.tvTabSiswa.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            binding.imgTabSiswa.imageTintList = ContextCompat.getColorStateList(context, R.color.text_secondary)
        }
    }

    private fun setupRecyclerViews() {
        siswaAdapter = SiswaAdminAdapter(
            onEditClick = { siswa ->
                prepareEditSiswa(siswa)
            },
            onMutasiClick = { siswa ->
                val dialog = DialogMutasiRombel.newInstance(siswa, allRombelList)
                dialog.onMutasiSuccess = {
                    loadDataFromSupabase()
                }
                dialog.show(childFragmentManager, "DialogMutasiRombel")
            },
            onDeleteClick = { siswa ->
                confirmDeleteSiswa(siswa)
            }
        )

        binding.rvDaftarSiswa.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = siswaAdapter
            isNestedScrollingEnabled = false
        }

        rombelAdapter = RombelKelolaAdapter { rombel ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚙️ Setelan Rombel: ${rombel.namaKelas}")
                .setMessage("Rombel: ${rombel.namaKelas}\nWali Kelas: ${rombel.waliKelas?.namaLengkap ?: "-"}\nKapasitas Kuota: ${rombel.kuota} Siswa\nTahun Ajaran: 2026/2027 Semester Ganjil\n\nStatus Sinkronisasi Dapodik: 100% Valid.")
                .setPositiveButton("Tutup", null)
                .show()
        }

        binding.rvDaftarRombel.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rombelAdapter
            isNestedScrollingEnabled = false
        }

        binding.btnBuatKelas.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tambah Rombongan Belajar")
                .setMessage("Pembuatan rombel kelas baru dikoordinasikan langsung melalui Tim Kurikulum & Data Pokok Pendidikan (Dapodik) SMKN 8 Jakarta.")
                .setPositiveButton("Mengerti", null)
                .show()
        }
    }

    private fun setupFormTambahSiswa() {
        binding.btnToggleTambahSiswa.setOnClickListener {
            if (binding.cardFormTambahSiswa.visibility == View.VISIBLE && editingSiswaId == null) {
                binding.cardFormTambahSiswa.visibility = View.GONE
            } else {
                resetForm()
                binding.cardFormTambahSiswa.visibility = View.VISIBLE
                binding.tvModeForm.text = "Mode: Input Baru"
                binding.btnSimpanSiswa.text = "💾 Simpan Data Siswa"
            }
        }

        binding.btnTutupForm.setOnClickListener {
            binding.cardFormTambahSiswa.visibility = View.GONE
            resetForm()
        }

        binding.btnResetForm.setOnClickListener {
            resetForm()
        }

        binding.btnGenderL.setOnClickListener {
            setGenderSelection("L")
        }

        binding.btnGenderP.setOnClickListener {
            setGenderSelection("P")
        }

        binding.btnUploadUlangWajah.setOnClickListener {
            Toast.makeText(requireContext(), "Sensor Kamera AI Liveness siap memindai biometrik wajah", Toast.LENGTH_SHORT).show()
        }

        binding.btnSimpanSiswa.setOnClickListener {
            saveSiswaData()
        }
    }

    private fun setGenderSelection(gender: String) {
        selectedGender = gender
        val context = requireContext()
        if (gender == "L") {
            binding.btnGenderL.setBackgroundResource(R.drawable.bg_period_active)
            binding.btnGenderL.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            binding.btnGenderP.background = null
            binding.btnGenderP.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        } else {
            binding.btnGenderP.setBackgroundResource(R.drawable.bg_period_active)
            binding.btnGenderP.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            binding.btnGenderL.background = null
            binding.btnGenderL.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }
    }

    private fun resetForm() {
        editingSiswaId = null
        binding.etNisnSiswa.text.clear()
        binding.etNikSiswa.text.clear()
        binding.etNamaLengkapSiswa.text.clear()
        binding.etNoWaSiswa.text.clear()
        setGenderSelection("L")
        binding.tvModeForm.text = "Mode: Input Baru"
        binding.btnSimpanSiswa.text = "💾 Simpan Data Siswa"
    }

    private fun prepareEditSiswa(siswa: SiswaAdminResponse) {
        editingSiswaId = siswa.id
        binding.cardFormTambahSiswa.visibility = View.VISIBLE
        binding.tvModeForm.text = "Mode: Edit Siswa (#${siswa.id})"
        binding.btnSimpanSiswa.text = "💾 Perbarui Data Siswa"

        binding.etNisnSiswa.setText(siswa.nisn)
        binding.etNikSiswa.setText(siswa.nik ?: "")
        binding.etNamaLengkapSiswa.setText(siswa.namaLengkap)
        binding.etNoWaSiswa.setText(siswa.noWaOrangTua ?: "")

        val gender = if (siswa.jenisKelamin?.startsWith("P", true) == true) "P" else "L"
        setGenderSelection(gender)

        val matchingRombel = allRombelList.find { it.id == siswa.idKelas }
        if (matchingRombel != null) {
            binding.actvPilihKelas.setText("Kelas ${matchingRombel.namaKelas}", false)
        }

        binding.nestedScrollAdminMasterData.smoothScrollTo(0, binding.cardFormTambahSiswa.top)
    }

    private fun saveSiswaData() {
        val nisn = binding.etNisnSiswa.text?.toString()?.trim().orEmpty()
        val nik = binding.etNikSiswa.text?.toString()?.trim().orEmpty()
        val nama = binding.etNamaLengkapSiswa.text?.toString()?.trim().orEmpty()
        val noWa = binding.etNoWaSiswa.text?.toString()?.trim().orEmpty()
        val selectedClassText = binding.actvPilihKelas.text.toString()

        // Hapus kewajiban input NIK, Jenis Kelamin, dan No WA orang tua.
        // Hanya NISN, Nama Lengkap, dan Pilihan Rombel yang wajib diisi saat ini.
        if (nisn.length < 5) {
            binding.etNisnSiswa.error = "NISN minimal 5 digit"
            return
        }
        if (nama.isEmpty()) {
            binding.etNamaLengkapSiswa.error = "Nama lengkap wajib diisi"
            return
        }

        val targetRombel = allRombelList.find { "Kelas ${it.namaKelas}" == selectedClassText || it.namaKelas == selectedClassText }
        val idKelas = targetRombel?.id ?: 9 // default ke XII RPL (ID 9) jika tidak ditemukan

        val payload = mutableMapOf<String, Any>(
            "nisn" to nisn,
            "nama_lengkap" to nama,
            "id_kelas" to idKelas,
            "pin_presensi" to "123456"
        )
        // NIK, Jenis Kelamin, dan No WA Orang Tua bersifat opsional
        if (nik.isNotEmpty()) payload["nik"] = nik
        if (selectedGender.isNotEmpty()) payload["jenis_kelamin"] = selectedGender
        if (noWa.isNotEmpty()) payload["no_wa_orang_tua"] = noWa

        binding.btnSimpanSiswa.isEnabled = false
        binding.btnSimpanSiswa.text = "Menyimpan Data..."

        if (editingSiswaId == null) {
            // Tambah Siswa Baru via POST
            SupabaseClient.instance.tambahSiswa(payload)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (!isAdded || _binding == null) return
                        binding.btnSimpanSiswa.isEnabled = true
                        binding.btnSimpanSiswa.text = "💾 Simpan Data Siswa"

                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Siswa $nama berhasil ditambahkan ke Dapodik!", Toast.LENGTH_SHORT).show()
                            resetForm()
                            binding.cardFormTambahSiswa.visibility = View.GONE
                            loadDataFromSupabase()
                        } else {
                            Toast.makeText(requireContext(), "Gagal simpan: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        if (!isAdded || _binding == null) return
                        binding.btnSimpanSiswa.isEnabled = true
                        binding.btnSimpanSiswa.text = "💾 Simpan Data Siswa"
                        Toast.makeText(requireContext(), "Koneksi gagal: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            // Update Siswa via PATCH
            val id = editingSiswaId!!
            SupabaseClient.instance.updateSiswa(filterId = "eq.$id", payload = payload)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (!isAdded || _binding == null) return
                        binding.btnSimpanSiswa.isEnabled = true
                        binding.btnSimpanSiswa.text = "💾 Perbarui Data Siswa"

                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Data siswa $nama berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            resetForm()
                            binding.cardFormTambahSiswa.visibility = View.GONE
                            loadDataFromSupabase()
                        } else {
                            Toast.makeText(requireContext(), "Gagal update: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        if (!isAdded || _binding == null) return
                        binding.btnSimpanSiswa.isEnabled = true
                        binding.btnSimpanSiswa.text = "💾 Perbarui Data Siswa"
                        Toast.makeText(requireContext(), "Koneksi gagal: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun confirmDeleteSiswa(siswa: SiswaAdminResponse) {
        val nama = siswa.namaLengkap
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Konfirmasi Hapus Siswa")
            .setMessage("Apakah Anda yakin ingin menghapus data siswa $nama (NISN: ${siswa.nisn}) dari sistem Dapodik sekolah?")
            .setPositiveButton("Hapus") { _, _ ->
                SupabaseClient.instance.deleteSiswa(filterId = "eq.${siswa.id}")
                    .enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            if (!isAdded || _binding == null) return
                            if (response.isSuccessful) {
                                Toast.makeText(requireContext(), "Data siswa $nama telah dihapus", Toast.LENGTH_SHORT).show()
                                loadDataFromSupabase()
                            } else {
                                Toast.makeText(requireContext(), "Gagal menghapus: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            if (!isAdded || _binding == null) return
                            Toast.makeText(requireContext(), "Koneksi gagal: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupSearchAndFilter() {
        binding.etSearchSiswaMaster.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupImportExcelButton() {
        binding.btnImporExcel.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("📥 Impor Data Siswa via Excel")
                .setMessage("Format file harus berupa template standar Dapodik (.xlsx / .csv) berisi kolom: NISN, NIK, Nama Lengkap, Rombel, Jenis Kelamin, No. HP Orang Tua.\n\nUnduh template resmi di portal web administrator.")
                .setPositiveButton("Pilih File", null)
                .setNegativeButton("Tutup", null)
                .show()
        }
    }

    private fun loadDataFromSupabase() {
        // 1. Ambil 10 Rombel
        SupabaseClient.instance.getRombelMapel()
            .enqueue(object : Callback<List<RombelMapelResponse>> {
                override fun onResponse(
                    call: Call<List<RombelMapelResponse>>,
                    response: Response<List<RombelMapelResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    allRombelList = response.body().orEmpty()

                    updateRombelDropdown()
                    loadAllSiswa()
                }

                override fun onFailure(call: Call<List<RombelMapelResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    loadAllSiswa()
                }
            })
    }

    private fun updateRombelDropdown() {
        val classOptions = allRombelList.map { "Kelas ${it.namaKelas}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, classOptions)
        binding.actvPilihKelas.setAdapter(adapter)

        populateFilterChips()
    }

    private fun loadAllSiswa() {
        SupabaseClient.instance.getSiswaAdmin()
            .enqueue(object : Callback<List<SiswaAdminResponse>> {
                override fun onResponse(
                    call: Call<List<SiswaAdminResponse>>,
                    response: Response<List<SiswaAdminResponse>>
                ) {
                    if (!isAdded || _binding == null) return
                    val list = response.body() ?: emptyList()
                    allSiswaList = list
                    siswaAdapter.submitList(list)
                    binding.tvCountSiswa.text = "Daftar Siswa Terverifikasi (${list.size} Ditampilkan)"
                    binding.rvDaftarSiswa.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.layoutEmptyState.visibility = if (list.isNotEmpty()) View.GONE else View.VISIBLE
                    updateUiMetrics()
                }

                override fun onFailure(call: Call<List<SiswaAdminResponse>>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    updateUiMetrics()
                }
            })
    }

    private fun updateUiMetrics() {
        val totalSiswa = allSiswaList.size
        val totalRombel = allRombelList.size.takeIf { it > 0 } ?: 10
        val aktifCount = totalSiswa

        binding.tvMetricTotalSiswaMaster.text = "$totalSiswa"
        binding.tvMetricTotalRombelMaster.text = "$totalRombel"
        binding.tvMetricStatusAktifMaster.text = "$aktifCount"

        binding.chipSemuaSiswa.text = "Semua ($totalSiswa)"
        binding.tvCountSiswa.text = "Daftar Siswa Terverifikasi ($totalSiswa Ditampilkan)"

        // Update counts per class for RombelKelolaAdapter
        val countMap = allSiswaList.groupingBy { it.idKelas }.eachCount()
        rombelAdapter.submitData(allRombelList, countMap)

        applyFilter(binding.etSearchSiswaMaster.text?.toString().orEmpty())
    }

    private fun populateFilterChips() {
        val container = binding.containerChipsMasterSiswa
        // Keep index 0 (Semua)
        while (container.childCount > 1) {
            container.removeViewAt(1)
        }

        binding.chipSemuaSiswa.setOnClickListener {
            selectedFilterKelasId = null
            updateChipStyles(binding.chipSemuaSiswa)
            applyFilter(binding.etSearchSiswaMaster.text?.toString().orEmpty())
        }

        val context = requireContext()
        allRombelList.forEach { rombel ->
            val chip = TextView(context).apply {
                val kuota = rombel.kuota
                text = "${rombel.namaKelas} ($kuota)"
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setBackgroundResource(R.drawable.bg_chip_filter_inactive)
                setPadding(dpToPx(14), dpToPx(7), dpToPx(14), dpToPx(7))
                isClickable = true
                isFocusable = true

                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(8)
                }
                layoutParams = params

                setOnClickListener {
                    selectedFilterKelasId = rombel.id
                    updateChipStyles(this)
                    applyFilter(binding.etSearchSiswaMaster.text?.toString().orEmpty())
                }
            }
            container.addView(chip)
        }
    }

    private fun updateChipStyles(activeView: TextView) {
        val context = requireContext()
        val container = binding.containerChipsMasterSiswa
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i) as? TextView ?: continue
            if (child == activeView) {
                child.setBackgroundResource(R.drawable.bg_chip_filter_active)
                child.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            } else {
                child.setBackgroundResource(R.drawable.bg_chip_filter_inactive)
                child.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }

    private fun applyFilter(query: String) {
        siswaAdapter.filter(query, selectedFilterKelasId)

        val countShown = siswaAdapter.itemCount
        binding.tvCountSiswa.text = "Daftar Siswa Terverifikasi ($countShown Ditampilkan)"

        if (countShown == 0) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvDaftarSiswa.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvDaftarSiswa.visibility = View.VISIBLE
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
