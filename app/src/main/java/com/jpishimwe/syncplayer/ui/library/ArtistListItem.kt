package com.jpishimwe.syncplayer.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.jpishimwe.syncplayer.model.Artist

@Composable
fun ArtistListItem(
    artist: Artist,
    onArtistClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        leadingContent = {
            Icon(Icons.Default.Person, contentDescription = null)
        },
        headlineContent = {
            Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            val albumLabel = if (artist.albumCount == 1) "album" else "albums"
            val songLabel = if (artist.songCount == 1) "song" else "songs"
            Text("${artist.songCount} $songLabel, ${artist.albumCount} $albumLabel")
        },
    )
}
