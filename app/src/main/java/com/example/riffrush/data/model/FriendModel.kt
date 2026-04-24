package com.example.riffrush.data.model

data class FriendModel(
    val uid: String = "",
    val name: String = "",
    val pfpUrl: String = "",
    val status: String = "",
    val email: String = "",
    val role: String = "user",
    val isBlocked: Boolean = false,
    val gamesPlayed: Int = 0,
    val songsPlayed: Int = 0,
    val maxDailyStreak: Int = 0
)