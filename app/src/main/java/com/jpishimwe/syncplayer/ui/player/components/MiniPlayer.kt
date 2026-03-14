package com.jpishimwe.syncplayer.ui.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jpishimwe.syncplayer.model.PlaybackState
import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.frostedGlass
import com.jpishimwe.syncplayer.ui.theme.frostedGlassRendered
import com.jpishimwe.syncplayer.ui.theme.myAccentColor

// Referenced by other files for bottom padding calculations
val MiniPlayerHeight = 96.dp
val MiniPlayerBottomMargin = 8.dp
val MiniPlayerPeek = MiniPlayerHeight + MiniPlayerBottomMargin

private val PillShape = RoundedCornerShape(16.dp)
private val InfoBackgroundShape = RoundedCornerShape(8.dp)

@Composable
fun MiniPlayer(
    uiState: PlayerUiState,
    onEvent: (PlayerEvent) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = uiState.currentSong != null
    val isPlaying = uiState.playbackState == PlaybackState.PLAYING

    // Accent text when a song is loaded (playing or paused)
    val textColor =
        if (isActive && (isPlaying || uiState.playbackState == PlaybackState.PAUSED)) {
            myAccentColor
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val borderBrush =
        Brush.linearGradient(
            colors = listOf(myAccentColor.copy(alpha = 0.24f), myAccentColor.copy(alpha = 0.75f), myAccentColor.copy(alpha = 0.24f)),
        )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .clip(PillShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Layer 0 -> frosted background
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(MiniPlayerHeight)
                    .border(BorderStroke(1.dp, borderBrush), PillShape)
                    .clip(PillShape)
                    .frostedGlassRendered(alpha = .5f, blurRadius = 60.dp),
        )

        // Layer 1 -> darker frosted background for the information row
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(56.dp)
                    .clip(InfoBackgroundShape)
                    .frostedGlass(
                        alpha = .9f,
//                        color = MaterialTheme.colorScheme.surface,
                    ),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art — 56x56 with rounded corners
            AsyncImage(
                model = uiState.currentSong?.albumArtUri,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(4.dp)),
            )

            Spacer(Modifier.width(12.dp))

            // Track info — title / album · artist
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = uiState.currentSong?.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        buildSubtitle(
                            album = uiState.currentSong?.album,
                            artist = uiState.currentSong?.artist,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) textColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Controls: skip prev | play/pause | skip next
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // Skip previous
                IconButton(
                    onClick = { onEvent(PlayerEvent.SkipToPrevious) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Play/Pause — accent-tinted background when active
                IconButton(
                    onClick = { onEvent(PlayerEvent.PlayPause) },
                    modifier = Modifier.size(54.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isActive) {
                                        myAccentColor.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp),
                            tint = if (isActive) myAccentColor else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Skip next
                IconButton(
                    onClick = { onEvent(PlayerEvent.SkipToNext) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun buildSubtitle(
    album: String?,
    artist: String?,
): String {
    val parts =
        listOfNotNull(
            album?.takeIf { it.isNotBlank() },
            artist?.takeIf { it.isNotBlank() },
        )
    return parts.joinToString(" · ")
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun MiniPlayerPreview() {
    SyncPlayerTheme(darkTheme = true) {
        val fakeSong =
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            // No song loaded
            MiniPlayer(
                uiState = PlayerUiState(),
                onEvent = {},
                onClick = {},
            )

            Spacer(Modifier.height(8.dp))
            // Paused
            MiniPlayer(
                uiState = PlayerUiState(currentSong = fakeSong, playbackState = PlaybackState.PAUSED),
                onEvent = {},
                onClick = {},
            )
            Spacer(Modifier.height(8.dp))
            // Playing
            MiniPlayer(
                uiState = PlayerUiState(currentSong = fakeSong, playbackState = PlaybackState.PLAYING),
                onEvent = {},
                onClick = {},
            )
        }
    }
}
