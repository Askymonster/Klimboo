package com.example.klimboo.data

import android.util.Log
import com.example.klimboo.data.SupabaseClient.client
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

object SupabaseQueries {

    @Serializable
    data class Ferramenta(
        val id: Int,
        val nome: String,
        val local: Int
    )

    @Serializable
    data class Armario(
        val id: Int,
        val nome: String
    )

    suspend fun fetchArmarios(): List<Armario> {
        return try {
            client.from("armarios").select().decodeList<Armario>()
        } catch (e: Exception) {
            Log.e("SUPABASE", "fetchArmarios error: ${e.message}")
            emptyList()
        }
    }

    suspend fun insertArmario(nome: String) {
        try {
            client.from("armarios").insert(mapOf("nome" to nome))
        } catch (e: Exception) {
            Log.e("SUPABASE", "insertArmario error: ${e.message}")
        }
    }

    suspend fun updateArmario(id: Int, novoNome: String) {
        try {
            client.from("armarios")
                .update(mapOf("nome" to novoNome)) {
                    filter { eq("id", id) }
                }
        } catch (e: Exception) {
            Log.e("SUPABASE", "updateArmario error: ${e.message}")
        }
    }

    suspend fun deleteArmario(id: Int, armarioDestinoId: Int?) {
        try {
            if (armarioDestinoId != null) {
                client.from("ferramentas")
                    .update(mapOf("local" to armarioDestinoId)) {
                        filter { eq("local", id) }
                    }
            } else {
                client.from("ferramentas")
                    .delete { filter { eq("local", id) } }
            }
            client.from("armarios")
                .delete { filter { eq("id", id) } }
        } catch (e: Exception) {
            Log.e("SUPABASE", "deleteArmario error: ${e.message}")
        }
    }

    suspend fun fetchFerramentas(): List<Ferramenta> {
        return try {
            client.from("ferramentas").select().decodeList<Ferramenta>()
        } catch (e: Exception) {
            Log.e("SUPABASE", "fetchFerramentas error: ${e.message}")
            emptyList()
        }
    }

    suspend fun insertFerramenta(nome: String, armarioId: Int) {
        try {
            client.from("ferramentas")
                .insert(mapOf("nome" to nome, "local" to armarioId))
        } catch (e: Exception) {
            Log.e("SUPABASE", "insertFerramenta error: ${e.message}")
        }
    }

    suspend fun updateFerramenta(id: Int, novoNome: String, novoArmarioId: Int) {
        try {
            client.from("ferramentas")
                .update(mapOf("nome" to novoNome, "local" to novoArmarioId)) {
                    filter { eq("id", id) }
                }
        } catch (e: Exception) {
            Log.e("SUPABASE", "updateFerramenta error: ${e.message}")
        }
    }

    suspend fun deleteFerramenta(id: Int) {
        try {
            client.from("ferramentas")
                .delete { filter { eq("id", id) } }
        } catch (e: Exception) {
            Log.e("SUPABASE", "deleteFerramenta error: ${e.message}")
        }
    }
}