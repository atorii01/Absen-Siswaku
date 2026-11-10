package com.andev.absensiswaku.util

import android.content.Context
import android.util.Log
import com.andev.absensiswaku.data.network.SupabaseClient
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object OfflinePresensiManager {

    private const val TAG = "OfflinePresensiManager"
    private const val PREF_OFFLINE = "PREF_OFFLINE_STORAGE"
    private const val KEY_QUEUE = "KEY_PENDING_PRESENSI_QUEUE"

    /**
     * Menyimpan data presensi yang belum terkirim (offline) ke penyimpanan lokal aman
     */
    fun savePresensiOffline(context: Context, data: Map<String, Any>) {
        val pref = context.getSharedPreferences(PREF_OFFLINE, Context.MODE_PRIVATE)
        val currentJson = pref.getString(KEY_QUEUE, "[]") ?: "[]"

        try {
            val jsonArray = JSONArray(currentJson)
            val jsonObject = JSONObject(data)
            jsonArray.put(jsonObject)

            pref.edit().putString(KEY_QUEUE, jsonArray.toString()).apply()
            Log.d(TAG, "Antrean presensi offline ditambahkan: ${data["tanggal"]}. Total: ${jsonArray.length()}")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengantrekan presensi offline", e)
        }
    }

    /**
     * Menyimpan data presensi pulang yang belum terkirim (offline)
     */
    fun savePresensiPulangOffline(context: Context, presensiId: String, waktuPulang: String) {
        val data = mapOf<String, Any>(
            "_type" to "pulang",
            "id" to presensiId,
            "waktu_pulang" to waktuPulang
        )
        savePresensiOffline(context, data)
    }

    /**
     * Menghitung jumlah antrean presensi offline yang menunggu sinkronisasi
     */
    fun getPendingCount(context: Context): Int {
        val pref = context.getSharedPreferences(PREF_OFFLINE, Context.MODE_PRIVATE)
        val currentJson = pref.getString(KEY_QUEUE, "[]") ?: "[]"
        return try {
            JSONArray(currentJson).length()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Mengambil daftar seluruh presensi offline yang tersimpan di memori perangkat
     */
    fun getPendingList(context: Context): List<Map<String, Any>> {
        val pref = context.getSharedPreferences(PREF_OFFLINE, Context.MODE_PRIVATE)
        val currentJson = pref.getString(KEY_QUEUE, "[]") ?: "[]"
        val list = mutableListOf<Map<String, Any>>()

        try {
            val jsonArray = JSONArray(currentJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val map = mutableMapOf<String, Any>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = obj.get(key)
                }
                list.add(map)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca antrean presensi", e)
        }
        return list
    }

    /**
     * Menyinkronkan seluruh antrean presensi offline ke database Supabase
     */
    fun syncPendingPresensi(context: Context, onComplete: ((syncedCount: Int, failedCount: Int) -> Unit)? = null) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            onComplete?.invoke(0, getPendingCount(context))
            return
        }

        val pending = getPendingList(context)
        if (pending.isEmpty()) {
            onComplete?.invoke(0, 0)
            return
        }

        var successCount = 0
        var failedCount = 0
        var processedCount = 0
        val remaining = mutableListOf<Map<String, Any>>()

        fun checkFinish() {
            if (processedCount == pending.size) {
                val pref = context.getSharedPreferences(PREF_OFFLINE, Context.MODE_PRIVATE)
                val newArray = JSONArray()
                for (rem in remaining) {
                    newArray.put(JSONObject(rem))
                }
                pref.edit().putString(KEY_QUEUE, newArray.toString()).apply()

                if (successCount > 0) {
                    NotificationHelper.showNotifikasiPresensi(
                        context,
                        "Tersinkronisasi Otomatis ($successCount Data)"
                    )
                }

                onComplete?.invoke(successCount, failedCount)
            }
        }

        for (item in pending) {
            val type = item["_type"]?.toString()
            if (type == "pulang") {
                val presensiId = item["id"]?.toString() ?: ""
                val waktuPulang = item["waktu_pulang"]?.toString() ?: ""
                SupabaseClient.instance.updatePresensiHarian("eq.$presensiId", mapOf("waktu_pulang" to waktuPulang))
                    .enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            processedCount++
                            if (response.isSuccessful) {
                                successCount++
                            } else {
                                failedCount++
                                remaining.add(item)
                            }
                            checkFinish()
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            processedCount++
                            failedCount++
                            remaining.add(item)
                            checkFinish()
                        }
                    })
            } else {
                val cleanData = item.filterKeys { !it.startsWith("_") }
                SupabaseClient.instance.simpanPresensi(cleanData).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        processedCount++
                        if (response.isSuccessful || response.code() == 409) {
                            // 409 berarti record sudah ada di server, tandai sebagai tersinkron
                            successCount++
                        } else {
                            failedCount++
                            remaining.add(item)
                        }
                        checkFinish()
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        processedCount++
                        failedCount++
                        remaining.add(item)
                        checkFinish()
                    }
                })
            }
        }
    }
}
