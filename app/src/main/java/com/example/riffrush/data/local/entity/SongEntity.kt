package com.example.riffrush.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.riffrush.data.model.Song

/*
 Entidad Room que representa una canción favorita almacenada localmente en SQLite.
 Actúa como caché local para las canciones favoritas del usuario, permitiendo
 acceso instantáneo offline sin necesidad de consultar Firestore en cada apertura de la app.
 */
@Entity(tableName = "favorite_songs")
data class SongEntity(
    @PrimaryKey val id: Long,           // ID oficial de Deezer
    val title: String,                  // Título de la canción
    val artist: String,                 // Artista o grupo musical
    val album: String,                  // Nombre del álbum
    val preview: String,                // URL del preview MP3 de 30 segundos
    val cover: String,                  // URL de la carátula del álbum
    val ownerId: String,                // UID del usuario propietario del favorito
    val addedAt: Long = System.currentTimeMillis() // Marca temporal de adición
)

// Convierte un modelo de dominio Song en una entidad Room almacenable
fun Song.toEntity(ownerId: String) = SongEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    preview = preview,
    cover = cover,
    ownerId = ownerId
)

// Convierte una entidad Room en un modelo de dominio Song para la capa de UI
fun SongEntity.toSong() = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    preview = preview,
    cover = cover
)
