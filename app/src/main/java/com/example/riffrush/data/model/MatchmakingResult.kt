package com.example.riffrush.data.model

sealed class MatchmakingResult {
    data class Waiting(val queueId: String) : MatchmakingResult()
    data class Matched(val matchId: String) : MatchmakingResult()
}