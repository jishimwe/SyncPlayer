package com.jpishimwe.syncplayer.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme

private val previewAlbumSongs = listOf(
    previewSongBlindingLights,
    previewSongSaveYourTears,
    previewSongHeartless,
    previewSongFaith,
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