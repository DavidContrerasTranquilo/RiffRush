package com.example.riffrush.data.model

data class Song(
    val id: Long = 0,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val preview: String = "",
    val cover: String = ""
) {
    constructor() : this(0, "", "", "", "", "")
}