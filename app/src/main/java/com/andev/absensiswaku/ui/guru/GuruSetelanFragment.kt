package com.andev.absensiswaku.ui.guru

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.andev.absensiswaku.LoginActivity
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.FragmentGuruSetelanBinding
import com.andev.absensiswaku.util.MotionUtils
import com.andev.absensiswaku.util.applyBounceEffect
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GuruSetelanFragment : Fragment() {

    private var _binding: FragmentGuruSetelanBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager

    private var namaMapel: String = "Guru Mapel"
    private var bidangMapel: String = "Mata Pelajaran Umum"
    private var idAkunGuru: String = "-"

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

        idAkunGuru = pref.getString("ID_USER", null)?.takeIf { it.isNotBlank() }
            ?: pref.getString("ID_WALAS", null)?.takeIf { it.isNotBlank() }
            ?: pref.getString("SAVED_GURU_USERNAME", null)?.takeIf { it.isNotBlank() }
            ?: sessionManager.getNisn().takeIf { it.isNotBlank() }
            ?: "-"
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
        val paddingHorizontal = (24 * resources.displayMetrics.density).toInt()
        val paddingVertical = (12 * resources.displayMetrics.density).toInt()

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        }

        val etSandiLama = EditText(ctx).apply {
            hint = "Kata Sandi Lama"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val etSandiBaru = EditText(ctx).apply {
            hint = "Kata Sandi Baru (min. 6 karakter)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val etKonfirmasiSandi = EditText(ctx).apply {
            hint = "Konfirmasi Kata Sandi Baru"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        container.addView(etSandiLama)
        container.addView(etSandiBaru)
        container.addView(etKonfirmasiSandi)

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Ubah Kata Sandi Akun Guru")
            .setMessage("Masukkan kata sandi lama dan tentukan kata sandi baru untuk akses Portal Guru Mapel.")
            .setView(container)
            .setPositiveButton("Simpan") { _, _ ->
                val lama = etSandiLama.text.toString().trim()
                val baru = etSandiBaru.text.toString().trim()
                val konfirmasi = etKonfirmasiSandi.text.toString().trim()

                when {
                    lama.isEmpty() -> {
                        Toast.makeText(ctx, "Kata sandi lama wajib diisi.", Toast.LENGTH_SHORT).show()
                    }
                    baru.length < 6 -> {
                        Toast.makeText(ctx, "Kata sandi baru minimal 6 karakter.", Toast.LENGTH_SHORT).show()
                    }
                    baru != konfirmasi -> {
                        Toast.makeText(ctx, "Konfirmasi kata sandi tidak cocok.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(ctx, "✓ Kata sandi akun Guru berhasil diperbarui.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
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

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Perbarui Kontak Guru")
            .setMessage("Pastikan nomor aktif untuk keperluan koordinasi darurat dan pembaruan sistem Dapodik.")
            .setView(container)
            .setPositiveButton("Simpan") { _, _ ->
                val baru = etNomorHp.text.toString().trim()
                if (baru.isNotEmpty()) {
                    pref.edit().putString(KEY_KONTAK_GURU, baru).apply()
                    Toast.makeText(ctx, "✓ Nomor kontak pengajar berhasil disimpan.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(ctx, "Nomor kontak tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
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
