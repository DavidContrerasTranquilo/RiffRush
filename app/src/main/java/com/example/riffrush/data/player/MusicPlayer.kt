package com.example.riffrush.data.player

import android.media.MediaPlayer
import android.util.Log

class MusicPlayer {

    private var mediaPlayer: MediaPlayer? = MediaPlayer().apply {
        setOnPreparedListener { mp ->
            Log.d("MusicPlayer", "Audio listo, empezando reproducción")
            mp.start()
        }
        setOnErrorListener { _, what, extra ->
            Log.e("MusicPlayer", "Error de MediaPlayer: what=$what, extra=$extra")
            true
        }
    }

    private var currentUrl: String? = null

    /**
     * Comprueba si la canción con esta URL está sonando actualmente.
     */
    fun isPlaying(url: String): Boolean {
        return mediaPlayer?.isPlaying == true && currentUrl == url
    }

    /**
     * Reproduce el audio desde una URL de Deezer.
     */
    fun playPreview(url: String) {
        if (url.isBlank()) {
            Log.e("MusicPlayer", "URL vacía")
            return
        }
        if (isPlaying(url)) return

        try {
            currentUrl = url
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                setDataSource(url)
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error al preparar el audio: ${e.message}")
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun stop() {
        mediaPlayer?.stop()
        currentUrl = null
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentUrl = null
    }
}