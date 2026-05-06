package com.example.riffrush.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.riffrush.viewmodels.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val darkMode by settingsViewModel.darkMode.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsCategory(title = "General")
            
            var notificationsEnabled by remember { mutableStateOf(true) }
            ToggleSetting(
                title = "Notificaciones",
                subtitle = "Recibir avisos de nuevos lanzamientos",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )

            ToggleSetting(
                title = "Modo Oscuro",
                subtitle = "Cambiar la apariencia de la app",
                checked = darkMode,
                onCheckedChange = { settingsViewModel.setDarkMode(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SettingsCategory(title = "Audio y Calidad")

            SettingItem(
                title = "Calidad de Streaming",
                subtitle = "Alta (320 kbps)",
                onClick = { /* Abrir diálogo de selección */ }
            )

            SettingItem(
                title = "Descargas",
                subtitle = "Solo vía Wi-Fi",
                onClick = { /* Abrir diálogo */ }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SettingsCategory(title = "Privacidad")

            SettingItem(
                title = "Historial de búsqueda",
                subtitle = "Borrar historial",
                onClick = { /* Borrar */ }
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "RiffRush v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun ToggleSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
