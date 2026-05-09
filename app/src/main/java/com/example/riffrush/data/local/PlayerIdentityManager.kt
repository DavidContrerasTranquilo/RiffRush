package com.example.riffrush.data.local

import android.content.Context
import com.example.riffrush.R
import com.google.firebase.auth.FirebaseAuth

/*
 Gestor de identidad del jugador.
 Centraliza la lógica para determinar si el usuario actual está autenticado mediante Firebase Auth
 o si es un usuario invitado (utilizando GuestIdManager), unificando el acceso a su ID y nombre.
 */
class PlayerIdentityManager(
    private val auth: FirebaseAuth,
    private val guestIdManager: GuestIdManager,
    private val context: Context
) {
    // Retorna el UID del usuario registrado en Firebase, o en su defecto, el ID local persistido de invitado
    fun getPlayerId(): String {
        return auth.currentUser?.uid ?: guestIdManager.getGuestId()
    }

    // Retorna el nombre visible del usuario registrado, o el string por defecto para "Invitado"
    fun getPlayerName(): String {
        return auth.currentUser?.displayName ?: context.getString(R.string.default_guest_name)
    }

    // Indica si el usuario actual está navegando como invitado (no tiene sesión de Firebase iniciada)
    fun isGuest(): Boolean {
        return auth.currentUser == null
    }
}