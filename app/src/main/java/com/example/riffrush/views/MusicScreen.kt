package com.example.riffrush.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.riffrush.ui.components.SongItem
import com.example.riffrush.viewmodel.MusicViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(viewModel: MusicViewModel = koinViewModel()) {

    val songs by viewModel.songs.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var playingUrl by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadMyMusic()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSearching) {
                IconButton(onClick = {
                    query = ""
                    viewModel.loadMyMusic()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar canciones...") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { if (query.isNotBlank()) viewModel.searchSongs(query) },
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("Buscar")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                SongItem(
                    song = song,
                    isFavorite = favoriteSongs.any { it.id == song.id },
                    isPlaying = playingUrl == song.preview,
                    onPlayPause = {
                        viewModel.playPause(song)
                        playingUrl = if (playingUrl == song.preview) null else song.preview
                    },
                    onFavoriteToggle = {
                        viewModel.toggleFavorite(song)
                    }
                )
            }
        }
    }
}