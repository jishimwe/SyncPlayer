package com.jpishimwe.syncplayer.ui.library

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab { SONGS, ALBUMS, ARTISTS }

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Loaded(
        val songs: List<Song>,
        val albums: List<Album>,
        val artists: List<Artist>,
    ) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(LibraryTab.SONGS)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    val uiState: StateFlow<LibraryUiState> = combine(
        songRepository.getAllSongs(),
        songRepository.getAllAlbums(),
        songRepository.getAllArtists(),
    ) { songs, albums, artists ->
        LibraryUiState.Loaded(songs, albums, artists)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState.Loading
    )

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            try {
                songRepository.refreshLibrary()
            } catch (e: Exception) {
                // Error state is handled via the Flow — if the DB has no data,
                // the Loaded state with empty lists will show the empty state UI
            }
        }
    }
}
