package com.example.riffrush.data.model

data class MatchModel(
    val matchId: String = "",
    val player1Id: String = "",
    val player2Id: String = "",
    val status: MatchStatus = MatchStatus.WAITING,
    val staticSongList: List<Long> = emptyList(),
    val scoreP1: Int = 0,
    val scoreP2: Int = 0
)

enum class MatchStatus {
    WAITING, PLAYING, FINISHED
}