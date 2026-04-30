package com.example.riffrush.data.model

data class RoundResultModel(
    val round: Int = 0,
    val songTitle: String = "",
    val songArtist: String = "",
    val player1Correct: Boolean = false,
    val player2Correct: Boolean = false
)