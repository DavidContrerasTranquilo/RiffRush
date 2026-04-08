package com.example.riffrush.viewmodel

import android.service.autofill.FieldClassification
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
import kotlinx.coroutines.launch

class MatchViewModel(
    private val repository: MatchRepository,
    private val deezerApi: DeezerApi
) : ViewModel() {

    private val _currentMatch = MutableStateFlow<MatchModel?>(null)
    val currentMatch: MutableStateFlow<MatchModel?> = _currentMatch

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun listenToMatch(matchId: String) {
        viewModelScope.launch {
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
                        title = track.title ?: "Unknown Title",
                        artist = track.artist?.name ?: "Unknown Artist",
                        album = track.album?.title ?: "Unknown Album",
                        preview = track.preview ?: "",
                        cover = track.album?.cover_medium ?: ""
                    )
                }
                _songs.value = loadedSongs
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("MatchViewModel", "Error loading songs from Deezer", e)
                _error.value = "Error al cargar canciones: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun onAnswerSelected(isCorrect: Boolean) {
        // Lógica para manejar la respuesta
    }
}
