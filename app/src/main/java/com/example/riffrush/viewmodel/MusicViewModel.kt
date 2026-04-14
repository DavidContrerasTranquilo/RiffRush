package com.example.riffrush.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.player.MusicPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MusicViewModel(
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

    /**
     * Carga la música inicial o guardada localmente
     */
    fun loadMyMusic() {
        _isSearching.value = false
        viewModelScope.launch {
            // Aquí llamarías a tu repositorio para cargar música guardada
            // _songs.value = repository.getSavedSongs()
        }
    }

    /**
     * Busca canciones nuevas por texto
     */
    fun searchSongs(query: String) {
        _isSearching.value = true
        viewModelScope.launch {
            // Aquí llamarías a tu api de Deezer
            // val results = repository.searchSongs(query)
            // _songs.value = results
        }
    }

    /**
     * Agrega o quita de favoritos
     */
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            // Lógica para actualizar favoritos
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
        // Opcional: musicPlayer.stop()
    }
}