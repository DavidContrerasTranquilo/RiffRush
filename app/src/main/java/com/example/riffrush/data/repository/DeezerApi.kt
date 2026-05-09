package com.example.riffrush.data.repository

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

interface DeezerApi {
    @GET("search")
    suspend fun search(@Query("q") query: String): DeezerSearchResponse

    @GET("chart")
    suspend fun getChart(): DeezerChartResponse

    @GET("track/{id}")
    suspend fun getTrack(@Path("id") trackId: Long): DeezerTrack
}

// Clases de respuesta para el parseo de JSON
data class DeezerSearchResponse(
    val data: List<DeezerTrack>
)

data class DeezerChartResponse(
    val tracks: DeezerTrackData
)

data class DeezerTrackData(
    val data: List<DeezerTrack>
)

data class DeezerTrack(
    val id: Long,
    val title: String?,
    val preview: String?,
    val artist: DeezerArtist?,
    val album: DeezerAlbum?
)

data class DeezerArtist(
    val name: String?
)

data class DeezerAlbum(
    val title: String?,
    @SerializedName("cover_medium") val covermedium: String?
)