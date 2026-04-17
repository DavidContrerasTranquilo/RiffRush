package com.example.riffrush.data.player

import android.media.MediaPlayer
import android.util.Log

class MusicPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null

    private fun getPlayer(): MediaPlayer {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setOnPreparedListener { mp ->
                    Log.d("MusicPlayer", "Audio listo, iniciando...")
                    mp.start()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("MusicPlayer", "Error: $what, $extra")
                    mp.reset()
                    false
                }
            }
        }
        return mediaPlayer!!
    }

    fun isPlaying(url: String): Boolean {
        return mediaPlayer?.isPlaying == true && currentUrl == url
    }

    fun playPreview(url: String) {
        if (url.isBlank()) return
        if (isPlaying(url)) return

        try {
            val player = getPlayer()
            player.stop()
            player.reset()

            currentUrl = url
            player.setDataSource(url)
            player.prepareAsync()

        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error al cambiar de canción: ${e.message}")
            currentUrl = null
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
        } catch (e: Exception) { }
        currentUrl = null
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentUrl = null
    }
}