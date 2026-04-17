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
        matchJob = viewModelScope.launch {
            try {
                repository.listenToMatch(matchId).collect { match ->
                    _currentMatch.value = match
                    if (match != null && _songs.value.isEmpty()) {
                        loadSongs(match.staticSongList)
                    }
                    if (match?.status == MatchStatus.WAITING && match.player1Id.isNotEmpty() && match.player2Id.isNotEmpty()) {
                        repository.updateMatchStatus(matchId, MatchStatus.PLAYING)
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
        val round = _currentRound.value
        val isOffline = match.matchId.startsWith("offline")

        viewModelScope.launch {
            if (isCorrect) {
                if (isOffline) {
                    _currentMatch.value = _currentMatch.value?.copy(
                        scoreP1 = match.scoreP1 + 1
                    )
                }
            }

            if (isOffline) {
                val botHits = (0..1).random() == 1
                if (botHits) {
                    val currentScoreP2 = _currentMatch.value?.scoreP2 ?: 0
                    _currentMatch.value = _currentMatch.value?.copy(
                        scoreP2 = currentScoreP2 + 1
                    )
                }
            }

            val totalSongs = _songs.value.size
            if (round < totalSongs - 1) {
                _currentRound.value = round + 1
            } else {
                if (isOffline) {
                    _currentMatch.value = _currentMatch.value?.copy(
                        status = MatchStatus.FINISHED
                    )
                } else {
                    repository.updateMatchStatus(match.matchId, MatchStatus.FINISHED)
                }
            }
        }
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
    }

    fun startOfflineMode(category: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val offlineMatch = repository.getOfflineMatch(category)

            _currentMatch.value = offlineMatch
            loadSongs(offlineMatch.staticSongList)
            _currentRound.value = 0
        }
    }
}