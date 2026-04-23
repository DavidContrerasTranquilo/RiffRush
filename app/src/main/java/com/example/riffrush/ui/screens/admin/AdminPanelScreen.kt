package com.example.riffrush.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.riffrush.viewmodels.AdminViewModel
import org.koin.androidx.compose.koinViewModel

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
                title = { Text("Panel Admin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar por nombre o email") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredUsers) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(user.name, style = MaterialTheme.typography.titleMedium)
                            Text(user.email, style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (user.isBlocked) "Estado: bloqueado" else "Estado: activo",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (user.isBlocked) {
                                    OutlinedButton(
                                        onClick = { viewModel.unblockUser(user.uid) }
                                    ) {
                                        Icon(Icons.Default.LockOpen, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Desbloquear")
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.blockUser(user.uid) }
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Bloquear")
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.clearProfilePicture(user.uid) }
                                ) {
                                    Icon(Icons.Default.ImageNotSupported, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Quitar foto")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}