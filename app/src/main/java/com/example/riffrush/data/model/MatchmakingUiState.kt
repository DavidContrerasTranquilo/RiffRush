package com.example.riffrush.data.model

sealed class MatchmakingUiState {
    object Idle : MatchmakingUiState()
    object Searching : MatchmakingUiState()
    data class Waiting(val playlist: String) : MatchmakingUiState()
    data class Matched(val matchId: String) : MatchmakingUiState()
    data class Error(val message: String) : MatchmakingUiState()
}