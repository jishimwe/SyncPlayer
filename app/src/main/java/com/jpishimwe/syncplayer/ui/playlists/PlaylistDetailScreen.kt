package com.jpishimwe.syncplayer.ui.playlists

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.components.FrostedGlassPill
import com.jpishimwe.syncplayer.ui.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import com.jpishimwe.syncplayer.ui.theme.BlurredBackground
import com.jpishimwe.syncplayer.ui.theme.myAccentColor
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

    val accentBorderBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    myAccentColor.copy(alpha = 0.24f),
                    myAccentColor.copy(alpha = 0.75f),
                    myAccentColor.copy(alpha = 0.24f),
                ),
        )

    // ── Root ─────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Layer 0 — blurred screenshot of previous screen
        BlurredBackground()

        // Main content column: action bar + song list
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    // Top padding to clear the pinned header row
                    .padding(top = 56.dp),
        ) {
            // ── Action bar: song count + shuffle + play ──────────────────
            PlaylistActionBar(
                label = "${playlistSongs.size} songs · ${formatPlaylistTotalDuration(totalDuration)}",
                accentBorderBrush = accentBorderBrush,
                onShuffleClick = onShuffleClick,
                onPlayAllClick = onPlayAllClick,
            )

            // ── Song list (reorderable) ──────────────────────────────────
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = MiniPlayerPeek),
            ) {
                itemsIndexed(
                    items = playlistSongs,
                    key = { _, item -> item.id },
                ) { index, item ->
                    ReorderableItem(reorderableLazyListState, key = item.id) { isDragging ->
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background),
                        ) {
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
        }

        // ── Pinned header: back + playlist name + add songs (always visible) ──
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Back button
            FrostedGlassPill(
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // Playlist name + metadata pill
            FrostedGlassPill(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(4.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = playlistName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${playlistSongs.size} songs · ${formatPlaylistTotalDuration(totalDuration)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // Add songs button
            FrostedGlassPill(
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                IconButton(
                    onClick = { showSongPicker = true },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add songs",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }

    // Song picker outside the Box — same position as before
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
