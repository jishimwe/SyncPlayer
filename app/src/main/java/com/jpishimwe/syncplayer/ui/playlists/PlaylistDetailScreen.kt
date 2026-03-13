package com.jpishimwe.syncplayer.ui.playlists

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// Formats a total playlist duration (ms) as "Xh Ym" or "Ym" for values under an hour
private fun formatPlaylistTotalDuration(totalMs: Long): String {
    val totalSeconds = totalMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    playerViewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val allSongs by viewModel.getAllSongs().collectAsStateWithLifecycle(emptyList())
    val playlistSongs by viewModel
        .getSongsForPlaylist(
            playlistId = playlistId,
        ).collectAsStateWithLifecycle(emptyList())

    PlaylistDetailScreenContent(
        playlistName = playlistName,
        playlistSongs = playlistSongs,
        allSongs = allSongs,
        onSongClick = { index ->
            playerViewModel.onEvent(PlayerEvent.PlaySongs(playlistSongs, index))
            onNavigateToNowPlaying()
        },
        onShuffleClick = {
            if (playlistSongs.isNotEmpty()) {
                playerViewModel.onEvent(PlayerEvent.PlaySongs(playlistSongs.shuffled(), 0))
                onNavigateToNowPlaying()
            }
        },
        onPlayAllClick = {
            if (playlistSongs.isNotEmpty()) {
                playerViewModel.onEvent(PlayerEvent.PlaySongs(playlistSongs, 0))
                onNavigateToNowPlaying()
            }
        },
        onRemoveSong = { songId -> viewModel.onEvent(PlaylistEvent.RemoveSongFromPlaylist(playlistId, songId)) },
        onRemoveSongs = { songIds -> viewModel.onEvent(PlaylistEvent.RemoveSongsFromPlaylist(playlistId, songIds)) },
        onReorderSongs = { orderedSongsIds -> viewModel.onEvent(PlaylistEvent.ReorderSongs(playlistId, orderedSongsIds)) },
        onAddSongs = { songIds -> viewModel.onEvent(PlaylistEvent.AddSongsToPlaylist(playlistId, songIds)) },
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun PlaylistDetailScreenContent(
    playlistName: String,
    playlistSongs: List<Song>,
    allSongs: List<Song>,
    onSongClick: (index: Int) -> Unit,
    onShuffleClick: () -> Unit,
    onPlayAllClick: () -> Unit,
    onRemoveSong: (songId: Long) -> Unit,
    onRemoveSongs: (songIds: List<Long>) -> Unit,
    onReorderSongs: (orderedSongsIds: List<Long>) -> Unit,
    onAddSongs: (songIds: List<Long>) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var showSongPicker by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            onReorderSongs(
                playlistSongs
                    .mapIndexed { _, song -> song.id }
                    .toMutableList()
                    .apply { add(to.index, removeAt(from.index)) },
            )
        }

    val totalDuration = remember(playlistSongs) { playlistSongs.sumOf { it.duration } }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header: back · name + count/duration · add ─────────────────────
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    text = playlistName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${playlistSongs.size} songs · ${formatPlaylistTotalDuration(totalDuration)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = { showSongPicker = true },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add songs",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // ── Sub-header: shuffle + play-all ──────────────────────────────────
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onShuffleClick) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onPlayAllClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play all",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // ── Song list ────────────────────────────────────────────────────────
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(
                items = playlistSongs,
                key = { _, item -> item.id },
            ) { index, item ->
                ReorderableItem(reorderableLazyListState, key = item.id) { isDragging ->
                    PlaylistSongItem(
                        song = item,
                        onSongClick = { onSongClick(index) },
                        onRemove = { onRemoveSong(item.id) },
                        isDragging = isDragging,
                        modifier = Modifier.draggableHandle(),
                    )
                }
            }
        }
    }

    // Song picker outside the Column — same position as before
    if (showSongPicker) {
        SongPickerSheet(
            allSongs = allSongs,
            selectedSongs = playlistSongs,
            onDismiss = { showSongPicker = false },
            onAddSongs = onAddSongs,
            onRemoveSongs = onRemoveSongs,
        )
    }
}

