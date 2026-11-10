package com.andev.absensiswaku.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager

/**
 * Helper validasi jaringan Wi-Fi resmi SMKN 8 Jakarta.
 * Berfungsi sebagai dual-layer geofence pendukung saat sinyal satelit GPS terhalang struktur gedung (indoor multipath).
 */
object SchoolWifiHelper {

    private val KNOWN_SCHOOL_SSIDS = listOf(
        "SMKN8_SEKOLAH",
        "SMKN 8 JAKARTA",
        "SMKN8",
        "SMKN8_GURU",
        "SMKN8_SISWA",
        "SMKN8_LAB",
        "Eduroam"
    )

    /**
     * Memeriksa apakah perangkat sedang terhubung ke salah satu jaringan Wi-Fi resmi sekolah
     */
    fun isConnectedToSchoolWifi(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false

            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return false
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return false
            val connectionInfo: WifiInfo? = wifiManager.connectionInfo
            val ssid = connectionInfo?.ssid?.replace("\"", "")?.trim().orEmpty()

            if (ssid.isEmpty() || ssid == "<unknown ssid>") {
                false
            } else {
                KNOWN_SCHOOL_SSIDS.any { known -> ssid.contains(known, ignoreCase = true) }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Mengambil nama SSID Wi-Fi aktif
     */
    fun getConnectedSsid(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
            val connectionInfo = wifiManager.connectionInfo ?: return null
            val ssid = connectionInfo.ssid?.replace("\"", "")?.trim()
            if (ssid.isNullOrEmpty() || ssid == "<unknown ssid>") null else ssid
        } catch (e: Exception) {
            null
        }
    }
}
