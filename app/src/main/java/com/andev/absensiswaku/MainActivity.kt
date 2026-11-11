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

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    private lateinit var presensiFragment: Fragment
    private val riwayatFragment by lazy { RiwayatFragment() }
    private val pengajuanFragment by lazy { PengajuanFragment() }
    private val setelanFragment by lazy { SetelanFragment() }
    private var activeFragment: Fragment? = null
    var currentTabId: Int = R.id.nav_presensi
        private set

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
        initFragments(savedInstanceState)
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

    private fun initFragments(savedInstanceState: Bundle?) {
        val isGuru = sessionManager.getRole().equals("GURU", true)
        presensiFragment = if (isGuru) WalasPresensiFragment() else PresensiFragment()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, presensiFragment, "presensi")
                .commit()
            activeFragment = presensiFragment
            currentTabId = R.id.nav_presensi
        } else {
            presensiFragment = supportFragmentManager.findFragmentByTag("presensi") ?: presensiFragment
            activeFragment = supportFragmentManager.fragments.firstOrNull { it.isVisible } ?: presensiFragment
        }
        updateNavUI(currentTabId, animate = false)
    }

    private fun setupBottomNavigation() {
        val interpolator = PathInterpolator(0.05f, 0.7f, 0.1f, 1.0f)

        fun bindTab(tabView: View, iconView: View, targetTabId: Int) {
            tabView.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        iconView.animate()
                            .scaleX(0.85f)
                            .scaleY(0.85f)
                            .setDuration(100)
                            .setInterpolator(interpolator)
                            .start()
                        false
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        iconView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(interpolator)
                            .start()
                        false
                    }
                    else -> false
                }
            }
            tabView.setOnClickListener {
                selectTab(targetTabId, animate = true)
            }
        }

        bindTab(binding.tabPresensi, binding.iconPresensi, R.id.nav_presensi)
        bindTab(binding.tabRiwayat, binding.iconRiwayat, R.id.nav_riwayat)
        bindTab(binding.tabPengajuan, binding.iconPengajuan, R.id.nav_pengajuan)
        bindTab(binding.tabSetelan, binding.iconSetelan, R.id.nav_setelan)
    }

    fun selectTab(tabId: Int, animate: Boolean = true) {
        if (currentTabId == tabId && activeFragment != null) return

        val (targetFragment, tag) = when (tabId) {
            R.id.nav_presensi -> Pair(presensiFragment, "presensi")
            R.id.nav_riwayat -> Pair(riwayatFragment, "riwayat")
            R.id.nav_pengajuan -> Pair(pengajuanFragment, "pengajuan")
            R.id.nav_setelan -> Pair(setelanFragment, "setelan")
            else -> return
        }

        val transaction = supportFragmentManager.beginTransaction()
        if (animate) {
            transaction.setCustomAnimations(R.anim.fragment_fade_enter, R.anim.fragment_fade_exit)
        }

        activeFragment?.let { transaction.hide(it) }

        val existing = supportFragmentManager.findFragmentByTag(tag)
        if (existing == null) {
            transaction.add(R.id.fragmentContainer, targetFragment, tag)
        } else {
            transaction.show(existing)
        }
        transaction.commit()
        activeFragment = targetFragment
        currentTabId = tabId

        updateNavUI(tabId, animate)
    }

    private fun updateNavUI(selectedTabId: Int, animate: Boolean) {
        val tabList = listOf(
            Triple(R.id.nav_presensi, binding.pillPresensi, binding.iconPresensi),
            Triple(R.id.nav_riwayat, binding.pillRiwayat, binding.iconRiwayat),
            Triple(R.id.nav_pengajuan, binding.pillPengajuan, binding.iconPengajuan),
            Triple(R.id.nav_setelan, binding.pillSetelan, binding.iconSetelan)
        )

        val activeColor = ContextCompat.getColor(this, R.color.jade_light_mint)
        val inactiveColor = Color.parseColor("#80FFFFFF")
        val interpolator = PathInterpolator(0.05f, 0.7f, 0.1f, 1.0f)

        tabList.forEach { (id, pill, icon) ->
            val isSelected = (id == selectedTabId)
            if (isSelected) {
                if (pill.visibility != View.VISIBLE) {
                    pill.visibility = View.VISIBLE
                    if (animate) {
                        pill.alpha = 0f
                        pill.scaleX = 0.6f
                        pill.scaleY = 0.6f
                        pill.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(240)
                            .setInterpolator(interpolator)
                            .start()
                    } else {
                        pill.alpha = 1f
                        pill.scaleX = 1f
                        pill.scaleY = 1f
                    }
                }
                icon.imageTintList = ColorStateList.valueOf(activeColor)
                if (animate) {
                    icon.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(120)
                        .withEndAction {
                            icon.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
                        }.start()
                }
            } else {
                if (pill.visibility == View.VISIBLE) {
                    if (animate) {
                        pill.animate()
                            .alpha(0f)
                            .scaleX(0.6f)
                            .scaleY(0.6f)
                            .setDuration(180)
                            .withEndAction { pill.visibility = View.GONE }
                            .start()
                    } else {
                        pill.visibility = View.GONE
                    }
                }
                icon.imageTintList = ColorStateList.valueOf(inactiveColor)
                icon.scaleX = 1f
                icon.scaleY = 1f
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentTabId != R.id.nav_presensi) {
                    selectTab(R.id.nav_presensi, animate = true)
                } else {
                    finish()
                }
            }
        })
    }

    fun loadFragmentFromOutside(fragment: Fragment, navItemId: Int? = null) {
        val tabId = navItemId ?: when (fragment) {
            is PresensiFragment, is WalasPresensiFragment -> R.id.nav_presensi
            is RiwayatFragment -> R.id.nav_riwayat
            is PengajuanFragment -> R.id.nav_pengajuan
            is SetelanFragment -> R.id.nav_setelan
            else -> R.id.nav_presensi
        }
        selectTab(tabId, animate = true)
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