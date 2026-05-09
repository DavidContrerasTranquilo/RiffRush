package com.example.riffrush.data.player

import android.media.MediaPlayer
import android.util.Log

/*
 Reproductor de música básico de la aplicación.
 Utiliza Android MediaPlayer de manera asíncrona para reproducir previews de audio (URLs de streaming de 30 segundos)
 obtenidos desde la API de Deezer. Controla estados del player, reseteos de error, paradas y liberación de memoria.
 */
class MusicPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null

    // Obtiene o inicializa la instancia de MediaPlayer configurando listeners de preparación y errores
    private fun getPlayer(): MediaPlayer {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                // Comienza la reproducción automáticamente una vez preparado el búfer de red asíncronamente
                setOnPreparedListener { mp ->
                    mp.start()
                }

                // Manejo de errores de conexión o streaming
                setOnErrorListener { mp, what, extra ->
                    Log.e("MusicPlayer", "Error MediaPlayer: what=$what extra=$extra")
                    try {
                        mp.reset() // Resetea el reproductor a su estado IDLE para evitar bloqueos
                    } catch (e: Exception) {
                        Log.e("MusicPlayer", "Error reseteando player", e)
                    }
                    currentUrl = null
                    true // Indica que manejamos el error internamente
                }
            }
        }

        return mediaPlayer!!
    }

    // Comprueba si el reproductor está reproduciendo activamente la URL consultada
    fun isPlaying(url: String): Boolean {
        return mediaPlayer?.isPlaying == true && currentUrl == url
    }

    // Reproduce una URL de preview de audio de Deezer de forma asíncrona sin bloquear el hilo principal de UI
    fun playPreview(url: String) {
        if (url.isBlank()) return
        if (isPlaying(url)) return // Evita reiniciar el audio si ya está sonando la misma canción

        try {
            val player = getPlayer()

            try {
                player.stop() // Detiene la reproducción en curso anterior si la hay
            } catch (_: Exception) {
            }

            player.reset() // Prepara el player para una nueva fuente de datos
            currentUrl = url
            player.setDataSource(url) // Asigna la URL de streaming
            player.prepareAsync() // Inicia la precarga en segundo plano

        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error al reproducir preview", e)
            currentUrl = null

            try {
                mediaPlayer?.reset()
            } catch (_: Exception) {
            }
        }
    }

    // Detiene la reproducción de la canción actual y resetea el reproductor
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

    // Libera por completo los recursos nativos utilizados por MediaPlayer al destruir las vistas
    fun release() {
        stop()
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }
}