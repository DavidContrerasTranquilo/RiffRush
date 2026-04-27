package com.example.riffrush.data.model

data class PlaylistModel(
    val id: String = "",
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val songsCount: Int = 0
)