package com.jpishimwe.syncplayer.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpishimwe.syncplayer.data.PlaylistRepository
import com.jpishimwe.syncplayer.data.SongRepository
import com.jpishimwe.syncplayer.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel
    @Inject
    constructor(
        private val playlistRepository: PlaylistRepository,
        private val songRepository: SongRepository,
    ) : ViewModel() {
        val uiState: StateFlow<PlaylistUiState> =
            playlistRepository
                .getAllPlaylists()
                .map { PlaylistUiState.Loaded(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistUiState.Loading)

        fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> = playlistRepository.getSongsForPlaylist(playlistId)

        fun getAllSongs(): Flow<List<Song>> = songRepository.getAllSongs()

        fun onEvent(event: PlaylistEvent) {
            when (event) {
                is PlaylistEvent.CreatePlaylist -> {
                    viewModelScope.launch {
                        if (event.name.isNotBlank()) {
                            playlistRepository.createPlaylist(
                                event.name,
                            )
                        }
                    }
                }

                is PlaylistEvent.RenamePlaylist -> {
                    viewModelScope.launch {
                        if (event.newName.isNotBlank()) {
                            playlistRepository.renamePlaylist(
                                event.playlistId,
                                event.newName,
                            )
                        }
                    }
                }

                is PlaylistEvent.DeletePlaylist -> {
                    viewModelScope.launch {
                        playlistRepository.deletePlaylist(event.playlistId)
                    }
                }

                is PlaylistEvent.AddSongsToPlaylist -> {
                    viewModelScope.launch {
                        event.songIds.forEach {
                            playlistRepository.addSongToPlaylist(
                                event.playlistId,
                                it,
                            )
                        }
                    }
                }

                is PlaylistEvent.RemoveSongsFromPlaylist -> {
                    viewModelScope.launch {
                        event.songIds.forEach {
                            playlistRepository.removeSongFromPlaylist(
                                event.playlistId,
                                it,
                            )
                        }
                    }
                }

                is PlaylistEvent.RemoveSongFromPlaylist -> {
                    viewModelScope.launch {
                        playlistRepository.removeSongFromPlaylist(
                            event.playlistId,
                            event.songId,
                        )
                    }
                }

                is PlaylistEvent.ReorderSongs -> {
                    viewModelScope.launch {
                        playlistRepository.reorderSongs(
                            event.playlistId,
                            event.orderedSongIds,
                        )
                    }
                }
            }
        }
    }
