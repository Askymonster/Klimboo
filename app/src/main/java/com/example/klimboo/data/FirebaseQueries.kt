package com.example.klimboo.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

object FirebaseQueries {

    private val db by lazy { FirebaseFirestore.getInstance() }

    // ── Modelos ────────────────────────────────────────────────────────────────

    data class Locker(
        val id: String = "",
        val name: String = "",
        val local: String = "",
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
                    local = doc.getString("local") ?: "",
                    photoUrl = doc.getString("photoUrl")
                )
            }
        } catch (e: Exception) {
            logError("fetchLockers", e)
            emptyList()
        }
    }

    suspend fun insertLocker(nome: String, photoUrl: String? = null, local: String) {
        try {
            val data = mutableMapOf<String, Any>("nome" to nome, "local" to local)
            if (photoUrl != null) data["photoUrl"] = photoUrl
            db.collection("armarios").add(data).await()
        } catch (e: Exception) {
            logError("insertLocker", e)
        }
    }

    suspend fun updateLocker(id: String, newName: String, newLocal: String) {
        try {
            db.collection("armarios").document(id)
                .update(mapOf("nome" to newName, "local" to newLocal)).await()
        } catch (e: Exception) {
            logError("updateLocker", e)
        }
    }

    suspend fun updateLockerPhoto(id: String, photoUrl: String?) {
        try {
            db.collection("armarios").document(id)
                .update("photoUrl", photoUrl).await()
        } catch (e: Exception) {
            logError("updateLockerPhoto", e)
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
            logError("deleteLocker", e)
        }
    }

    // ── Ferramentas ───────────────────────────────────────────────────────────

    suspend fun fetchTools(): List<Tool> {
        return try {
            db.collection("ferramentas").get().await().documents.map { doc ->
                Tool(
                    id = doc.id,
                    name = doc.getString("nome") ?: "",
                    local = doc.getString("local") ?: "",
                    photoUrl = doc.getString("photoUrl")
                )
            }
        } catch (e: Exception) {
            logError("fetchTools", e)
            emptyList()
        }
    }

    suspend fun insertTool(nome: String, lockerId: String, photoUrl: String? = null) {
        try {
            val data = mutableMapOf<String, Any>("nome" to nome, "local" to lockerId)
            if (photoUrl != null) data["photoUrl"] = photoUrl
            db.collection("ferramentas").add(data).await()
        } catch (e: Exception) {
            logError("insertTool", e)
        }
    }

    suspend fun updateTool(id: String, novoNome: String, newLockerId: String) {
        try {
            db.collection("ferramentas").document(id)
                .update(mapOf("nome" to novoNome, "local" to newLockerId)).await()
        } catch (e: Exception) {
            logError("updateTool", e)
        }
    }

    suspend fun updateToolPhoto(id: String, photoUrl: String?) {
        try {
            db.collection("ferramentas").document(id)
                .update("photoUrl", photoUrl).await()
        } catch (e: Exception) {
            logError("updateToolPhoto", e)
        }
    }

    suspend fun deleteTool(id: String) {
        try {
            db.collection("ferramentas").document(id).delete().await()
        } catch (e: Exception) {
            logError("deleteTool", e)
        }
    }


    fun listenToLockers(onChange: (List<Locker>) -> Unit): ListenerRegistration {
        return db.collection("armarios")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logError("listenToLockers", error)
                    return@addSnapshotListener
                }
                val lockers = snapshot?.documents?.map { doc ->
                    Locker(
                        id = doc.id,
                        name = doc.getString("nome") ?: "",
                        local = doc.getString("local") ?: "",
                        photoUrl = doc.getString("photoUrl")
                    )
                } ?: emptyList()
                onChange(lockers)
            }
    }

    fun listenToTools(onChange: (List<Tool>) -> Unit): ListenerRegistration {
        return db.collection("ferramentas")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logError("listenToTools", error)
                    return@addSnapshotListener
                }
                val tools = snapshot?.documents?.map { doc ->
                    Tool(
                        id = doc.id,
                        name = doc.getString("nome") ?: "",
                        local = doc.getString("local") ?: "",
                        photoUrl = doc.getString("photoUrl")
                    )
                } ?: emptyList()
                onChange(tools)
            }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun logError(functionName: String, exception: Exception) {
        Log.e("FIREBASE", "$functionName error: ${exception.message}", exception)
    }

    private fun logError(functionName: String, exception: com.google.firebase.FirebaseException) {
        Log.e("FIREBASE", "$functionName error: ${exception.message}", exception)
    }
}