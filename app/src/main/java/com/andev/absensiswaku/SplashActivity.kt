package com.andev.absensiswaku

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.andev.absensiswaku.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#00685F")
        window.navigationBarColor = Color.parseColor("#00685F")

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Muat animasi XML
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.splash_logo_anim)
        val textAnim = AnimationUtils.loadAnimation(this, R.anim.splash_text_slide_up)

        // 2. Jalankan animasi pada komponen
        binding.cardSplashLogo.startAnimation(logoAnim)
        binding.tvAppName.startAnimation(textAnim)
        binding.tvAppSubtitle.startAnimation(textAnim)
        binding.tvFooterSplash.startAnimation(textAnim)

        // 3. Pindah ke LoginActivity setelah durasi 1.8 detik
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1800)
    }
}
