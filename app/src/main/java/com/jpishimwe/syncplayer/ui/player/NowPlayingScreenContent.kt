package com.jpishimwe.syncplayer.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlaylistAddCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jpishimwe.syncplayer.R
import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.RepeatMode
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.player.components.PlayerControls
import com.jpishimwe.syncplayer.ui.player.components.QueueSheet
import com.jpishimwe.syncplayer.ui.player.components.SeekBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreenContent(
    uiState: PlayerUiState,
    onEvent: (PlayerEvent) -> Unit,
    onNavigateBack: () -> Unit,
    formatTime: (Long) -> String,
    rating: Rating,
) {
    var showQueue by remember { mutableStateOf(false) }

    if (showQueue) {
        QueueSheet(
            queue = uiState.queue,
            currentIndex = uiState.currentQueueIndex,
            onDismiss = { showQueue = false },
            onSongClick = { index ->
                onEvent(PlayerEvent.SeekToQueueItem(index))
                onEvent(PlayerEvent.PlayPause)
            },
            onRemove = { id -> onEvent(PlayerEvent.RemoveFromQueue(id)) },
            onReorder = { id, position -> onEvent(PlayerEvent.ReorderQueue(id, position)) },
            formatTime = formatTime,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = { BackButon(onClick = onNavigateBack) },
                actions = { QueueButton(onClick = { showQueue = true }) },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.5f))

            AlbumArtwork(
                song = uiState.currentSong,
                modifier = Modifier.size(200.dp),
            )

            Spacer(Modifier.height(32.dp))

            TrackInfo(song = uiState.currentSong)

            Spacer(Modifier.height(24.dp))

            SeekBar(
                currentPosition = uiState.currentPosition,
                duration = uiState.duration,
                onSeek = { onEvent(PlayerEvent.SeekTo(it)) },
                formatTime = formatTime,
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ShuffleButton(
                    isEnabled = uiState.isShuffleEnabled,
                    onClick = { onEvent(PlayerEvent.ToggleShuffle) },
                )
                FavoriteButton(
                    rating = rating,
                    onClick = {
                        if (rating == Rating.NONE) {
                            onEvent(PlayerEvent.SetRating(Rating.FAVORITE))
                        } else {
                            onEvent(PlayerEvent.SetRating(Rating.NONE))
                        }
                    },
                )
                RepeatButton(
                    mode = uiState.repeatMode,
                    onClick = { onEvent(PlayerEvent.ToggleRepeat) },
                )
            }

            StarRating(
                rating = rating,
                onSetRating = { stars -> onEvent(PlayerEvent.SetRating(stars)) },
            )

            Spacer(Modifier.height(32.dp))

            PlayerControls(
                playbackState = uiState.playbackState,
                onPlayPause = { onEvent(PlayerEvent.PlayPause) },
                onSkipToNext = { onEvent(PlayerEvent.SkipToNext) },
                onSkipToPrevious = { onEvent(PlayerEvent.SkipToPrevious) },
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun FavoriteButton(
    rating: Rating,
    onClick: () -> Unit,
) {
    val isFavorite = rating == Rating.FAVORITE
    IconButton(onClick = onClick) {
        Icon(
            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = if (isFavorite) MaterialTheme.colorScheme.error else LocalContentColor.current,
        )
    }
}

@Composable
fun StarRating(
    rating: Rating,
    onSetRating: (stars: Rating) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Rating.entries
            .filter {
                it != Rating.NONE
            }.forEach { star ->
                IconButton(onClick = { onSetRating(if (rating == star) Rating.NONE else star) }) {
                    Icon(
                        if (star.value <= rating.value) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "${star.value} stars",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
    }
}

@Composable
fun RepeatButton(
    mode: RepeatMode,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        when (mode) {
            RepeatMode.ALL -> Icon(Icons.Default.RepeatOn, contentDescription = "Repeat all")
            RepeatMode.ONE -> Icon(Icons.Default.RepeatOne, contentDescription = "Repeat one")
            RepeatMode.OFF -> Icon(Icons.Default.Repeat, contentDescription = "Repeat off")
        }
    }
}

@Composable
fun ShuffleButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick) {
        if (isEnabled) {
            Icon(Icons.Default.ShuffleOn, contentDescription = "Shuffle on")
        } else {
            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle off")
        }
    }
}

@Composable
fun TrackInfo(song: Song?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = song?.title ?: "", style = MaterialTheme.typography.headlineMedium)
        Text(text = song?.album ?: "-", style = MaterialTheme.typography.headlineSmall)
        Text(text = song?.artist ?: "", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun AlbumArtwork(
    song: Song?,
    modifier: Modifier,
) {
    Surface(modifier = modifier) {
        SubcomposeAsyncImage(
            model = song?.albumArtUri,
            contentDescription = "Album art",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth(),
            loading = {
                CircularProgressIndicator()
                Image(
                    painter = painterResource(R.drawable.album_default_foreground),
                    contentDescription = "Loading album art",
                )
            },
            error = {
                Image(
                    painter = painterResource(R.drawable.album_default_foreground),
                    contentDescription = "No album art",
                )
            },
        )
    }
}

@Composable
fun QueueButton(onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(Icons.Default.PlaylistAddCircle, contentDescription = "Queue")
    }
}

@Composable
fun BackButon(onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
    }
}