// ── Preview data ───────────────────────────────────────────────────────────────

private val previewPlaylistSongs =
    listOf(
        Song(
            id = 1,
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            duration = 200_000L,
            albumArtUri = null,
            playCount = 12,
            rating = Rating.GREAT.value,
            lastPlayed = 0L,
            albumId = 1,
            trackNumber = 1,
            year = 2020,
            dateAdded = 1000L,
            contentUri = null,
            lastModified = 0L,
        ),
        Song(
            id = 2,
            title = "Levitating",
            artist = "Dua Lipa",
            album = "Future Nostalgia",
            duration = 203_000L,
            albumArtUri = null,
            playCount = 7,
            rating = Rating.GOOD.value,
            lastPlayed = 0L,
            albumId = 2,
            trackNumber = 1,
            year = 2020,
            dateAdded = 1001L,
            contentUri = null,
            lastModified = 0L,
        ),
        Song(
            id = 3,
            title = "As It Was",
            artist = "Harry Styles",
            album = "Harry's House",
            duration = 167_000L,
            albumArtUri = null,
            playCount = 4,
            rating = Rating.NONE.value,
            lastPlayed = 0L,
            albumId = 3,
            trackNumber = 1,
            year = 2022,
            dateAdded = 1002L,
            contentUri = null,
            lastModified = 0L,
        ),
        Song(
            id = 4,
            title = "Shivers",
            artist = "Ed Sheeran",
            album = "=",
            duration = 207_000L,
            albumArtUri = null,
            playCount = 2,
            rating = Rating.NONE.value,
            lastPlayed = 0L,
            albumId = 4,
            trackNumber = 1,
            year = 2021,
            dateAdded = 1003L,
            contentUri = null,
            lastModified = 0L,
        ),
    )

/**
 * Playlist with songs — the reorder drag handle renders but won't respond to drag
 * in preview; that's expected since gesture handling requires a real device/emulator.
 */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "With songs")
@Composable
private fun PlaylistDetailWithSongsPreview() {
    SyncPlayerTheme(darkTheme = true) {
        PlaylistDetailScreenContent(
            playlistName = "My Favourites",
            playlistSongs = previewPlaylistSongs,
            allSongs = previewPlaylistSongs,
            onSongClick = {},
            onShuffleClick = {},
            onPlayAllClick = {},
            onRemoveSong = {},
            onRemoveSongs = {},
            onReorderSongs = {},
            onAddSongs = {},
            onNavigateBack = {},
        )
    }
}

/** Empty playlist — just the header/sub-header with no songs. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Empty playlist")
@Composable
private fun PlaylistDetailEmptyPreview() {
    SyncPlayerTheme(darkTheme = true) {
        PlaylistDetailScreenContent(
            playlistName = "New Playlist",
            playlistSongs = emptyList(),
            allSongs = previewPlaylistSongs,
            onSongClick = {},
            onShuffleClick = {},
            onPlayAllClick = {},
            onRemoveSong = {},
            onRemoveSongs = {},
            onReorderSongs = {},
            onAddSongs = {},
            onNavigateBack = {},
        )
    }
}

/** Long playlist name — verifies single-line truncation in the header. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Long name")
@Composable
private fun PlaylistDetailLongNamePreview() {
    SyncPlayerTheme(darkTheme = true) {
        PlaylistDetailScreenContent(
            playlistName = "My Very Long and Detailed Playlist Name That Overflows",
            playlistSongs = previewPlaylistSongs,
            allSongs = previewPlaylistSongs,
            onSongClick = {},
            onShuffleClick = {},
            onPlayAllClick = {},
            onRemoveSong = {},
            onRemoveSongs = {},
            onReorderSongs = {},
            onAddSongs = {},
            onNavigateBack = {},
        )
    }
}
