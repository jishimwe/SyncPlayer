package com.jpishimwe.syncplayer.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpishimwe.syncplayer.data.PlaylistRepository
import com.jpishimwe.syncplayer.data.SongRepository
import com.jpishimwe.syncplayer.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<PlaylistUiState> =
            playlistRepository
                .getAllPlaylists()
                .flatMapLatest { playlists ->
                    if (playlists.isEmpty()) {
                        flowOf(PlaylistUiState.Loaded(playlists))
                    } else {
                        val artFlows = playlists.map { playlist ->
                            playlistRepository.getArtUrisForPlaylist(playlist.id)
                                .map { uris -> playlist.id to uris }
                        }
                        combine(artFlows) { artPairs ->
                            PlaylistUiState.Loaded(
                                playlists = playlists,
                                playlistArtUris = artPairs.toMap(),
                            )
                        }
                    }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistUiState.Loading)

        fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> = playlistRepository.getSongsForPlaylist(playlistId)

        fun getAllSongs(): Flow<List<Song>> = songRepository.getAllSongs()

        fun onEvent(event: PlaylistEvent) {
            when (event) {
                is PlaylistEvent.CreatePlaylist -> {
                    viewModelScope.launch {
                        val trimmed = event.name.trim()
                        if (trimmed.isNotBlank()) {
                            playlistRepository.createPlaylist(trimmed)
                        }
                    }
                }

                is PlaylistEvent.RenamePlaylist -> {
                    viewModelScope.launch {
                        val trimmed = event.newName.trim()
                        if (trimmed.isNotBlank()) {
                            playlistRepository.renamePlaylist(
                                playlistId = event.playlistId,
                                newName = trimmed,
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
