package com.example.riffrush.ui.viewmodel

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.riffrush.data.model.Song

@Composable
fun SongItem(song: Song) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        AsyncImage(
            model = song.cover,
            contentDescription = null,
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(song.title)
            Text(song.artist)
        }

        IconButton(onClick = { }) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
        }

        IconButton(onClick = { }) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = null)
        }
    }
}