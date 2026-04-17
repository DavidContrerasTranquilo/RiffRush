package com.example.riffrush.ui.screens.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.riffrush.ui.components.SongItem
import com.example.riffrush.viewmodels.MusicViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(viewModel: MusicViewModel = koinViewModel()) {
    val songs by viewModel.songs.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    //David, aquí guardamos el id de la canción que se está reproduciendo
    val playingSongId by viewModel.playingSongId.collectAsState()

    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadMyMusic()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
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
            //David, aquí recorremos la lista de canciones
            itemsIndexed(songs, key = { index, song -> "${song.id}_$index" }) { _, song ->
               //Comprobamos si estamos buscando o no, decidimos si mostrar el corazon o no
                if (!isSearching) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                viewModel.toggleFavorite(song)
                                true
                            } else false
                        }
                    )
                    //Aqui añado el swipe para borrar
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
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White)
                            }
                        }
                    ) {
                        //Aqui la cancion
                        SongItem(
                            song = song,
                            isFavorite = true,
                            isPlaying = playingSongId == song.id,
                            showFavoriteIcon = false,
                            onPlayPause = {
                                viewModel.playPause(song)
                            },
                            onFavoriteToggle = {
                                viewModel.toggleFavorite(song)
                            }
                        )
                    }
                } else {

                    SongItem(
                        song = song,
                        isFavorite = favoriteSongs.any { it.id == song.id },
                        isPlaying = playingSongId == song.id,
                        showFavoriteIcon = true,
                        onPlayPause = {
                            viewModel.playPause(song)
                        },
                        onFavoriteToggle = {
                            viewModel.toggleFavorite(song)
                        }
                    )
                }
            }
        }
    }
}