package com.example.riffrush.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.data.repository.DeezerRepository
import kotlinx.coroutines.launch

class SongViewModel(
    private val repository: DeezerRepository,
    private val musicPlayer: MusicPlayer
) : ViewModel() {

    fun searchAndPlay(songName: String) {
        viewModelScope.launch {
            val songs = repository.searchSongs(songName)

            if (songs.isNotEmpty()) {
                val firstSong = songs[0]
                musicPlayer.playPreview(firstSong.preview)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayer.release()
    }
}