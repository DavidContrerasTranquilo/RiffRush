package com.example.riffrush.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.local.GuestIdManager
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.data.repository.AuthRepository
import com.example.riffrush.data.repository.DeezerRepository
import com.example.riffrush.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MusicViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val deezerRepository: DeezerRepository,
    private val guestIdManager: GuestIdManager,
    private val musicPlayer: MusicPlayer
) : ViewModel() {
    // Estado para la lista de canciones que se muestra
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    // Estado para los favoritos
    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs

    // Estado para saber si estamos en modo búsqueda o viendo "Mi música"
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    init {
        loadMyMusic()
    }

    private fun getOwnerId(): String {
        return authRepository.getCurrentUser()?.uid ?: guestIdManager.getGuestId()
    }

    fun loadMyMusic() {
        _isSearching.value = false
        val ownerId = getOwnerId()
        viewModelScope.launch {
            firebaseRepository.getFavorites(ownerId).collect { favs ->
                _favoriteSongs.value = favs
                if (!_isSearching.value) {
                    _songs.value = favs
                }
            }
        }
    }

    /**
     * Busca canciones nuevas por texto
     */
    fun searchSongs(query: String) {
        if (query.isBlank()) return
        _isSearching.value = true
        viewModelScope.launch {
            try {
                val results = deezerRepository.searchSongs(query)
                _songs.value = results
            } catch (e: Exception) {
                _songs.value = emptyList()
            }
        }
    }

    /**
     * Agrega o quita de favoritos
     */
    fun toggleFavorite(song: Song) {
        val ownerId = getOwnerId()
        viewModelScope.launch {
            val isFavorite = _favoriteSongs.value.any { it.id == song.id }
            if (isFavorite) {
                firebaseRepository.removeFavorite(ownerId, song.id.toString())
            } else {
                firebaseRepository.addFavorite(ownerId, song)
            }
        }
    }

    /**
     * Lógica de reproducción
     */
    fun playPause(song: Song) {
        if (musicPlayer.isPlaying(song.preview)) {
            musicPlayer.pause()
        } else {
            musicPlayer.playPreview(song.preview)
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayer.release()
    }

    fun clearLocalData() {
        _favoriteSongs.value = emptyList()
        _songs.value = emptyList()
    }
}