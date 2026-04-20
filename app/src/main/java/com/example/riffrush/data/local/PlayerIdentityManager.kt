package com.example.riffrush.data.local

import com.google.firebase.auth.FirebaseAuth

class PlayerIdentityManager(
    private val auth: FirebaseAuth,
    private val guestIdManager: GuestIdManager
) {
    fun getPlayerId(): String {
        return auth.currentUser?.uid ?: guestIdManager.getGuestId()
    }

    fun getPlayerName(): String {
        return auth.currentUser?.displayName ?: "Invitado"
    }

    fun isGuest(): Boolean {
        return auth.currentUser == null
    }
}