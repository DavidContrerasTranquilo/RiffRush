package com.example.riffrush.data.player

import android.media.MediaPlayer

class MusicPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null

    /**
     * Reproduce el preview. Si ya se está reproduciendo la misma canción, no reinicia.
     */
    fun playPreview(url: String) {
        if (currentUrl == url && mediaPlayer?.isPlaying == true) return

        stop() // para cualquier canción anterior

        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener { start() }
            prepareAsync()
        }
        currentUrl = url
    }

    /**
     * Pausa la canción actual
     */
    fun pause() {
        mediaPlayer?.pause()
    }

    /**
     * Comprueba si la canción dada se está reproduciendo actualmente
     */
    fun isPlaying(url: String): Boolean {
        return mediaPlayer?.isPlaying == true && currentUrl == url
    }

    /**
     * Detiene y libera recursos
     */
    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentUrl = null
    }

    /**
     * Llamar cuando el ViewModel se destruye
     */
    fun release() {
        stop()
    }
}