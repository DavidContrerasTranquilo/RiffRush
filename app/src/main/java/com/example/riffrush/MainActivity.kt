package com.example.riffrush

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.riffrush.ui.theme.RiffRushTheme
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RiffRushTheme {
                SongPreviewScreen { songName ->
                    fetchSongPreview(songName)
                }
            }
        }
    }

    private fun fetchSongPreview(song: String) {

        val url = "https://api.deezer.com/search?q=$song"

        val request = Request.Builder()
            .url(url)
            .build()

        Thread {
            try {

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                val json = JSONObject(body!!)
                val data = json.getJSONArray("data")

                if (data.length() > 0) {

                    val firstSong = data.getJSONObject(0)
                    val previewUrl = firstSong.getString("preview")

                    playPreview(previewUrl)
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }

        }.start()
    }

    private fun playPreview(url: String) {

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer()

        mediaPlayer?.apply {
            setDataSource(url)
            prepare()
            start()
        }
    }
}

@Composable
fun SongPreviewScreen(onSearch: (String) -> Unit) {

    var songName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text("Buscar canción (Deezer)")

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = songName,
            onValueChange = { songName = it },
            label = { Text("Nombre de canción") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            onSearch(songName)
        }) {
            Text("Reproducir preview")
        }
    }}