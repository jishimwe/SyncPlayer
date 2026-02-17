package com.jpishimwe.syncplayer.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jpishimwe.syncplayer.model.Song

@Composable
fun PlaylistSongItem(
    song: Song,
    onSongClick: () -> Unit,
    onRemove: () -> Unit,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier =
            modifier
                .clickable(onClick = onSongClick)
                .background(
                    if (isDragging) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        Color.Transparent
                    },
                ),
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    modifier = modifier,
                )
                Spacer(Modifier.width(8.dp))
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                )
            }
        },
        headlineContent = {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        },
        supportingContent = {
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        },
        tonalElevation = if (isDragging) 4.dp else 0.dp,
    )
}
