package com.jpishimwe.syncplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jpishimwe.syncplayer.R
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.myAccentColor

/**
 * Circular artist image. Falls back to a Person icon on a surface-variant
 * background when [imageUri] is null or fails to load.
 *
 * [size] defaults to 56.dp — callers can override for list vs. detail contexts.
 */
@Composable
fun CircularArtistImage(
    imageUri: String?,
    artistName: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    isPlaying: Boolean = false,
) {
    SubcomposeAsyncImage(
        model = imageUri,
        contentDescription = "Artist image for $artistName",
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .then(
                    if (isPlaying) {
                        Modifier.border(1.5.dp, myAccentColor, CircleShape)
                    } else {
                        Modifier
                    },
                ),
        error = {
            Box(
                modifier =
                    Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size * 0.55f),
                )
            }
        },
        loading = {
            Box(
                modifier =
                    Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun CircularArtistImagePreview() {
    SyncPlayerTheme(darkTheme = true) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Default
            CircularArtistImage(
                imageUri = "android.resource://com.jpishimwe.syncplayer/${R.drawable.artist_art}",
                artistName = "The Weeknd",
                size = 96.dp,
            )
            // Playing — accent border
            CircularArtistImage(
                imageUri = "android.resource://com.jpishimwe.syncplayer/${R.drawable.artist_art}",
                artistName = "NMIXX",
                size = 96.dp,
                isPlaying = true,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}
