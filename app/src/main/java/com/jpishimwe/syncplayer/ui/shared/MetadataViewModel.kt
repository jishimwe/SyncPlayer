package com.jpishimwe.syncplayer.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpishimwe.syncplayer.data.SongRepository
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MetadataViewModel
    @Inject
    constructor(
        private val songRepository: SongRepository,
    ) : ViewModel() {
        private val _favoriteSortOrder = MutableStateFlow(SortOrder.BY_TITLE)
        val favoriteSortOrder: StateFlow<SortOrder> = _favoriteSortOrder.asStateFlow()

        val uiState: StateFlow<MetadataUiState> =
            combine(
                songRepository.getFavoriteSongs(),
                songRepository.getMostPlayedSongs(),
                songRepository.getRecentlyPlayed(),
                songRepository.getRecentlyPlayedAlbums(),
                songRepository.getRecentlyPlayedArtists(),
                _favoriteSortOrder,
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val favorites = values[0] as List<Song>
                @Suppress("UNCHECKED_CAST")
                val mostPlayed = values[1] as List<Song>
                @Suppress("UNCHECKED_CAST")
                val recentlyPlayed = values[2] as List<Song>
                @Suppress("UNCHECKED_CAST")
                val recentlyPlayedAlbums = values[3] as List<Album>
                @Suppress("UNCHECKED_CAST")
                val recentlyPlayedArtists = values[4] as List<Artist>
                val sortOrder = values[5] as SortOrder

                val sortedFavorites = when (sortOrder) {
                    SortOrder.BY_TITLE -> favorites.sortedBy { it.title.lowercase() }
                    SortOrder.BY_ARTIST -> favorites.sortedBy { it.artist }
                    SortOrder.BY_ALBUM -> favorites.sortedBy { it.album }
                    SortOrder.BY_DURATION -> favorites.sortedBy { it.duration }
                    SortOrder.BY_DATE_ADDED -> favorites.sortedByDescending { it.dateAdded }
                    SortOrder.BY_PLAY_COUNT -> favorites.sortedByDescending { it.playCount }
                }

                @Suppress("RedundantExplicitType")
                val result: MetadataUiState = MetadataUiState.Loaded(
                    sortedFavorites,
                    mostPlayed,
                    recentlyPlayed,
                    recentlyPlayedAlbums,
                    recentlyPlayedArtists,
                )
                result
            }.catch { e ->
                emit(MetadataUiState.Error(e.message ?: "Unknown error"))
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                MetadataUiState.Loading,
            )

        fun onFavoriteSortOrder(order: SortOrder) {
            _favoriteSortOrder.value = order
        }
    }

sealed interface MetadataUiState {
    data object Loading : MetadataUiState

    data class Loaded(
        val favorites: List<Song>,
        val mostPlayed: List<Song>,
        val recentlyPlayed: List<Song>,
        val recentlyPlayedAlbums: List<Album>,
        val recentlyPlayedArtists: List<Artist>,
    ) : MetadataUiState

    data class Error(
        val message: String,
    ) : MetadataUiState
}
