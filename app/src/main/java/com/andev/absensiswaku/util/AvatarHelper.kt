package com.andev.absensiswaku.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.widget.ImageView
import com.andev.absensiswaku.R
import java.io.File
import java.io.FileOutputStream

object AvatarHelper {

    const val FILE_AVATAR_WALAS = "avatar_walas.jpg"
    const val FILE_AVATAR_GURU = "avatar_guru.jpg"
    const val FILE_AVATAR_ADMIN = "avatar_admin.jpg"

    fun getAvatarFileForSiswa(siswaId: Int): String {
        return "avatar_siswa_$siswaId.jpg"
    }

    /**
     * Memotong bitmap secara simetris dari tengah dan mengubahnya menjadi lingkaran sempurna
     */
    fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        val squared = Bitmap.createBitmap(bitmap, x, y, size, size)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val rect = Rect(0, 0, size, size)
        canvas.drawBitmap(squared, rect, rect, paint)

        if (squared != bitmap && !squared.isRecycled) {
            squared.recycle()
        }

        return output
    }

    /**
     * Menyimpan foto profil yang sudah dipotong bulat ke direktori privat aplikasi
     */
    fun saveAvatarPhoto(context: Context, filename: String, rawBitmap: Bitmap): Bitmap? {
        return try {
            val circularBitmap = getCircularBitmap(rawBitmap)
            val file = File(context.filesDir, filename)
            FileOutputStream(file).use { out ->
                circularBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            circularBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Menghapus file foto profil kustom
     */
    fun deleteAvatarPhoto(context: Context, filename: String): Boolean {
        return try {
            val file = File(context.filesDir, filename)
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Memeriksa apakah foto profil kustom tersimpan
     */
    fun hasCustomAvatar(context: Context, filename: String): Boolean {
        return File(context.filesDir, filename).exists()
    }

    /**
     * Memuat foto profil ke ImageView. Jika ada file tersimpan, dimuat dalam bentuk bulat.
     * Jika tidak ada, kembali ke placeholder default.
     */
    fun loadAvatar(
        context: Context,
        filename: String,
        imageView: ImageView,
        placeholderRes: Int = R.drawable.ic_avatar
    ) {
        try {
            val file = File(context.filesDir, filename)
            if (file.exists() && file.length() > 0) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val circular = getCircularBitmap(bitmap)
                    imageView.setImageBitmap(circular)
                    return
                }
            }
            imageView.setImageResource(placeholderRes)
        } catch (e: Exception) {
            imageView.setImageResource(placeholderRes)
        }
    }
}
