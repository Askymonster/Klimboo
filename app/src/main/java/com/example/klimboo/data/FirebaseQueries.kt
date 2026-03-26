package com.example.klimboo.data

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

object FirebaseQueries {

    private val db by lazy { FirebaseFirestore.getInstance() }

    // ── Modelos ────────────────────────────────────────────────────────────────

    data class Locker(
        val id: String = "",
        val name: String = "",
        val photoUrl: String? = null
    )

    data class Tool(
        val id: String = "",
        val name: String = "",
        val local: String = "",
        val photoUrl: String? = null
    )

    // ── Armarios ──────────────────────────────────────────────────────────────

    suspend fun fetchLockers(): List<Locker> {
        return try {
            db.collection("armarios").get().await().documents.map { doc ->
                Locker(
                    id = doc.id,
                    name = doc.getString("nome") ?: "",
                    photoUrl = doc.getString("photoUrl")
                )
            }
        } catch (e: Exception) {
            Log.e("FIREBASE", "fetchLockers error: ${e.message}")
            emptyList()
        }
    }

    suspend fun insertLocker(nome: String, photoUrl: String? = null) {
        try {
            val data = mutableMapOf<String, Any>("nome" to nome)
            if (photoUrl != null) data["photoUrl"] = photoUrl
            db.collection("armarios").add(data).await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "insertArmario error: ${e.message}")
        }
    }

    suspend fun updateLocker(id: String, novoNome: String) {
        try {
            db.collection("armarios").document(id)
                .update("nome", novoNome).await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "updateLocker error: ${e.message}")
        }
    }

    suspend fun updateLockerPhoto(id: String, photoUrl: String?) {
        try {
            db.collection("armarios").document(id)
                .update("photoUrl", photoUrl).await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "updateLockerPhoto error: ${e.message}")
        }
    }

    suspend fun deleteLocker(id: String, lockerDestinyId: String?) {
        try {
            val tools = db.collection("ferramentas")
                .whereEqualTo("local", id).get().await()
            for (doc in tools.documents) {
                if (lockerDestinyId != null) {
                    doc.reference.update("local", lockerDestinyId).await()
                } else {
                    doc.reference.delete().await()
                }
            }
            db.collection("armarios").document(id).delete().await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "deleteLocker error: ${e.message}")
        }
    }

    // ── Ferramentas ───────────────────────────────────────────────────────────

    suspend fun fetchTools(): List<Tool> {
        return try {
            db.collection("ferramentas").get().await().documents.map { doc ->
                Tool(
                    id = doc.id,
                    name = doc.getString("nome") ?: "",
                    local = (doc.get("local") as? DocumentReference)?.id
                        ?: doc.getString("local") ?: "",
                    photoUrl = doc.getString("photoUrl")
                )
            }
        } catch (e: Exception) {
            Log.e("FIREBASE", "fetchTools error: ${e.message}")
            emptyList()
        }
    }

    suspend fun insertTool(nome: String, lockerId: String, photoUrl: String? = null) {
        try {
            val data = mutableMapOf<String, Any>("nome" to nome, "local" to lockerId)
            if (photoUrl != null) data["photoUrl"] = photoUrl
            db.collection("ferramentas").add(data).await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "insertFerramenta error: ${e.message}")
        }
    }

    suspend fun updateFerramenta(id: String, novoNome: String, newLockerId: String) {
        try {
            db.collection("ferramentas").document(id)
                .update(mapOf("nome" to novoNome, "local" to newLockerId)).await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "updateTool error: ${e.message}")
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
            Log.e("FIREBASE", "deleteTool error: ${e.message}")
        }
    }

    suspend fun fetchToolsByLocker(lockerId: String): List<Tool> {
        return try {
            db.collection("ferramentas")
                .whereEqualTo("local", lockerId)
                .get().await().documents.map { doc ->
                    Tool(
                        id = doc.id,
                        name = doc.getString("nome") ?: "",
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

    fun listenToLockers(onChange: (List<Locker>) -> Unit): ListenerRegistration {
        return db.collection("armarios")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("FIREBASE", "listenLockers error: ${error.message}"); return@addSnapshotListener }
                val lockers = snapshot?.documents?.map { doc ->
                    Locker(
                        id = doc.id,
                        name = doc.getString("nome") ?: "",
                        photoUrl = doc.getString("photoUrl")
                    )
                } ?: emptyList()
                onChange(lockers)
            }
    }

    fun listenToTools(onChange: (List<Tool>) -> Unit): ListenerRegistration {
        return db.collection("ferramentas")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("FIREBASE", "listenTools error: ${error.message}"); return@addSnapshotListener }
                val tools = snapshot?.documents?.map { doc ->
                    Tool(
                        id = doc.id,
                        name = doc.getString("nome") ?: "",
                        local = (doc.get("local") as? DocumentReference)?.id
                            ?: doc.getString("local") ?: "",
                        photoUrl = doc.getString("photoUrl")
                    )
                } ?: emptyList()
                onChange(tools)
            }
    }
}