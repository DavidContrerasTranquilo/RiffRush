package com.example.riffrush.viewmodel

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
            val song = repository.searchSong(songName)
            song?.let {
                musicPlayer.playPreview(it.previewUrl)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayer.release()
    }
}
