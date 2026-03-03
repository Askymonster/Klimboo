package com.example.klimboo

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Extensão para criar o DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

class ThemeManager(private val context: Context) {
    private val darkmodeKey = booleanPreferencesKey("dark_mode")

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[darkmodeKey] ?: false
    }

    suspend fun setDarkMode(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[darkmodeKey] = isEnabled
        }
    }
}

fun AppCompatActivity.observeTheme(themeManager: ThemeManager) {
    lifecycleScope.launch {
        themeManager.isDarkMode.collect { isDark ->
            val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}