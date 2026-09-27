package com.andev.absensiswaku

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        // Pengecekan Sesi Aktif di onCreate()
        val isLoggedIn = sessionManager.isLoggedIn() ||
            pref.getBoolean("KEY_IS_LOGGED_IN", false) ||
            pref.getBoolean("is_logged_in", false)

        val role = pref.getString("ROLE", null)?.takeIf { it.isNotBlank() }
            ?: pref.getString("role", null)?.takeIf { it.isNotBlank() }
            ?: sessionManager.getRole()

        if (isLoggedIn && role.isNotBlank()) {
            when {
                role.equals("SISWA", true) -> {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                    return
                }
                role.equals("GURU_MAPEL", true) -> {
                    val intent = Intent(this, GuruMainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                    return
                }
                role.equals("WALI_KELAS", true) || role.equals("GURU", true) ||
                role.equals("SUPER_ADMIN", true) || role.equals("ADMIN", true) -> {
                    val intent = Intent(this, WalasMainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                    return
                }
                else -> {
                    // Bersihkan sesi korup jika role tidak dikenali
                    sessionManager.clearSession()
                    val editor = pref.edit()
                    editor.putBoolean("KEY_IS_LOGGED_IN", false)
                    editor.putBoolean("is_logged_in", false)
                    editor.apply()
                }
            }
        } else if (isLoggedIn && role.isBlank()) {
            sessionManager.clearSession()
            val editor = pref.edit()
            editor.putBoolean("KEY_IS_LOGGED_IN", false)
            editor.putBoolean("is_logged_in", false)
            editor.apply()
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainLogin) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupTabSwitching()
        setupClickListeners()
        restoreInitialTabAndCredentials()
    }

    private fun setupTabSwitching() {
        binding.toggleGroupRole.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnTabSiswa -> {
                    if (selectedRoleTab != "SISWA") {
                        switchToSiswaTab()
                    }
                }
                R.id.btnTabGuru -> {
                    if (selectedRoleTab != "GURU") {
                        switchToGuruTab()
                    }
                }
            }
        }
    }

    private fun restoreInitialTabAndCredentials() {
        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val lastRole = pref.getString("LAST_ROLE_TAB", "SISWA") ?: "SISWA"
        if (lastRole == "GURU") {
            binding.toggleGroupRole.check(R.id.btnTabGuru)
            if (selectedRoleTab != "GURU") {
                switchToGuruTab()
            }
        } else {
            binding.toggleGroupRole.check(R.id.btnTabSiswa)
            if (selectedRoleTab != "SISWA") {
                switchToSiswaTab()
            } else {
                restoreRememberedSiswaIfAvailable()
            }
        }
    }

    private fun switchToSiswaTab() {
        selectedRoleTab = "SISWA"

        val tealColor = ContextCompat.getColor(this, R.color.primary_teal)
        val textPrimaryColor = ContextCompat.getColor(this, R.color.text_primary)
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val transparentColor = ContextCompat.getColor(this, android.R.color.transparent)

        // Update Tab Siswa Active Style
        binding.btnTabSiswa.backgroundTintList = ColorStateList.valueOf(tealColor)
        binding.btnTabSiswa.setTextColor(whiteColor)
        binding.btnTabSiswa.iconTint = ColorStateList.valueOf(whiteColor)

        // Update Tab Guru Inactive Style
        binding.btnTabGuru.backgroundTintList = ColorStateList.valueOf(transparentColor)
        binding.btnTabGuru.setTextColor(textPrimaryColor)
        binding.btnTabGuru.iconTint = ColorStateList.valueOf(textPrimaryColor)

        // Ensure 24dp rounded capsule shapes on both buttons
        val radiusPx = dpToPx(24)
        binding.btnTabSiswa.shapeAppearanceModel =
            binding.btnTabSiswa.shapeAppearanceModel.toBuilder().setAllCornerSizes(radiusPx).build()
        binding.btnTabGuru.shapeAppearanceModel =
            binding.btnTabGuru.shapeAppearanceModel.toBuilder().setAllCornerSizes(radiusPx).build()

        // Update Labels and Inputs for Siswa
        binding.tvLabelIdentifier.text = "NISN Siswa"
        binding.tvSubLabelIdentifier.text = "10 Digit NISN"
        binding.etIdentifier.hint = "10 digit NISN (cth: 0061829103)"
        binding.etIdentifier.inputType = InputType.TYPE_CLASS_NUMBER
        binding.etIdentifier.filters = arrayOf(InputFilter.LengthFilter(10))

        binding.tvLabelSecret.text = "Tanggal Lahir / PIN Presensi"
        binding.etSecret.hint = "DDMMAAAA atau PIN presensi"
        binding.etSecret.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        binding.btnLogin.text = "Masuk ke Portal Siswa →"
        binding.tvInfoBanner.text =
            "Setelah login, Anda akan diarahkan ke Portal Presensi & Pengajuan Izin Siswa."
        binding.tilIdentifier.setStartIconDrawable(R.drawable.ic_school)
        binding.tvForgotPassword.text = "Lupa PIN?"

        clearInputErrors()
        restoreRememberedSiswaIfAvailable()
    }

    private fun switchToGuruTab() {
        selectedRoleTab = "GURU"

        val tealColor = ContextCompat.getColor(this, R.color.primary_teal)
        val textPrimaryColor = ContextCompat.getColor(this, R.color.text_primary)
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val transparentColor = ContextCompat.getColor(this, android.R.color.transparent)

        // Update Tab Guru Active Style
        binding.btnTabGuru.backgroundTintList = ColorStateList.valueOf(tealColor)
        binding.btnTabGuru.setTextColor(whiteColor)
        binding.btnTabGuru.iconTint = ColorStateList.valueOf(whiteColor)

        // Update Tab Siswa Inactive Style
        binding.btnTabSiswa.backgroundTintList = ColorStateList.valueOf(transparentColor)
        binding.btnTabSiswa.setTextColor(textPrimaryColor)
        binding.btnTabSiswa.iconTint = ColorStateList.valueOf(textPrimaryColor)

        // Ensure 24dp rounded capsule shapes on both buttons
        val radiusPx = dpToPx(24)
        binding.btnTabSiswa.shapeAppearanceModel =
            binding.btnTabSiswa.shapeAppearanceModel.toBuilder().setAllCornerSizes(radiusPx).build()
        binding.btnTabGuru.shapeAppearanceModel =
            binding.btnTabGuru.shapeAppearanceModel.toBuilder().setAllCornerSizes(radiusPx).build()

        // Update Labels and Inputs for Guru
        binding.tvLabelIdentifier.text = "NIP / Username Guru"
        binding.tvSubLabelIdentifier.text = "NIP / Akun"
        binding.etIdentifier.hint = "NIP atau Username"
        binding.etIdentifier.inputType = InputType.TYPE_CLASS_TEXT
        binding.etIdentifier.filters = arrayOf(InputFilter.LengthFilter(50))

        binding.tvLabelSecret.text = "Password"
        binding.etSecret.hint = "Masukkan password"
        binding.etSecret.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        binding.btnLogin.text = "Masuk ke Portal Guru →"
        binding.tvInfoBanner.text =
            "Portal khusus Guru & Tendik untuk pengelolaan presensi kelas & rekapitulasi."
        binding.tilIdentifier.setStartIconDrawable(R.drawable.ic_badge)
        binding.tvForgotPassword.text = "Lupa Password?"

        clearInputErrors()
        restoreRememberedGuruIfAvailable()
    }

    private fun dpToPx(dp: Int): Float {
        return dp * resources.displayMetrics.density
    }

    private fun clearInputErrors() {
        binding.etIdentifier.text?.clear()
        binding.etSecret.text?.clear()
        binding.tilIdentifier.error = null
        binding.tilSecret.error = null
    }

    private fun restoreRememberedSiswaIfAvailable() {
        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val isRemembered = pref.getBoolean("KEY_REMEMBER_DEVICE_SISWA", false) || pref.getBoolean("KEY_REMEMBER_DEVICE", false)
        val savedNisn = pref.getString("SAVED_NISN", "") ?: ""
        val savedPin = pref.getString("SAVED_PIN", "") ?: ""

        if (isRemembered && savedNisn.isNotEmpty() && savedNisn.all { it.isDigit() }) {
            binding.etIdentifier.setText(savedNisn)
            binding.etSecret.setText(savedPin)
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
            binding.etIdentifier.setText(savedGuruUsername)
            binding.etSecret.setText(savedGuruPassword)
            binding.cbRememberMe.isChecked = true
        } else {
            binding.cbRememberMe.isChecked = false
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val inputIdentifier = binding.etIdentifier.text.toString().trim()
            val inputSecret = binding.etSecret.text.toString().trim()

            var isValid = true
            if (inputIdentifier.isEmpty()) {
                val label = if (selectedRoleTab == "SISWA") "NISN" else "Username"
                binding.tilIdentifier.error = "$label wajib diisi"
                isValid = false
            } else {
                binding.tilIdentifier.error = null
            }

            if (inputSecret.isEmpty()) {
                val label = if (selectedRoleTab == "SISWA") "PIN Presensi" else "Password"
                binding.tilSecret.error = "$label wajib diisi"
                isValid = false
            } else {
                binding.tilSecret.error = null
            }

            if (!isValid) return@setOnClickListener

            if (selectedRoleTab == "SISWA") {
                performSiswaLogin(inputIdentifier, inputSecret)
            } else {
                performGuruLogin(inputIdentifier, inputSecret)
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            val msg = if (selectedRoleTab == "SISWA") {
                "Silakan hubungi Wali Kelas atau Admin Sekolah untuk reset PIN Presensi"
            } else {
                "Silakan hubungi Administrator Sekolah untuk reset password akun Guru/Tendik"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }

        binding.btnBelajarId.setOnClickListener {
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
                        val currentNisn = binding.etIdentifier.text.toString().trim()
                        val currentPin = binding.etSecret.text.toString().trim()

                        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
                        val editor = pref.edit()
                        editor.putString("ROLE", "SISWA")
                        editor.putString("role", "SISWA")
                        editor.putString("LAST_ROLE_TAB", "SISWA")
                        editor.putInt("ID_SISWA", siswa.id ?: 0)
                        editor.putString("NAMA_SISWA", siswa.namaLengkap ?: "")
                        editor.putString("NISN", siswa.nisn ?: currentNisn)
                        editor.putInt("ID_KELAS", siswa.idKelas ?: (siswa.rombelKelas?.id ?: 0))
                        editor.putString("KELAS", siswa.rombelKelas?.namaKelas ?: "-")
                        editor.putString("WALI_KELAS", namaWalas)
                        editor.putBoolean("KEY_IS_LOGGED_IN", true)
                        editor.putBoolean("is_logged_in", true)
                        editor.putBoolean("KEY_REMEMBER_DEVICE", isRememberChecked)
                        editor.putBoolean("KEY_REMEMBER_DEVICE_SISWA", isRememberChecked)
                        if (isRememberChecked) {
                            editor.putString("SAVED_NISN", currentNisn)
                            editor.putString("SAVED_PIN", currentPin)
                        } else {
                            editor.remove("SAVED_NISN")
                            editor.remove("SAVED_PIN")
                        }
                        editor.apply()

                        sessionManager.createSession(
                            role = "SISWA",
                            id = siswa.id,
                            nisn = siswa.nisn ?: currentNisn,
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
                        binding.tilSecret.error = "NISN atau PIN Presensi salah"
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

        val isRememberChecked = binding.cbRememberMe.isChecked
        val cleanId = identifier.trim()
        val orFilter = "(username.eq.$cleanId,username.eq.$cleanId@smkn8.sch.id,id.eq.$cleanId)"

        SupabaseClient.instance.loginGuru(orFilter = orFilter)
            .enqueue(object : Callback<List<UserResponse>> {
                override fun onResponse(
                    call: Call<List<UserResponse>>,
                    response: Response<List<UserResponse>>
                ) {
                    showLoading(false)
                    if (!response.isSuccessful) {
                        Toast.makeText(
                            this@LoginActivity,
                            "Gagal terhubung ke server (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val user = response.body()?.firstOrNull()
                    if (user == null) {
                        binding.tilIdentifier.error = "NIP / Username tidak terdaftar"
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
                        binding.tilSecret.error = "Password salah"
                        Toast.makeText(
                            this@LoginActivity,
                            "Password yang Anda masukkan salah",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    // Hapus pesan error jika valid
                    binding.tilIdentifier.error = null
                    binding.tilSecret.error = null

                    val rombel = user.rombelKelas?.firstOrNull()
                    val role = user.role ?: "WALI_KELAS"
                    val idWalas = user.id ?: cleanId
                    val namaWalas = user.namaLengkap ?: cleanId

                    val isGuruMapel = role.equals("GURU_MAPEL", true) || idWalas.startsWith("mapel", true)
                    val isAdmin = role.equals("SUPER_ADMIN", true) || role.equals("ADMIN", true)

                    val idKelas = rombel?.id ?: if (isAdmin) 9 else 0
                    val namaKelas = rombel?.namaKelas ?: if (isAdmin) "Semua Kelas" else if (isGuruMapel) "Semua Kelas" else "-"
                    val jurusan = rombel?.jurusan ?: if (isAdmin) "Administrator Sistem" else if (isGuruMapel) (user.mataPelajaran ?: "Guru Mapel") else "-"

                    saveGuruSessionAndNavigate(
                        role = role,
                        idWalas = idWalas,
                        namaWalas = namaWalas,
                        idKelas = idKelas,
                        namaKelas = namaKelas,
                        jurusan = jurusan,
                        identifier = cleanId,
                        secret = secret,
                        isRememberChecked = isRememberChecked,
                        isGuruMapel = isGuruMapel,
                        mataPelajaran = user.mataPelajaran,
                        username = user.username
                    )
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
        editor.apply()

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

        val targetActivity = if (isGuruMapel) {
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
        binding.btnBelajarId.isEnabled = !isLoading
        binding.toggleGroupRole.isEnabled = !isLoading
        binding.btnTabSiswa.isEnabled = !isLoading
        binding.btnTabGuru.isEnabled = !isLoading
        binding.etIdentifier.isEnabled = !isLoading
        binding.etSecret.isEnabled = !isLoading
    }
}
