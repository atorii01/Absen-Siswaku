package com.andev.absensiswaku

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
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
    var currentTabId: Int = R.id.nav_walas_presensi
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainWalas) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.mainWalas.setPadding(navBars.left, statusBars.top, navBars.right, 0)

            val baseMargin = (16 * resources.displayMetrics.density).toInt()
            val lp = binding.cardFloatingNavContainer.layoutParams as android.view.ViewGroup.MarginLayoutParams
            lp.bottomMargin = baseMargin + navBars.bottom
            binding.cardFloatingNavContainer.layoutParams = lp
            insets
        }

        val isAdmin = userRole.equals("SUPER_ADMIN", true) || userRole.equals("ADMIN", true)
        if (isAdmin) {
            binding.tvWalasLaporan.text = "Rekap"
            binding.tvWalasSiswa.text = "Master Data"
        } else {
            binding.tvWalasLaporan.text = "Laporan"
            binding.tvWalasSiswa.text = "Siswa"
        }

        setupBottomNavigation()
        setupBackPressHandler()

        if (savedInstanceState == null) {
            selectTab(R.id.nav_walas_presensi, animate = false)
        } else {
            val restoredTabId = savedInstanceState.getInt("CURRENT_TAB_ID", R.id.nav_walas_presensi)
            currentTabId = restoredTabId
            updateNavDockVisuals(restoredTabId, animate = false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("CURRENT_TAB_ID", currentTabId)
    }

    private fun setupBottomNavigation() {
        binding.tabWalasPresensi.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectTab(R.id.nav_walas_presensi)
        }
        binding.tabWalasLaporan.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectTab(R.id.nav_walas_laporan)
        }
        binding.tabWalasSiswa.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectTab(R.id.nav_walas_siswa)
        }
        binding.tabWalasSetelan.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectTab(R.id.nav_walas_setelan)
        }
    }

    fun selectTab(tabId: Int, animate: Boolean = true) {
        if (currentTabId == tabId && supportFragmentManager.findFragmentById(R.id.fragmentContainer) != null && animate) {
            return
        }
        updateNavDockVisuals(tabId, animate)
        loadTabFragment(tabId, animate)
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
            Triple(R.id.nav_walas_presensi, binding.tabWalasPresensi, Pair(binding.icWalasPresensi, binding.tvWalasPresensi)),
            Triple(R.id.nav_walas_laporan, binding.tabWalasLaporan, Pair(binding.icWalasLaporan, binding.tvWalasLaporan)),
            Triple(R.id.nav_walas_siswa, binding.tabWalasSiswa, Pair(binding.icWalasSiswa, binding.tvWalasSiswa)),
            Triple(R.id.nav_walas_setelan, binding.tabWalasSetelan, Pair(binding.icWalasSetelan, binding.tvWalasSetelan))
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

    private fun loadTabFragment(tabId: Int, animate: Boolean = true) {
        val pref = getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val userRole = pref.getString("ROLE", "")?.takeIf { it.isNotEmpty() } ?: sessionManager.getRole()
        val isAdmin = userRole.equals("SUPER_ADMIN", true) || userRole.equals("ADMIN", true)

        val fragment: Fragment = when (tabId) {
            R.id.nav_walas_presensi -> if (isAdmin) com.andev.absensiswaku.ui.admin.AdminPresensiFragment() else WalasPresensiFragment()
            R.id.nav_walas_laporan -> if (isAdmin) com.andev.absensiswaku.ui.admin.AdminRekapFragment() else WalasLaporanFragment()
            R.id.nav_walas_siswa -> if (isAdmin) com.andev.absensiswaku.ui.admin.AdminMasterDataFragment() else WalasSiswaFragment()
            R.id.nav_walas_setelan -> if (isAdmin) com.andev.absensiswaku.ui.admin.AdminSetelanFragment() else WalasSetelanFragment()
            else -> WalasPresensiFragment()
        }

        currentTabId = tabId

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commitNowAllowingStateLoss()

        fragment.view?.let { v ->
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
                if (currentTabId != R.id.nav_walas_presensi) {
                    selectTab(R.id.nav_walas_presensi)
                } else {
                    finish()
                }
            }
        })
    }

    fun loadFragment(fragment: Fragment) {
        val tabId = when (fragment) {
            is WalasPresensiFragment, is com.andev.absensiswaku.ui.admin.AdminPresensiFragment -> R.id.nav_walas_presensi
            is WalasLaporanFragment, is com.andev.absensiswaku.ui.admin.AdminRekapFragment -> R.id.nav_walas_laporan
            is WalasSiswaFragment, is com.andev.absensiswaku.ui.admin.AdminMasterDataFragment -> R.id.nav_walas_siswa
            is WalasSetelanFragment, is com.andev.absensiswaku.ui.admin.AdminSetelanFragment -> R.id.nav_walas_setelan
            else -> R.id.nav_walas_presensi
        }
        selectTab(tabId)
    }
}
