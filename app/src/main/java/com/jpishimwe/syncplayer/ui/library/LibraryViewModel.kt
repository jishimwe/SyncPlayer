package com.jpishimwe.syncplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpishimwe.syncplayer.data.SongRepository
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab(
    val label: String,
) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FAVORITES("Faves"),
    MOST_PLAYED("Top Plays"),
    RECENTLY_PLAYED("Recent"),
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Loaded(
        val songs: List<Song>,
        val albums: List<Album>,
        val artists: List<Artist>,
        val favorites: List<Song>,
        val mostPlayed: List<Song>,
        val recentlyPlayed: List<Song>,
    ) : LibraryUiState

    data class Error(
        val message: String,
    ) : LibraryUiState
}

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val songRepository: SongRepository,
    ) : ViewModel() {
        private val _selectedTab = MutableStateFlow(LibraryTab.SONGS)
        private val _isRefreshing = MutableStateFlow(false)
        private val lastScanTimestamp = MutableStateFlow(0L)
        private val refreshError = MutableStateFlow<String?>(null)

        val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()
        val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

        val metadataFlows =
            combine(
                songRepository.getFavoriteSongs(),
                songRepository.getMostPlayedSongs(),
                songRepository.getRecentlyPlayed(),
            ) { favorites, mostPlayed, recentlyPlayed ->
                Triple(favorites, mostPlayed, recentlyPlayed)
            }

        val uiState: StateFlow<LibraryUiState> =
            combine(
                songRepository.getAllSongs(),
                songRepository.getAllAlbums(),
                songRepository.getAllArtists(),
                refreshError,
                metadataFlows,
            ) { songs, albums, artists, error, (favorites, mostPlayed, recentlyPlayed) ->
                if (error != null && songs.isEmpty()) {
                    return@combine LibraryUiState.Error(error)
                } else {
                    LibraryUiState.Loaded(
                        songs,
                        albums,
                        artists,
                        favorites = favorites,
                        mostPlayed = mostPlayed,
                        recentlyPlayed = recentlyPlayed,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LibraryUiState.Loading,
            )

        fun selectTab(tab: LibraryTab) {
            _selectedTab.value = tab
        }

        fun refreshLibrary() {
            viewModelScope.launch {
                _isRefreshing.value = true
                lastScanTimestamp.value = System.currentTimeMillis()
                refreshError.value = null
                try {
                    songRepository.refreshLibrary()
                } catch (e: Exception) {
                    refreshError.value = e.message ?: "Unknown error"
                } finally {
                    _isRefreshing.value = false
                }
            }
        }

        fun onAppResumed() {
            val now = System.currentTimeMillis()
            val lastScan = lastScanTimestamp.value
            val refreshDelay = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
            if (now - lastScan > refreshDelay) {
                refreshLibrary()
            }
        }

        fun getSongsByAlbum(albumId: Long): Flow<List<Song>> = songRepository.getSongsByAlbum(albumId)

        fun getSongsByArtist(artist: String): Flow<List<Song>> = songRepository.getSongsByArtist(artist)
    }
