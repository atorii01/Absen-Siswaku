package com.andev.absensiswaku.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.andev.absensiswaku.R
import com.andev.absensiswaku.SplashActivity
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_ID_ALARM = "CHANNEL_SMKN8_ALARM_PRESENSI"
    const val CHANNEL_ID_GURU = "CHANNEL_SMKN8_NOTIFIKASI_GURU"
    const val CHANNEL_ID_PRESENSI = "CHANNEL_SMKN8_PRESENSI_STATUS"

    private const val NOTIF_ID_ALARM = 1001
    private const val NOTIF_ID_GURU = 1002
    private const val NOTIF_ID_PRESENSI = 1003
    private const val NOTIF_ID_PENGAJUAN = 1004

    const val ACTION_PRESENSI_ALARM = "com.andev.absensiswaku.ACTION_PRESENSI_ALARM"

    /**
     * Membuat seluruh Notification Channel yang dibutuhkan aplikasi
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Channel Alarm Pengingat Presensi Pagi
            val channelAlarm = NotificationChannel(
                CHANNEL_ID_ALARM,
                "Alarm Pengingat Presensi Pagi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengingat alarm masuk presensi sekolah pukul 06:00 WIB"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }

            // 2. Channel Notifikasi Guru & Verifikasi
            val channelGuru = NotificationChannel(
                CHANNEL_ID_GURU,
                "Notifikasi Verifikasi Guru",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pemberitahuan verifikasi absensi dan surat izin dari wali kelas"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
            }

            // 3. Channel Status Presensi & Pengajuan
            val channelPresensi = NotificationChannel(
                CHANNEL_ID_PRESENSI,
                "Status Presensi & Izin Siswa",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Konfirmasi berhasil presensi harian dan pengiriman izin"
            }

            notificationManager.createNotificationChannels(
                listOf(channelAlarm, channelGuru, channelPresensi)
            )
        }
    }

    /**
     * Menjadwalkan alarm masuk presensi pagi (Pukul 06:00 WIB)
     */
    fun scheduleAlarmPagi(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PresensiAlarmReceiver::class.java).apply {
            action = ACTION_PRESENSI_ALARM
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, 101, intent, flags)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Membatalkan alarm presensi pagi
     */
    fun cancelAlarmPagi(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PresensiAlarmReceiver::class.java).apply {
            action = ACTION_PRESENSI_ALARM
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 101, intent, flags)
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Menampilkan Notifikasi Alarm Pengingat Presensi
     */
    fun showPresensiAlarmNotification(context: Context, isPreview: Boolean = false) {
        createNotificationChannels(context)

        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val title = if (isPreview) {
            "⏰ Alarm Presensi Pagi Aktif (SMKN 8)"
        } else {
            "⏰ Waktunya Presensi Pagi! (06:00 WIB)"
        }

        val message = if (isPreview) {
            "Alarm pengingat kehadiran aktif setiap hari sekolah. Jam masuk sekolah maksimal pukul 06:30 WIB."
        } else {
            "Selamat pagi! Gerbang sekolah telah dibuka. Segera lakukan presensi kehadiran tepat waktu sebelum 06:30 WIB."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSound)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID_ALARM, builder.build())
    }

    /**
     * Menampilkan Notifikasi dari Guru / Wali Kelas
     */
    fun showNotifikasiGuru(context: Context, title: String, message: String) {
        createNotificationChannels(context)

        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_GURU)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSound)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID_GURU, builder.build())
    }

    /**
     * Menampilkan Notifikasi Status Presensi Berhasil
     */
    fun showNotifikasiPresensi(context: Context, status: String) {
        createNotificationChannels(context)

        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val message = "Presensi kehadiran berhasil dicatat ($status). Data tersinkronisasi ke server SMKN 8 Jakarta."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_PRESENSI)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("✓ Presensi Berhasil Tercatat")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID_PRESENSI, builder.build())
    }

    /**
     * Menampilkan Notifikasi Pengajuan Izin Terkirim
     */
    fun showNotifikasiPengajuan(context: Context, jenisIzin: String) {
        createNotificationChannels(context)

        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val message = "Surat pengajuan $jenisIzin telah berhasil terkirim dan menunggu verifikasi wali kelas."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_PRESENSI)
            .setSmallIcon(R.drawable.ic_assignment)
            .setContentTitle("✓ Pengajuan $jenisIzin Terkirim")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID_PENGAJUAN, builder.build())
    }
}
