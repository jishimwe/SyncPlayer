package com.jpishimwe.syncplayer.ui.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.room.util.TableInfo
import com.jpishimwe.syncplayer.R
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.myAccentColor
import com.jpishimwe.syncplayer.ui.theme.noRippleClickable

sealed interface ArtistPlaybackState {
    // Name + overflow menu only
    data object Default : ArtistPlaybackState

    // Name + play icon + overflow menu
    data object Playable : ArtistPlaybackState

    // Accent tint, play icon + name + overflow menu
    data object Paused : ArtistPlaybackState

    // Accent tint, pause icon + name + overflow menu
    data object Playing : ArtistPlaybackState
}

/**
 * Artist portrait card: circular image with a frosted-glass pill overlaid
 * at the bottom. The pill content adapts to [playbackState].
 *
 * Layout: Box stacks [CircularArtistImage] and [FrostedGlassPill] — pill
 * sits at BottomCenter, partially overlapping the image.
 */
@Composable
fun ArtistItem(
    artistName: String,
    imageUri: String?,
    playbackState: ArtistPlaybackState,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageSize: Dp = 152.dp,
) {
    val isActive =
        playbackState is ArtistPlaybackState.Playing ||
            playbackState is ArtistPlaybackState.Paused
    val iconTint = if (isActive) myAccentColor else MaterialTheme.colorScheme.onSurface
    val textColor = if (isActive) myAccentColor else MaterialTheme.colorScheme.onSurface

    Box(
        modifier =
            modifier
                .width(imageSize)
                .noRippleClickable { onClick() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Artist portrait
        CircularArtistImage(
            imageUri = imageUri,
            artistName = artistName,
            size = imageSize,
            isPlaying = isActive,
            modifier = Modifier.padding(bottom = 20.dp), // leave room for pill overlap
        )

        // Pill overlaid at bottom
        FrostedGlassPill(
            modifier = Modifier.padding(horizontal = 4.dp),
            active = isActive,
        ) {
            // Play/Pause icon — shown for all states except Default
            if (playbackState != ArtistPlaybackState.Default) {
                Icon(
                    imageVector =
                        if (playbackState is ArtistPlaybackState.Playing) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                    contentDescription =
                        if (playbackState is ArtistPlaybackState.Playing) {
                            "Pause"
                        } else {
                            "Play"
                        },
                    tint = iconTint,
                    modifier =
                        Modifier
                            .size(20.dp)
                            .noRippleClickable { onPlayPause() },
                )
                Spacer(Modifier.width(6.dp))
            }

            // Artist name
            Text(
                text = artistName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(4.dp))

            // Overflow menu
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = iconTint,
                modifier =
                    Modifier
                        .size(18.dp)
                        .noRippleClickable { onMenuClick() },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun ArtistItemPreview2() {
    val states =
        listOf(
            ArtistPlaybackState.Default,
            ArtistPlaybackState.Playable,
            ArtistPlaybackState.Playing,
            ArtistPlaybackState.Paused,
        )
    SyncPlayerTheme(darkTheme = true) {
        FlowRow(
            modifier = Modifier.padding(16.dp),
            maxItemsInEachRow = 2,
        ) {
            states.forEach { state ->
                ArtistItem(
                    artistName = "NMIXX",
                    imageUri = "android.resource://com.jpishimwe.syncplayer/${R.drawable.artist_art}",
                    playbackState = state,
                    onClick = {},
                    onPlayPause = {},
                    onMenuClick = {},
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
