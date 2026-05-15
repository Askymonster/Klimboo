package com.example.klimboo.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import android.util.Log
import java.io.ByteArrayOutputStream

object PhotoManager {

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    // ── Gerencia todas as fotos de armários/ferramentas ──────────────────────────────────────────────────────────────
    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    fun base64ToBitmap(base64: String): Bitmap? {
        val cacheKey = base64.hashCode().toString()
        bitmapCache.get(cacheKey)?.let { return it }

        return try {
            val clean = base64.replace("\\s".toRegex(), "")
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                bitmapCache.put(cacheKey, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            Log.e("PHOTO", "base64ToBitmap error", e)
            null
        }
    }

    private fun cacheSizeKb(): Int {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        return maxMemoryKb / 8
    }

}
