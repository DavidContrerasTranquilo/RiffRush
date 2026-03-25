package com.example.riffrush.views

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SongPreviewScreen(
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit
) {
    var songName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Buscar canción (Deezer)",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = songName,
            onValueChange = { songName = it },
            label = { Text("Nombre de canción") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSearch(songName) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reproducir preview")
        }
    }
}