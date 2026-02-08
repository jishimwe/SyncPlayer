package com.jpishimwe.syncplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val trackNumber: Int,
    val year: Int,
    val dateAdded: Long,
    val contentUri: String,
    val albumArtUri: String?
)
