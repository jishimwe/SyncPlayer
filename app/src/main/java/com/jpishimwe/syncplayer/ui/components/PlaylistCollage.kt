package com.jpishimwe.syncplayer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/**
 * 2×2 grid of album art thumbnails at 56×56dp total (28×28dp per cell).
 *
 * Accepts up to 4 album art URIs. Any missing slots show a grey background.
 * If [artUris] is empty the entire box shows a centred music note icon instead,
 * matching the single-image fallback used by SongItem.
 */
@Composable
fun PlaylistCollage(
    artUris: List<String?>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(56.dp).clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (artUris.isEmpty()) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.6f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // Pad to exactly 4 entries so index access is safe
            val cells = (artUris + listOf(null, null, null, null)).take(4)
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageCell(uri = cells[0], modifier = Modifier.weight(1f).fillMaxSize())
                    CollageCell(uri = cells[1], modifier = Modifier.weight(1f).fillMaxSize())
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageCell(uri = cells[2], modifier = Modifier.weight(1f).fillMaxSize())
                    CollageCell(uri = cells[3], modifier = Modifier.weight(1f).fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun CollageCell(
    uri: String?,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        error = {
            Box(
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}
