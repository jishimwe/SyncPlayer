package com.jpishimwe.syncplayer.ui.home.tabs

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jpishimwe.syncplayer.ui.player.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.player.components.PlaylistItem
import com.jpishimwe.syncplayer.ui.player.components.PlaylistPlaybackState
import com.jpishimwe.syncplayer.ui.player.components.PlaylistsActionBar
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
            val playlists = (uiState as PlaylistUiState.Loaded).playlists

/*            if (playlists.isEmpty()) {
                Box(modifier.fillMaxSize()) {
                    PlaylistsActionBar(
                        onCreatePlaylist = { viewModel.onEvent(PlaylistEvent.CreatePlaylist("New Playlist")) },
                    )

                    Text("No playlists yet")
                }
                return
            }*/

            LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = MiniPlayerPeek)) {
                stickyHeader {
                    PlaylistsActionBar(
                        onCreatePlaylist = { viewModel.onEvent(PlaylistEvent.CreatePlaylist("New Playlist")) },
                    )
                }

                items(playlists, key = { it.id }) { playlist ->
                    // Resolve playback state for this specific row.
                    // Only the currently active playlist gets a coloured border — all
                    // others stay Default regardless of the global isPlaying flag.
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
                        onRename = { viewModel.onEvent(PlaylistEvent.RenamePlaylist(playlist.id, "")) },
                        onDelete = { viewModel.onEvent(PlaylistEvent.DeletePlaylist(playlist.id)) },
                        // artUris stays empty until a DAO query is added to fetch the
                        // first 4 album art URIs per playlist — PlaylistCollage handles
                        // the empty-list case with a music note fallback.
                        artUris = emptyList(),
                    )
                }
            }
        }
    }
}
