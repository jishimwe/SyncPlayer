package com.jpishimwe.syncplayer.model

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val albumArtUri: String?
)
