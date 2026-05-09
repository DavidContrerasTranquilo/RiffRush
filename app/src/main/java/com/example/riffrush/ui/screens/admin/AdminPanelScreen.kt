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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = koinViewModel()
) {
    val users by viewModel.users.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var query by remember { mutableStateOf("") }

    var challengeDate by remember { mutableStateOf("") }
    var song1 by remember { mutableStateOf("") }
    var song2 by remember { mutableStateOf("") }
    var song3 by remember { mutableStateOf("") }
    var song4 by remember { mutableStateOf("") }
    var song5 by remember { mutableStateOf("") }
    var song6 by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

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
            item {
                Text(
                    text = stringResource(R.string.title_user_management),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_search_user)) },
                    singleLine = true
                )
            }

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

                            OutlinedButton(onClick = { viewModel.clearProfilePicture(user.uid) }) {
                                Icon(Icons.Default.ImageNotSupported, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_remove_photo))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.title_configure_challenge),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                OutlinedTextField(
                    value = challengeDate,
                    onValueChange = { challengeDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_date_format)) },
                    singleLine = true
                )
            }

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

                            if (trimmedIds.any { it.isBlank() }) {
                                viewModel.showMessage(errorMsg)
                            } else {
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