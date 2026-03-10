package com.example.riffrush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.data.repository.DeezerRepository
import com.example.riffrush.ui.theme.RiffRushTheme
import com.example.riffrush.viewmodel.SongViewModel
import com.example.riffrush.views.SongPreviewScreen
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Manual Injection for simplicity, consider Koin or Hilt for larger projects
        val client = OkHttpClient()
        val repository = DeezerRepository(client)
        val musicPlayer = MusicPlayer()
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SongViewModel(repository, musicPlayer) as T
            }
        })[SongViewModel::class.java]

        setContent {
            RiffRushTheme {
                Scaffold { innerPadding ->
                    SongPreviewScreen(
                        modifier = Modifier.padding(innerPadding),
                        onSearch = { songName ->
                            viewModel.searchAndPlay(songName)
                        }
                    )
                }
            }
        }
    }
}
