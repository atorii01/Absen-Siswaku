package com.andev.absensiswaku.data.pref

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val pref: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = pref.edit()

    companion object {
        private const val PREF_NAME = "PREF_SMKN8_SESSION"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_ROLE = "role"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NISN = "nisn"
        private const val KEY_NAMA = "nama"
        private const val KEY_ID_KELAS = "id_kelas"
        private const val KEY_NAMA_KELAS = "nama_kelas"
        private const val KEY_JURUSAN = "jurusan"
        private const val KEY_WALI_KELAS = "wali_kelas"
    }

    fun createSession(
        role: String?,
        id: Int?,
        nisn: String?,
        nama: String?,
        kelasId: Int?,
        namaKelas: String?,
        jurusan: String?,
        waliKelas: String?
    ) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_ROLE, role ?: "")
        editor.putInt(KEY_USER_ID, id ?: 0)
        editor.putString(KEY_NISN, nisn ?: "")
        editor.putString(KEY_NAMA, nama ?: "")
        editor.putInt(KEY_ID_KELAS, kelasId ?: 0)
        editor.putString(KEY_NAMA_KELAS, namaKelas ?: "")
        editor.putString(KEY_JURUSAN, jurusan ?: "")
        editor.putString(KEY_WALI_KELAS, waliKelas ?: "")
        editor.apply()
    }

    fun isLoggedIn(): Boolean = pref.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserId(): Int = pref.getInt(KEY_USER_ID, 0)
    fun getNisn(): String = pref.getString(KEY_NISN, "") ?: ""
    fun getNama(): String = pref.getString(KEY_NAMA, "") ?: ""
    fun getIdKelas(): Int = pref.getInt(KEY_ID_KELAS, 0)
    fun getNamaKelas(): String = pref.getString(KEY_NAMA_KELAS, "") ?: ""
    fun getJurusan(): String = pref.getString(KEY_JURUSAN, "") ?: ""
    fun getWaliKelas(): String = pref.getString(KEY_WALI_KELAS, "") ?: ""
    fun getRole(): String = pref.getString(KEY_ROLE, "") ?: ""

    fun clearSession() {
        editor.clear()
        editor.apply()
    }
}
