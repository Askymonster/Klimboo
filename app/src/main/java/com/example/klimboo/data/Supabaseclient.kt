package com.example.klimboo.data

import com.example.klimboo.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY  // anon key legacy (eyJ...)
    ) {
        install(Postgrest)
    }
}
