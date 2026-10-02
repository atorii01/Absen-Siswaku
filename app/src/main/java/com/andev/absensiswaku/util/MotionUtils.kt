package com.andev.absensiswaku.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

object MotionUtils {

    /**
     * Efek Sentuhan Pegas Elastis (Spring/Bounce) pada Tombol & Card:
     * Saat ditekan mengecil halus (scale 0.96) dan bergetar haptic halus,
     * saat dilepas membal kembali ke 1.0 dengan OvershootInterpolator.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun View.applyBounceEffect(onClick: (() -> Unit)? = null) {
        this.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    val scaleX = ObjectAnimator.ofFloat(v, "scaleX", 0.96f).setDuration(120)
                    val scaleY = ObjectAnimator.ofFloat(v, "scaleY", 0.96f).setDuration(120)
                    AnimatorSet().apply {
                        playTogether(scaleX, scaleY)
                        interpolator = DecelerateInterpolator()
                        start()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1.0f).setDuration(220)
                    val scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1.0f).setDuration(220)
                    AnimatorSet().apply {
                        playTogether(scaleX, scaleY)
                        interpolator = OvershootInterpolator(2.5f)
                        start()
                    }
                    if (event.action == MotionEvent.ACTION_UP) {
                        v.postDelayed({ onClick?.invoke() }, 80)
                    }
                }
            }
            true
        }
    }

    /**
     * Animasi Staggered Waterfall:
     * Menampilkan list komponen form satu per satu mengalir dari atas ke bawah.
     */
    fun animateStaggeredEntrance(vararg views: View, initialDelay: Long = 50L, interval: Long = 60L) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 40f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator(1.8f))
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
fun animateStaggeredEntrance(vararg views: View, initialDelay: Long = 50L, interval: Long = 60L) {
    MotionUtils.animateStaggeredEntrance(*views, initialDelay = initialDelay, interval = interval)
}
