package com.andev.absensiswaku.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.andev.absensiswaku.data.network.RombelMapelResponse
import com.andev.absensiswaku.data.network.SiswaAdminResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.databinding.DialogMutasiRombelBinding
import com.andev.absensiswaku.util.applyBounceEffect
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DialogMutasiRombel : BottomSheetDialogFragment() {

    private var _binding: DialogMutasiRombelBinding? = null
    private val binding get() = _binding!!

    private var currentSiswa: SiswaAdminResponse? = null
    private var availableRombels: List<RombelMapelResponse> = emptyList()
    var onMutasiSuccess: (() -> Unit)? = null

    companion object {
        fun newInstance(
            siswa: SiswaAdminResponse,
            rombelList: List<RombelMapelResponse>
        ): DialogMutasiRombel {
            val dialog = DialogMutasiRombel()
            dialog.currentSiswa = siswa
            dialog.availableRombels = rombelList
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMutasiRombelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val siswa = currentSiswa ?: return
        val currentKelasName = siswa.rombelKelas?.namaKelas ?: "Kelas ID ${siswa.idKelas}"
        binding.tvMutasiSubtitle.text = "${siswa.namaLengkap} (Rombel Saat Ini: $currentKelasName)"

        binding.btnCloseMutasiDialog.applyBounceEffect {
            dismiss()
        }

        val rombelOptions = availableRombels.map { "Kelas ${it.namaKelas}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, rombelOptions)
        binding.actvMutasiRombelTujuan.setAdapter(adapter)

        if (rombelOptions.isNotEmpty()) {
            binding.actvMutasiRombelTujuan.setText(rombelOptions[0], false)
        }

        binding.btnKonfirmasiMutasi.applyBounceEffect {
            val alasan = binding.etAlasanMutasi.text?.toString()?.trim().orEmpty()
            if (alasan.isEmpty()) {
                binding.etAlasanMutasi.error = "Harap masukkan alasan mutasi rombel"
                return@applyBounceEffect
            }

            val selectedText = binding.actvMutasiRombelTujuan.text.toString()
            val targetRombel = availableRombels.find { "Kelas ${it.namaKelas}" == selectedText || it.namaKelas == selectedText }
            if (targetRombel == null) {
                Toast.makeText(requireContext(), "Pilih rombel tujuan yang valid", Toast.LENGTH_SHORT).show()
                return@applyBounceEffect
            }

            binding.btnKonfirmasiMutasi.isEnabled = false
            binding.btnKonfirmasiMutasi.text = "Memproses Mutasi..."

            val payload = mapOf(
                "id_kelas" to targetRombel.id
            )

            SupabaseClient.instance.updateSiswa(
                filterId = "eq.${siswa.id}",
                payload = payload
            ).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (!isAdded || _binding == null) return
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "Siswa ${siswa.namaLengkap} berhasil dimutasi ke ${targetRombel.namaKelas}",
                            Toast.LENGTH_LONG
                        ).show()
                        onMutasiSuccess?.invoke()
                        dismiss()
                    } else {
                        binding.btnKonfirmasiMutasi.isEnabled = true
                        binding.btnKonfirmasiMutasi.text = "🔄 Konfirmasi Mutasi Siswa"
                        Toast.makeText(requireContext(), "Gagal mutasi: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    binding.btnKonfirmasiMutasi.isEnabled = true
                    binding.btnKonfirmasiMutasi.text = "🔄 Konfirmasi Mutasi Siswa"
                    Toast.makeText(requireContext(), "Koneksi gagal: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
