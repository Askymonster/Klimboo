package com.example.klimboo.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

object FirebaseQueries {

    private val db by lazy { FirebaseFirestore.getInstance() }

    // ── Modelos ────────────────────────────────────────────────────────────────

    data class Locker(
        val id: String = "",
        val name: String = "",
        val local: String = "",
        val photoUrl: String? = null,
        val itemCount: Long = 0
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
                    photoUrl = doc.getString("photoUrl"),
                    itemCount = doc.getLong("itemCount") ?: 0
                )
            }
        } catch (e: Exception) {
            logError("fetchLockers", e)
            emptyList()
        }
    }


    // ── Lógica de inserção/edição/exclusào de armário ──────────────────────────────────────────────────────────────
    suspend fun insertLocker(nome: String, photoUrl: String? = null, local: String) {
        try {
            val data = mutableMapOf<String, Any>(
                "nome" to nome,
                "local" to local,
                "itemCount" to 0
            )
            if (photoUrl != null) data["photoUrl"] = photoUrl
            db.collection("armarios").add(data).await()
        } catch (e: Exception) {
            logError("insertLocker", e)
            throw e
        }
    }

    suspend fun updateLocker(id: String, newName: String, newLocal: String) {
        try {
            db.collection("armarios").document(id)
                .update(mapOf("nome" to newName, "local" to newLocal)).await()
        } catch (e: Exception) {
            logError("updateLocker", e)
            throw e
        }
    }

    suspend fun updateLockerPhoto(id: String, photoUrl: String?) {
        try {
            db.collection("armarios").document(id)
                .update("photoUrl", photoUrl).await()
        } catch (e: Exception) {
            logError("updateLockerPhoto", e)
            throw e
        }
    }

    suspend fun hasToolsInLocker(id: String): Boolean {
        return try {
            !db.collection("ferramentas")
                .whereEqualTo("local", id)
                .limit(1)
                .get()
                .await()
                .isEmpty
        } catch (e: Exception) {
            logError("hasToolsInLocker", e)
            true
        }
    }

    suspend fun deleteLocker(id: String, lockerDestinyId: String?) {
        try {
            val tools = db.collection("ferramentas")
                .whereEqualTo("local", id).get().await()

            if (lockerDestinyId != null && !tools.isEmpty) {
                val batch = db.batch()
                for (doc in tools.documents) {
                    batch.update(doc.reference, "local", lockerDestinyId)
                }
                batch.update(
                    db.collection("armarios").document(id),
                    "itemCount",
                    0
                )
                batch.update(
                    db.collection("armarios").document(lockerDestinyId),
                    "itemCount",
                    FieldValue.increment(tools.size().toLong())
                )
                batch.commit().await()
            }

            db.collection("armarios").document(id).delete().await()
        } catch (e: Exception) {
            logError("deleteLocker", e)
            throw e
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

    // ── Lógica de inserção/edição/exclusão de ferramentas ──────────────────────────────────────────────────────────────
    suspend fun insertTool(nome: String, lockerId: String, photoUrl: String? = null) {
        try {
            val data = mutableMapOf<String, Any>("nome" to nome, "local" to lockerId)
            if (photoUrl != null) data["photoUrl"] = photoUrl
            val batch = db.batch()
            batch.set(db.collection("ferramentas").document(), data)
            batch.update(
                db.collection("armarios").document(lockerId),
                "itemCount",
                FieldValue.increment(1)
            )
            batch.commit().await()
        } catch (e: Exception) {
            logError("insertTool", e)
            throw e
        }
    }

    suspend fun updateTool(id: String, novoNome: String, oldLockerId: String, newLockerId: String) {
        try {
            val batch = db.batch()
            batch.update(
                db.collection("ferramentas").document(id),
                mapOf("nome" to novoNome, "local" to newLockerId)
            )
            if (oldLockerId != newLockerId) {
                batch.update(
                    db.collection("armarios").document(oldLockerId),
                    "itemCount",
                    FieldValue.increment(-1)
                )
                batch.update(
                    db.collection("armarios").document(newLockerId),
                    "itemCount",
                    FieldValue.increment(1)
                )
            }
            batch.commit().await()
        } catch (e: Exception) {
            logError("updateTool", e)
            throw e
        }
    }

    suspend fun updateToolPhoto(id: String, photoUrl: String?) {
        try {
            db.collection("ferramentas").document(id)
                .update("photoUrl", photoUrl).await()
        } catch (e: Exception) {
            logError("updateToolPhoto", e)
            throw e
        }
    }

    suspend fun deleteTool(id: String, lockerId: String) {
        try {
            val batch = db.batch()
            batch.delete(db.collection("ferramentas").document(id))
            batch.update(
                db.collection("armarios").document(lockerId),
                "itemCount",
                FieldValue.increment(-1)
            )
            batch.commit().await()
        } catch (e: Exception) {
            logError("deleteTool", e)
            throw e
        }
    }


    // ── Funções de listening para atualizar armários e ferramentas ──────────────────────────────────────────────────────────────
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
                        photoUrl = doc.getString("photoUrl"),
                        itemCount = doc.getLong("itemCount") ?: 0
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
