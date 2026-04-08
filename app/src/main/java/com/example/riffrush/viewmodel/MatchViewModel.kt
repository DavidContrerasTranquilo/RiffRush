package com.example.riffrush.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.model.MatchModel
import com.example.riffrush.data.model.MatchStatus
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.repository.DeezerApi
import com.example.riffrush.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MatchViewModel(
    private val repository: MatchRepository,
    private val deezerApi: DeezerApi
) : ViewModel() {

    private val _currentMatch = MutableStateFlow<MatchModel?>(null)
    val currentMatch: StateFlow<MatchModel?> = _currentMatch.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _currentRound = MutableStateFlow(0)
    val currentRound: StateFlow<Int> = _currentRound.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun startMatchmaking(opponentId: String) {
        val currentUserId = repository.getCurrentUserId()
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val matchId = UUID.randomUUID().toString()

                // Lista estática de IDs para la prueba (puedes cambiarla por lógica aleatoria)
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
                _error.value = "Error al crear partida"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun listenToMatch(matchId: String) {
        viewModelScope.launch {
            try {
                repository.listenToMatch(matchId).collect { match ->
                    _currentMatch.value = match
                    if (match != null) {
                        if (_songs.value.isEmpty() && match.staticSongList.isNotEmpty()) {
                            loadSongs(match.staticSongList)
                        }
                        if (match.status == MatchStatus.WAITING &&
                            match.player1Id.isNotEmpty() &&
                            match.player2Id.isNotEmpty()) {
                            repository.updateMatchStatus(matchId, MatchStatus.PLAYING)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MatchViewModel", "Error listening to match", e)
            }
        }
    }

    private fun loadSongs(ids: List<Long>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val loadedSongs = ids.map { id ->
                    val track = deezerApi.getTrack(id)
                    Song(
                        id = track.id,
                        title = track.title ?: "Sin título",
                        artist = track.artist?.name ?: "Desconocido",
                        album = track.album?.title ?: "",
                        preview = track.preview ?: "",
                        cover = track.album?.cover_medium ?: ""
                    )
                }
                _songs.value = loadedSongs
            } catch (e: Exception) {
                _error.value = "Error al cargar canciones"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onAnswerSelected(isCorrect: Boolean) {
        val match = _currentMatch.value ?: return
        val round = _currentRound.value

        viewModelScope.launch {
            if (isCorrect) {
                val currentUserId = repository.getCurrentUserId()
                repository.updateScore(match.matchId, currentUserId)
            }

            if (round < 4) {
                _currentRound.value = round + 1
            } else {
                repository.updateMatchStatus(match.matchId, MatchStatus.FINISHED)
            }
        }
    }
}