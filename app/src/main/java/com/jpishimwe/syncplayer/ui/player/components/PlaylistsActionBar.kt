package com.jpishimwe.syncplayer.ui.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.frostedGlassRendered
import com.jpishimwe.syncplayer.ui.theme.myAccentColor

/**
 * Sticky action bar for the Playlists tab.
 *
 * Visually identical to [SortFilterBar] — same frosted glass background and
 * gradient accent border — but carries no sort controls. The only action is a
 * single "create playlist" button on the trailing edge.
 *
 * Usage: place as a `stickyHeader` in the playlists [LazyColumn], or above a
 * [LazyColumn] with an `onSizeChanged` content-padding offset (grid pattern).
 */
@Composable
fun PlaylistsActionBar(
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    myAccentColor.copy(alpha = 0.24f),
                    myAccentColor.copy(alpha = 0.75f),
                    myAccentColor.copy(alpha = 0.24f),
                ),
        )
    val barShape = RoundedCornerShape(8.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .border(BorderStroke(1.dp, borderBrush), barShape)
                .clip(barShape),
    ) {
        // Frosted glass background layer — must be first so content renders on top
        Box(modifier = Modifier.matchParentSize().frostedGlassRendered())

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left side intentionally empty — reserved for future search/filter
            Spacer(modifier = Modifier.weight(1f))

            // Create playlist
            IconButton(onClick = onCreatePlaylist) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "New playlist",
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
        PlaylistsActionBar(onCreatePlaylist = {})
    }
}
