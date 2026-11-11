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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.ActivityGuruMainBinding
import com.andev.absensiswaku.ui.guru.GuruPresensiFragment
import com.andev.absensiswaku.ui.guru.GuruRekapFragment
import com.andev.absensiswaku.ui.guru.GuruSetelanFragment

class GuruMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuruMainBinding
    private lateinit var sessionManager: SessionManager
    private var backPressedTime: Long = 0
    var currentTabId: Int = R.id.nav_guru_presensi
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
        binding = ActivityGuruMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainGuru) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.mainGuru.setPadding(navBars.left, statusBars.top, navBars.right, 0)

            val baseMargin = (16 * resources.displayMetrics.density).toInt()
            val lp = binding.cardFloatingNavContainer.layoutParams as android.view.ViewGroup.MarginLayoutParams
            lp.bottomMargin = baseMargin + navBars.bottom
            binding.cardFloatingNavContainer.layoutParams = lp
            insets
        }

        setupBottomNavigation()
        setupBackPressHandler()

        if (savedInstanceState == null) {
            selectTab(R.id.nav_guru_presensi, animate = false)
        } else {
            val restoredTabId = savedInstanceState.getInt("CURRENT_TAB_ID", R.id.nav_guru_presensi)
            currentTabId = restoredTabId
            updateNavDockVisuals(restoredTabId, animate = false)
            loadTabFragment(restoredTabId, animate = false)
        }

        android.os.Looper.myQueue().addIdleHandler {
            preWarmFragments()
            false
        }
    }

    private fun preWarmFragments() {
        if (isFinishing || isDestroyed) return
        val transaction = supportFragmentManager.beginTransaction()
        var hasAdded = false
        if (supportFragmentManager.findFragmentByTag("guru_rekap") == null) {
            val f = GuruRekapFragment()
            transaction.add(R.id.fragmentContainer, f, "guru_rekap").hide(f)
            hasAdded = true
        }
        if (supportFragmentManager.findFragmentByTag("guru_setelan") == null) {
            val f = GuruSetelanFragment()
            transaction.add(R.id.fragmentContainer, f, "guru_setelan").hide(f)
            hasAdded = true
        }
        if (hasAdded && !supportFragmentManager.isStateSaved) {
            transaction.commitAllowingStateLoss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("CURRENT_TAB_ID", currentTabId)
    }

    private fun setupBottomNavigation() {
        binding.tabGuruPresensi.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectTab(R.id.nav_guru_presensi)
        }
        binding.tabGuruRekap.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectTab(R.id.nav_guru_rekap)
        }
        binding.tabGuruSetelan.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectTab(R.id.nav_guru_setelan)
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
            Triple(R.id.nav_guru_presensi, binding.tabGuruPresensi, Pair(binding.icGuruPresensi, binding.tvGuruPresensi)),
            Triple(R.id.nav_guru_rekap, binding.tabGuruRekap, Pair(binding.icGuruRekap, binding.tvGuruRekap)),
            Triple(R.id.nav_guru_setelan, binding.tabGuruSetelan, Pair(binding.icGuruSetelan, binding.tvGuruSetelan))
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
        val targetTag = when (tabId) {
            R.id.nav_guru_presensi -> "guru_presensi"
            R.id.nav_guru_rekap -> "guru_rekap"
            R.id.nav_guru_setelan -> "guru_setelan"
            else -> return
        }

        currentTabId = tabId

        val transaction = supportFragmentManager.beginTransaction()

        for (f in supportFragmentManager.fragments) {
            if (f.tag != targetTag && !f.isHidden) {
                transaction.hide(f)
            }
        }

        var targetFragment = supportFragmentManager.findFragmentByTag(targetTag)
        if (targetFragment == null) {
            targetFragment = when (tabId) {
                R.id.nav_guru_presensi -> GuruPresensiFragment()
                R.id.nav_guru_rekap -> GuruRekapFragment()
                R.id.nav_guru_setelan -> GuruSetelanFragment()
                else -> return
            }
            transaction.add(R.id.fragmentContainer, targetFragment, targetTag)
        } else {
            transaction.show(targetFragment)
        }

        transaction.commitAllowingStateLoss()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentTabId != R.id.nav_guru_presensi) {
                    selectTab(R.id.nav_guru_presensi)
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

    fun loadFragment(fragment: Fragment) {
        val tabId = when (fragment) {
            is GuruPresensiFragment -> R.id.nav_guru_presensi
            is GuruRekapFragment -> R.id.nav_guru_rekap
            is GuruSetelanFragment -> R.id.nav_guru_setelan
            else -> R.id.nav_guru_presensi
        }
        selectTab(tabId)
    }
}
