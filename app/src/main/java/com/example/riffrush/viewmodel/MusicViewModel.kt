package com.example.riffrush.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.data.repository.DeezerRepository
import com.example.riffrush.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.riffrush.data.local.GuestIdManager

class MusicViewModel(
    private val deezerRepository: DeezerRepository,
    private val firebaseRepository: FirebaseRepository,
    private val musicPlayer: MusicPlayer,
    guestIdManager: GuestIdManager
) : ViewModel() {
    private val guestId = guestIdManager.getGuestId()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _favoriteSongs = MutableStateFlow<Set<String>>(emptySet())
    val favoriteSongs: StateFlow<Set<String>> = _favoriteSongs

    // Carga canciones de usuario/invitado
    fun loadMyMusic(userId: String? = null) {
        val id = userId ?: guestId
        viewModelScope.launch {
            _isSearching.value = false
            // Traemos las canciones desde Firebase
            val loaded = firebaseRepository.getUserSongs(id)
            _songs.value = loaded
            // Actualizamos favoritos según Firebase
            _favoriteSongs.value = loaded.map { it.previewUrl }.toSet()
        }
    }

    // Buscar canciones en Deezer
    fun searchSongs(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _songs.value = deezerRepository.searchSongs(query)
        }
    }

    // Alterna favorito: guarda si no existe, elimina si ya está
    fun toggleFavorite(song: Song, userId: String? = null) {
        val id = userId ?: guestId
        viewModelScope.launch {
            val currentFavorites = _favoriteSongs.value.toMutableSet()
            if (currentFavorites.contains(song.previewUrl)) {
                // eliminar de Firebase
                firebaseRepository.deleteSong(id, song.previewUrl)
                currentFavorites.remove(song.previewUrl)
            } else {
                // guardar en Firebase
                firebaseRepository.saveSong(id, song)
                currentFavorites.add(song.previewUrl)
            }
            _favoriteSongs.value = currentFavorites
        }
    }

    // Play o Pause del preview
    fun playPause(song: Song) {
        if (musicPlayer.isPlaying(song.previewUrl)) {
            musicPlayer.pause()
        } else {
            musicPlayer.playPreview(song.previewUrl)
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayer.release()
    }
}