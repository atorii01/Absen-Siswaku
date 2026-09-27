package com.andev.absensiswaku

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.ActivityWalasMainBinding
import com.andev.absensiswaku.ui.walas.WalasLaporanFragment
import com.andev.absensiswaku.ui.walas.WalasPresensiFragment
import com.andev.absensiswaku.ui.walas.WalasSetelanFragment
import com.andev.absensiswaku.ui.walas.WalasSiswaFragment

open class WalasMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWalasMainBinding
    private lateinit var sessionManager: SessionManager

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

        val isLoggedIn = pref.getBoolean("KEY_IS_LOGGED_IN", false) || pref.getBoolean(
            "is_logged_in",
            false
        ) || sessionManager.isLoggedIn()

        if (!isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val userRole = pref.getString("ROLE", "")?.takeIf { it.isNotEmpty() } ?: sessionManager.getRole()
        if (userRole.equals("SISWA", true)) {
            val siswaIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(siswaIntent)
            finish()
            return
        } else if (userRole.equals("GURU_MAPEL", true)) {
            val guruIntent = Intent(this, GuruMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(guruIntent)
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityWalasMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainWalas) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupBottomNavigation()
        setupBackPressHandler()

        val isAdmin = userRole.equals("SUPER_ADMIN", true) || userRole.equals("ADMIN", true)

        if (savedInstanceState == null) {
            if (isAdmin) {
                loadFragment(com.andev.absensiswaku.ui.admin.AdminPresensiFragment())
            } else {
                loadFragment(WalasPresensiFragment())
            }
        }
    }

    private fun setupBottomNavigation() {
        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val userRole = pref.getString("ROLE", "")?.takeIf { it.isNotEmpty() } ?: sessionManager.getRole()
        val isAdmin = userRole.equals("SUPER_ADMIN", true) || userRole.equals("ADMIN", true)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (binding.bottomNavigation.selectedItemId == item.itemId && supportFragmentManager.findFragmentById(R.id.fragmentContainer) != null) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_walas_presensi -> {
                    if (isAdmin) {
                        loadFragment(com.andev.absensiswaku.ui.admin.AdminPresensiFragment())
                    } else {
                        loadFragment(WalasPresensiFragment())
                    }
                    true
                }

                R.id.nav_walas_laporan -> {
                    if (isAdmin) {
                        loadFragment(com.andev.absensiswaku.ui.admin.AdminRekapFragment())
                    } else {
                        loadFragment(WalasLaporanFragment())
                    }
                    true
                }

                R.id.nav_walas_siswa -> {
                    if (isAdmin) {
                        loadFragment(com.andev.absensiswaku.ui.admin.AdminMasterDataFragment())
                    } else {
                        loadFragment(WalasSiswaFragment())
                    }
                    true
                }

                R.id.nav_walas_setelan -> {
                    if (isAdmin) {
                        loadFragment(com.andev.absensiswaku.ui.admin.AdminSetelanFragment())
                    } else {
                        loadFragment(WalasSetelanFragment())
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bottomNavigation.selectedItemId != R.id.nav_walas_presensi) {
                    binding.bottomNavigation.selectedItemId = R.id.nav_walas_presensi
                } else {
                    finish()
                }
            }
        })
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
