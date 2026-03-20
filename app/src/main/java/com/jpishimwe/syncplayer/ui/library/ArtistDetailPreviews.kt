package com.jpishimwe.syncplayer.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme

private val previewSongs = listOf(
    previewSongBlindingLights,
    previewSongSaveYourTears,
    previewSongStarboy,
)

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