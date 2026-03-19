package com.jpishimwe.syncplayer.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.model.Playlist
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.myAccentColor

/**
 * Mirrors AlbumPlaybackState — three visual states for a playlist row.
 *
 * Default  → no border, white/grey text and icons
 * Playing  → accent border on entire item, accent text, pause icon
 * Paused   → accent border on entire item, accent text, play icon
 */
enum class PlaylistPlaybackState { Default, Playing, Paused }

/**
 * A 72dp playlist row matching the visual design spec.
 *
 * Layout: [56×56 collage] [name / #songs · duration] [play/pause] [overflow ⋮]
 *
 * The border wraps the entire row (not just the collage) when [playbackState] is
 * anything other than Default. This differs from SongItem where only the album art
 * thumbnail gets the border — playlists are larger visual units so the full-row
 * border reads more clearly.
 *
 * @param artUris Up to 4 album art URIs forwarded to [PlaylistCollage]. Pass an
 *   empty list to show the music-note fallback until per-playlist art URIs are
 *   wired through the data layer.
 */
@Composable
fun PlaylistItem(
    playlist: Playlist,
    playbackState: PlaylistPlaybackState,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    artUris: List<String?> = emptyList(),
) {
    val isActive = playbackState != PlaylistPlaybackState.Default

    // Accent colour drives text + icon tint when this playlist is active
    val primaryTextColor = if (isActive) myAccentColor else MaterialTheme.colorScheme.onPrimary
    val secondaryTextColor = if (isActive) myAccentColor else MaterialTheme.colorScheme.onSurfaceVariant
    val iconTint = if (isActive) myAccentColor else MaterialTheme.colorScheme.onSurfaceVariant

    var menuExpanded by remember { mutableStateOf(false) }

    // The border wraps the entire row; absent in Default state so no layout shift
    val borderModifier =
        if (isActive) {
            Modifier.border(width = 1.5.dp, color = myAccentColor, shape = RoundedCornerShape(8.dp))
        } else {
            Modifier
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(72.dp)
                .then(borderModifier)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Leading: 2×2 collage ──────────────────────────────────────────
        PlaylistCollage(
            artUris = artUris,
            modifier = Modifier.size(56.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // ── Text block — expands to fill available space ──────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = primaryTextColor,
                maxLines = 1,
            )
            Text(
                // Bullet separator matches the Figma spec: "#Songs • 00:00:00"
                text = "${playlist.songCount} Songs \u2022 ${formatPlaylistDuration(playlist.totalDuration)}",
                style = MaterialTheme.typography.labelSmall,
                color = secondaryTextColor,
                maxLines = 1,
            )
        }

        // ── Play / Pause control ──────────────────────────────────────────
        // Shows Pause when actively playing; Play in every other state (Default
        // and Paused both show the play arrow — tint difference conveys state).
        IconButton(onClick = onPlayClick) {
            Icon(
                imageVector =
                    if (playbackState == PlaylistPlaybackState.Playing) {
                        Icons.Filled.Pause
                    } else {
                        Icons.Filled.PlayArrow
                    },
                contentDescription =
                    if (playbackState == PlaylistPlaybackState.Playing) {
                        "Pause playlist"
                    } else {
                        "Play playlist"
                    },
                tint = iconTint,
            )
        }

        // ── Overflow menu ─────────────────────────────────────────────────
        // Box anchors the DropdownMenu so it appears below the icon, not at the
        // top of the screen (a common Compose gotcha with floating menus).
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = iconTint,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}

// ── Duration formatting ───────────────────────────────────────────────────────

/**
 * Formats a duration in milliseconds as "m:ss" (< 1 hr) or "h:mm:ss" (≥ 1 hr).
 *
 * Examples: 185_000 ms → "3:05", 4_200_000 ms → "1:10:00"
 */
private fun formatPlaylistDuration(ms: Long): String {
    val totalSeconds = ms / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

// ── Previews — one per playback state ────────────────────────────────────────

// Shared sample data so all three previews show the same content
private val samplePlaylist =
    Playlist(
        id = 1L,
        name = "Playlist Name",
        createdAt = 0L,
        songCount = 12,
        totalDuration = 2_940_000L, // 49 minutes → "49:00"
    )

/** Default state: white text, grey icons, no border. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Default")
@Composable
private fun PlaylistItemDefaultPreview() {
    SyncPlayerTheme {
        PlaylistItem(
            playlist = samplePlaylist,
            playbackState = PlaylistPlaybackState.Default,
            onClick = {},
            onPlayClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}

/** Playing state: accent border, accent text, pause icon. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Playing")
@Composable
private fun PlaylistItemPlayingPreview() {
    SyncPlayerTheme {
        PlaylistItem(
            playlist = samplePlaylist,
            playbackState = PlaylistPlaybackState.Playing,
            onClick = {},
            onPlayClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}

/** Paused state: accent border, accent text, play icon (not pause). */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Paused")
@Composable
private fun PlaylistItemPausedPreview() {
    SyncPlayerTheme {
        PlaylistItem(
            playlist = samplePlaylist,
            playbackState = PlaylistPlaybackState.Paused,
            onClick = {},
            onPlayClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}
