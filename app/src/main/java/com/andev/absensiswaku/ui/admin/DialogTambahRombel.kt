package com.andev.absensiswaku.ui.admin

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.andev.absensiswaku.data.network.RombelIdResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.data.network.TambahRombelRequest
import com.andev.absensiswaku.data.network.UserResponse
import com.andev.absensiswaku.databinding.DialogTambahRombelBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Dialog Form Penambahan Rombongan Belajar (Rombel) Baru.
 * Terhubung langsung via HTTP POST ke tabel `rombel_kelas` di Supabase.
 */
class DialogTambahRombel(
    var onRombelCreated: (() -> Unit)? = null
) : DialogFragment() {

    constructor() : this(null)

    private var _binding: DialogTambahRombelBinding? = null
    private val binding get() = _binding!!

    data class WalasOption(val id: String?, val nama: String)
    private val walasOptions = mutableListOf<WalasOption>()
    private var selectedWalasId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTambahRombelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val displayWidth = (resources.displayMetrics.widthPixels * 0.92).toInt()
        dialog?.window?.setLayout(displayWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupJurusanDropdown()
        loadWaliKelasList()
        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupJurusanDropdown() {
        val listJurusan = listOf(
            "Pengembangan Perangkat Lunak & Gim (RPL)",
            "Akuntansi dan Keuangan Lembaga (AKL)",
            "Manajemen Perkantoran (MP)",
            "Bisnis Retail (BR)",
            "Bisnis Digital (BD)",
            "Usaha Layanan Pariwisata (UPW)"
        )
        val adapterJurusan = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listJurusan)
        binding.actvJurusan.setAdapter(adapterJurusan)
    }

    private fun parseJurusanCode(jurusanText: String): String {
        return when {
            jurusanText.contains("RPL", true) -> "RPL"
            jurusanText.contains("AKL", true) -> "AKL"
            jurusanText.contains("MP", true) -> "MP"
            jurusanText.contains("BR", true) -> "BR"
            jurusanText.contains("BD", true) -> "BD"
            jurusanText.contains("UPW", true) -> "UPW"
            else -> jurusanText.trim()
        }
    }

    private fun loadWaliKelasList() {
        walasOptions.clear()
        walasOptions.add(WalasOption(null, "Belum Ditentukan (Kosong)"))

        binding.progressBarDialogRombel.visibility = View.VISIBLE

        SupabaseClient.instance.getWaliKelasList()
            .enqueue(object : Callback<List<UserResponse>> {
                override fun onResponse(
                    call: Call<List<UserResponse>>,
                    response: Response<List<UserResponse>>
                ) {
                    if (_binding == null) return
                    binding.progressBarDialogRombel.visibility = View.GONE

                    val teachers = response.body().orEmpty()
                    if (teachers.isNotEmpty()) {
                        teachers.forEach { t ->
                            val nama = t.namaLengkap?.trim().takeIf { !it.isNullOrBlank() } ?: t.username ?: "Guru"
                            walasOptions.add(WalasOption(t.id, nama))
                        }
                    } else {
                        loadFallbackTeachers()
                    }
                    setupWalasAdapter()
                }

                override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                    if (_binding == null) return
                    binding.progressBarDialogRombel.visibility = View.GONE
                    loadFallbackTeachers()
                    setupWalasAdapter()
                }
            })
    }

    private fun loadFallbackTeachers() {
        val defaultTeachers = listOf(
            WalasOption("1", "Farauk Pratama, S.Kom."),
            WalasOption("2", "Herlina S.E"),
            WalasOption("3", "Sholeha Anshyoria M.Pd"),
            WalasOption("4", "Aini Freshawinda M.Pd"),
            WalasOption("5", "Isnaeni Rumiyati M.M"),
            WalasOption("6", "Hendro Utomo M.Pd"),
            WalasOption("7", "Dwi Puji Lestari M.Pd"),
            WalasOption("8", "Sri Mardini M.Pd"),
            WalasOption("9", "Abdullah M.Pd"),
            WalasOption("10", "Novita Nurbani S.Ikom")
        )
        walasOptions.addAll(defaultTeachers)
    }

    private fun setupWalasAdapter() {
        if (_binding == null || !isAdded) return
        val adapterWalas = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            walasOptions.map { it.nama }
        )
        binding.actvWaliKelas.setAdapter(adapterWalas)
        binding.actvWaliKelas.setText(walasOptions.first().nama, false)
        selectedWalasId = walasOptions.first().id

        binding.actvWaliKelas.setOnItemClickListener { _, _, position, _ ->
            selectedWalasId = walasOptions.getOrNull(position)?.id
        }
    }

    private fun setupListeners() {
        binding.btnCloseDialogRombel.setOnClickListener {
            dismiss()
        }

        binding.btnBatalRombel.setOnClickListener {
            dismiss()
        }

        binding.btnSimpanRombel.setOnClickListener {
            val namaKelas = binding.etNamaKelasBaru.text?.toString()?.trim().orEmpty()
            val jurusanRaw = binding.actvJurusan.text?.toString()?.trim().orEmpty()
            val kapasitasStr = binding.etKapasitasKelas.text?.toString()?.trim().orEmpty()

            if (namaKelas.isEmpty()) {
                binding.tilNamaKelas.error = "Nama kelas wajib diisi"
                return@setOnClickListener
            } else {
                binding.tilNamaKelas.error = null
            }

            if (jurusanRaw.isEmpty()) {
                binding.tilJurusan.error = "Pilih program keahlian / jurusan"
                return@setOnClickListener
            } else {
                binding.tilJurusan.error = null
            }

            val kapasitas = kapasitasStr.toIntOrNull() ?: 36
            val jurusanCode = parseJurusanCode(jurusanRaw)

            simpanRombelBaru(namaKelas, jurusanCode, selectedWalasId, kapasitas)
        }
    }

    private fun simpanRombelBaru(
        namaKelas: String,
        jurusan: String,
        walasId: String?,
        kuota: Int
    ) {
        binding.progressBarDialogRombel.visibility = View.VISIBLE
        binding.btnSimpanRombel.isEnabled = false
        binding.btnSimpanRombel.text = "Menyimpan..."

        // 1. Ambil ID terbesar dari tabel rombel_kelas lalu tambahkan 1
        SupabaseClient.instance.getMaxRombelId().enqueue(object : Callback<List<RombelIdResponse>> {
            override fun onResponse(
                call: Call<List<RombelIdResponse>>,
                response: Response<List<RombelIdResponse>>
            ) {
                val maxId = response.body()?.firstOrNull()?.id ?: 10
                val nextId = maxId + 1
                kirimRombelKeSupabase(nextId, namaKelas, jurusan, walasId, kuota)
            }

            override fun onFailure(call: Call<List<RombelIdResponse>>, t: Throwable) {
                // Fallback ID
                val fallbackId = 11
                kirimRombelKeSupabase(fallbackId, namaKelas, jurusan, walasId, kuota)
            }
        })
    }

    private fun kirimRombelKeSupabase(
        newId: Int,
        namaKelas: String,
        jurusan: String,
        walasId: String?,
        kuota: Int
    ) {
        val request = TambahRombelRequest(
            id = newId,
            namaKelas = namaKelas,
            jurusan = jurusan,
            tingkat = "12",
            waliKelasId = walasId,
            tahunAjaran = "2026/2027",
            kapasitasKuota = kuota
        )

        SupabaseClient.instance.tambahRombel(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (_binding == null || !isAdded) return
                binding.progressBarDialogRombel.visibility = View.GONE
                binding.btnSimpanRombel.isEnabled = true
                binding.btnSimpanRombel.text = "💾 Simpan Kelas Baru"

                if (response.isSuccessful || response.code() == 201 || response.code() == 200 || response.code() == 204) {
                    Toast.makeText(
                        requireContext(),
                        "✓ Rombel $namaKelas berhasil dibuat!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onRombelCreated?.invoke()
                    dismiss()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Gagal membuat kelas: HTTP ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                if (_binding == null || !isAdded) return
                binding.progressBarDialogRombel.visibility = View.GONE
                binding.btnSimpanRombel.isEnabled = true
                binding.btnSimpanRombel.text = "💾 Simpan Kelas Baru"
                Toast.makeText(
                    requireContext(),
                    "Koneksi gagal: ${t.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
