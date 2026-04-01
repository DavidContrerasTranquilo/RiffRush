package com.example.riffrush.views

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.riffrush.viewmodel.AuthViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.input.PasswordVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = koinViewModel()
) {
    var name by remember { mutableStateOf(authViewModel.currentUserName) }
    var bio by remember { mutableStateOf(authViewModel.userBio.value) }
    var password by remember { mutableStateOf("") }

    val uiMessage by authViewModel.uiMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { authViewModel.loadUserBio() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { /* ... igual que antes ... */ }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Biografía") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Nueva Contraseña (dejar en blanco para no cambiar)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // Cambia updateProfileData por updateFullProfile
                    authViewModel.updateFullProfile(
                        name,
                        bio,
                        if (password.isEmpty()) null else password
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Guardar cambios")
            }
        }
    }
}