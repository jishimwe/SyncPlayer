package com.jpishimwe.syncplayer.model

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

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
    val contentUri: String?,
    val albumArtUri: String?,
    val playCount: Int = 0,
    val rating: Int = 0,
    val lastPlayed: Long = 0,
)

enum class Rating(
    val value: Int,
) {
    NONE(0),
    POOR(1),
    FAIR(2),
    GOOD(3),
    GREAT(4),
    FAVORITE(5),
    ;

    companion object {
        fun fromInt(value: Int): Rating = entries.find { it.value == value } ?: NONE
    }
}

class RatingConverter {
    @TypeConverter
    fun toInt(rating: Rating): Int = rating.value

    @TypeConverter
    fun fromInt(value: Int): Rating = Rating.fromInt(value)
}

fun Song.toMediaItem(): MediaItem =
    MediaItem
        .Builder()
        .setMediaId(id.toString())
        .setUri(contentUri)
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri?.toUri())
                .build(),
        ).build()

fun MediaItem.toSong(): Song? {
    val id = mediaId.toLongOrNull() ?: return null
    return Song(
        id = id,
        title = mediaMetadata.title.toString(),
        artist = mediaMetadata.artist.toString(),
        album = mediaMetadata.albumTitle.toString(),
        albumId = 0L,
        albumArtUri = mediaMetadata.artworkUri?.toString(),
        duration = mediaMetadata.durationMs ?: 0,
        trackNumber = mediaMetadata.trackNumber ?: 0,
        year = mediaMetadata.releaseYear ?: 0,
        contentUri = "",
        dateAdded = 0,
    )
}
