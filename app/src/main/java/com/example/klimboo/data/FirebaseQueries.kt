package com.example.klimboo.data

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseQueries {

    private val db = FirebaseFirestore.getInstance()

    // ── Models ────────────────────────────────────────────────────────────────

    data class Armario(
        val id: String = "",
        val nome: String = "",
        val photoUrl: String? = null
    )

    data class Ferramenta(
        val id: String = "",
        val nome: String = "",
        val local: String = "",
        val photoUrl: String? = null
    )

    // ── Armarios ──────────────────────────────────────────────────────────────

    suspend fun fetchArmarios(): List<Armario> {
        return try {
            db.collection("armarios").get().await().documents.map { doc ->
                Armario(
                    id = doc.id,
                    nome = doc.getString("nome") ?: "",
                    photoUrl = doc.getString("photoUrl")
                )
            }
        } catch (e: Exception) {
            Log.e("FIREBASE", "fetchArmarios error: ${e.message}")
            emptyList()
        }
    }

    suspend fun insertArmario(nome: String, photoUrl: String? = null) {
        try {
            val data = mutableMapOf<String, Any>("nome" to nome)
            if (photoUrl != null) data["photoUrl"] = photoUrl
            db.collection("armarios").add(data).await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "insertArmario error: ${e.message}")
        }
    }

    suspend fun updateArmario(id: String, novoNome: String) {
        try {
            db.collection("armarios").document(id)
                .update("nome", novoNome).await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "updateArmario error: ${e.message}")
        }
    }

    suspend fun updateArmarioPhoto(id: String, photoUrl: String?) {
        try {
            db.collection("armarios").document(id)
                .update("photoUrl", photoUrl).await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "updateArmarioPhoto error: ${e.message}")
        }
    }

    suspend fun deleteArmario(id: String, armarioDestinoId: String?) {
        try {
            val ferramentas = db.collection("ferramentas")
                .whereEqualTo("local", id).get().await()
            for (doc in ferramentas.documents) {
                if (armarioDestinoId != null) {
                    doc.reference.update("local", armarioDestinoId).await()
                } else {
                    doc.reference.delete().await()
                }
            }
            db.collection("armarios").document(id).delete().await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "deleteArmario error: ${e.message}")
        }
    }

    // ── Ferramentas ───────────────────────────────────────────────────────────

    suspend fun fetchFerramentas(): List<Ferramenta> {
        return try {
            db.collection("ferramentas").get().await().documents.map { doc ->
                Ferramenta(
                    id = doc.id,
                    nome = doc.getString("nome") ?: "",
                    local = (doc.get("local") as? DocumentReference)?.id
                        ?: doc.getString("local") ?: "",
                    photoUrl = doc.getString("photoUrl")
                )
            }
        } catch (e: Exception) {
            Log.e("FIREBASE", "fetchFerramentas error: ${e.message}")
            emptyList()
        }
    }

    suspend fun insertFerramenta(nome: String, armarioId: String, photoUrl: String? = null) {
        try {
            val data = mutableMapOf<String, Any>("nome" to nome, "local" to armarioId)
            if (photoUrl != null) data["photoUrl"] = photoUrl
            db.collection("ferramentas").add(data).await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "insertFerramenta error: ${e.message}")
        }
    }

    suspend fun updateFerramenta(id: String, novoNome: String, novoArmarioId: String) {
        try {
            db.collection("ferramentas").document(id)
                .update(mapOf("nome" to novoNome, "local" to novoArmarioId)).await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "updateFerramenta error: ${e.message}")
        }
    }

    suspend fun updateFerramentaPhoto(id: String, photoUrl: String?) {
        try {
            db.collection("ferramentas").document(id)
                .update("photoUrl", photoUrl).await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "updateFerramentaPhoto error: ${e.message}")
        }
    }

    suspend fun deleteFerramenta(id: String) {
        try {
            db.collection("ferramentas").document(id).delete().await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "deleteFerramenta error: ${e.message}")
        }
    }

    suspend fun fetchFerramentasByLocker(lockerId: String): List<Ferramenta> {
        return try {
            db.collection("ferramentas")
                .whereEqualTo("local", lockerId)
                .get().await().documents.map { doc ->
                    Ferramenta(
                        id = doc.id,
                        nome = doc.getString("nome") ?: "",
                        local = (doc.get("local") as? DocumentReference)?.id
                            ?: doc.getString("local") ?: "",
                        photoUrl = doc.getString("photoUrl")
                    )
                }
        } catch (e: Exception) {
            Log.e("FIREBASE", "fetchFerramentasByLocker error: ${e.message}")
            emptyList()
        }
    }
}