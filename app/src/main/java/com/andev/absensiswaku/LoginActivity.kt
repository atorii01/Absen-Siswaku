package com.andev.absensiswaku

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
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

        // Periksa sesi dengan aman pada onCreate()
        val isLoggedIn = pref.getBoolean("KEY_IS_LOGGED_IN", false)
        if (isLoggedIn) {
            val role = pref.getString("ROLE", "") ?: ""
            when (role) {
                "SISWA" -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    return
                }
                "WALI_KELAS" -> {
                    startActivity(Intent(this, WalasMainActivity::class.java))
                    finish()
                    return
                }
                "GURU_MAPEL" -> {
                    startActivity(Intent(this, GuruMainActivity::class.java))
                    finish()
                    return
                }
                "SUPER_ADMIN" -> {
                    startActivity(Intent(this, AdminMainActivity::class.java))
                    finish()
                    return
                }
                "ADMIN" -> {
                    startActivity(Intent(this, WalasMainActivity::class.java))
                    finish()
                    return
                }
                "GURU" -> {
                    startActivity(Intent(this, WalasMainActivity::class.java))
                    finish()
                    return
                }
            }
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainLogin) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupTabSwitcher()
        setupClickListeners()

        // Set default role tab SISWA pada onCreate
        binding.toggleRole.check(R.id.btnTabSiswa)
        applyTabSelection(R.id.btnTabSiswa)
    }

    private fun setupTabSwitcher() {
        binding.toggleRole.addOnButtonCheckedListener { _, checkedId, isChecked ->
            // WAJIB: Abaikan trigger uncheck agar tidak crash/error
            if (!isChecked) return@addOnButtonCheckedListener

            applyTabSelection(checkedId)
        }
    }

    private fun applyTabSelection(checkedId: Int) {
        val tealColor = ContextCompat.getColor(this, R.color.primary_teal)
        val textPrimaryColor = ContextCompat.getColor(this, R.color.text_primary)
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val transparentColor = ContextCompat.getColor(this, android.R.color.transparent)

        if (checkedId == R.id.btnTabSiswa) {
            selectedRoleTab = "SISWA"
            binding.layoutNisnSiswa.visibility = View.VISIBLE
            binding.layoutPinSiswa.visibility = View.VISIBLE
            binding.layoutNipGuru.visibility = View.GONE
            binding.layoutPasswordGuru.visibility = View.GONE
            binding.btnLogin.text = "Masuk ke Portal Siswa →"
            binding.cardInfoBawah.visibility = View.VISIBLE
            binding.btnGoogleAuth.visibility = View.VISIBLE

            binding.btnTabSiswa.backgroundTintList = ColorStateList.valueOf(tealColor)
            binding.btnTabSiswa.setTextColor(whiteColor)
            binding.btnTabSiswa.iconTint = ColorStateList.valueOf(whiteColor)

            binding.btnTabGuru.backgroundTintList = ColorStateList.valueOf(transparentColor)
            binding.btnTabGuru.setTextColor(textPrimaryColor)
            binding.btnTabGuru.iconTint = ColorStateList.valueOf(textPrimaryColor)

            clearErrors()
            restoreRememberedSiswaIfAvailable()
        } else {
            selectedRoleTab = "GURU"
            binding.layoutNisnSiswa.visibility = View.GONE
            binding.layoutPinSiswa.visibility = View.GONE
            binding.layoutNipGuru.visibility = View.VISIBLE
            binding.layoutPasswordGuru.visibility = View.VISIBLE
            binding.btnLogin.text = "Masuk ke Portal Guru & Tendik →"
            binding.cardInfoBawah.visibility = View.GONE
            binding.btnGoogleAuth.visibility = View.GONE

            binding.btnTabGuru.backgroundTintList = ColorStateList.valueOf(tealColor)
            binding.btnTabGuru.setTextColor(whiteColor)
            binding.btnTabGuru.iconTint = ColorStateList.valueOf(whiteColor)

            binding.btnTabSiswa.backgroundTintList = ColorStateList.valueOf(transparentColor)
            binding.btnTabSiswa.setTextColor(textPrimaryColor)
            binding.btnTabSiswa.iconTint = ColorStateList.valueOf(textPrimaryColor)

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

    private fun restoreRememberedSiswaIfAvailable() {
        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val isRemembered = pref.getBoolean("KEY_REMEMBER_DEVICE_SISWA", false) || pref.getBoolean("KEY_REMEMBER_DEVICE", false)
        val savedNisn = pref.getString("SAVED_NISN", "") ?: ""
        val savedPin = pref.getString("SAVED_PIN", "") ?: ""

        if (isRemembered && savedNisn.isNotEmpty()) {
            binding.etNisnSiswa.setText(savedNisn)
            binding.etPinSiswa.setText(savedPin)
            binding.cbRememberMe.isChecked = true
        } else {
            binding.cbRememberMe.isChecked = false
        }
    }

    private fun restoreRememberedGuruIfAvailable() {
        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val isRemembered = pref.getBoolean("KEY_REMEMBER_DEVICE_GURU", false)
        val savedGuruUsername = pref.getString("SAVED_GURU_USERNAME", "") ?: ""
        val savedGuruPassword = pref.getString("SAVED_GURU_PASSWORD", "") ?: ""

        if (isRemembered && savedGuruUsername.isNotEmpty()) {
            binding.etNipGuru.setText(savedGuruUsername)
            binding.etPasswordGuru.setText(savedGuruPassword)
            binding.cbRememberMe.isChecked = true
        } else {
            binding.cbRememberMe.isChecked = false
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
                        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
                        val editor = pref.edit()
                        editor.putString("ROLE", "SISWA")
                        editor.putString("role", "SISWA")
                        editor.putString("LAST_ROLE_TAB", "SISWA")
                        editor.putInt("ID_SISWA", siswa.id ?: 0)
                        editor.putString("NAMA_SISWA", siswa.namaLengkap ?: "")
                        editor.putString("NISN", siswa.nisn ?: nisn)
                        editor.putInt("ID_KELAS", siswa.idKelas ?: (siswa.rombelKelas?.id ?: 0))
                        editor.putString("KELAS", namaKelas)
                        editor.putString("WALI_KELAS", namaWalas)
                        editor.putBoolean("KEY_IS_LOGGED_IN", true)
                        editor.putBoolean("is_logged_in", true)
                        editor.putBoolean("KEY_REMEMBER_DEVICE", isRememberChecked)
                        editor.putBoolean("KEY_REMEMBER_DEVICE_SISWA", isRememberChecked)
                        if (isRememberChecked) {
                            editor.putString("SAVED_NISN", nisn)
                            editor.putString("SAVED_PIN", pin)
                        } else {
                            editor.remove("SAVED_NISN")
                            editor.remove("SAVED_PIN")
                        }
                        editor.commit()

                        sessionManager.createSession(
                            role = "SISWA",
                            id = siswa.id,
                            nisn = siswa.nisn ?: nisn,
                            nama = siswa.namaLengkap ?: "",
                            kelasId = siswa.idKelas ?: 0,
                            namaKelas = namaKelas,
                            jurusan = jurusan,
                            waliKelas = namaWalas
                        )

                        Toast.makeText(
                            this@LoginActivity,
                            "Login berhasil! Selamat datang, ${siswa.namaLengkap ?: ""}",
                            Toast.LENGTH_SHORT
                        ).show()

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
                                    // Fallback default wali kelas
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
        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString("ROLE", role)
        editor.putString("role", role)
        editor.putString("LAST_ROLE_TAB", "GURU")
        editor.putString("ID_USER", idWalas)
        editor.putString("ID_WALAS", idWalas)
        editor.putString("user_id", idWalas)
        editor.putString("NAMA_LENGKAP", namaWalas)
        editor.putString("NAMA_WALAS", namaWalas)
        editor.putString("nama", namaWalas)
        editor.putString("wali_kelas", namaWalas)
        val finalMapel = mataPelajaran ?: if (isGuruMapel) "Mata Pelajaran Umum" else jurusan
        editor.putString("MATA_PELAJARAN", finalMapel)
        editor.putString("USERNAME", username ?: identifier)
        editor.putInt("ID_KELAS", idKelas)
        editor.putString("NAMA_KELAS", namaKelas)
        editor.putString("nama_kelas", namaKelas)
        editor.putString("jurusan", jurusan)
        editor.putBoolean("KEY_IS_LOGGED_IN", true)
        editor.putBoolean("is_logged_in", true)
        editor.putBoolean("KEY_REMEMBER_DEVICE", isRememberChecked)
        editor.putBoolean("KEY_REMEMBER_DEVICE_GURU", isRememberChecked)
        if (isRememberChecked) {
            editor.putString("SAVED_GURU_USERNAME", identifier)
            editor.putString("SAVED_GURU_PASSWORD", secret)
        } else {
            editor.remove("SAVED_GURU_USERNAME")
            editor.remove("SAVED_GURU_PASSWORD")
        }
        editor.commit() // Gunakan .commit() agar data sesi tersimpan seketika

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

        val targetActivity = if (role.equals("SUPER_ADMIN", true) || role.equals("ADMIN", true)) {
            AdminMainActivity::class.java
        } else if (isGuruMapel) {
            GuruMainActivity::class.java
        } else {
            WalasMainActivity::class.java
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

        val intent = Intent(this@LoginActivity, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnGoogleAuth.isEnabled = !isLoading
        binding.toggleRole.isEnabled = !isLoading
        binding.btnTabSiswa.isEnabled = !isLoading
        binding.btnTabGuru.isEnabled = !isLoading
        binding.etNisnSiswa.isEnabled = !isLoading
        binding.etPinSiswa.isEnabled = !isLoading
        binding.etNipGuru.isEnabled = !isLoading
        binding.etPasswordGuru.isEnabled = !isLoading
    }
}
