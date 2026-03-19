package com.jpishimwe.syncplayer.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme

// ── Preview data ───────────────────────────────────────────────────────────────

internal val previewSongs =
    listOf(
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
        ),
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
        ),
        Song(
            id = 3,
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
        ),
    )

internal val previewAlbums =
    listOf(
        Album(id = 1L, name = "After Hours", artist = "The Weeknd", songCount = 14, albumArtUri = null),
        Album(id = 2L, name = "Starboy", artist = "The Weeknd", songCount = 18, albumArtUri = null),
        Album(id = 3L, name = "Beauty Behind the Madness", artist = "The Weeknd", songCount = 14, albumArtUri = null),
    )

internal val previewArtist = Artist(name = "The Weeknd", songCount = 42, albumCount = 5, artUri = null)

/** Songs sub-tab, first song playing. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Songs sub-tab")
@Composable
private fun ArtistDetailSongsPreview() {
    SyncPlayerTheme(darkTheme = true) {
        ArtistDetailScreenContent(
            artist = previewArtist,
            artistName = "The Weeknd",
            songs = previewSongs,
            albums = previewAlbums,
            currentSongId = 1L,
            currentAlbumId = null,
            onSongClick = { _, _ -> },
            onAlbumClick = { _, _ -> },
            onPlayNext = {},
            onAddToQueue = {},
            onNavigateBack = {},
        )
    }
}

/** Empty state — no songs or albums loaded yet. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Empty state")
@Composable
private fun ArtistDetailEmptyPreview() {
    SyncPlayerTheme(darkTheme = true) {
        ArtistDetailScreenContent(
            artist = null,
            artistName = "Unknown Artist",
            songs = emptyList(),
            albums = emptyList(),
            currentSongId = null,
            currentAlbumId = null,
            onSongClick = { _, _ -> },
            onAlbumClick = { _, _ -> },
            onPlayNext = {},
            onAddToQueue = {},
            onNavigateBack = {},
        )
    }
}
