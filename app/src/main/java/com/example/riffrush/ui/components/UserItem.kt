package com.example.riffrush.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.riffrush.data.model.FriendModel
import androidx.compose.foundation.clickable
@Composable
fun UserItem(
    user: FriendModel,
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit,
    onMatchClick: (() -> Unit)? = null,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onUserClick(user.uid) },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (user.pfpUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.pfpUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (onMatchClick != null) {
                IconButton(onClick = onMatchClick) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Retar",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }

            IconButton(onClick = onFollowClick) {
                Icon(
                    imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                    contentDescription = if (isFollowing) "Siguiendo" else "Seguir",
                    tint = if (isFollowing) Color.Unspecified else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}