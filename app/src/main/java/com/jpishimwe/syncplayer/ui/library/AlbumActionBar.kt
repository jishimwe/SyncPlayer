package com.jpishimwe.syncplayer.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.theme.frostedGlassRendered

private val BarShape = RoundedCornerShape(8.dp)

/**
 * Sticky action bar for the album detail screen.
 * Shows song count, shuffle and play-all buttons inside a frosted-glass container.
 */
@Composable
internal fun AlbumActionBar(
    songCount: Int,
    songs: List<Song>,
    accentBorderBrush: Brush,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .border(BorderStroke(1.dp, accentBorderBrush), BarShape)
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
                // Song count label
                Text(
                    text = "$songCount songs",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )

                Spacer(Modifier.weight(1f))

                // Shuffle button
                IconButton(onClick = { if (songs.isNotEmpty()) onSongClick(songs.shuffled(), 0) }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Play all button
                IconButton(onClick = { if (songs.isNotEmpty()) onSongClick(songs, 0) }) {
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
}
