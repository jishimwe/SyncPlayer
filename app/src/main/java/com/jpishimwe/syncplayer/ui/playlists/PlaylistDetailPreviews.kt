package com.jpishimwe.syncplayer.ui.playlists

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme

// ── Preview data ───────────────────────────────────────────────────────────────

internal val previewPlaylistSongs =
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
            title = "Levitating",
            artist = "Dua Lipa  Feat. DaBaby",
            albumArtist = "Dua Lipa",
            album = "Future Nostalgia",
            duration = 203_000L,
            albumArtUri = null,
            playCount = 7,
            rating = Rating.GOOD.value,
            lastPlayed = 0L,
            albumId = 2,
            trackNumber = 1,
            year = 2020,
            dateAdded = 1001L,
            contentUri = null,
            lastModified = 0L,
        ),
        Song(
            id = 3,
            title = "As It Was",
            artist = "Harry Styles",
            albumArtist = "Harry Styles",
            album = "Harry's House",
            duration = 167_000L,
            albumArtUri = null,
            playCount = 4,
            rating = Rating.NONE.value,
            lastPlayed = 0L,
            albumId = 3,
            trackNumber = 1,
            year = 2022,
            dateAdded = 1002L,
            contentUri = null,
            lastModified = 0L,
        ),
        Song(
            id = 4,
            title = "Shivers",
            artist = "Ed Sheeran",
            albumArtist = "Ed Sheeran",
            album = "=",
            duration = 207_000L,
            albumArtUri = null,
            playCount = 2,
            rating = Rating.NONE.value,
            lastPlayed = 0L,
            albumId = 4,
            trackNumber = 1,
            year = 2021,
            dateAdded = 1003L,
            contentUri = null,
            lastModified = 0L,
        ),
    )

/**
 * Playlist with songs — the reorder drag handle renders but won't respond to drag
 * in preview; that's expected since gesture handling requires a real device/emulator.
 */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "With songs")
@Composable
private fun PlaylistDetailWithSongsPreview() {
    SyncPlayerTheme(darkTheme = true) {
        PlaylistDetailScreenContent(
            playlistName = "My Favourites",
            playlistSongs = previewPlaylistSongs,
            allSongs = previewPlaylistSongs,
            onSongClick = {},
            onShuffleClick = {},
            onPlayAllClick = {},
            onRemoveSong = {},
            onRemoveSongs = {},
            onReorderSongs = {},
            onAddSongs = {},
            onNavigateBack = {},
        )
    }
}

/** Empty playlist — just the header/sub-header with no songs. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Empty playlist")
@Composable
private fun PlaylistDetailEmptyPreview() {
    SyncPlayerTheme(darkTheme = true) {
        PlaylistDetailScreenContent(
            playlistName = "New Playlist",
            playlistSongs = emptyList(),
            allSongs = previewPlaylistSongs,
            onSongClick = {},
            onShuffleClick = {},
            onPlayAllClick = {},
            onRemoveSong = {},
            onRemoveSongs = {},
            onReorderSongs = {},
            onAddSongs = {},
            onNavigateBack = {},
        )
    }
}

/** Long playlist name — verifies single-line truncation in the header. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Long name")
@Composable
private fun PlaylistDetailLongNamePreview() {
    SyncPlayerTheme(darkTheme = true) {
        PlaylistDetailScreenContent(
            playlistName = "My Very Long and Detailed Playlist Name That Overflows",
            playlistSongs = previewPlaylistSongs,
            allSongs = previewPlaylistSongs,
            onSongClick = {},
            onShuffleClick = {},
            onPlayAllClick = {},
            onRemoveSong = {},
            onRemoveSongs = {},
            onReorderSongs = {},
            onAddSongs = {},
            onNavigateBack = {},
        )
    }
}
