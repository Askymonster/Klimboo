package com.example.klimboo.data

import android.util.Log
import com.example.klimboo.data.SupabaseClient.client
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable


object SupabaseQueries {

    suspend fun testeFetch() {
        val result = client
            .from("ferramentas")
            .select()
            .decodeList<Ferramenta>()

        Log.d("SUPABASE_TEST", result.toString())
    }

    @Serializable
    data class Ferramenta(
        val id: Int,
        val nome: String,
        val local: Int
    )
}


