package com.jpishimwe.syncplayer.ui.playlists

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
        onRemoveSong = { songId -> viewModel.onEvent(PlaylistEvent.RemoveSongFromPlaylist(playlistId, songId)) },
        onRemoveSongs = { songIds -> viewModel.onEvent(PlaylistEvent.RemoveSongsFromPlaylist(playlistId, songIds)) },
        onReorderSongs = { orderedSongsIds -> viewModel.onEvent(PlaylistEvent.ReorderSongs(playlistId, orderedSongsIds)) },
        onAddSongs = { songIds -> viewModel.onEvent(PlaylistEvent.AddSongsToPlaylist(playlistId, songIds)) },
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreenContent(
    playlistName: String,
    playlistSongs: List<Song>,
    allSongs: List<Song>,
    onSongClick: (index: Int) -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSongPicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add songs")
            }
        },
    ) { padding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.padding(padding),
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
