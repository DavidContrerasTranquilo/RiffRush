package com.example.riffrush.data.model

data class DailyChallengeConfig(
    val date: String = "",
    val songIds: List<String> = emptyList(), // 🔥 CAMBIO
    val isActive: Boolean = true
)