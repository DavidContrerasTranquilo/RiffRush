package com.example.riffrush.data.repository

import com.example.riffrush.data.model.Song
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeezerRepository(private val client: OkHttpClient) {

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val url = "https://api.deezer.com/search?q=$query"
        val request = Request.Builder().url(url).build()

        val result = mutableListOf<Song>()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            val json = JSONObject(body)
            val data = json.getJSONArray("data")

            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)

                result.add(
                    Song(
                        title = item.getString("title"),
                        artist = item.getJSONObject("artist").getString("name"),
                        previewUrl = item.getString("preview"),
                        coverUrl = item.getJSONObject("album").getString("cover")
                    )
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        result
    }
}