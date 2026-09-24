package com.andev.absensiswaku

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.ActivityMainBinding
import com.andev.absensiswaku.ui.presensi.PresensiFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(PresensiFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_presensi -> {
                    loadFragment(PresensiFragment())
                    true
                }
                R.id.nav_riwayat -> {
                    Toast.makeText(this, "Tab Riwayat Presensi", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_pengajuan -> {
                    Toast.makeText(this, "Tab Pengajuan Izin/Sakit", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_setelan -> {
                    Toast.makeText(this, "Tab Setelan Akun", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        // Auto-Logout: Reset / clear session when application is closed / finished
        if (::sessionManager.isInitialized) {
            sessionManager.clearSession()
        }
    }
}
