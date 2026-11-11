package com.andev.absensiswaku

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
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

    private lateinit var presensiFragment: Fragment
    private val riwayatFragment by lazy { RiwayatFragment() }
    private val pengajuanFragment by lazy { PengajuanFragment() }
    private val setelanFragment by lazy { SetelanFragment() }
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
        val existingPresensi = supportFragmentManager.findFragmentByTag("presensi")
        presensiFragment = existingPresensi ?: if (isGuru) WalasPresensiFragment() else PresensiFragment()

        if (savedInstanceState == null) {
            navigateToTab(R.id.nav_presensi, animate = false)
            updateNavDockVisuals(R.id.nav_presensi, animate = false)
        } else {
            val visibleFragment = supportFragmentManager.fragments.firstOrNull { it.isVisible }
            val activeTabId = when (visibleFragment?.tag) {
                "riwayat" -> R.id.nav_riwayat
                "pengajuan" -> R.id.nav_pengajuan
                "setelan" -> R.id.nav_setelan
                else -> R.id.nav_presensi
            }
            navigateToTab(activeTabId, animate = false)
            updateNavDockVisuals(activeTabId, animate = false)
        }
    }

    private fun setupBottomNavigation() {
        binding.tabPresensi.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectTab(R.id.nav_presensi)
        }
        binding.tabRiwayat.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectTab(R.id.nav_riwayat)
        }
        binding.tabPengajuan.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectTab(R.id.nav_pengajuan)
        }
        binding.tabSetelan.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectTab(R.id.nav_setelan)
        }
    }

    fun selectTab(tabId: Int, animate: Boolean = true) {
        if (currentTabId == tabId && supportFragmentManager.findFragmentById(R.id.fragmentContainer) != null) {
            return
        }
        // Jalankan transisi navbar dan pergantian form bersamaan tanpa jeda
        updateNavDockVisuals(tabId, animate)
        navigateToTab(tabId, animate)
    }

    private fun updateNavDockVisuals(selectedId: Int, animate: Boolean = true) {
        if (animate) {
            val transition = TransitionSet().apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                addTransition(ChangeBounds())
                addTransition(Fade())
                duration = 160
                interpolator = DecelerateInterpolator()
            }
            TransitionManager.beginDelayedTransition(binding.dockNavLayout, transition)
        }

        val items = listOf(
            Triple(R.id.nav_presensi, binding.tabPresensi, Pair(binding.icPresensi, binding.tvPresensi)),
            Triple(R.id.nav_riwayat, binding.tabRiwayat, Pair(binding.icRiwayat, binding.tvRiwayat)),
            Triple(R.id.nav_pengajuan, binding.tabPengajuan, Pair(binding.icPengajuan, binding.tvPengajuan)),
            Triple(R.id.nav_setelan, binding.tabSetelan, Pair(binding.icSetelan, binding.tvSetelan))
        )

        val density = resources.displayMetrics.density
        val activePaddingH = (16 * density).toInt()
        val inactivePaddingH = (14 * density).toInt()

        val tintActive = ContextCompat.getColor(this, R.color.white)
        val tintInactive = Color.parseColor("#80B7E5BA")

        for ((id, tabLayout, views) in items) {
            val (ic, tv) = views
            val isActive = (id == selectedId)
            if (isActive) {
                tabLayout.setBackgroundResource(R.drawable.bg_nav_pill_active)
                tabLayout.setPadding(activePaddingH, 0, activePaddingH, 0)
                ic.imageTintList = ColorStateList.valueOf(tintActive)
                tv.visibility = View.VISIBLE
            } else {
                tabLayout.setBackgroundColor(Color.TRANSPARENT)
                tabLayout.setPadding(inactivePaddingH, 0, inactivePaddingH, 0)
                ic.imageTintList = ColorStateList.valueOf(tintInactive)
                tv.visibility = View.GONE
            }
        }
    }

    private fun navigateToTab(tabId: Int, animate: Boolean = true) {
        val targetTag = when (tabId) {
            R.id.nav_presensi -> "presensi"
            R.id.nav_riwayat -> "riwayat"
            R.id.nav_pengajuan -> "pengajuan"
            R.id.nav_setelan -> "setelan"
            else -> return
        }

        currentTabId = tabId

        val transaction = supportFragmentManager.beginTransaction()

        // 1. Sembunyikan semua fragment non-aktif seketika
        for (f in supportFragmentManager.fragments) {
            if (f.tag != targetTag) {
                transaction.hide(f)
                f.view?.visibility = View.GONE
            }
        }

        // 2. Dapatkan atau buat fragment target
        var targetFragment = supportFragmentManager.findFragmentByTag(targetTag)
        if (targetFragment == null) {
            targetFragment = when (tabId) {
                R.id.nav_presensi -> presensiFragment
                R.id.nav_riwayat -> riwayatFragment
                R.id.nav_pengajuan -> pengajuanFragment
                R.id.nav_setelan -> setelanFragment
                else -> return
            }
            if (!targetFragment.isAdded) {
                transaction.add(R.id.fragmentContainer, targetFragment, targetTag)
            } else {
                transaction.show(targetFragment)
            }
        } else {
            transaction.show(targetFragment)
        }

        transaction.commitNowAllowingStateLoss()

        // Sinkronisasi animasi fade-in form dengan durasi 160ms yang persis sama dengan ekspansi navbar pill
        targetFragment.view?.let { v ->
            v.visibility = View.VISIBLE
            if (animate) {
                v.alpha = 0f
                v.animate()
                    .alpha(1f)
                    .setDuration(160)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            } else {
                v.alpha = 1f
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentTabId != R.id.nav_presensi) {
                    selectTab(R.id.nav_presensi)
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
        selectTab(tabId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
            val isRemembered = pref.getBoolean("KEY_REMEMBER_DEVICE", false) ||
                    pref.getBoolean("KEY_REMEMBER_ME", false) ||
                    pref.getBoolean("KEY_REMEMBER", false)
            if (!isRemembered && ::sessionManager.isInitialized) {
                sessionManager.clearSession()
            }
        }
    }
}
