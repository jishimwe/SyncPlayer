package com.jpishimwe.syncplayer.ui.player

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.ui.components.PlayerControls
import com.jpishimwe.syncplayer.ui.components.QueueSheet
import com.jpishimwe.syncplayer.ui.components.SeekBar
import com.jpishimwe.syncplayer.ui.effect.BlurredBackground
import kotlinx.coroutines.launch
import kotlin.math.abs

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

            // --- Album art hero (swipe gestures with 3D tilt feedback) ---
            val swipeThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
            val artOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
            val scope = rememberCoroutineScope()

            // Max tilt angles and visual limits
            val maxTiltDeg = 12f
            val maxTranslationFraction = 0.15f // art shifts at most 15% of its width/height

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .pointerInput(Unit) {
                            val sizePx = size.width.toFloat()
                            detectDragGestures(
                                onDragStart = {
                                    scope.launch { artOffset.snapTo(Offset.Zero) }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        artOffset.snapTo(artOffset.value + dragAmount)
                                    }
                                },
                                onDragEnd = {
                                    val (dx, dy) = artOffset.value
                                    val triggered =
                                        if (abs(dx) > abs(dy) && abs(dx) > swipeThresholdPx) {
                                            if (dx < 0) {
                                                onEvent(PlayerEvent.SkipToNext)
                                            } else {
                                                onEvent(PlayerEvent.SkipToPrevious)
                                            }
                                            true
                                        } else if (abs(dy) > abs(dx) && abs(dy) > swipeThresholdPx) {
                                            if (dy < 0) {
                                                showQueue = true
                                            } else {
                                                onNavigateBack()
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    scope.launch {
                                        if (triggered) {
                                            // Tilt further + fade, then reset
                                            val exitOffset =
                                                Offset(
                                                    x = (dx / abs(dx).coerceAtLeast(1f)) * sizePx * 0.4f,
                                                    y = (dy / abs(dy).coerceAtLeast(1f)) * sizePx * 0.4f,
                                                )
                                            artOffset.animateTo(exitOffset, tween(180))
                                            artOffset.snapTo(Offset.Zero)
                                        } else {
                                            artOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.6f, stiffness = 400f))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        artOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.6f, stiffness = 400f))
                                    }
                                },
                            )
                        },
            ) {
                val dx = artOffset.value.x
                val dy = artOffset.value.y
                val dragProgress = (artOffset.value.getDistance() / (swipeThresholdPx * 2f)).coerceIn(0f, 1f)

                // 3D tilt: rotationY from horizontal drag, rotationX from vertical drag (inverted)
                val rotY = (dx / swipeThresholdPx).coerceIn(-1f, 1f) * maxTiltDeg
                val rotX = -(dy / swipeThresholdPx).coerceIn(-1f, 1f) * maxTiltDeg

                // Dampened translation — art shifts subtly, not 1:1
                val txDampened = dx * maxTranslationFraction
                val tyDampened = dy * maxTranslationFraction

                // Scale down slightly as you drag
                val artScale = 1f - dragProgress * 0.08f
                val artAlpha = 1f - dragProgress * 0.4f

                AlbumArtwork(
                    song = uiState.currentSong,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = txDampened
                                translationY = tyDampened
                                rotationY = rotY
                                rotationX = rotX
                                scaleX = artScale
                                scaleY = artScale
                                alpha = artAlpha
                                cameraDistance = 12f * density
                            },
                )
            }

            Spacer(Modifier.height(0.dp))

            // --- Seek bar ---
            SeekBar(
                currentPosition = uiState.currentPosition,
                playbackState = uiState.playbackState,
                color = dominantColor,
                animateColor = animatedBgColor,
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
