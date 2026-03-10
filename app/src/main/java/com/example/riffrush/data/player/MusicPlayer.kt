package com.example.riffrush.data.player

import android.media.MediaPlayer

class MusicPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun playPreview(url: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            prepare()
            start()
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
