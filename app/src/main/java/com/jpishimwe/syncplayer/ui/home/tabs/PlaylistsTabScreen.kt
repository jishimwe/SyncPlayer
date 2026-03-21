package com.jpishimwe.syncplayer.ui.home.tabs

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.R
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jpishimwe.syncplayer.model.Playlist
import com.jpishimwe.syncplayer.ui.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.components.PlaylistItem
import com.jpishimwe.syncplayer.ui.components.PlaylistPlaybackState
import com.jpishimwe.syncplayer.ui.components.PlaylistsActionBar
import com.jpishimwe.syncplayer.ui.playlists.PlaylistEvent
import com.jpishimwe.syncplayer.ui.playlists.PlaylistUiState
import com.jpishimwe.syncplayer.ui.playlists.PlaylistViewModel

/**
 * Playlists tab — wraps [PlaylistViewModel] and renders each playlist as a
 * [PlaylistItem] row with collage thumbnail, play/pause control, and overflow menu.
 *
 * [currentPlaylistId] drives the playing/paused border highlight. It defaults to
 * null (no highlight) until PlayerViewModel is wired through HomeScreen — at that
 * point, pass `playerUiState.currentPlaylistId` here. The [isPlaying] flag
 * differentiates Playing vs Paused so the correct icon is shown.
 */
@Composable
fun PlaylistsTabScreen(
    onPlaylistClick: (Long, String) -> Unit,
    onPlayPlaylist: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    currentPlaylistId: Long? = null, // null = no playlist active
    isPlaying: Boolean = false,
    viewModel: PlaylistViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is PlaylistUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is PlaylistUiState.Loaded -> {
            val loaded = uiState as PlaylistUiState.Loaded
            PlaylistsTabScreenContent(
                playlists = loaded.playlists,
                playlistArtUris = loaded.playlistArtUris,
                currentPlaylistId = currentPlaylistId,
                isPlaying = isPlaying,
                onPlaylistClick = onPlaylistClick,
                onPlayPlaylist = onPlayPlaylist,
                onCreatePlaylist = { name -> viewModel.onEvent(PlaylistEvent.CreatePlaylist(name)) },
                onRenamePlaylist = { id, name -> viewModel.onEvent(PlaylistEvent.RenamePlaylist(id, name)) },
                onDeletePlaylist = { viewModel.onEvent(PlaylistEvent.DeletePlaylist(it)) },
                modifier = modifier,
            )
        }
    }
}

@Composable
fun PlaylistsTabScreenContent(
    playlists: List<Playlist>,
    playlistArtUris: Map<Long, List<String>>,
    currentPlaylistId: Long?,
    isPlaying: Boolean,
    onPlaylistClick: (Long, String) -> Unit,
    onPlayPlaylist: (Long, String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var renameTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = MiniPlayerPeek)) {
        stickyHeader {
            PlaylistsActionBar(onCreatePlaylist = { showCreateDialog = true })
        }

        if (playlists.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillParentMaxHeight(0.7f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.empty_playlists),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(playlists, key = { it.id }) { playlist ->
            val playbackState =
                when {
                    playlist.id == currentPlaylistId && isPlaying -> PlaylistPlaybackState.Playing
                    playlist.id == currentPlaylistId -> PlaylistPlaybackState.Paused
                    else -> PlaylistPlaybackState.Default
                }

            PlaylistItem(
                playlist = playlist,
                playbackState = playbackState,
                onClick = { onPlaylistClick(playlist.id, playlist.name) },
                onPlayClick = { onPlayPlaylist(playlist.id, playlist.name) },
                onRename = { renameTarget = playlist.id to playlist.name },
                onDelete = { onDeletePlaylist(playlist.id) },
                artUris = playlistArtUris[playlist.id] ?: emptyList(),
            )
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    renameTarget?.let { (playlistId, currentName) ->
        RenamePlaylistDialog(
            currentName = currentName,
            onConfirm = { newName ->
                onRenamePlaylist(playlistId, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_playlist_title)) },
        text = {
            Column {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(R.string.create_playlist_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun RenamePlaylistDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_playlist_title)) },
        text = {
            Column {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(R.string.rename_playlist_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun PlaylistsTabScreenPreview() {
    SyncPlayerTheme(darkTheme = true) {
        PlaylistsTabScreenContent(
            playlists = listOf(
                Playlist(id = 1, name = "Chill Vibes", createdAt = 0, songCount = 12),
                Playlist(id = 2, name = "Workout Mix", createdAt = 0, songCount = 8),
            ),
            playlistArtUris = emptyMap(),
            currentPlaylistId = null,
            isPlaying = false,
            onPlaylistClick = { _, _ -> },
            onPlayPlaylist = { _, _ -> },
            onCreatePlaylist = {},
            onRenamePlaylist = { _, _ -> },
            onDeletePlaylist = {},
        )
    }
}
