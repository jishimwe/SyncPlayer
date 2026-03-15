package com.jpishimwe.syncplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artist_images")
data class ArtistImage(
    @PrimaryKey val artistName: String,
    val imageUrl: String?,
    val fetchedAt: Long,
)
