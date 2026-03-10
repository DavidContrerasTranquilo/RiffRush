package com.example.riffrush.data.repository

import com.example.riffrush.data.model.Song
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeezerRepository(private val client: OkHttpClient) {

    suspend fun searchSong(query: String): Song? = withContext(Dispatchers.IO) {
        val url = "https://api.deezer.com/search?q=$query"
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            
            val json = JSONObject(body)
            val data = json.getJSONArray("data")

            if (data.length() > 0) {
                val firstSong = data.getJSONObject(0)
                return@withContext Song(
                    title = firstSong.getString("title"),
                    previewUrl = firstSong.getString("preview")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}
