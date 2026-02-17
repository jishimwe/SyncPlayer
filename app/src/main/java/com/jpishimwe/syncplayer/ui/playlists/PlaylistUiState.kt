package com.jpishimwe.syncplayer.ui.playlists

import com.jpishimwe.syncplayer.model.Playlist

sealed interface PlaylistUiState {
    data object Loading : PlaylistUiState

    data class Loaded(
        val playlists: List<Playlist>,
    ) : PlaylistUiState
}
