package com.example.riffrush.viewmodels

import com.example.riffrush.data.player.MusicPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.model.MatchModel
import com.example.riffrush.data.model.MatchStatus
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.repository.DeezerApi
import com.example.riffrush.data.repository.MatchRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.riffrush.data.model.MatchmakingUiState
import com.example.riffrush.data.model.MatchmakingResult

class MatchViewModel(
    private val repository: MatchRepository,
    private val deezerApi: DeezerApi,
    private val auth: FirebaseAuth,
    private val musicPlayer: MusicPlayer
) : ViewModel() {

    private val _currentMatch = MutableStateFlow<MatchModel?>(null)
    val currentMatch: StateFlow<MatchModel?> = _currentMatch

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _currentRound = MutableStateFlow(0)
    val currentRound: StateFlow<Int> = _currentRound

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var matchJob: Job? = null
    private val _matchmakingState = MutableStateFlow<MatchmakingUiState>(MatchmakingUiState.Idle)
    val matchmakingState: StateFlow<MatchmakingUiState> = _matchmakingState

    private var queueJob: Job? = null
    private var currentQueueId: String? = null

    fun startMatchmaking(opponentId: String) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val currentUserId = repository.getCurrentUserId()
                val matchId = java.util.UUID.randomUUID().toString()
                val songIds = listOf(1109152L, 2109522L, 3109522L, 4109522L, 5109522L)
                val newMatch = MatchModel(
                    matchId = matchId,
                    player1Id = currentUserId,
                    player2Id = opponentId,
                    status = MatchStatus.WAITING,
                    staticSongList = songIds
                )
                repository.createMatch(newMatch)
                listenToMatch(matchId)

            } catch (e: Exception) {
                Log.e("MatchViewModel", "Error creating match", e)
                _error.value = "Error al crear la partida: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun listenToMatch(matchId: String) {
        matchJob?.cancel()
        _currentMatch.value = null
        _songs.value = emptyList()
        _currentRound.value = 0
        _isLoading.value = true
        matchJob = viewModelScope.launch {
            try {
                repository.listenToMatch(matchId).collect { match ->
                    _currentMatch.value = match

                    match?.let { currentMatch ->
                        val isOffline = currentMatch.matchId.startsWith("offline")

                        // Cargar canciones solo una vez por partida
                        if (_songs.value.isEmpty() && currentMatch.staticSongList.isNotEmpty()) {
                            loadSongs(currentMatch.staticSongList)
                        }

                        // En online, la ronda real viene del documento compartido
                        if (!isOffline) {
                            _currentRound.value = currentMatch.currentRound
                        }

                        // Si por alguna razón la partida online sigue en WAITING y ya tiene dos jugadores, arrancarla
                        if (!isOffline &&
                            currentMatch.status == MatchStatus.WAITING &&
                            currentMatch.player1Id.isNotEmpty() &&
                            currentMatch.player2Id.isNotEmpty()
                        ) {
                            repository.updateMatchStatus(matchId, MatchStatus.PLAYING)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MatchViewModel", "Error listening to match", e)
                _error.value = "Error de conexión: ${e.message}"
            }
        }
    }
    private fun loadSongs(ids: List<Long>) {
        viewModelScope.launch {
            try {
                val loadedSongs = ids.map { id ->
                    val track = deezerApi.getTrack(id)
                    Song(
                        id = track.id,
                        title = track.title ?: "Sin título",
                        artist = track.artist?.name ?: "Artista desconocido",
                        album = track.album?.title ?: "Álbum desconocido",
                        preview = track.preview ?: "",
                        cover = track.album?.cover_medium ?: ""
                    )
                }
                _songs.value = loadedSongs
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("MatchViewModel", "Error loading songs", e)
                _isLoading.value = false
            }
        }
    }

    fun onAnswerSelected(isCorrect: Boolean) {
        musicPlayer.stop()
        val match = _currentMatch.value ?: return
        val isOffline = match.matchId.startsWith("offline")

        viewModelScope.launch {
            try {
                if (isOffline) {
                    handleOfflineAnswer(match, isCorrect)
                } else {
                    repository.submitAnswer(match.matchId, isCorrect)
                }
            } catch (e: Exception) {
                Log.e("MatchViewModel", "Error submitting answer", e)
                _error.value = "Error al enviar la respuesta: ${e.message}"
            }
        }
    }

    private fun handleOfflineAnswer(match: MatchModel, isCorrect: Boolean) {
        var updatedMatch = match

        if (isCorrect) {
            updatedMatch = updatedMatch.copy(
                scoreP1 = updatedMatch.scoreP1 + 1
            )
        }

        val botHits = (0..1).random() == 1
        if (botHits) {
            updatedMatch = updatedMatch.copy(
                scoreP2 = updatedMatch.scoreP2 + 1
            )
        }

        val totalSongs = _songs.value.size
        val currentRoundValue = _currentRound.value

        if (currentRoundValue < totalSongs - 1) {
            _currentRound.value = currentRoundValue + 1
            updatedMatch = updatedMatch.copy(
                currentRound = currentRoundValue + 1
            )
        } else {
            updatedMatch = updatedMatch.copy(
                status = MatchStatus.FINISHED
            )
        }

        _currentMatch.value = updatedMatch
    }

    private fun incrementScore() {
        val match = _currentMatch.value ?: return
        val isOffline = match.matchId == "offline_test_id"

        viewModelScope.launch {
            try {
                if (isOffline) {
                    _currentMatch.value = match.copy(
                        scoreP1 = match.scoreP1 + 1
                    )
                }
            } catch (e: Exception) {
                Log.e("MatchViewModel", "Error updating score", e)
            }
        }
    }

    fun finishMatch() {
        val match = _currentMatch.value ?: return
        viewModelScope.launch {
            try {
                repository.updateMatchStatus(match.matchId, MatchStatus.FINISHED)
            } catch (e: Exception) {
                Log.e("MatchViewModel", "Error finishing match", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        matchJob?.cancel()
        queueJob?.cancel()
    }

    fun startOfflineMode(category: String) {
        _isLoading.value = true
        _error.value = null
        _songs.value = emptyList()
        _currentRound.value = 0
        _currentMatch.value = null

        viewModelScope.launch {
            try {
                val offlineMatch = repository.getOfflineMatch(category).copy(
                    currentRound = 0
                )

                _currentMatch.value = offlineMatch
                loadSongs(offlineMatch.staticSongList)

            } catch (e: Exception) {
                Log.e("MatchViewModel", "Error starting offline mode", e)
                _error.value = "Error al iniciar la partida offline: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun listenToQueue(queueId: String) {
        queueJob?.cancel()
        queueJob = viewModelScope.launch {
            repository.listenToQueueEntry(queueId).collect { entry ->
                val matchId = entry?.matchId
                if (entry?.status == "MATCHED" && !matchId.isNullOrBlank()) {
                    _matchmakingState.value = MatchmakingUiState.Matched(matchId)
                    listenToMatch(matchId)
                }
            }
        }
    }

    fun joinPlaylistMatchmaking(playlist: String) {
        _matchmakingState.value = MatchmakingUiState.Searching

        viewModelScope.launch {
            try {
                when (val result = repository.joinMatchmaking(playlist)) {
                    is MatchmakingResult.Matched -> {
                        _matchmakingState.value = MatchmakingUiState.Matched(result.matchId)
                        listenToMatch(result.matchId)
                    }
                    is MatchmakingResult.Waiting -> {
                        currentQueueId = result.queueId
                        _matchmakingState.value = MatchmakingUiState.Waiting(playlist)
                        listenToQueue(result.queueId)
                    }
                }
            } catch (e: Exception) {
                _matchmakingState.value = MatchmakingUiState.Error("Error en matchmaking: ${e.message}")
            }
        }
    }

    fun cancelMatchmaking() {
        val queueId = currentQueueId ?: return
        viewModelScope.launch {
            repository.cancelMatchmaking(queueId)
            currentQueueId = null
            _matchmakingState.value = MatchmakingUiState.Idle
        }
    }

    fun getCurrentPlayerId(): String {
        return repository.getCurrentUserId()
    }
}