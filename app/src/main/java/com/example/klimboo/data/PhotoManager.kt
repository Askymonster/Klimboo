package com.example.klimboo.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

object PhotoManager {

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val clean = base64.replace("\\s".toRegex(), "")
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            Log.d("PHOTO_DEBUG", "bitmap resultado: $bitmap | bytes: ${bytes.size}")
            bitmap
        } catch (e: Exception) {
            Log.e("PHOTO", "base64ToBitmap error", e)
            null
        }
    }

}