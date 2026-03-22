package com.jpishimwe.syncplayer.ui.shared

import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song

// ── Shared preview data for detail screen previews ──────────────────────────────

internal val previewSongBlindingLights =
    Song(
        id = 1,
        title = "Blinding Lights",
        artist = "The Weeknd",
        albumArtist = "The Weeknd",
        album = "After Hours",
        duration = 200_000L,
        albumArtUri = null,
        playCount = 12,
        rating = Rating.GREAT.value,
        lastPlayed = 0L,
        albumId = 1,
        trackNumber = 1,
        year = 2020,
        dateAdded = 1000L,
        contentUri = null,
        lastModified = 0L,
    )

internal val previewSongSaveYourTears =
    Song(
        id = 2,
        title = "Save Your Tears",
        artist = "The Weeknd",
        albumArtist = "The Weeknd",
        album = "After Hours",
        duration = 215_000L,
        albumArtUri = null,
        playCount = 8,
        rating = Rating.GOOD.value,
        lastPlayed = 0L,
        albumId = 1,
        trackNumber = 2,
        year = 2020,
        dateAdded = 1001L,
        contentUri = null,
        lastModified = 0L,
    )

internal val previewSongHeartless =
    Song(
        id = 3,
        title = "Heartless",
        artist = "The Weeknd",
        albumArtist = "The Weeknd",
        album = "After Hours",
        duration = 198_000L,
        albumArtUri = null,
        playCount = 3,
        rating = Rating.NONE.value,
        lastPlayed = 0L,
        albumId = 1,
        trackNumber = 3,
        year = 2020,
        dateAdded = 1002L,
        contentUri = null,
        lastModified = 0L,
    )

internal val previewSongFaith =
    Song(
        id = 4,
        title = "Faith",
        artist = "The Weeknd",
        albumArtist = "The Weeknd",
        album = "After Hours",
        duration = 412_000L,
        albumArtUri = null,
        playCount = 1,
        rating = Rating.NONE.value,
        lastPlayed = 0L,
        albumId = 1,
        trackNumber = 4,
        year = 2020,
        dateAdded = 1003L,
        contentUri = null,
        lastModified = 0L,
    )

internal val previewSongStarboy =
    Song(
        id = 5,
        title = "Starboy",
        artist = "The Weeknd",
        albumArtist = "The Weeknd",
        album = "Starboy",
        duration = 230_000L,
        albumArtUri = null,
        playCount = 5,
        rating = Rating.NONE.value,
        lastPlayed = 0L,
        albumId = 2,
        trackNumber = 1,
        year = 2016,
        dateAdded = 1002L,
        contentUri = null,
        lastModified = 0L,
    )

internal val previewAlbums =
    listOf(
        Album(id = 1L, name = "After Hours", artist = "The Weeknd", songCount = 14, albumArtUri = null),
        Album(id = 2L, name = "Starboy", artist = "The Weeknd", songCount = 18, albumArtUri = null),
        Album(id = 3L, name = "Beauty Behind the Madness", artist = "The Weeknd", songCount = 14, albumArtUri = null),
    )

internal val previewArtist = Artist(name = "The Weeknd", songCount = 42, albumCount = 5, artUri = null)
