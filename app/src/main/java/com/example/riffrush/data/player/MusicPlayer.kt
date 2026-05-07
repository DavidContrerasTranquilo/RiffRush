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
                    mp.start()
                }

                setOnErrorListener { mp, what, extra ->
                    Log.e("MusicPlayer", "Error MediaPlayer: what=$what extra=$extra")
                    try {
                        mp.reset()
                    } catch (e: Exception) {
                        Log.e("MusicPlayer", "Error reseteando player", e)
                    }
                    currentUrl = null
                    true
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

            try {
                player.stop()
            } catch (_: Exception) {
            }

            player.reset()
            currentUrl = url
            player.setDataSource(url)
            player.prepareAsync()

        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error al reproducir preview", e)
            currentUrl = null

            try {
                mediaPlayer?.reset()
            } catch (_: Exception) {
            }
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }

        try {
            mediaPlayer?.reset()
        } catch (_: Exception) {
        }

        currentUrl = null
    }
}