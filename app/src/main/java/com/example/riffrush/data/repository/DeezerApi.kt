package com.example.riffrush.data.repository

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

/*
 Interfaz de Retrofit para conectar con la API oficial de Deezer.
 Declara las llamadas HTTP GET necesarias para buscar música, obtener tendencias y descargar tracks de playlists dinámicas.
 */
interface DeezerApi {
    // Búsqueda libre de canciones mediante un término de texto
    @GET("search")
    suspend fun search(@Query("q") query: String): DeezerSearchResponse

    // Obtención de la lista de canciones en tendencia actuales
    @GET("chart")
    suspend fun getChart(): DeezerChartResponse

    // Obtención de los metadatos detallados de un track individual por su ID único de Deezer
    @GET("track/{id}")
    suspend fun getTrack(@Path("id") trackId: Long): DeezerTrack

    // Descarga de todas las canciones que forman parte de una playlist de Deezer por su ID (usado para las categorías)
    @GET("playlist/{id}")
    suspend fun getPlaylistTracks(@Path("id") playlistId: Long): DeezerPlaylistResponse
}

// ── Clases de respuesta para el parseo de JSON mediante GSON ──

// Estructura contenedora para resultados de búsqueda libre
data class DeezerSearchResponse(
    val data: List<DeezerTrack>
)

// Estructura contenedora para el top global de canciones (chart)
data class DeezerChartResponse(
    val tracks: DeezerTrackData
)

// Estructura contenedora para canciones integradas en una playlist oficial
data class DeezerPlaylistResponse(
    val tracks: DeezerTrackData
)

// Listado de tracks dentro de una estructura intermedia
data class DeezerTrackData(
    val data: List<DeezerTrack>
)

// Datos atómicos de una canción mapeados desde el JSON de Deezer
data class DeezerTrack(
    val id: Long,
    val title: String?,
    val preview: String?, // URL del mp3 de prueba de 30 segundos
    val artist: DeezerArtist?,
    val album: DeezerAlbum?
)

// Datos del artista
data class DeezerArtist(
    val name: String?
)

// Datos del álbum, mapeando "cover_medium" para que coincida con el snake_case de la API
data class DeezerAlbum(
    val title: String?,
    @SerializedName("cover_medium") val covermedium: String?
)