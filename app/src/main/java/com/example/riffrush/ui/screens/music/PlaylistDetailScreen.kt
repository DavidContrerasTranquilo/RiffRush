package com.example.riffrush.ui.screens.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.riffrush.R
import com.example.riffrush.ui.components.SongItem
import com.example.riffrush.viewmodels.MusicViewModel
import com.example.riffrush.viewmodels.PlaylistViewModel
import org.koin.androidx.compose.koinViewModel

/*
 Pantalla de Detalle de Playlist.
 Muestra los temas musicales agregados a una lista de reproducción determinada.
 Permite reproducir una partida utilizando exclusivamente las canciones de esta playlist (siempre que contenga al menos 2 canciones)
 y quitar tracks mediante gestos de deslizamiento (Swipe-to-Dismiss).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    playlistName: String,
    onBack: () -> Unit,
    onPlayPlaylist: (String) -> Unit,
    // ViewModels inyectados mediante Koin
    playlistViewModel: PlaylistViewModel = koinViewModel(),
    musicViewModel: MusicViewModel = koinViewModel()
) {
    // Escucha interactiva de canciones cargadas en la playlist actual
    val songs by playlistViewModel.selectedPlaylistSongs.collectAsState()
    val playingSongId by musicViewModel.playingSongId.collectAsState()
    val uiMessage by playlistViewModel.uiMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Solicita cargar los temas musicales de la playlist activa al inicializar la vista o cambiar de playlist
    LaunchedEffect(playlistId) {
        playlistViewModel.loadPlaylistSongs(playlistId)
    }

    // Gestiona mensajes de confirmación de bases de datos
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
                            contentDescription = stringResource(R.string.btn_back)
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
            // Botón interactivo para iniciar una partida personalizada usando los temas de esta lista
            Button(
                onClick = { onPlayPlaylist(playlistId) },
                // Regla de diseño: requiere un mínimo de 2 canciones para iniciar juego válido
                enabled = songs.size >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.btn_play_with_playlist))
            }

            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.msg_playlist_empty))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Muestra de manera informativa el conteo total de canciones guardadas
                    item {
                        Text(
                            text = stringResource(R.string.msg_songs_count, songs.size),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    itemsIndexed(
                        songs,
                        key = { index, song -> "${song.id}_$index" }
                    ) { _, song ->
                        // Controla la acción deslizable para borrar canciones individuales de la playlist
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (
                                    it == SwipeToDismissBoxValue.EndToStart ||
                                    it == SwipeToDismissBoxValue.StartToEnd
                                ) {
                                    // Invoca la remoción del track de la base de datos de Firebase
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
                                        contentDescription = stringResource(R.string.btn_delete),
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