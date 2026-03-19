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
        val nome: String = ""
    )

    data class Ferramenta(
        val id: String = "",
        val nome: String = "",
        val local: String = "" // ID do armário
    )

    // ── Armarios ──────────────────────────────────────────────────────────────

    suspend fun fetchArmarios(): List<Armario> {
        return try {
            db.collection("armarios").get().await().documents.map { doc ->
                Armario(id = doc.id, nome = doc.getString("nome") ?: "")
            }
        } catch (e: Exception) {
            Log.e("FIREBASE", "fetchArmarios error: ${e.message}")
            emptyList()
        }
    }

    suspend fun insertArmario(nome: String) {
        try {
            db.collection("armarios").add(mapOf("nome" to nome)).await()
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
                Log.d("FIREBASE_DEBUG", "doc id: ${doc.id}")
                Log.d("FIREBASE_DEBUG", "campos: ${doc.data}")
                Log.d("FIREBASE_DEBUG", "local tipo: ${doc.get("local")?.javaClass?.name}")
                Ferramenta(
                    id = doc.id,
                    nome = doc.getString("nome") ?: "",
                    local = (doc.get("local") as? DocumentReference)?.id ?: doc.getString("local") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("FIREBASE", "fetchFerramentas error: ${e.message}")
            emptyList()
        }
    }

    suspend fun insertFerramenta(nome: String, armarioId: String) {
        try {
            db.collection("ferramentas").add(mapOf(
                "nome" to nome,
                "local" to armarioId
            )).await()
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

    suspend fun deleteFerramenta(id: String) {
        try {
            db.collection("ferramentas").document(id).delete().await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "deleteFerramenta error: ${e.message}")
        }
    }
}