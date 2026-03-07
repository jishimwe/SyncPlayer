package com.jpishimwe.syncplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpishimwe.syncplayer.data.SongRepository
import com.jpishimwe.syncplayer.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MetadataViewModel
    @Inject
    constructor(
        private val songRepository: SongRepository,
    ) : ViewModel() {
        val uiState: StateFlow<MetadataUiState> =
            combine(
                songRepository.getFavoriteSongs(),
                songRepository.getMostPlayedSongs(),
                songRepository.getRecentlyPlayed(),
            ) { favorites, mostPlayed, recentlyPlayed ->
                MetadataUiState.Loaded(favorites, mostPlayed, recentlyPlayed)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetadataUiState.Loading)
    }

sealed interface MetadataUiState {
    data object Loading : MetadataUiState

    data class Loaded(
        val favorites: List<Song>,
        val mostPlayed: List<Song>,
        val recentlyPlayed: List<Song>,
    ) : MetadataUiState
}
