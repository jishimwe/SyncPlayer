package com.jpishimwe.syncplayer.ui.player

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.ui.components.PlayerControls
import com.jpishimwe.syncplayer.ui.components.QueueSheet
import com.jpishimwe.syncplayer.ui.components.SeekBar
import com.jpishimwe.syncplayer.ui.theme.BlurredBackground

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
                if (index == uiState.currentQueueIndex) {
                    showQueue = false
                } else {
                    onEvent(PlayerEvent.SeekToQueueItem(index))
                }
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
