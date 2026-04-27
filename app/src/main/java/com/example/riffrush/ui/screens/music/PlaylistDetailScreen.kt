package com.example.riffrush.ui.screens.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.riffrush.ui.components.SongItem
import com.example.riffrush.viewmodels.MusicViewModel
import com.example.riffrush.viewmodels.PlaylistViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    playlistName: String,
    onBack: () -> Unit,
    onPlayPlaylist: (String) -> Unit,
    playlistViewModel: PlaylistViewModel = koinViewModel(),
    musicViewModel: MusicViewModel = koinViewModel()
) {
    val songs by playlistViewModel.selectedPlaylistSongs.collectAsState()
    val playingSongId by musicViewModel.playingSongId.collectAsState()
    val uiMessage by playlistViewModel.uiMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(playlistId) {
        playlistViewModel.loadPlaylistSongs(playlistId)
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            playlistViewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Button(
                onClick = { onPlayPlaylist(playlistId) },
                enabled = songs.size >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Jugar con esta playlist")
            }

            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Esta playlist aún no tiene canciones")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Text(
                            text = "${songs.size} canciones",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    itemsIndexed(
                        songs,
                        key = { index, song -> "${song.id}_$index" }
                    ) { _, song ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (
                                    it == SwipeToDismissBoxValue.EndToStart ||
                                    it == SwipeToDismissBoxValue.StartToEnd
                                ) {
                                    playlistViewModel.removeSongFromPlaylist(
                                        playlistId = playlistId,
                                        songId = song.id.toString()
                                    )
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                                    else -> Color.Red
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = Color.White
                                    )
                                }
                            }
                        ) {
                            SongItem(
                                song = song,
                                isFavorite = false,
                                isPlaying = playingSongId == song.id,
                                showFavoriteIcon = false,
                                onPlayPause = {
                                    musicViewModel.playPause(song)
                                },
                                onFavoriteToggle = {}
                            )
                        }
                    }
                }
            }
        }
    }
}