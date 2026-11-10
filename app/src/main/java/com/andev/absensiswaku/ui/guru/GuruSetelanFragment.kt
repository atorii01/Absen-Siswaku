package com.andev.absensiswaku.ui.guru

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.andev.absensiswaku.LoginActivity
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.DialogUbahKataSandiGuruBinding
import com.andev.absensiswaku.databinding.FragmentGuruSetelanBinding
import com.andev.absensiswaku.util.MotionUtils
import com.andev.absensiswaku.util.applyBounceEffect
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class GuruSetelanFragment : Fragment() {

    private var _binding: FragmentGuruSetelanBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager

    private var namaMapel: String = "Guru Mapel"
    private var bidangMapel: String = "Mata Pelajaran Umum"
    private var idAkunGuru: String = "-"

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            saveAndApplyProfilePhoto(bitmap)
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    saveAndApplyProfilePhoto(bitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memproses foto dari galeri", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val PREF_SESSION_NAME = "PREF_SMKN8_SESSION"
        private const val KEY_MODE_GELAP = "KEY_DARK_MODE"
        private const val KEY_NOTIF_JAM_MASUK = "KEY_GURU_NOTIF_JAM_MASUK"
        private const val KEY_RINGKASAN_KEHADIRAN = "KEY_GURU_RINGKASAN_KEHADIRAN"
        private const val KEY_KONTAK_GURU = "KEY_KONTAK_GURU"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuruSetelanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = context ?: return
        sessionManager = SessionManager(ctx)

        loadSessionData()
        setupProfileUI()
        setupPreferenceSwitches()
        setupActionButtons()
        setupLogoutButton()

        MotionUtils.animateStaggeredEntrance(
            binding.cardProfilGuruSetelan,
            binding.btnUbahKataSandi,
            binding.btnEditKontak,
            binding.btnLogoutGuru
        )
    }

    override fun onResume() {
        super.onResume()
        loadSessionData()
        setupProfileUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Mengambil data identitas guru dari SharedPreferences ("PREF_SMKN8_SESSION")
     */
    private fun loadSessionData() {
        val ctx = context ?: return
        val pref = ctx.getSharedPreferences(PREF_SESSION_NAME, Context.MODE_PRIVATE)

        namaMapel = pref.getString("NAMA_LENGKAP", null)?.takeIf { it.isNotBlank() }
            ?: pref.getString("NAMA_WALAS", null)?.takeIf { it.isNotBlank() }
            ?: pref.getString("nama", null)?.takeIf { it.isNotBlank() }
            ?: sessionManager.getNama().takeIf { it.isNotBlank() }
            ?: "Guru Mapel"

        bidangMapel = pref.getString("MATA_PELAJARAN", null)?.takeIf { it.isNotBlank() }
            ?: pref.getString("jurusan", null)?.takeIf { it.isNotBlank() && !it.equals("Semua Kelas", true) }
            ?: sessionManager.getJurusan().takeIf { it.isNotBlank() && !it.equals("Semua Kelas", true) }
            ?: "Mata Pelajaran Umum"

        idAkunGuru = pref.getString("USERNAME", null)?.takeIf { it.isNotBlank() }
            ?: pref.getString("ID_USER", null)?.takeIf { it.isNotBlank() }
            ?: pref.getString("ID_WALAS", null)?.takeIf { it.isNotBlank() }
            ?: pref.getString("SAVED_GURU_USERNAME", null)?.takeIf { it.isNotBlank() }
            ?: sessionManager.getNisn().takeIf { it.isNotBlank() }
            ?: "mapel.mtk@smkn8.sch.id"
    }

    /**
     * Memasang data identitas pengajar ke kartu profil dan ringkasan mengajar
     */
    private fun setupProfileUI() {
        if (_binding == null) return

        binding.tvNamaAkun.text = namaMapel
        binding.tvSublabelMapel.text = "Guru Mata Pelajaran $bidangMapel • Akun Pengajar"
        binding.tvBidangStudiDiampu.text = bidangMapel
        binding.tvNipBadge.text = if (idAkunGuru.startsWith("ID:", true) || idAkunGuru.startsWith("NIP:", true)) {
            idAkunGuru
        } else {
            "ID: $idAkunGuru"
        }
        binding.tvVersiAplikasiGuru.text = "SMKN 8 Jakarta • Presensi Siswa v${com.andev.absensiswaku.BuildConfig.VERSION_NAME}"

        loadGuruProfilePhoto()

        binding.imgProfileAvatar.applyBounceEffect {
            showPilihFotoGuruDialog()
        }
        binding.imgTopAvatar.applyBounceEffect {
            showPilihFotoGuruDialog()
        }
    }

    private fun saveAndApplyProfilePhoto(bitmap: Bitmap) {
        val ctx = context ?: return
        try {
            val file = File(ctx.filesDir, "avatar_guru.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

            binding.imgProfileAvatar.setImageBitmap(bitmap)
            binding.imgTopAvatar.setImageBitmap(bitmap)
            Toast.makeText(ctx, "✓ Foto profil Guru Pengajar berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(ctx, "Gagal menyimpan foto: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGuruProfilePhoto() {
        val ctx = context ?: return
        val photoFile = File(ctx.filesDir, "avatar_guru.jpg")
        if (photoFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            if (bitmap != null) {
                binding.imgProfileAvatar.setImageBitmap(bitmap)
                binding.imgTopAvatar.setImageBitmap(bitmap)
                return
            }
        }
        binding.imgProfileAvatar.setImageResource(R.drawable.ic_avatar)
        binding.imgTopAvatar.setImageResource(R.drawable.ic_avatar)
    }

    private fun showPilihFotoGuruDialog() {
        val ctx = context ?: return
        val photoFile = File(ctx.filesDir, "avatar_guru.jpg")
        val hasCustomPhoto = photoFile.exists()

        val options = if (hasCustomPhoto) {
            arrayOf("📸 Ambil Foto Baru (Kamera)", "🖼️ Pilih dari Galeri", "🗑️ Hapus Foto Profil (Avatar Default)")
        } else {
            arrayOf("📸 Ambil Foto Baru (Kamera)", "🖼️ Pilih dari Galeri")
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Ubah Foto Profil Guru Mapel")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePictureLauncher.launch(null)
                    1 -> galleryLauncher.launch("image/*")
                    2 -> {
                        if (photoFile.exists()) photoFile.delete()
                        binding.imgProfileAvatar.setImageResource(R.drawable.ic_avatar)
                        binding.imgTopAvatar.setImageResource(R.drawable.ic_avatar)
                        Toast.makeText(ctx, "Foto profil dikembalikan ke avatar default.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Konfigurasi switch: Mode Gelap, Notifikasi Jam Masuk, Ringkasan Kehadiran
     */
    private fun setupPreferenceSwitches() {
        val ctx = context ?: return
        val pref = ctx.getSharedPreferences(PREF_SESSION_NAME, Context.MODE_PRIVATE)

        // 1. Tema Gelap (Dark Mode)
        val isDarkMode = pref.getBoolean(KEY_MODE_GELAP, false)
        binding.switchDarkMode.isChecked = isDarkMode
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean(KEY_MODE_GELAP, isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // 2. Notifikasi Jam Masuk Kelas
        val isNotifJamMasuk = pref.getBoolean(KEY_NOTIF_JAM_MASUK, true)
        binding.switchNotifJamMasuk.isChecked = isNotifJamMasuk
        binding.switchNotifJamMasuk.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean(KEY_NOTIF_JAM_MASUK, isChecked).apply()
            val status = if (isChecked) "diaktifkan" else "dinonaktifkan"
            Toast.makeText(requireContext(), "Notifikasi batas presensi jam masuk $status.", Toast.LENGTH_SHORT).show()
        }

        // 3. Ringkasan Kehadiran Rombel
        val isRingkasan = pref.getBoolean(KEY_RINGKASAN_KEHADIRAN, true)
        binding.switchRingkasanKehadiran.isChecked = isRingkasan
        binding.switchRingkasanKehadiran.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean(KEY_RINGKASAN_KEHADIRAN, isChecked).apply()
            val status = if (isChecked) "diaktifkan" else "dinonaktifkan"
            Toast.makeText(requireContext(), "Panel ringkasan kehadiran rombel $status.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Konfigurasi tombol aksi profil: Ubah Sandi, Edit Kontak, Pusat Bantuan
     */
    private fun setupActionButtons() {
        // Tombol Ubah Sandi
        binding.btnUbahKataSandi.applyBounceEffect {
            showDialogUbahSandi()
        }

        // Tombol Edit Kontak
        binding.btnEditKontak.applyBounceEffect {
            showDialogEditKontak()
        }

        // Tombol Pusat Bantuan & Panduan Guru
        binding.btnPusatBantuan.applyBounceEffect {
            showDialogPusatBantuan()
        }
    }

    /**
     * Dialog Ubah Kata Sandi Akun Pengajar
     */
    private fun showDialogUbahSandi() {
        val ctx = context ?: return
        val dialog = BottomSheetDialog(ctx)
        val dBinding = DialogUbahKataSandiGuruBinding.inflate(layoutInflater)
        dialog.setContentView(dBinding.root)

        dBinding.btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        dBinding.btnSimpanSandi.setOnClickListener {
            val lama = dBinding.etSandiLama.text?.toString()?.trim().orEmpty()
            val baru = dBinding.etSandiBaru.text?.toString()?.trim().orEmpty()
            val konfirmasi = dBinding.etKonfirmasiSandi.text?.toString()?.trim().orEmpty()

            dBinding.tilSandiLama.error = null
            dBinding.tilSandiBaru.error = null
            dBinding.tilKonfirmasiSandi.error = null

            if (lama.isEmpty()) {
                dBinding.tilSandiLama.error = "Kata sandi saat ini wajib diisi"
                return@setOnClickListener
            }
            if (baru.length < 6) {
                dBinding.tilSandiBaru.error = "Kata sandi baru minimal 6 karakter"
                return@setOnClickListener
            }
            if (baru != konfirmasi) {
                dBinding.tilKonfirmasiSandi.error = "Konfirmasi kata sandi tidak cocok"
                return@setOnClickListener
            }

            Toast.makeText(ctx, "Memverifikasi kata sandi lama...", Toast.LENGTH_SHORT).show()

            SupabaseClient.instance.getUserByUsername("eq.$idAkunGuru")
                .enqueue(object : Callback<List<Map<String, Any>>> {
                    override fun onResponse(
                        call: Call<List<Map<String, Any>>>,
                        response: Response<List<Map<String, Any>>>
                    ) {
                        val users = response.body()
                        val currentUser = users?.firstOrNull()
                        val currentPasswordHash = currentUser?.get("password_hash")?.toString() ?: ""

                        if (currentPasswordHash.isNotEmpty() && currentPasswordHash != lama) {
                            dBinding.tilSandiLama.error = "Kata sandi saat ini salah"
                            Toast.makeText(ctx, "Kata sandi saat ini salah! Mohon periksa kembali.", Toast.LENGTH_LONG).show()
                            return
                        }

                        SupabaseClient.instance.updateUser(
                            usernameFilter = "eq.$idAkunGuru",
                            payload = mapOf("password_hash" to baru)
                        ).enqueue(object : Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                if (response.isSuccessful) {
                                    dialog.dismiss()
                                    Toast.makeText(ctx, "✓ Kata sandi akun $namaMapel berhasil diperbarui di cloud!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(ctx, "Gagal memperbarui sandi: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Toast.makeText(ctx, "Gagal terhubung ke cloud: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }

                    override fun onFailure(call: Call<List<Map<String, Any>>>, t: Throwable) {
                        Toast.makeText(ctx, "Koneksi verifikasi gagal: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        dialog.show()
    }

    /**
     * Dialog Edit Kontak & Email Guru
     */
    private fun showDialogEditKontak() {
        val ctx = context ?: return
        val pref = ctx.getSharedPreferences(PREF_SESSION_NAME, Context.MODE_PRIVATE)
        val currentKontak = pref.getString(KEY_KONTAK_GURU, "081234567890") ?: "081234567890"

        val paddingHorizontal = (24 * resources.displayMetrics.density).toInt()
        val paddingVertical = (12 * resources.displayMetrics.density).toInt()

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        }

        val etNomorHp = EditText(ctx).apply {
            hint = "Nomor WhatsApp / Ponsel Guru"
            inputType = InputType.TYPE_CLASS_PHONE
            setText(currentKontak)
        }

        container.addView(etNomorHp)

        val alertDialog = MaterialAlertDialogBuilder(ctx)
            .setTitle("Perbarui Kontak Guru")
            .setMessage("Pastikan nomor aktif untuk keperluan koordinasi darurat dan pembaruan sistem Dapodik.")
            .setView(container)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()

        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val baru = etNomorHp.text.toString().trim()
            if (baru.length >= 9) {
                Toast.makeText(ctx, "Menyimpan ke cloud...", Toast.LENGTH_SHORT).show()

                SupabaseClient.instance.updateUser(
                    usernameFilter = "eq.$idAkunGuru",
                    payload = mapOf("no_telepon" to baru)
                ).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            pref.edit().putString(KEY_KONTAK_GURU, baru).apply()
                            alertDialog.dismiss()
                            Toast.makeText(ctx, "✓ Kontak WhatsApp $namaMapel berhasil disimpan di cloud: $baru", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(ctx, "Gagal menyimpan kontak: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Toast.makeText(ctx, "Koneksi cloud gagal: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(ctx, "Nomor kontak tidak valid (minimal 9 digit).", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Dialog Pusat Bantuan & Panduan Guru Mapel
     */
    private fun showDialogPusatBantuan() {
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Pusat Bantuan Guru Mapel")
            .setMessage(
                "Layanan Informasi & Bantuan Portal Presensi SMKN 8 Jakarta:\n\n" +
                "• Jam Layanan: Senin - Jumat (07.00 - 16.00 WIB)\n" +
                "• Ruang Tata Usaha & Dapodik: Gedung A Lantai 2\n" +
                "• Narahubung IT Sekolah: ext 108 / admin.presensi@smkn8.sch.id\n\n" +
                "Hak akses akun Guru Mapel bersifat Read-Only untuk monitoring KBM seluruh kelas."
            )
            .setPositiveButton("Tutup", null)
            .show()
    }

    /**
     * Tombol Logout Akun Guru dengan dialog konfirmasi
     */
    private fun setupLogoutButton() {
        binding.btnLogoutGuru.applyBounceEffect {
            val ctx = context ?: return@applyBounceEffect
            MaterialAlertDialogBuilder(ctx)
                .setTitle("Keluar Akun Guru")
                .setMessage("Apakah Anda yakin ingin keluar dari Portal Guru Mapel SMKN 8 Jakarta?")
                .setPositiveButton("Ya, Keluar") { _, _ ->
                    performLogout()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun performLogout() {
        val pref = requireContext().getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        pref.edit().clear().commit()
        sessionManager.clearSession()

        Toast.makeText(requireContext(), "Anda telah keluar dari Portal Guru Mapel.", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}
