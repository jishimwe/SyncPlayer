package com.jpishimwe.syncplayer.ui.player.components

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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.library.formatDuration
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.myAccentColor
import com.jpishimwe.syncplayer.ui.theme.noRippleClickable
import sh.calvin.reorderable.ReorderableListItemScope

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = {
            SubcomposeAsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                error = { Icon(Icons.Default.MusicNote, contentDescription = null) },
            )
        },
        headlineContent = {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            Row {
                if (song.playCount > 0) {
                    Text(song.playCount.toString())
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(formatDuration(song.duration))
            }
        },
    )
}

sealed interface SongMenuAction {
    // Place it next in queue
    data object PlayNext : SongMenuAction

    // Place it next in queue and immediately plays it
    data object PlayNow : SongMenuAction

    // Place it at the end of the queue
    data object AddToQueue : SongMenuAction

    data object AddToPlaylist : SongMenuAction

    data object GoToArtist : SongMenuAction

    data object GoToAlbum : SongMenuAction
}

sealed interface SongItemVariant {
    data object Default : SongItemVariant

    data object Reorderable : SongItemVariant

    // Deletable passes its action via onDelete rather than onMenuAction
    data object Deletable : SongItemVariant

    data object Checkable : SongItemVariant
}

@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    isSelected: Boolean = false,
    showRating: Boolean = false,
    variant: SongItemVariant = SongItemVariant.Default,
    menuActions: List<SongMenuAction> = emptyList(),
    onMenuAction: (SongMenuAction) -> Unit = {},
    onDelete: () -> Unit = {},
    reorderableScope: ReorderableListItemScope? = null,
) {
    val textColor = if (isPlaying) myAccentColor else MaterialTheme.colorScheme.onSurface

    val artShape = RoundedCornerShape(4.dp)

    val itemModifier =
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .then(
                if (isSelected) {
                    Modifier.border(
                        1.dp,
                        myAccentColor,
                        RoundedCornerShape(4.dp),
                    )
                } else {
                    Modifier
                },
            ).noRippleClickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)

    Row(modifier = itemModifier, verticalAlignment = Alignment.CenterVertically) {
        // Album art
        Box {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "Album art for ${song.album}",
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.Album),
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(artShape)
                        .then(
                            if (isPlaying) {
                                Modifier.border(1.5.dp, myAccentColor, artShape)
                            } else {
                                Modifier
                            },
                        ),
            )
        }

        Spacer(Modifier.width(12.dp))

        // Title + artist & album
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
            )

            Text(
                text = "${song.artist} • ${song.album}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Light),
                color = textColor,
            )
        }

/*        // Optional star rating display (non-interactive)
        if (showRating && song.rating > 0) {
            Spacer(Modifier.padding(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating ${song.rating}",
                        tint = if (isPlaying) myAccentColor else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = song.rating.toString(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }*/

        // Optional star rating display (non-interactive)
        if (showRating && song.rating > 0) {
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(song.rating) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isPlaying) myAccentColor else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))

        // Trailing action based on variant
        when (variant) {
            SongItemVariant.Default -> {
                if (menuActions.isNotEmpty()) {
                    SongMenuOverflow(
                        actions = menuActions,
                        onAction = onMenuAction,
                        isPlaying,
                    )
                }
            }

            SongItemVariant.Deletable -> {
                Row {
                    if (menuActions.isNotEmpty()) {
                        SongMenuOverflow(
                            actions = menuActions,
                            onAction = onMenuAction,
                            isPlaying,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = if (isPlaying) myAccentColor else MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier
                                .size(48.dp)
                                .noRippleClickable { onDelete() }
                                .padding(12.dp),
                    )
                }
            }

            SongItemVariant.Reorderable -> {
                if (menuActions.isNotEmpty()) {
                    SongMenuOverflow(
                        actions = menuActions,
                        onAction = onMenuAction,
                        isPlaying,
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Drag handle — scope must be provided by ReorderableLazyColumn caller
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Reorder",
                    tint = if (isPlaying) myAccentColor else MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier.size(48.dp).padding(12.dp).then(
                            if (reorderableScope != null) {
                                with(reorderableScope) { Modifier.draggableHandle() }
                            } else {
                                Modifier
                            },
                        ),
                )
            }

            else -> {} // TODO: Add the other variants
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun SongItemPreview() {
    SyncPlayerTheme(darkTheme = true) {
        val fakeSong =
            Song(
                id = 1,
                title = "Blinding Lights",
                artist = "The Weeknd",
                album = "After Hours",
                duration = 200000L,
                albumArtUri = null,
                playCount = 12,
                rating = Rating.GREAT.value,
                lastPlayed = 0L,
                albumId = 1,
                trackNumber = 1,
                year = 1993,
                dateAdded = 2026,
                contentUri = null,
                lastModified = 0L,
            )
        val actions =
            listOf(
                SongMenuAction.PlayNext,
                SongMenuAction.PlayNow,
                SongMenuAction.AddToQueue,
                SongMenuAction.AddToPlaylist,
                SongMenuAction.GoToArtist,
                SongMenuAction.GoToAlbum,
            )
        Column {
            SongItem(
                song = fakeSong,
                onClick = {},
                isSelected = true,
                menuActions = actions,
                onMenuAction = {},
            )
            SongItem(
                song = fakeSong,
                onClick = {},
                isPlaying = true,
                menuActions = actions,
                onMenuAction = {},
            )
            SongItem(
                song = fakeSong,
                onClick = {},
                showRating = true,
                menuActions = actions,
                onMenuAction = {},
            )
            SongItem(
                song = fakeSong,
                onClick = {},
                variant = SongItemVariant.Deletable,
                menuActions = actions,
                onDelete = {},
            )
            SongItem(
                song = fakeSong,
                onClick = {},
                variant = SongItemVariant.Reorderable,
                menuActions = actions,
                onDelete = {},
            )
        }
    }
}
