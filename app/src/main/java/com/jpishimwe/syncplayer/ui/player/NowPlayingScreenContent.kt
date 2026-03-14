package com.jpishimwe.syncplayer.ui.player

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.jpishimwe.syncplayer.R
import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.player.components.BlurredBackground
import com.jpishimwe.syncplayer.ui.player.components.PlayerControls
import com.jpishimwe.syncplayer.ui.player.components.QueueSheet
import com.jpishimwe.syncplayer.ui.player.components.SeekBar
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.myAccentColor

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
            onClearQueue = { onEvent(PlayerEvent.ClearQueue) },
        )
    }

    // --- Palette color extraction from album art ---
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color.Transparent) }
    val animatedBgColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 600),
        label = "bgTint",
    )

    LaunchedEffect(uiState.currentSong?.albumArtUri) {
        val uri = uiState.currentSong?.albumArtUri
        if (uri == null) {
            dominantColor = Color.Transparent
            return@LaunchedEffect
        }
        try {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(uri)
                    .allowHardware(false) // Palette needs software bitmap
                    .build()
            val result = coil3.SingletonImageLoader.get(context).execute(request)
            val bitmap: Bitmap? = result.image?.toBitmap()
            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                val swatch = palette.lightVibrantSwatch ?: palette.vibrantSwatch ?: palette.darkVibrantSwatch
//                    palette.darkVibrantSwatch
//                        ?: palette.darkMutedSwatch
//                        ?: palette.mutedSwatch
                dominantColor =
                    if (swatch != null) {
                        Color(swatch.rgb).copy(alpha = 0.3f)
                    } else {
                        Color.Transparent
                    }
            }
        } catch (e: Exception) {
            dominantColor = Color.Transparent
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
//                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
    ) {
        // Layer 0 — Blurred screenshot of previous screen
        BlurredBackground(blurRadiusPx = 60f, scrimAlpha = 0.7f)

        // Layer 1 — Palette color tint gradient on top of blur
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    animatedBgColor,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                ),
                        ),
                    ),
        )

        // Layer 2 — Content
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // --- Top bar: back chevron + queue icon ---
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // --- Album art hero ---
            AlbumArtwork(
                song = uiState.currentSong,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
//                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            )

            Spacer(Modifier.height(0.dp))

            // --- Seek bar ---
            SeekBar(
                currentPosition = uiState.currentPosition,
                duration = uiState.duration,
                onSeek = { onEvent(PlayerEvent.SeekTo(it)) },
                formatTime = formatTime,
            )

            Spacer(Modifier.height(12.dp))

            // --- Star rating + heart favorite ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StarRating(
                    rating = rating,
                    onSetRating = { stars -> onEvent(PlayerEvent.SetRating(stars)) },
                )

                Spacer(Modifier.width(12.dp))

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
            }

            Spacer(Modifier.height(32.dp))

            // --- Track info: title / album / artist ---
            TrackInfo(song = uiState.currentSong)

            Spacer(Modifier.height(32.dp))

            // --- Controls pill: repeat | prev | play | next | shuffle ---
            PlayerControls(
                playbackState = uiState.playbackState,
                repeatMode = uiState.repeatMode,
                isShuffleEnabled = uiState.isShuffleEnabled,
                onPlayPause = { onEvent(PlayerEvent.PlayPause) },
                onSkipToNext = { onEvent(PlayerEvent.SkipToNext) },
                onSkipToPrevious = { onEvent(PlayerEvent.SkipToPrevious) },
                onToggleRepeat = { onEvent(PlayerEvent.ToggleRepeat) },
                onToggleShuffle = { onEvent(PlayerEvent.ToggleShuffle) },
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun FavoriteButton(
    rating: Rating,
    onClick: () -> Unit,
) {
    val isFavorite = rating == Rating.FAVORITE
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(
            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = myAccentColor,
            modifier = Modifier.size(40.dp),
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
            .filter { it != Rating.NONE }
            .forEach { star ->
                IconButton(onClick = { onSetRating(if (rating == star) Rating.NONE else star) }) {
                    Icon(
                        if (star.value <= rating.value) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "${star.value} stars",
                        tint = myAccentColor,
                    )
                }
            }
    }
}

@Composable
fun TrackInfo(song: Song?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val titleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        val albumShape = RoundedCornerShape(4.dp)
        val artistShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(titleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = song?.title ?: "",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(albumShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = song?.album ?: "-",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(4.dp))

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(artistShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = song?.artist ?: "",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AlbumArtwork(
    song: Song?,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = song?.albumArtUri,
        contentDescription = "Album art",
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Image(
                    painter = painterResource(R.drawable.album_default_foreground),
                    contentDescription = "Loading album art",
                )
            }
        },
        error = {
            Image(
                painter = painterResource(R.drawable.album_default_foreground),
                contentDescription = "No album art",
            )
        },
    )
}

// region Previews

private val previewSong =
    Song(
        id = 1L,
        title = "Fe3O4: FORWARD",
        artist = "NMIXX",
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
                    playbackState = com.jpishimwe.syncplayer.model.PlaybackState.PLAYING,
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
