package com.andev.absensiswaku

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/**
 * Inisialisasi tingkat aplikasi SMKN 8 Jakarta.
 * Mengunci tema ke Light Mode agar palet warna emerald/teal tidak terdistorsi
 * oleh auto-inversion Android Dark Mode bawaan sistem.
 */
class AbsenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
