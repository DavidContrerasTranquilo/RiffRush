package com.example.riffrush.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extensión para declarar una instancia única de DataStore Preferences con el nombre "settings"
private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/*
 Almacenamiento local reactivo para configuraciones de usuario.
 Utiliza Jetpack DataStore para guardar y leer de manera segura y no bloqueante (basado en Corrutinas de Kotlin)
 preferencias simples como la elección del Modo Oscuro.
 */
class SettingsDataStore(
    private val context: Context
) {
    // Clave para guardar la preferencia de mdo Oscuro
    private val darkModeKey = booleanPreferencesKey("dark_mode")

    // Flujo (Flow) reactivo que emite el estado del modo oscuro. Emite 'false' por defecto si no se ha guardado nada.
    val darkModeFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences ->
            preferences[darkModeKey] ?: false
        }

    // Guarda de forma asíncrona si el Modo Oscuro está habilitado o no
    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[darkModeKey] = enabled
        }
    }
}