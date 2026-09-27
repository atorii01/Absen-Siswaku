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
import com.andev.absensiswaku.databinding.ActivityGuruMainBinding
import com.andev.absensiswaku.ui.guru.GuruPresensiFragment
import com.andev.absensiswaku.ui.walas.WalasLaporanFragment
import com.andev.absensiswaku.ui.walas.WalasSetelanFragment

class GuruMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuruMainBinding
    private lateinit var sessionManager: SessionManager
    private var backPressedTime: Long = 0

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

        val isLoggedIn = pref.getBoolean("KEY_IS_LOGGED_IN", false) || pref.getBoolean(
            "is_logged_in",
            false
        ) || sessionManager.isLoggedIn()

        if (!isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityGuruMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainGuru) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupBottomNavigation()
        setupBackPressHandler()

        if (savedInstanceState == null) {
            loadFragment(GuruPresensiFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (binding.bottomNavigation.selectedItemId == item.itemId && supportFragmentManager.findFragmentById(R.id.fragmentContainer) != null) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_guru_presensi -> {
                    loadFragment(GuruPresensiFragment())
                    true
                }

                R.id.nav_guru_rekap -> {
                    // Gunakan Laporan / Rekap Presensi Global
                    loadFragment(WalasLaporanFragment())
                    true
                }

                R.id.nav_guru_setelan -> {
                    loadFragment(WalasSetelanFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (currentFragment !is GuruPresensiFragment) {
                    binding.bottomNavigation.selectedItemId = R.id.nav_guru_presensi
                    return
                }

                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish()
                } else {
                    Toast.makeText(
                        this@GuruMainActivity,
                        "Tekan sekali lagi untuk keluar dari aplikasi",
                        Toast.LENGTH_SHORT
                    ).show()
                    backPressedTime = System.currentTimeMillis()
                }
            }
        })
    }
}
