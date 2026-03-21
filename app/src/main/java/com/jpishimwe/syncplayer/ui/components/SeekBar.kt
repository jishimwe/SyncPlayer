package com.jpishimwe.syncplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.ui.player.PlaybackState
import com.jpishimwe.syncplayer.ui.theme.gradientBorderStroke
import java.util.Locale

private val TrackHeight = 12.dp
private val TrackShape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
private val ThumbWidth = 8.dp
private val ThumbHeight = 20.dp
private val ThumbShape = RoundedCornerShape(3.dp)

@Composable
fun SeekBar(
    currentPosition: Long,
    duration: Long,
    color: Color,
    animateColor: Color,
    playbackState: PlaybackState,
    onSeek: (Long) -> Unit,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    val safeDuration = duration.coerceAtLeast(1L)
    val displayFraction =
        if (isDragging) {
            dragFraction
        } else {
            (currentPosition.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
        }

    val displayPosition =
        if (isDragging) {
            (dragFraction * safeDuration).toLong().coerceIn(0L, safeDuration)
        } else {
            currentPosition
        }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Custom track + thumb
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(ThumbHeight) // Touch target height
                    .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(safeDuration) {
                        detectTapGestures { offset ->
                            val fraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                            onSeek((fraction * safeDuration).toLong())
                        }
                    }.pointerInput(safeDuration) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                dragFraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                onSeek((dragFraction * safeDuration).toLong())
                                isDragging = false
                            },
                            onDragCancel = {
                                isDragging = false
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragFraction = (dragFraction + dragAmount / trackWidthPx).coerceIn(0f, 1f)
                            },
                        )
                    },
            contentAlignment = Alignment.CenterStart,
        ) {
            // Inactive track (full width)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .gradientBorderStroke(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                                ),
                            shape = TrackShape,
                        ).height(TrackHeight)
                        .clip(TrackShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            )

            // Active track (filled portion)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction = displayFraction + .008f)
                        .height(TrackHeight)
                        .clip(TrackShape)
                        .then(
                            if (playbackState == PlaybackState.PLAYING) {
                                Modifier.background(
//                                    LocalExtendedColorScheme.current.accentColor.color
//                                        .copy(alpha = 0.4f),
                                    color,
                                )
                            } else {
                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            },
                        ),
            )

            // Thumb — small pill positioned at the progress edge
            Box(
                modifier =
                    Modifier
                        .padding(
                            start =
                                with(LocalDensity.current) {
                                    // Position thumb at the fraction point, centered on its width
                                    val thumbWidthPx = ThumbWidth.toPx()
                                    val offsetPx =
                                        (displayFraction * trackWidthPx - thumbWidthPx / 2)
                                            .coerceIn(0f, (trackWidthPx - thumbWidthPx).coerceAtLeast(0f))
                                    offsetPx.toDp()
                                },
                        ).width(ThumbWidth)
                        .height(ThumbHeight)
                        .clip(ThumbShape)
//                        .background(myAccentColor.copy(alpha = 0.7f)),
                        .then(
                            if (playbackState == PlaybackState.PLAYING) {
//                                Modifier.background(LocalExtendedColorScheme.current.accentColor.color)
                                Modifier.background(animateColor)
                            } else {
                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                            },
                        ).border(1.dp, MaterialTheme.colorScheme.surfaceVariant, ThumbShape),
            )
        }

        // Timestamps
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(displayPosition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = formatTime(safeDuration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun thisFormatTime(ms: Long): String {
    val totalSeconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = (ms / (1000 * 60 * 60))
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, totalSeconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, totalSeconds)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
fun SeekBarPlayingPreview() {
    SeekBar(
        currentPosition = 65000L,
        playbackState = PlaybackState.PLAYING,
        duration = 213000L,
        onSeek = {},
        formatTime = { thisFormatTime(it) },
        color = Color.Red,
        animateColor = Color.Green,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
fun SeekBarPlayingPaused() {
    SeekBar(
        currentPosition = 65000L,
        playbackState = PlaybackState.PAUSED,
        duration = 213000L,
        onSeek = {},
        formatTime = { thisFormatTime(it) },
        color = Color.Red,
        animateColor = Color.Green,
    )
}
