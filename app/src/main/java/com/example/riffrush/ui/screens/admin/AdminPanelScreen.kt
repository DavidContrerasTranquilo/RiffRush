package com.example.riffrush.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.riffrush.R
import com.example.riffrush.viewmodels.AdminViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material.icons.automirrored.filled.ArrowBack

/*
 Panel de Administración (Admin Panel Screen).
 Interfaz reservada únicamente para administradores de RiffRush.
 Ofrece herramientas críticas de moderación:
 1. Gestión de usuarios (listar, buscar, bloquear, desbloquear y borrar fotos de perfil infractoras).
 2. Configuración y programación directa del Reto Diario mediante la asignación de 6 IDs de canciones de Deezer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onBack: () -> Unit,
    // Inyección de dependencias mediante Koin para acceder al estado de administración
    viewModel: AdminViewModel = koinViewModel()
) {
    // Listado reactivo de todos los usuarios registrados en el sistema
    val users by viewModel.users.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados de entrada para moderación y búsqueda
    var query by remember { mutableStateOf("") }

    // Estados de entrada para la fecha y los 6 IDs que conformarán el próximo reto diario
    var challengeDate by remember { mutableStateOf("") }
    var song1 by remember { mutableStateOf("") }
    var song2 by remember { mutableStateOf("") }
    var song3 by remember { mutableStateOf("") }
    var song4 by remember { mutableStateOf("") }
    var song5 by remember { mutableStateOf("") }
    var song6 by remember { mutableStateOf("") }

    // Solicita la carga inicial de los usuarios registrados de Firebase Firestore
    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    // Despliega Snackbars flotantes para confirmar acciones exitosas o errores de validación
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Filtra dinámicamente la lista de usuarios según el término de búsqueda ingresado
    val filteredUsers = users.filter {
        it.name.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_admin_panel)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================== SECCIÓN 1: GESTIÓN DE USUARIOS ==================
            item {
                Text(
                    text = stringResource(R.string.title_user_management),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Campo para la búsqueda en vivo de usuarios
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_search_user)) },
                    singleLine = true
                )
            }

            // Listado de tarjetas de usuario moderables
            items(filteredUsers) { user ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(user.name, style = MaterialTheme.typography.titleMedium)
                        Text(user.email, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (user.isBlocked) stringResource(R.string.msg_status_blocked) else stringResource(R.string.msg_status_active),
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Alterna el estado de bloqueo del usuario en la base de datos
                            if (user.isBlocked) {
                                OutlinedButton(onClick = { viewModel.unblockUser(user.uid) }) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_unblock))
                                }
                            } else {
                                Button(onClick = { viewModel.blockUser(user.uid) }) {
                                    Icon(Icons.Default.Block, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_block))
                                }
                            }

                            // Limpia y elimina de forma remota cualquier avatar/foto inapropiada
                            OutlinedButton(onClick = { viewModel.clearProfilePicture(user.uid) }) {
                                Icon(Icons.Default.ImageNotSupported, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_remove_photo))
                            }
                        }
                    }
                }
            }

            // ================== SECCIÓN 2: PLANIFICADOR DEL RETO DIARIO ==================
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.title_configure_challenge),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Entrada de fecha de programación (Formato esperado: YYYY-MM-DD)
            item {
                OutlinedTextField(
                    value = challengeDate,
                    onValueChange = { challengeDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_date_format)) },
                    singleLine = true
                )
            }

            // Siguientes items: Configuración obligatoria de los 6 IDs de canción de Deezer API
            item {
                OutlinedTextField(
                    value = song1,
                    onValueChange = { song1 = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_song_id_1)) },
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = song2,
                    onValueChange = { song2 = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_song_id_2)) },
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = song3,
                    onValueChange = { song3 = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_song_id_3)) },
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = song4,
                    onValueChange = { song4 = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_song_id_4)) },
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = song5,
                    onValueChange = { song5 = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_song_id_5)) },
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = song6,
                    onValueChange = { song6 = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_song_id_6)) },
                    singleLine = true
                )
            }

            // Botones interactivos para confirmar o revertir/borrar la configuración del reto para esa fecha
            item {
                val errorMsg = stringResource(R.string.msg_error_fill_all_ids)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val rawIds = listOf(song1, song2, song3, song4, song5, song6)
                            val trimmedIds = rawIds.map { it.trim() }

                            // Validación obligatoria: todos los IDs de pista Deezer deben ser proporcionados
                            if (trimmedIds.any { it.isBlank() }) {
                                viewModel.showMessage(errorMsg)
                            } else {
                                // Guarda o actualiza los datos estructurados en la base de datos de Firebase
                                viewModel.saveDailyChallenge(
                                    date = challengeDate.trim(),
                                    songIds = trimmedIds,
                                    isActive = true
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_save_challenge))
                    }

                    OutlinedButton(
                        onClick = {
                            // Elimina de forma remota la planificación del reto diario de la fecha indicada
                            viewModel.deleteDailyChallenge(challengeDate)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_delete_challenge))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}