package com.example.riffrush.data.local

import android.content.Context
import java.util.UUID
import androidx.core.content.edit

/*
 Gestor encargado de generar y persistir un ID único para usuarios invitados (guests).
 Utiliza SharedPreferences para almacenar el ID localmente en el dispositivo de forma que
 se mantenga el mismo identificador entre sesiones de la app.
 */
class GuestIdManager(private val context: Context) {

    private val prefsName = "RiffRushPrefs"
    private val keyGuestId = "guestId"

    // Obtiene el ID del invitado actual( o genera uno nuevo si no existe)
    fun getGuestId(): String {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.getString(keyGuestId, null) ?: createGuestId(prefs)
    }

    // Genera un ID aleatorio único con prefijo "guest_" y lo guarda en las SharedPreferences
    private fun createGuestId(prefs: android.content.SharedPreferences): String {
        val newId = "guest_${UUID.randomUUID()}"
        prefs.edit {
            putString(keyGuestId, newId)
        }
        return newId
    }
}