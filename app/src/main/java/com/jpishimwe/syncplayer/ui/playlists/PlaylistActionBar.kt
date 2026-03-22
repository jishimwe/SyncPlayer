package com.jpishimwe.syncplayer.ui.playlists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.frostedGlassRendered
import com.jpishimwe.syncplayer.ui.theme.gradientBorderStroke

private val BarShape = RoundedCornerShape(8.dp)

/**
 * Sticky action bar for the playlist detail screen.
 * Shows song count + duration, shuffle and play-all buttons.
 */
@Composable
internal fun PlaylistActionBar(
    label: String,
    onShuffleClick: () -> Unit,
    onPlayAllClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .gradientBorderStroke()
                .clip(BarShape),
    ) {
        // Frosted glass background layer
        Box(modifier = Modifier.matchParentSize().frostedGlassRendered())

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )

            Spacer(Modifier.weight(1f))

            // Shuffle button
            IconButton(onClick = onShuffleClick) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Play all button
            IconButton(onClick = onPlayAllClick) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play all",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun PlaylistsActionBarPreview() {
    SyncPlayerTheme(darkTheme = true) {
        PlaylistActionBar(
            label = "35 songs · 211 minutes",
            onShuffleClick = {},
            onPlayAllClick = {},
        )
    }
}
