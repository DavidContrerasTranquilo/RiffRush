package com.example.riffrush.ui.screens.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.riffrush.data.model.Song
import com.example.riffrush.ui.components.SongItem
import com.example.riffrush.viewmodels.MusicViewModel
import com.example.riffrush.viewmodels.PlaylistViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    viewModel: MusicViewModel = koinViewModel(),
    playlistViewModel: PlaylistViewModel = koinViewModel(),
    onNavigateToPlaylistDetail: (String, String) -> Unit = { _, _ -> }
) {
    val songs by viewModel.songs.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val playingSongId by viewModel.playingSongId.collectAsState()

    val playlists by playlistViewModel.playlists.collectAsState()
    val uiMessage by playlistViewModel.uiMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadMyMusic()
        playlistViewModel.loadPlaylists()
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            playlistViewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearching) {
                    IconButton(
                        onClick = {
                            query = ""
                            viewModel.loadMyMusic()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
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
                    onClick = {
                        if (query.isNotBlank()) {
                            selectedTab = 0
                            viewModel.searchSongs(query)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Buscar")
                }
            }

            if (!isSearching) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Favoritos") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Playlists") }
                    )
                }
            }

            when {
                isSearching -> {
                    SearchResultsList(
                        songs = songs,
                        favoriteSongs = favoriteSongs,
                        playingSongId = playingSongId,
                        onPlayPause = { song -> viewModel.playPause(song) },
                        onFavoriteToggle = { song -> viewModel.toggleFavorite(song) },
                        onAddToPlaylist = { song -> songToAddToPlaylist = song }
                    )
                }

                selectedTab == 0 -> {
                    FavoritesList(
                        songs = songs,
                        playingSongId = playingSongId,
                        onPlayPause = { song -> viewModel.playPause(song) },
                        onFavoriteToggle = { song -> viewModel.toggleFavorite(song) }
                    )
                }

                selectedTab == 1 -> {
                    PlaylistsList(
                        playlists = playlists,
                        onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                        onPlaylistClick = { playlist ->
                            onNavigateToPlaylistDetail(playlist.id, playlist.name)
                        },
                        onDeletePlaylist = { playlist ->
                            playlistViewModel.deletePlaylist(playlist.id)
                        }
                    )
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                newPlaylistName = ""
            },
            title = { Text("Crear playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Nombre de la playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        playlistViewModel.createPlaylist(newPlaylistName)
                        newPlaylistName = ""
                        showCreatePlaylistDialog = false
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreatePlaylistDialog = false
                        newPlaylistName = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    songToAddToPlaylist?.let { song ->
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            title = { Text("Añadir a playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("No tienes playlists creadas todavía.")
                } else {
                    LazyColumn {
                        items(playlists) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                supportingContent = {
                                    Text("${playlist.songsCount} canciones")
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.PlaylistPlay,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    playlistViewModel.addSongToPlaylist(playlist.id, song)
                                    songToAddToPlaylist = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Añadir a ${playlist.name}")
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { songToAddToPlaylist = null }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesList(
    songs: List<Song>,
    playingSongId: Long?,
    onPlayPause: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        itemsIndexed(songs, key = { index, song -> "${song.id}_$index" }) { _, song ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (
                        it == SwipeToDismissBoxValue.EndToStart ||
                        it == SwipeToDismissBoxValue.StartToEnd
                    ) {
                        onFavoriteToggle(song)
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
                    isFavorite = true,
                    isPlaying = playingSongId == song.id,
                    showFavoriteIcon = false,
                    onPlayPause = { onPlayPause(song) },
                    onFavoriteToggle = { onFavoriteToggle(song) }
                )
            }
        }
    }
}

@Composable
fun SearchResultsList(
    songs: List<Song>,
    favoriteSongs: List<Song>,
    playingSongId: Long?,
    onPlayPause: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        itemsIndexed(songs, key = { index, song -> "${song.id}_$index" }) { _, song ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SongItem(
                        song = song,
                        isFavorite = favoriteSongs.any { it.id == song.id },
                        isPlaying = playingSongId == song.id,
                        showFavoriteIcon = true,
                        onPlayPause = { onPlayPause(song) },
                        onFavoriteToggle = { onFavoriteToggle(song) }
                    )
                }

                IconButton(onClick = { onAddToPlaylist(song) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Añadir a playlist"
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistsList(
    playlists: List<com.example.riffrush.data.model.PlaylistModel>,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (com.example.riffrush.data.model.PlaylistModel) -> Unit,
    onDeletePlaylist: (com.example.riffrush.data.model.PlaylistModel) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onCreatePlaylistClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crear playlist")
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Todavía no tienes playlists")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlists) { playlist ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onPlaylistClick(playlist) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${playlist.songsCount} canciones",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            IconButton(onClick = { onDeletePlaylist(playlist) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar playlist"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}