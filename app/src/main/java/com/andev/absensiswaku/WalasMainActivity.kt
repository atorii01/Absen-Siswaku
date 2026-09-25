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
import com.andev.absensiswaku.ui.riwayat.RiwayatFragment
import com.andev.absensiswaku.ui.setelan.SetelanFragment
import com.andev.absensiswaku.ui.walas.WalasPresensiFragment

class WalasMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWalasMainBinding
    private lateinit var sessionManager: SessionManager

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

        val isLoggedIn = pref.getBoolean("KEY_IS_LOGGED_IN", false) || pref.getBoolean("is_logged_in", false) || sessionManager.isLoggedIn()

        if (!isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
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

        if (savedInstanceState == null) {
            loadFragment(WalasPresensiFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (binding.bottomNavigation.selectedItemId == item.itemId && supportFragmentManager.findFragmentById(R.id.fragmentContainer) != null) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_walas_presensi -> {
                    loadFragment(WalasPresensiFragment())
                    true
                }
                R.id.nav_walas_laporan -> {
                    loadFragment(RiwayatFragment())
                    true
                }
                R.id.nav_walas_siswa -> {
                    Toast.makeText(this, "Daftar Siswa Kelas Binaan Aktif", Toast.LENGTH_SHORT).show()
                    loadFragment(WalasPresensiFragment())
                    true
                }
                R.id.nav_walas_setelan -> {
                    loadFragment(SetelanFragment())
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
