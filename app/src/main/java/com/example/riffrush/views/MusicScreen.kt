package com.example.riffrush.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.riffrush.ui.components.SongItem
import com.example.riffrush.viewmodel.MusicViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MusicScreen(viewModel: MusicViewModel = koinViewModel()) {

    val songs by viewModel.songs.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    var playingUrl by remember { mutableStateOf<String?>(null) }

    val isSearching by viewModel.isSearching.collectAsState()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadMyMusic() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de búsqueda
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            if (isSearching) {
                Button(onClick = {
                    query = ""
                    viewModel.loadMyMusic()
                }) { Text("←") }
            }

            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar canciones...") }
            )

            Button(onClick = { if (query.isNotEmpty()) viewModel.searchSongs(query) }) {
                Text("Buscar")
            }
        }

        LazyColumn {
            items(songs) { song ->
                SongItem(
                    song = song,
                    isFavorite = favoriteSongs.contains(song.previewUrl),
                    isPlaying = playingUrl == song.previewUrl,
                    onPlayPause = {
                        viewModel.playPause(song)
                        playingUrl = if (playingUrl == song.previewUrl) null else song.previewUrl
                    },
                    onFavoriteToggle = { viewModel.toggleFavorite(song) }
                )
            }
        }
    }
}