package com.jpishimwe.syncplayer.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.model.PlaybackState
import com.jpishimwe.syncplayer.model.RepeatMode
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.myAccentColor
import com.jpishimwe.syncplayer.ui.theme.myFrostedGlassSurface
import com.jpishimwe.syncplayer.ui.theme.tertiaryContainerDark

// --- Shape constants ---
private val PillShape = RoundedCornerShape(20.dp)
private val ButtonShape = RoundedCornerShape(12.dp)
private val PlayButtonShape = RoundedCornerShape(16.dp)

// --- Size constants ---
private val PillHeight = 56.dp
private val SmallButtonSize = 48.dp // repeat, shuffle
private val MediumButtonSize = 56.dp // prev, next — overflows pill
private val PlayButtonSize = 72.dp // play — largest, overflows pill

// --- Glass fills ---
private val GlassFill = myFrostedGlassSurface.copy(alpha = 0.95f)
private val GlassFillLight = tertiaryContainerDark.copy(alpha = 0.45f)
private val BorderColor = Color.White.copy(alpha = 0.12f)

@Composable
fun PlayerControls(
    playbackState: PlaybackState,
    repeatMode: RepeatMode,
    isShuffleEnabled: Boolean,
    onPlayPause: () -> Unit,
    onSkipToNext: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Layer 0 — Background pill (shorter than the buttons)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(PillHeight)
                    .clip(PillShape)
                    .background(GlassFillLight),
        )

        // Layer 1 — Buttons row (overflows the pill vertically)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Repeat — small, no border, glass bg
            GlassButton(
                size = SmallButtonSize,
                showBorder = false,
                onClick = onToggleRepeat,
            ) {
                when (repeatMode) {
                    RepeatMode.ALL -> {
                        Icon(
                            Icons.Default.RepeatOn,
                            contentDescription = "Repeat all",
                            tint = myAccentColor,
                        )
                    }

                    RepeatMode.ONE -> {
                        Icon(
                            Icons.Default.RepeatOne,
                            contentDescription = "Repeat one",
                            tint = myAccentColor,
                        )
                    }

                    RepeatMode.OFF -> {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Repeat off",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Skip previous — medium, bordered, glass bg
            GlassButton(
                size = MediumButtonSize,
                showBorder = true,
                onClick = onSkipToPrevious,
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Skip previous",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Play/Pause — diamond shape with shadow layers
            DiamondPlayButton(
                isPlaying = playbackState == PlaybackState.PLAYING,
                onClick = onPlayPause,
            )

            // Skip next — medium, bordered, glass bg
            GlassButton(
                size = MediumButtonSize,
                showBorder = true,
                onClick = onSkipToNext,
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Skip next",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Shuffle — small, no border, glass bg
            GlassButton(
                size = SmallButtonSize,
                showBorder = false,
                onClick = onToggleShuffle,
            ) {
                if (isShuffleEnabled) {
                    Icon(
                        Icons.Default.ShuffleOn,
                        contentDescription = "Shuffle on",
                        tint = myAccentColor,
                    )
                } else {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle off",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Rounded-square button with glass fill and optional border.
 * Used for repeat/shuffle (no border) and prev/next (with border).
 */
@Composable
private fun GlassButton(
    size: Dp,
    showBorder: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(ButtonShape)
                .background(GlassFill)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Border as overlay so it draws on top of background, not under content
        if (showBorder) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .border(1.dp, BorderColor, ButtonShape),
            )
        }
        content()
    }
}

/**
 * Play/Pause button: two imbricated rounded squares rotated 45°.
 *
 * Back layer:  rotated 45°, slightly larger, lower opacity — ghost effect
 * Front layer: rotated 45°, drop shadow, higher opacity — main button
 * Icon:        NOT rotated (counter-rotated -45° to stay upright)
 */
@Composable
private fun DiamondPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(PlayButtonSize)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Back layer — ghost diamond (larger, lower alpha)
        Box(
            modifier =
                Modifier
                    .size(PlayButtonSize)
                    .rotate(45f)
                    .clip(PlayButtonShape)
                    .background(GlassFill.copy(alpha = 0.3f)),
        )

        // Front layer — main diamond with drop shadow
        Box(
            modifier =
                Modifier
                    .size(PlayButtonSize)
                    .shadow(
                        elevation = 8.dp,
                        shape = PlayButtonShape,
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = Color.Black.copy(alpha = 0.4f),
                    ).clip(PlayButtonShape)
                    .background(GlassFill),
        )

        // Icon — stays upright (not rotated)
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            modifier = Modifier.size((80).dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun PlayerControlsIdlePreview() {
    SyncPlayerTheme(darkTheme = true) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 24.dp),
        ) {
            PlayerControls(
                playbackState = PlaybackState.IDLE,
                repeatMode = RepeatMode.OFF,
                isShuffleEnabled = false,
                onPlayPause = {},
                onSkipToNext = {},
                onSkipToPrevious = {},
                onToggleRepeat = {},
                onToggleShuffle = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun PlayerControlsPlayingPreview() {
    SyncPlayerTheme(darkTheme = true) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 24.dp),
        ) {
            PlayerControls(
                playbackState = PlaybackState.PLAYING,
                repeatMode = RepeatMode.ALL,
                isShuffleEnabled = true,
                onPlayPause = {},
                onSkipToNext = {},
                onSkipToPrevious = {},
                onToggleRepeat = {},
                onToggleShuffle = {},
            )
        }
    }
}
