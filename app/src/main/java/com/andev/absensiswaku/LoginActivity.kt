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
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.ActivityLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private var currentRoleTab: String = "SISWA"

    override fun onCreate(savedInstanceState: Bundle?) {
        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val isDarkMode = pref.getBoolean("KEY_DARK_MODE", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

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

        // Logika Auto-Fill di onCreate (tanpa bypass halaman login)
        val isRemembered = pref.getBoolean("KEY_REMEMBER_DEVICE", false)

        if (isRemembered) {
            val savedNisn = pref.getString("SAVED_NISN", "") ?: ""
            val savedPin = pref.getString("SAVED_PIN", "") ?: ""

            binding.etIdentifier.setText(savedNisn)
            binding.etSecret.setText(savedPin)
            binding.cbRememberMe.isChecked = true
        } else {
            binding.cbRememberMe.isChecked = false
        }
    }

    private fun setupTabSwitching() {
        // Initialize default UI state (Siswa)
        switchToSiswaTab()

        binding.toggleGroupRole.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnTabSiswa -> {
                    if (currentRoleTab != "SISWA") {
                        switchToSiswaTab()
                    }
                }
                R.id.btnTabGuru -> {
                    if (currentRoleTab != "GURU") {
                        switchToGuruTab()
                    }
                }
            }
        }
    }

    private fun switchToSiswaTab() {
        currentRoleTab = "SISWA"

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

        clearInputErrors()
        restoreRememberedSiswaIfAvailable()
    }

    private fun switchToGuruTab() {
        currentRoleTab = "GURU"

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

        clearInputErrors()
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
        val isRemembered = pref.getBoolean("KEY_REMEMBER_DEVICE", false)
        if (isRemembered) {
            val savedNisn = pref.getString("SAVED_NISN", "") ?: ""
            val savedPin = pref.getString("SAVED_PIN", "") ?: ""
            if (savedNisn.isNotEmpty()) {
                binding.etIdentifier.setText(savedNisn)
            }
            if (savedPin.isNotEmpty()) {
                binding.etSecret.setText(savedPin)
            }
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
                val label = if (currentRoleTab == "SISWA") "NISN" else "Username"
                binding.tilIdentifier.error = "$label wajib diisi"
                isValid = false
            } else {
                binding.tilIdentifier.error = null
            }

            if (inputSecret.isEmpty()) {
                val label = if (currentRoleTab == "SISWA") "PIN Presensi" else "Password"
                binding.tilSecret.error = "$label wajib diisi"
                isValid = false
            } else {
                binding.tilSecret.error = null
            }

            if (!isValid) return@setOnClickListener

            if (currentRoleTab == "SISWA") {
                performSiswaLogin(inputIdentifier, inputSecret)
            } else {
                performGuruLogin(inputIdentifier, inputSecret)
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(
                this,
                "Silakan hubungi Wali Kelas atau Admin Sekolah untuk reset PIN",
                Toast.LENGTH_LONG
            ).show()
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
                        pref.edit().apply {
                            putBoolean("KEY_IS_LOGGED_IN", true)
                            putBoolean("is_logged_in", true)
                            putBoolean("KEY_REMEMBER_DEVICE", isRememberChecked)
                            if (isRememberChecked) {
                                putString("SAVED_NISN", currentNisn)
                                putString("SAVED_PIN", currentPin)
                            } else {
                                remove("SAVED_NISN")
                                remove("SAVED_PIN")
                            }
                            // Simpan session siswa aktif untuk dipakai di portal
                            putInt("ID_SISWA", siswa.id ?: 0)
                            putString("NAMA_SISWA", siswa.namaLengkap ?: "")
                            putString("NISN", siswa.nisn ?: currentNisn)
                            siswa.rombelKelas?.id?.let { putInt("ID_KELAS", it) }
                            putString("KELAS", siswa.rombelKelas?.namaKelas ?: "-")
                            putString("WALI_KELAS", siswa.rombelKelas?.waliKelas?.namaLengkap ?: "-")
                            apply()
                        }

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

                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
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

        val namaWalas = if (identifier.contains("farauk", true)) {
            "Farauk Pratama, S.Kom."
        } else {
            "Herlina, S.E"
        }
        val namaKelas = if (identifier.contains("farauk", true)) {
            "12 RPL 1"
        } else {
            "XII AKL 1"
        }
        val jurusan = if (identifier.contains("farauk", true)) {
            "Rekayasa Perangkat Lunak"
        } else {
            "Akuntansi dan Keuangan Lembaga"
        }
        val idKelas = 1

        val isRememberChecked = binding.cbRememberMe.isChecked
        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        pref.edit().apply {
            putBoolean("KEY_IS_LOGGED_IN", true)
            putBoolean("is_logged_in", true)
            putString("role", "GURU")
            putString("ROLE", "GURU")
            putBoolean("KEY_REMEMBER_DEVICE", isRememberChecked)
            if (isRememberChecked) {
                putString("SAVED_NISN", identifier)
                putString("SAVED_PIN", secret)
            } else {
                remove("SAVED_NISN")
                remove("SAVED_PIN")
            }
            putInt("ID_KELAS", idKelas)
            putString("NAMA_WALAS", namaWalas)
            putString("NAMA_KELAS", namaKelas)
            putString("nama", namaWalas)
            putString("nama_kelas", namaKelas)
            putString("wali_kelas", namaWalas)
            apply()
        }

        sessionManager.createSession(
            role = "GURU",
            id = 101,
            nisn = identifier,
            nama = namaWalas,
            kelasId = idKelas,
            namaKelas = namaKelas,
            jurusan = jurusan,
            waliKelas = namaWalas
        )

        showLoading(false)
        Toast.makeText(
            this@LoginActivity,
            "✓ Selamat Datang, $namaWalas (Wali Kelas $namaKelas)",
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
