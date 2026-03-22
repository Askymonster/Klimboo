package com.example.klimboo.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

object PhotoManager {

    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadPhoto(bitmap: Bitmap, folder: String): String? {
        return try {
            val ref = storage.reference.child("$folder/${UUID.randomUUID()}.jpg")
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val bytes = stream.toByteArray()
            ref.putBytes(bytes).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("PHOTO", "uploadPhoto error: ${e.message}")
            null
        }
    }

    suspend fun deletePhoto(url: String) {
        try {
            storage.getReferenceFromUrl(url).delete().await()
        } catch (e: Exception) {
            Log.e("PHOTO", "deletePhoto error: ${e.message}")
        }
    }

    suspend fun updatePhoto(context: Context, bitmap: Bitmap, oldUrl: String?, folder: String): String? {
        if (oldUrl != null) deletePhoto(oldUrl)
        return uploadPhoto(bitmap, folder)
    }
}