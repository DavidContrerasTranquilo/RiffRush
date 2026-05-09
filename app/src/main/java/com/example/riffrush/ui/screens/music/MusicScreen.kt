package com.example.riffrush.ui.screens.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.riffrush.R
import com.example.riffrush.data.model.Song
import com.example.riffrush.ui.components.SongItem
import com.example.riffrush.viewmodels.MusicViewModel
import com.example.riffrush.viewmodels.PlaylistViewModel
import org.koin.androidx.compose.koinViewModel

/*
 Pantalla principal de la sección de Música de la aplicación RiffRush.
 Permite a los usuarios buscar canciones (integrando con la API de Deezer), preescuchar fragmentos
 de audio de 30 segundos, alternar favoritos y administrar listas de reproducción (playlists)
 sincronizadas directamente con Firebase Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    // Inyección de dependencias mediante Koin para gestionar el estado de canciones y de reproducción de audio
    viewModel: MusicViewModel = koinViewModel(),
    // Inyección del ViewModel para la gestión de las playlists del usuario
    playlistViewModel: PlaylistViewModel = koinViewModel(),
    onNavigateToPlaylistDetail: (String, String) -> Unit = { _, _ -> }
) {
    // Observación reactiva de la lista de canciones encontradas y favoritos del usuario
    val songs by viewModel.songs.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val playingSongId by viewModel.playingSongId.collectAsState()

    // Observación reactiva de las playlists disponibles de la base de datos de Firebase
    val playlists by playlistViewModel.playlists.collectAsState()
    val uiMessage by playlistViewModel.uiMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Estados mutables para búsquedas, pestañas y modales
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    // Asegura que al salir de la pantalla se detenga inmediatamente cualquier reproducción de música activa
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPlayback()
        }
    }

    // Al iniciar la pantalla, carga las canciones del usuario y sus playlists sincronizadas
    LaunchedEffect(Unit) {
        viewModel.loadMyMusic()
        playlistViewModel.loadPlaylists()
    }

    // Escucha de notificaciones o mensajes informativos provenientes de la lógica de negocio
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            playlistViewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barra superior de búsqueda de música
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Al buscar, se despliega un botón de retroceso para volver a la lista normal de favoritos/playlists
                if (isSearching) {
                    IconButton(
                        onClick = {
                            query = ""
                            viewModel.loadMyMusic()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                }

                // Entrada de texto de búsqueda que se activa al presionar la lupa del teclado virtual
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.hint_search_songs)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (query.isNotBlank()) {
                            viewModel.searchSongs(query)
                        }
                    })
                )
            }

            // Tabs principales de navegación ("Favoritos" vs "Playlists")
            if (!isSearching) {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.tab_favorites)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.tab_playlists)) }
                    )
                }
            }

            // Muestra una sub-pantalla u otra dependiendo del tab o de si el usuario está realizando una búsqueda activa
            when {
                isSearching -> {
                    SearchResultsList(
                        songs = songs,
                        favoriteSongs = favoriteSongs,
                        playingSongId = playingSongId,
                        onPlayPause = { song -> viewModel.playPause(song) },
                        onFavoriteToggle = { song ->
                            // Regla de negocio: se requiere inicio de sesión con cuenta real para marcar favoritos
                            val currentUser =
                                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            if (currentUser == null || currentUser.isAnonymous) {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.error_login_required_favorite_song),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.toggleFavorite(song)
                            }
                        },
                        onAddToPlaylist = { song ->
                            val currentUser =
                                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            if (currentUser == null || currentUser.isAnonymous) {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.error_login_required_create_playlist),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                songToAddToPlaylist = song
                            }
                        }
                    )
                }

                selectedTab == 0 -> {
                    FavoritesList(
                        songs = songs,
                        playingSongId = playingSongId,
                        onPlayPause = { song -> viewModel.playPause(song) },
                        onFavoriteToggle = { song ->
                            val currentUser =
                                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            if (currentUser == null || currentUser.isAnonymous) {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.error_login_required_favorite_song),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.toggleFavorite(song)
                            }
                        }
                    )
                }

                // Pestaña de Playlists creadas por el usuario
                selectedTab == 1 -> {
                    PlaylistsList(
                        playlists = playlists,
                        onCreatePlaylistClick = {
                            // Se requiere autenticación formal para poder crear nuevas listas
                            val currentUser =
                                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            if (currentUser == null || currentUser.isAnonymous) {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.error_login_required_create_playlist),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showCreatePlaylistDialog = true
                            }
                        },
                        onPlaylistClick = { playlist ->
                            // Navega a los detalles para ver las canciones de la playlist escogida
                            onNavigateToPlaylistDetail(playlist.id, playlist.name)
                        },
                        onDeletePlaylist = { playlist ->
                            // Elimina la lista completa de la cuenta de Firestore
                            playlistViewModel.deletePlaylist(playlist.id)
                        }
                    )
                }
            }
        }
    }

    // Cuadro de diálogo modal para la creación rápida de una nueva lista de reproducción
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                newPlaylistName = ""
            },
            title = { Text(stringResource(R.string.btn_create_playlist)) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(stringResource(R.string.label_playlist_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Solicita al ViewModel guardar la playlist y limpia el estado del modal
                        playlistViewModel.createPlaylist(newPlaylistName)
                        newPlaylistName = ""
                        showCreatePlaylistDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_create))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreatePlaylistDialog = false
                        newPlaylistName = ""
                    }
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    songToAddToPlaylist?.let { song ->
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            title = { Text(stringResource(R.string.dialog_title_add_to_playlist)) },
            text = {
                if (playlists.isEmpty()) {
                    Text(stringResource(R.string.msg_no_playlists_yet))
                } else {
                    LazyColumn {
                        items(playlists) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                supportingContent = {
                                    Text(stringResource(R.string.msg_songs_count, playlist.songsCount))
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistPlay,
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
                                Text(stringResource(R.string.btn_add_to_playlist_name, playlist.name))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { songToAddToPlaylist = null }) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        )
    }
}

// Componente que gestiona la lista de favoritos mediante gestos de deslizamiento lateral (Swipe-to-Dismiss)
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
            // Controla el estado del gesto deslizable para quitar favoritos con feedback visual rojo
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
                    // Revela un fondo rojo con un icono de borrado al deslizar el track hacia la izquierda
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
                // Elemento visual de la canción individual
                SongItem(
                    song = song,
                    isFavorite = true,
                    isPlaying = playingSongId == song.id,
                    showFavoriteIcon = false, // Oculto porque deslizar ya elimina la canción
                    onPlayPause = { onPlayPause(song) },
                    onFavoriteToggle = { onFavoriteToggle(song) }
                )
            }
        }
    }
}

// Lista optimizada para representar los tracks devueltos del buscador en tiempo real
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

                // Botón interactivo lateral para desplegar las playlists y adjuntar esta canción
                IconButton(onClick = { onAddToPlaylist(song) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.dialog_title_add_to_playlist)
                    )
                }
            }
        }
    }
}

// Representación visual de la colección de listas personalizadas creadas por el usuario
@Composable
fun PlaylistsList(
    playlists: List<com.example.riffrush.data.model.PlaylistModel>,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (com.example.riffrush.data.model.PlaylistModel) -> Unit,
    onDeletePlaylist: (com.example.riffrush.data.model.PlaylistModel) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Disparador superior para registrar una nueva playlist
        Button(
            onClick = onCreatePlaylistClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_create_playlist))
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.msg_no_playlists))
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
                                Icons.AutoMirrored.Filled.PlaylistPlay,
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
                                    text = stringResource(R.string.msg_songs_count, playlist.songsCount),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // Acción para borrar permanentemente la playlist de Firestore
                            IconButton(onClick = { onDeletePlaylist(playlist) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.btn_delete_playlist)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}