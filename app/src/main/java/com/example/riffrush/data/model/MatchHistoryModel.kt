package com.example.riffrush.data.model

data class MatchHistoryModel(
    val historyId: String = "",
    val matchId: String = "",
    val userId: String = "",
    val opponentId: String = "",
    val opponentName: String = "",
    val mode: String = "", // ONLINE, OFFLINE, FAVORITES
    val playlist: String = "",
    val myScore: Int = 0,
    val opponentScore: Int = 0,
    val result: String = "", // WIN, LOSE, DRAW
    val timestamp: Long = System.currentTimeMillis()
)