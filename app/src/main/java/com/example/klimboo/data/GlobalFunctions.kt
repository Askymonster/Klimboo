package com.example.klimboo.data

import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
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


//Cria o display generico para uso global
fun AppCompatActivity.showGenericDisplay(
    titleFun: String,
    msg: String,
    hint: String,
    isPassword: Boolean = false,
    forceLight: Boolean = false,
    action: (String) -> Unit
) {
    val input = EditText(this).apply {
        this.hint = hint
        inputType = if (isPassword) 129 else 32
    }

    val style = if (forceLight)
        com.google.android.material.R.style.ThemeOverlay_Material3_Light
    else
        com.google.android.material.R.style.MaterialAlertDialog_Material3

    AlertDialog.Builder(this, style)
        .setTitle(titleFun)
        .setMessage(msg)
        .setView(input)
        .setPositiveButton("Confirmar") { _, _ ->
            val text = input.text.toString()
            if (text.isNotEmpty()) action(text)
        }
        .setNegativeButton("Cancelar", null)
        .show()
}



