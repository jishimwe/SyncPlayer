package com.jpishimwe.syncplayer.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme

// ── Preview data ───────────────────────────────────────────────────────────────

internal val previewAlbumSongs =
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
        ),
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
        ),
    )

/** Default — no song currently playing. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Default")
@Composable
private fun AlbumDetailDefaultPreview() {
    SyncPlayerTheme(darkTheme = true) {
        AlbumDetailScreenContent(
            albumName = "After Hours",
            songs = previewAlbumSongs,
            currentSongId = null,
            onSongClick = { _, _ -> },
            onPlayNext = {},
            onAddToQueue = {},
            onNavigateToArtist = {},
            onNavigateBack = {},
        )
    }
}

/** Playing state — first track highlighted with accent color. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Song playing")
@Composable
private fun AlbumDetailPlayingPreview() {
    SyncPlayerTheme(darkTheme = true) {
        AlbumDetailScreenContent(
            albumName = "After Hours",
            songs = previewAlbumSongs,
            currentSongId = 1L,
            onSongClick = { _, _ -> },
            onPlayNext = {},
            onAddToQueue = {},
            onNavigateToArtist = {},
            onNavigateBack = {},
        )
    }
}

/** Empty state — album has no songs yet (e.g. still loading). */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Empty state")
@Composable
private fun AlbumDetailEmptyPreview() {
    SyncPlayerTheme(darkTheme = true) {
        AlbumDetailScreenContent(
            albumName = "Unknown Album",
            songs = emptyList(),
            currentSongId = null,
            onSongClick = { _, _ -> },
            onPlayNext = {},
            onAddToQueue = {},
            onNavigateToArtist = {},
            onNavigateBack = {},
        )
    }
}
