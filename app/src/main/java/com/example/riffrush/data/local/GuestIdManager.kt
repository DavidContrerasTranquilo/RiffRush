package com.example.riffrush.data.local

import android.content.Context
import java.util.UUID

class GuestIdManager(private val context: Context) {

    private val prefsName = "RiffRushPrefs"
    private val keyGuestId = "guestId"

    fun getGuestId(): String {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.getString(keyGuestId, null) ?: createGuestId(prefs)
    }

    private fun createGuestId(prefs: android.content.SharedPreferences): String {
        val newId = "guest_${UUID.randomUUID()}"
        prefs.edit().putString(keyGuestId, newId).apply()
        return newId
    }
}