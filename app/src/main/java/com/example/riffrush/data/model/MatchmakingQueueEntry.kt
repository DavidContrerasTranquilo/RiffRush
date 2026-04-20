package com.example.riffrush.data.model

data class MatchmakingQueueEntry(
    val queueId: String = "",
    val playerId: String = "",
    val playerName: String = "",
    val playlist: String = "",
    val status: String = "WAITING",
    val matchId: String? = null,
    val isGuest: Boolean = true
)