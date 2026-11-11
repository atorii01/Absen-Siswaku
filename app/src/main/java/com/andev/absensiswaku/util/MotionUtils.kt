package com.andev.absensiswaku.util

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator

object MotionUtils {

    private val EMPHASIZED_DECELERATE = PathInterpolator(0.05f, 0.7f, 0.1f, 1.0f)
    private val SPRING_RELEASE = PathInterpolator(0.2f, 0f, 0f, 1f)

    /**
     * Efek Sentuhan Mikro-Pegas (Micro-Spring) Material 3:
     * Ditekan mengecil halus (scale 0.975) dan bergetar haptic halus,
     * dilepas kembali ke 1.0 secara instan tanpa mengorbankan kelancaran scroll gesture.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun View.applyBounceEffect(onClick: (() -> Unit)? = null) {
        if (onClick != null) {
            this.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick.invoke()
            }
        }
        this.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.975f)
                        .scaleY(0.975f)
                        .setDuration(90)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(160)
                        .setInterpolator(SPRING_RELEASE)
                        .start()
                }
            }
            false
        }
    }

    /**
     * Animasi Staggered Entrance Material 3:
     * Menampilkan komponen halaman dengan pergeseran 18dp halus dan fade-in bergradasi.
     */
    fun animateStaggeredEntrance(vararg views: View, initialDelay: Long = 30L, interval: Long = 40L) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 18f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(EMPHASIZED_DECELERATE)
                .setStartDelay(initialDelay + (index * interval))
                .start()
        }
    }
}

/**
 * Top-level convenience extension for applyBounceEffect
 */
@SuppressLint("ClickableViewAccessibility")
fun View.applyBounceEffect(onClick: (() -> Unit)? = null) {
    MotionUtils.run {
        this@applyBounceEffect.applyBounceEffect(onClick)
    }
}

/**
 * Top-level convenience function for animateStaggeredEntrance
 */
fun animateStaggeredEntrance(vararg views: View, initialDelay: Long = 30L, interval: Long = 40L) {
    MotionUtils.animateStaggeredEntrance(*views, initialDelay = initialDelay, interval = interval)
}
