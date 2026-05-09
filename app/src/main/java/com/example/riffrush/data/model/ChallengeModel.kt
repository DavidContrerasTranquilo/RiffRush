package com.example.riffrush.data.model

data class ChallengeModel(
    val challengeId: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromPfpUrl: String = "",
    val toUid: String = "",
    val toName: String = "",
    val playlist: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED, EXPIRED
    val matchId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
