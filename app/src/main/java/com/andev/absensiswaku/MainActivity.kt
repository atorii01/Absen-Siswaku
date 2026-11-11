package com.andev.absensiswaku

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.ActivityMainBinding
import com.andev.absensiswaku.ui.pengajuan.PengajuanFragment
import com.andev.absensiswaku.ui.presensi.PresensiFragment
import com.andev.absensiswaku.ui.riwayat.RiwayatFragment
import com.andev.absensiswaku.ui.setelan.SetelanFragment
import com.andev.absensiswaku.ui.walas.WalasPresensiFragment
import com.andev.absensiswaku.util.NotificationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
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

        val isLoggedIn = pref.getBoolean("KEY_IS_LOGGED_IN", false) || pref.getBoolean("is_logged_in", false) || sessionManager.isLoggedIn()

        if (!isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val userRole = pref.getString("ROLE", "")?.takeIf { it.isNotEmpty() } ?: sessionManager.getRole()
        if (userRole.equals("GURU_MAPEL", true)) {
            val guruIntent = Intent(this, GuruMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(guruIntent)
            finish()
            return
        } else if (userRole.equals("GURU", true) || userRole.equals("WALI_KELAS", true) ||
            userRole.equals("SUPER_ADMIN", true) || userRole.equals("ADMIN", true)) {
            val walasIntent = Intent(this, WalasMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(walasIntent)
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createNotificationChannels(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.main.setPadding(navBars.left, statusBars.top, navBars.right, 0)

            val baseMargin = (16 * resources.displayMetrics.density).toInt()
            val lp = binding.cardFloatingNavContainer.layoutParams as android.view.ViewGroup.MarginLayoutParams
            lp.bottomMargin = baseMargin + navBars.bottom
            binding.cardFloatingNavContainer.layoutParams = lp
            insets
        }

        setupBottomNavigation()
        setupBackPressHandler()

        if (savedInstanceState == null) {
            val isGuru = sessionManager.getRole().equals("GURU", true)
            if (isGuru) {
                loadFragment(WalasPresensiFragment())
            } else {
                loadFragment(PresensiFragment())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (com.andev.absensiswaku.util.NetworkUtils.isNetworkAvailable(this) &&
            com.andev.absensiswaku.util.OfflinePresensiManager.getPendingCount(this) > 0) {
            com.andev.absensiswaku.util.OfflinePresensiManager.syncPendingPresensi(this) { success, _ ->
                if (success > 0) {
                    Toast.makeText(this, "✓ $success Presensi offline berhasil disinkronkan ke server!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Mencegah re-instantiate fragment jika tab yang sama ditekan berulang kali
            if (binding.bottomNavigation.selectedItemId == item.itemId && supportFragmentManager.findFragmentById(R.id.fragmentContainer) != null) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_presensi -> {
                    val isGuru = sessionManager.getRole().equals("GURU", true)
                    if (isGuru) {
                        loadFragment(WalasPresensiFragment())
                    } else {
                        loadFragment(PresensiFragment())
                    }
                    true
                }
                R.id.nav_riwayat -> {
                    loadFragment(RiwayatFragment())
                    true
                }
                R.id.nav_pengajuan -> {
                    loadFragment(PengajuanFragment())
                    true
                }
                R.id.nav_setelan -> {
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
                // Jika sedang di tab selain Presensi, navigasi kembali ke Tab Presensi (Home)
                if (binding.bottomNavigation.selectedItemId != R.id.nav_presensi) {
                    binding.bottomNavigation.selectedItemId = R.id.nav_presensi
                } else {
                    finish()
                }
            }
        })
    }

    fun loadFragmentFromOutside(fragment: Fragment, navItemId: Int? = null) {
        if (navItemId != null) {
            binding.bottomNavigation.selectedItemId = navItemId
        } else {
            when (fragment) {
                is PresensiFragment -> binding.bottomNavigation.selectedItemId = R.id.nav_presensi
                is WalasPresensiFragment -> binding.bottomNavigation.selectedItemId = R.id.nav_presensi
                is RiwayatFragment -> binding.bottomNavigation.selectedItemId = R.id.nav_riwayat
                is PengajuanFragment -> binding.bottomNavigation.selectedItemId = R.id.nav_pengajuan
                is SetelanFragment -> binding.bottomNavigation.selectedItemId = R.id.nav_setelan
            }
        }
        loadFragment(fragment)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_fade_enter,
                R.anim.fragment_fade_exit
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hanya hapus sesi jika activity benar-benar di-finish, bukan saat perubahan konfigurasi (rotasi layar/ganti tema)
        if (isFinishing) {
            val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
            val isRemembered = pref.getBoolean("KEY_REMEMBER_DEVICE", false)
            // Jika tidak memilih "Ingat di perangkat ini", bersihkan sesi saat keluar aplikasi
            if (!isRemembered && ::sessionManager.isInitialized) {
                sessionManager.clearSession()
            }
        }
    }
}