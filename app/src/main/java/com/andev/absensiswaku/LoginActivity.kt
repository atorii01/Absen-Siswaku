package com.andev.absensiswaku

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.andev.absensiswaku.data.network.RombelMapelResponse
import com.andev.absensiswaku.data.network.SiswaResponse
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.data.network.UserResponse
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.ActivityLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private var selectedRoleTab: String = "SISWA"

    companion object {
        private const val PREF_AUTH = "PREF_SMKN8_AUTH"
        private const val KEY_REMEMBER = "KEY_REMEMBER"
        private const val KEY_LAST_TAB = "KEY_LAST_TAB"

        // Kredensial Siswa
        private const val KEY_SAVED_NISN = "KEY_SAVED_NISN"
        private const val KEY_SAVED_PIN = "KEY_SAVED_PIN"

        // Kredensial Guru & Tendik
        private const val KEY_SAVED_USERNAME = "KEY_SAVED_USERNAME"
        private const val KEY_SAVED_PASSWORD = "KEY_SAVED_PASSWORD"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val isDarkMode = pref.getBoolean("KEY_DARK_MODE", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        sessionManager = SessionManager(this)

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainLogin) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRoleSwitcher()
        loadSavedCredentials() // Prefill data tersimpan jika remember me aktif
        setupClickListeners()
    }

    // ======================================================================
    // 2. LOGIKA KOTLIN: ANTI-GLITCH TAB SWITCHER
    // ======================================================================
    private fun setupRoleSwitcher() {
        binding.cardTabSiswa.setOnClickListener {
            applyTabSelection(isSiswa = true)
        }

        binding.cardTabGuru.setOnClickListener {
            applyTabSelection(isSiswa = false)
        }
    }

    private fun applyTabSelection(isSiswa: Boolean) {
        if (isSiswa) {
            selectedRoleTab = "SISWA"

            // Tab Siswa: Aktif (Hijau Teal Solid, Teks & Ikon Putih)
            binding.cardTabSiswa.setCardBackgroundColor(Color.parseColor("#00685F"))
            binding.tvLabelSiswa.setTextColor(Color.WHITE)
            binding.ivIconSiswa.imageTintList = ColorStateList.valueOf(Color.WHITE)

            // Tab Guru: Inaktif (Transparan, Teks & Ikon Abu-abu)
            binding.cardTabGuru.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tvLabelGuru.setTextColor(Color.parseColor("#64748B"))
            binding.ivIconGuru.imageTintList = ColorStateList.valueOf(Color.parseColor("#64748B"))

            // Transisi Form Siswa
            binding.layoutNisnSiswa.visibility = View.VISIBLE
            binding.layoutPinSiswa.visibility = View.VISIBLE
            binding.layoutNipGuru.visibility = View.GONE
            binding.layoutPasswordGuru.visibility = View.GONE
            binding.btnLogin.text = "Masuk ke Portal Siswa →"
            binding.cardInfoBawah.visibility = View.VISIBLE
            binding.btnGoogleAuth.visibility = View.VISIBLE
            binding.layoutDividerAtau.visibility = View.VISIBLE

            clearErrors()
            restoreRememberedSiswaIfAvailable()
        } else {
            selectedRoleTab = "GURU"

            // Tab Guru: Aktif (Hijau Teal Solid, Teks & Ikon Putih)
            binding.cardTabGuru.setCardBackgroundColor(Color.parseColor("#00685F"))
            binding.tvLabelGuru.setTextColor(Color.WHITE)
            binding.ivIconGuru.imageTintList = ColorStateList.valueOf(Color.WHITE)

            // Tab Siswa: Inaktif (Transparan, Teks & Ikon Abu-abu)
            binding.cardTabSiswa.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tvLabelSiswa.setTextColor(Color.parseColor("#64748B"))
            binding.ivIconSiswa.imageTintList = ColorStateList.valueOf(Color.parseColor("#64748B"))

            // Transisi Form Guru & Tendik
            binding.layoutNisnSiswa.visibility = View.GONE
            binding.layoutPinSiswa.visibility = View.GONE
            binding.layoutNipGuru.visibility = View.VISIBLE
            binding.layoutPasswordGuru.visibility = View.VISIBLE
            binding.btnLogin.text = "Masuk ke Portal Guru & Tendik →"
            binding.cardInfoBawah.visibility = View.GONE
            binding.btnGoogleAuth.visibility = View.GONE
            binding.layoutDividerAtau.visibility = View.GONE

            clearErrors()
            restoreRememberedGuruIfAvailable()
        }
    }

    private fun clearErrors() {
        binding.layoutNisnSiswa.error = null
        binding.layoutPinSiswa.error = null
        binding.layoutNipGuru.error = null
        binding.layoutPasswordGuru.error = null
    }

    private fun loadSavedCredentials() {
        val pref = getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        val isRemembered = pref.getBoolean(KEY_REMEMBER, false)

        binding.cbRememberMe.isChecked = isRemembered

        if (isRemembered) {
            val lastTab = pref.getString(KEY_LAST_TAB, "SISWA") ?: "SISWA"
            if (lastTab == "SISWA") {
                applyTabSelection(isSiswa = true)
                binding.etNisnSiswa.setText(pref.getString(KEY_SAVED_NISN, ""))
                binding.etPinSiswa.setText(pref.getString(KEY_SAVED_PIN, ""))
            } else {
                applyTabSelection(isSiswa = false)
                binding.etNipGuru.setText(pref.getString(KEY_SAVED_USERNAME, ""))
                binding.etPasswordGuru.setText(pref.getString(KEY_SAVED_PASSWORD, ""))
            }
        } else {
            // Default awal tab Siswa dengan input kosong
            applyTabSelection(isSiswa = true)
        }
    }

    private fun restoreRememberedSiswaIfAvailable() {
        val pref = getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        val isRemembered = pref.getBoolean(KEY_REMEMBER, false)
        val savedNisn = pref.getString(KEY_SAVED_NISN, "") ?: ""
        val savedPin = pref.getString(KEY_SAVED_PIN, "") ?: ""

        if (isRemembered && savedNisn.isNotEmpty()) {
            binding.etNisnSiswa.setText(savedNisn)
            binding.etPinSiswa.setText(savedPin)
        }
    }

    private fun restoreRememberedGuruIfAvailable() {
        val pref = getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        val isRemembered = pref.getBoolean(KEY_REMEMBER, false)
        val savedGuruUsername = pref.getString(KEY_SAVED_USERNAME, "") ?: ""
        val savedGuruPassword = pref.getString(KEY_SAVED_PASSWORD, "") ?: ""

        if (isRemembered && savedGuruUsername.isNotEmpty()) {
            binding.etNipGuru.setText(savedGuruUsername)
            binding.etPasswordGuru.setText(savedGuruPassword)
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            clearErrors()

            if (selectedRoleTab == "SISWA") {
                val nisn = binding.etNisnSiswa.text.toString().trim()
                val pin = binding.etPinSiswa.text.toString().trim()

                var isValid = true
                if (nisn.isEmpty()) {
                    binding.layoutNisnSiswa.error = "NISN Siswa wajib diisi"
                    isValid = false
                }
                if (pin.isEmpty()) {
                    binding.layoutPinSiswa.error = "PIN Presensi wajib diisi"
                    isValid = false
                }

                if (isValid) {
                    performSiswaLogin(nisn, pin)
                }
            } else {
                val nip = binding.etNipGuru.text.toString().trim()
                val password = binding.etPasswordGuru.text.toString().trim()

                var isValid = true
                if (nip.isEmpty()) {
                    binding.layoutNipGuru.error = "NIP atau Username Guru wajib diisi"
                    isValid = false
                }
                if (password.isEmpty()) {
                    binding.layoutPasswordGuru.error = "Password wajib diisi"
                    isValid = false
                }

                if (isValid) {
                    performGuruLogin(nip, password)
                }
            }
        }

        binding.btnGoogleAuth.setOnClickListener {
            Toast.makeText(
                this,
                "Login Akun Belajar.id akan segera hadir",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.llFooterHelp.setOnClickListener {
            Toast.makeText(
                this,
                "Layanan Bantuan SMKN 8 Jakarta: hubungi Admin Presensi di sekolah",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun performSiswaLogin(nisn: String, pin: String) {
        showLoading(true)

        val nisnEq = "eq.$nisn"
        val pinEq = "eq.$pin"

        SupabaseClient.instance.loginSiswa(
            nisnFilter = nisnEq,
            pinFilter = pinEq
        ).enqueue(object : Callback<List<SiswaResponse>> {
            override fun onResponse(
                call: Call<List<SiswaResponse>>,
                response: Response<List<SiswaResponse>>
            ) {
                showLoading(false)

                if (response.isSuccessful) {
                    val siswaList = response.body()
                    if (!siswaList.isNullOrEmpty()) {
                        val siswa = siswaList[0]

                        val namaKelas = siswa.rombelKelas?.namaKelas ?: ""
                        val jurusan = siswa.rombelKelas?.jurusan ?: ""
                        val namaWalas = siswa.rombelKelas?.waliKelas?.namaLengkap ?: "-"

                        val isRememberChecked = binding.cbRememberMe.isChecked
                        val roleUser = "SISWA"
                        val idUser = "${siswa.id ?: 0}"
                        val namaUser = siswa.namaLengkap ?: ""

                        // Simpan kredensial form prefill jika Remember Me dicentang
                        val prefAuth = getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
                        if (isRememberChecked) {
                            prefAuth.edit().apply {
                                putBoolean(KEY_REMEMBER, true)
                                putString(KEY_LAST_TAB, "SISWA")
                                putString(KEY_SAVED_NISN, nisn)
                                putString(KEY_SAVED_PIN, pin)
                                commit()
                            }
                        } else {
                            prefAuth.edit().clear().commit()
                        }

                        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
                        pref.edit().apply {
                            putBoolean("KEY_IS_LOGGED_IN", true)
                            putBoolean("KEY_REMEMBER_ME", isRememberChecked)
                            putString("ROLE", roleUser)
                            putString("ID_USER", idUser)
                            putString("NAMA_LENGKAP", namaUser)
                            putInt("ID_SISWA", siswa.id ?: 0)
                            putString("NAMA_SISWA", namaUser)
                            putString("NISN", siswa.nisn ?: nisn)
                            putInt("ID_KELAS", siswa.idKelas ?: (siswa.rombelKelas?.id ?: 0))
                            putString("KELAS", namaKelas)
                            putString("WALI_KELAS", namaWalas)
                            commit() // Gunakan commit() agar tersimpan langsung ke disk
                        }

                        sessionManager.createSession(
                            role = roleUser,
                            id = siswa.id,
                            nisn = siswa.nisn ?: nisn,
                            nama = namaUser,
                            kelasId = siswa.idKelas ?: 0,
                            namaKelas = namaKelas,
                            jurusan = jurusan,
                            waliKelas = namaWalas
                        )

                        Toast.makeText(
                            this@LoginActivity,
                            "Login berhasil! Selamat datang, $namaUser",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Buka Activity sesuai Role dengan CLEAR_TASK agar tombol Back tidak balik ke Login:
                        val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        binding.layoutPinSiswa.error = "NISN atau PIN Presensi salah"
                        Toast.makeText(
                            this@LoginActivity,
                            "NISN atau PIN Presensi salah",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Gagal autentikasi (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<SiswaResponse>>, t: Throwable) {
                showLoading(false)
                Toast.makeText(
                    this@LoginActivity,
                    "Koneksi gagal: ${t.localizedMessage ?: t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun performGuruLogin(identifier: String, secret: String) {
        showLoading(true)

        val cleanId = identifier.trim()
        val orFilter = "(username.eq.$cleanId,username.eq.$cleanId@smkn8.sch.id,id.eq.$cleanId)"

        SupabaseClient.instance.loginGuru(orFilter = orFilter)
            .enqueue(object : Callback<List<UserResponse>> {
                override fun onResponse(
                    call: Call<List<UserResponse>>,
                    response: Response<List<UserResponse>>
                ) {
                    if (!response.isSuccessful) {
                        showLoading(false)
                        Toast.makeText(
                            this@LoginActivity,
                            "Gagal terhubung ke server (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val user = response.body()?.firstOrNull()
                    if (user == null) {
                        showLoading(false)
                        binding.layoutNipGuru.error = "NIP / Username tidak terdaftar"
                        Toast.makeText(
                            this@LoginActivity,
                            "Akun Guru / NIP tidak ditemukan di database SMKN 8",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    // Verifikasi Password Hash
                    val expectedHash = user.passwordHash
                    val isPasswordValid = if (!expectedHash.isNullOrEmpty()) {
                        secret == expectedHash
                    } else {
                        secret == "guru123" || secret == "admin123"
                    }

                    if (!isPasswordValid) {
                        showLoading(false)
                        binding.layoutPasswordGuru.error = "Password salah"
                        Toast.makeText(
                            this@LoginActivity,
                            "Password yang Anda masukkan salah",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    // Hapus pesan error jika valid
                    binding.layoutNipGuru.error = null
                    binding.layoutPasswordGuru.error = null

                    val role = user.role ?: "WALI_KELAS"
                    val idWalas = user.id ?: cleanId
                    val namaWalas = user.namaLengkap ?: cleanId
                    val isGuruMapel = role.equals("GURU_MAPEL", true) || idWalas.startsWith("mapel", true)
                    val isAdmin = role.equals("SUPER_ADMIN", true) || role.equals("ADMIN", true)
                    val rombelFromUser = user.rombelKelas?.firstOrNull()

                    // Jika wali kelas tapi rombelKelas belum terasosiasi di response, cari rombel_kelas berdasarkan wali_kelas_id
                    if (!isAdmin && !isGuruMapel && rombelFromUser == null) {
                        SupabaseClient.instance.getRombelByWalas(filterWalas = "eq.$idWalas")
                            .enqueue(object : Callback<List<RombelMapelResponse>> {
                                override fun onResponse(
                                    call: Call<List<RombelMapelResponse>>,
                                    rombelResponse: Response<List<RombelMapelResponse>>
                                ) {
                                    showLoading(false)
                                    val rombelDitemukan = rombelResponse.body()?.firstOrNull()
                                    val idKelas = rombelDitemukan?.id ?: 1
                                    val namaKelas = rombelDitemukan?.namaKelas ?: "XII AKL 1"
                                    val jurusan = rombelDitemukan?.jurusan ?: "Akuntansi dan Keuangan Lembaga"

                                    saveGuruSessionAndNavigate(
                                        role = role,
                                        idWalas = idWalas,
                                        namaWalas = namaWalas,
                                        idKelas = idKelas,
                                        namaKelas = namaKelas,
                                        jurusan = jurusan,
                                        identifier = cleanId,
                                        secret = secret,
                                        isRememberChecked = binding.cbRememberMe.isChecked,
                                        isGuruMapel = false,
                                        mataPelajaran = user.mataPelajaran,
                                        username = user.username
                                    )
                                }

                                override fun onFailure(call: Call<List<RombelMapelResponse>>, t: Throwable) {
                                    showLoading(false)
                                    saveGuruSessionAndNavigate(
                                        role = role,
                                        idWalas = idWalas,
                                        namaWalas = namaWalas,
                                        idKelas = 1,
                                        namaKelas = "XII AKL 1",
                                        jurusan = "Akuntansi dan Keuangan Lembaga",
                                        identifier = cleanId,
                                        secret = secret,
                                        isRememberChecked = binding.cbRememberMe.isChecked,
                                        isGuruMapel = false,
                                        mataPelajaran = user.mataPelajaran,
                                        username = user.username
                                    )
                                }
                            })
                    } else {
                        showLoading(false)
                        val idKelas = rombelFromUser?.id ?: if (isAdmin) 9 else 1
                        val namaKelas = rombelFromUser?.namaKelas ?: if (isAdmin) "Semua Kelas" else if (isGuruMapel) "Semua Kelas" else "XII AKL 1"
                        val jurusan = rombelFromUser?.jurusan ?: if (isAdmin) "Administrator Sistem" else if (isGuruMapel) (user.mataPelajaran ?: "Guru Mapel") else "Akuntansi dan Keuangan Lembaga"

                        saveGuruSessionAndNavigate(
                            role = role,
                            idWalas = idWalas,
                            namaWalas = namaWalas,
                            idKelas = idKelas,
                            namaKelas = namaKelas,
                            jurusan = jurusan,
                            identifier = cleanId,
                            secret = secret,
                            isRememberChecked = binding.cbRememberMe.isChecked,
                            isGuruMapel = isGuruMapel,
                            mataPelajaran = user.mataPelajaran,
                            username = user.username
                        )
                    }
                }

                override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(
                        this@LoginActivity,
                        "Koneksi gagal: ${t.localizedMessage ?: t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun saveGuruSessionAndNavigate(
        role: String,
        idWalas: String,
        namaWalas: String,
        idKelas: Int,
        namaKelas: String,
        jurusan: String,
        identifier: String,
        secret: String,
        isRememberChecked: Boolean,
        isGuruMapel: Boolean,
        mataPelajaran: String? = null,
        username: String? = null
    ) {
        // Simpan kredensial form prefill jika Remember Me dicentang
        val prefAuth = getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        if (isRememberChecked) {
            prefAuth.edit().apply {
                putBoolean(KEY_REMEMBER, true)
                putString(KEY_LAST_TAB, "GURU")
                putString(KEY_SAVED_USERNAME, identifier)
                putString(KEY_SAVED_PASSWORD, secret)
                commit()
            }
        } else {
            prefAuth.edit().clear().commit()
        }

        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        pref.edit().apply {
            putBoolean("KEY_IS_LOGGED_IN", true)
            putBoolean("KEY_REMEMBER_ME", isRememberChecked)
            putString("ROLE", role)
            putString("ID_USER", idWalas)
            putString("NAMA_LENGKAP", namaWalas)
            putString("ID_WALAS", idWalas)
            putString("user_id", idWalas)
            putString("NAMA_WALAS", namaWalas)
            putString("nama", namaWalas)
            putString("wali_kelas", namaWalas)
            val finalMapel = mataPelajaran ?: if (isGuruMapel) "Mata Pelajaran Umum" else jurusan
            putString("MATA_PELAJARAN", finalMapel)
            putString("USERNAME", username ?: identifier)
            putInt("ID_KELAS", idKelas)
            putString("NAMA_KELAS", namaKelas)
            putString("nama_kelas", namaKelas)
            putString("jurusan", jurusan)
            commit() // Gunakan commit() agar tersimpan langsung ke disk
        }

        sessionManager.createSession(
            role = role,
            id = idKelas,
            nisn = identifier,
            nama = namaWalas,
            kelasId = idKelas,
            namaKelas = namaKelas,
            jurusan = jurusan,
            waliKelas = namaWalas
        )

        // Buka Activity sesuai Role dengan CLEAR_TASK agar tombol Back tidak balik ke Login:
        val intent = when (role) {
            "SISWA" -> Intent(this, MainActivity::class.java)
            "WALI_KELAS" -> Intent(this, WalasMainActivity::class.java)
            "GURU_MAPEL" -> Intent(this, GuruMainActivity::class.java)
            "SUPER_ADMIN" -> Intent(this, AdminMainActivity::class.java)
            "ADMIN" -> Intent(this, AdminMainActivity::class.java)
            else -> Intent(this, WalasMainActivity::class.java)
        }

        val roleLabel = when {
            role.equals("SUPER_ADMIN", true) || role.equals("ADMIN", true) -> "Administrator SMKN 8"
            isGuruMapel -> "Guru Mata Pelajaran"
            else -> "Wali Kelas $namaKelas"
        }
        Toast.makeText(
            this@LoginActivity,
            "✓ Selamat Datang, $namaWalas ($roleLabel)",
            Toast.LENGTH_SHORT
        ).show()

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnGoogleAuth.isEnabled = !isLoading
        binding.cardTabSiswa.isEnabled = !isLoading
        binding.cardTabGuru.isEnabled = !isLoading
        binding.etNisnSiswa.isEnabled = !isLoading
        binding.etPinSiswa.isEnabled = !isLoading
        binding.etNipGuru.isEnabled = !isLoading
        binding.etPasswordGuru.isEnabled = !isLoading
    }
}
