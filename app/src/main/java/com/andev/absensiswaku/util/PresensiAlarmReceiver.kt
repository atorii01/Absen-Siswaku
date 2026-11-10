package com.andev.absensiswaku.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PresensiAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pref = context.getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val isAlarmActive = pref.getBoolean("PREF_SETELAN_ALARM_PAGI", true)

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule alarm saat HP baru selesai restart/boot
            if (isAlarmActive) {
                NotificationHelper.scheduleAlarmPagi(context)
            }
        } else if (intent.action == NotificationHelper.ACTION_PRESENSI_ALARM) {
            // Trigger notifikasi alarm jika switch masih aktif
            if (isAlarmActive) {
                NotificationHelper.showPresensiAlarmNotification(context, isPreview = false)
                // Jadwalkan untuk hari berikutnya
                NotificationHelper.scheduleAlarmPagi(context)
            }
        }
    }
}
