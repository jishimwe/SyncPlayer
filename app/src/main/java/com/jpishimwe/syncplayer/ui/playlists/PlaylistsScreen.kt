package com.jpishimwe.syncplayer.ui.playlists

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PlaylistsScreen(
    onNavigateToPlaylistDetail: (playlistId: Long, playlistName: String) -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent

    PlaylistsScreenContent(
        uiState = uiState.value,
        onCreatePlaylist = { name -> onEvent(PlaylistEvent.CreatePlaylist(name)) },
        onRenamePlaylist = { id, name -> onEvent(PlaylistEvent.RenamePlaylist(id, name)) },
        onDeletePlaylist = { id -> onEvent(PlaylistEvent.DeletePlaylist(id)) },
        onPlaylistClick = { id, name -> onNavigateToPlaylistDetail(id, name) },
    )
}
