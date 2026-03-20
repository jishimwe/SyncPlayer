package com.jpishimwe.syncplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpishimwe.syncplayer.data.ArtistImageRepository
import com.jpishimwe.syncplayer.data.SongRepository
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

enum class LibraryTab(
    val label: String,
) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FAVORITES("Faves"),
    PLAYLISTS("Playlists"),
    HISTORY("History"),
}

enum class SortOrder(
    val label: String,
) {
    BY_TITLE("Songs"),
    BY_ARTIST("Artist"),
    BY_ALBUM("Albums"),
    BY_DURATION("Duration"),
    BY_DATE_ADDED("Date"),
    BY_PLAY_COUNT("Plays"),
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Loaded(
        val songs: List<Song>,
        val albums: List<Album>,
        val artists: List<Artist>,
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
        private val artistImageRepository: ArtistImageRepository,
    ) : ViewModel() {
        init {
            fetchMissingArtistImages()
        }

        private val _selectedTab = MutableStateFlow(LibraryTab.SONGS)
        private val _isRefreshing = MutableStateFlow(false)
        private val _searchQuery = MutableStateFlow("")
        private val _sortOrder = MutableStateFlow(SortOrder.BY_TITLE)
        private val _albumSortOrder = MutableStateFlow(SortOrder.BY_ALBUM)
        private val _artistSortOrder = MutableStateFlow(SortOrder.BY_ARTIST)
        private val lastScanTimestamp = MutableStateFlow(System.currentTimeMillis())
        private val refreshError = MutableStateFlow<String?>(null)

        val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()
        val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
        val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()
        val albumSortOrder: StateFlow<SortOrder> = _albumSortOrder.asStateFlow()
        val artistSortOrder: StateFlow<SortOrder> = _artistSortOrder.asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        private val songsFlow =
            combine(_searchQuery, _sortOrder) { query, sortOrder ->
                Pair(query, sortOrder)
            }.flatMapLatest { (query, sortOrder) ->
                val flow =
                    if (query.isBlank()) {
                        songRepository.getAllSongs()
                    } else {
                        songRepository.searchSongs(query)
                    }
                flow.map { songs ->
                    when (sortOrder) {
                        SortOrder.BY_TITLE -> songs.sortedBy { it.title.lowercase() }
                        SortOrder.BY_ARTIST -> songs.sortedBy { it.artist }
                        SortOrder.BY_ALBUM -> songs.sortedBy { it.album }
                        SortOrder.BY_DURATION -> songs.sortedBy { it.duration }
                        SortOrder.BY_DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
                        SortOrder.BY_PLAY_COUNT -> songs.sortedByDescending { it.playCount }
                    }
                }
            }

        @OptIn(ExperimentalCoroutinesApi::class)
        private val albumsFlow =
            combine(_searchQuery, _albumSortOrder) { query, sortOrder ->
                Pair(query, sortOrder)
            }.flatMapLatest { (query, sortOrder) ->
                val flow = if (query.isBlank()) songRepository.getAllAlbums() else songRepository.searchAlbums(query)
                flow.map { albums ->
                    when (sortOrder) {
                        SortOrder.BY_ALBUM -> albums.sortedBy { it.name }
                        SortOrder.BY_ARTIST -> albums.sortedBy { it.artist }
                        else -> albums
                    }
                }
            }

        @OptIn(ExperimentalCoroutinesApi::class)
        private val artistsFlow =
            combine(_searchQuery, _artistSortOrder) { query, sortOrder ->
                Pair(query, sortOrder)
            }.flatMapLatest { (query, sortOrder) ->
                val flow = if (query.isBlank()) songRepository.getAllArtists() else songRepository.searchArtists(query)
                flow.map { artists ->
                    when (sortOrder) {
                        SortOrder.BY_ARTIST -> artists.sortedBy { it.name }
                        SortOrder.BY_PLAY_COUNT -> artists.sortedByDescending { it.songCount }
                        else -> artists
                    }
                }
            }

        val uiState: StateFlow<LibraryUiState> =
            combine(
                songsFlow,
                albumsFlow,
                artistsFlow,
                refreshError,
            ) { songs, albums, artists, error ->
                if (error != null && songs.isEmpty()) {
                    return@combine LibraryUiState.Error(error)
                } else {
                    LibraryUiState.Loaded(
                        songs,
                        albums,
                        artists,
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

        fun getAlbumsByArtist(artist: String): Flow<List<Album>> = songRepository.getAlbumsByArtist(artist)

        fun getArtistByName(artistName: String): Flow<Artist?> =
            songRepository.getArtistByName(artistName)

        fun onSearchQueryChanged(query: String) {
            _searchQuery.value = query
        }

        fun onClearSearchQuery() {
            _searchQuery.value = ""
        }

        fun onSortOrder(order: SortOrder) {
            _sortOrder.value = order
        }

        fun onAlbumSortOrder(order: SortOrder) {
            _albumSortOrder.value = order
        }

        fun onArtistSortOrder(order: SortOrder) {
            _artistSortOrder.value = order
        }

        private var fetchArtistImagesJob: Job? = null

        fun fetchMissingArtistImages() {
            fetchArtistImagesJob?.cancel()
            fetchArtistImagesJob = viewModelScope.launch {
                // Wait until artists are actually loaded (non-empty after scan)
                val artists = songRepository.getAllArtists()
                    .filter { it.isNotEmpty() }
                    .first()
                // Call repo for every artist — it returns immediately for cached entries
                for (artist in artists) {
                    try {
                        artistImageRepository.getArtistImageUrl(artist.name)
                    } catch (_: Exception) {
                        // Skip failed fetches — don't abort remaining artists
                    }
                    delay(1_000) // throttle: 1 req/sec to respect Deezer rate limits
                }
            }
        }
    }
