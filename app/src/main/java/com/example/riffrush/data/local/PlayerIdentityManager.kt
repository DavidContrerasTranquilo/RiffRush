package com.example.riffrush.data.local

import android.content.Context
import com.example.riffrush.R
import com.google.firebase.auth.FirebaseAuth

class PlayerIdentityManager(
    private val auth: FirebaseAuth,
    private val guestIdManager: GuestIdManager,
    private val context: Context
) {
    fun getPlayerId(): String {
        return auth.currentUser?.uid ?: guestIdManager.getGuestId()
    }

    fun getPlayerName(): String {
        return auth.currentUser?.displayName ?: context.getString(R.string.default_guest_name)
    }

    fun isGuest(): Boolean {
        return auth.currentUser == null
    }
}