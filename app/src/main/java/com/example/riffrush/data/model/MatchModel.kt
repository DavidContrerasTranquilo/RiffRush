package com.example.riffrush.data.model

data class MatchModel(
    val matchId: String = "",
    val player1Id: String = "",
    val player2Id: String = "",
    val player1Name: String = "",
    val player2Name: String = "",
    val playlist: String = "",
    val status: MatchStatus = MatchStatus.WAITING,
    val staticSongList: List<Long> = emptyList(),
    val currentRound: Int = 0,
    val scoreP1: Int = 0,
    val scoreP2: Int = 0,
    val answeredP1: Boolean = false,
    val answeredP2: Boolean = false,
    val player1PfpUrl: String = "",
    val player2PfpUrl: String = "",
)

enum class MatchStatus {
    WAITING, PLAYING, FINISHED
}