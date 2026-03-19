package com.jpishimwe.syncplayer.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme

internal val previewSong =
    Song(
        id = 1L,
        title = "Fe3O4: FORWARD",
        artist = "NMIXX",
        albumArtist = "NMIXX",
        album = "Fe3O4",
        albumId = 1L,
        duration = 213000L,
        trackNumber = 1,
        year = 2024,
        dateAdded = 0L,
        contentUri = null,
        albumArtUri = null,
    )

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun NowPlayingScreenContentPreview() {
    SyncPlayerTheme(darkTheme = true) {
        NowPlayingScreenContent(
            uiState =
                PlayerUiState(
                    currentSong = previewSong,
                    playbackState = PlaybackState.PLAYING,
                    currentPosition = 65000L,
                    duration = 213000L,
                ),
            onEvent = {},
            onNavigateBack = {},
            formatTime = { ms -> "%d:%02d".format(ms / 60000, (ms / 1000) % 60) },
            rating = Rating.GOOD,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun NowPlayingScreenContentEmptyPreview() {
    SyncPlayerTheme(darkTheme = true) {
        NowPlayingScreenContent(
            uiState = PlayerUiState(),
            onEvent = {},
            onNavigateBack = {},
            formatTime = { ms -> "%d:%02d".format(ms / 60000, (ms / 1000) % 60) },
            rating = Rating.NONE,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun FavoriteButtonPreview() {
    SyncPlayerTheme(darkTheme = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            FavoriteButton(rating = Rating.NONE, onClick = {})
            FavoriteButton(rating = Rating.FAVORITE, onClick = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun StarRatingPreview() {
    SyncPlayerTheme(darkTheme = true) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(8.dp)) {
            StarRating(rating = Rating.NONE, onSetRating = {})
            StarRating(rating = Rating.FAIR, onSetRating = {})
            StarRating(rating = Rating.FAVORITE, onSetRating = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun TrackInfoPreview() {
    SyncPlayerTheme(darkTheme = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            TrackInfo(song = previewSong)
            Spacer(Modifier.height(8.dp))
            TrackInfo(song = null)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun AlbumArtworkPreview() {
    SyncPlayerTheme(darkTheme = true) {
        AlbumArtwork(
            song = previewSong,
            modifier =
                Modifier
                    .size(300.dp)
                    .clip(MaterialTheme.shapes.medium),
        )
    }
}
