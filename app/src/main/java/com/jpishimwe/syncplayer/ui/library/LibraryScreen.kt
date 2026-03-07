package com.jpishimwe.syncplayer.ui.library

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel

@Composable
fun LibraryScreen(
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbumDetail: (albumId: Long, albumName: String) -> Unit,
    onNavigateToArtistDetail: (artistName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    metadataViewModel: MetadataViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    playerViewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    PermissionHandler {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val metadataState by metadataViewModel.uiState.collectAsStateWithLifecycle()
        val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
        val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.refreshLibrary()
        }

        val lifeCycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifeCycleOwner) {
            val observer =
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.onAppResumed()
                    }
                }

            lifeCycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifeCycleOwner.lifecycle.removeObserver(observer)
            }
        }

        Column(modifier = Modifier.padding(8.dp)) {
            LibraryScreenContent(
                uiState = uiState,
                metadataState = metadataState,
                selectedTab = selectedTab,
                onTabSelected = viewModel::selectTab,
                onRetry = viewModel::refreshLibrary,
                onSongClick =
                    { songs, index ->
                        playerViewModel.onEvent(PlayerEvent.PlaySongs(songs, index))
                        onNavigateToNowPlaying()
                    },
                onAlbumClick = { albumId, albumName ->
                    onNavigateToAlbumDetail(albumId, albumName)
                },
                onArtistClick = { artistName ->
                    onNavigateToArtistDetail(artistName)
                },
                onQueryChanged = viewModel::onSearchQueryChanged,
                onClearSearchQuery = viewModel::onClearSearchQuery,
                onSortOrderChanged = viewModel::onSortOrder,
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenContent(
    uiState: LibraryUiState,
    metadataState: MetadataUiState,
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    onRetry: () -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onAlbumClick: (albumId: Long, albumName: String) -> Unit,
    onArtistClick: (artistName: String) -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearSearchQuery: () -> Unit,
    onSortOrderChanged: (sortOrder: SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (active) {
                DockedSearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = query,
                            onQueryChange = {
                                query = it
                                onQueryChanged(it)
                            },
                            onSearch = {},
                            expanded = active,
                            onExpandedChange = {
                                active = it
                            },
                            placeholder = { Text("Search") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        )
                    },
                    expanded = active,
                    onExpandedChange = {
                        active = it
                        if (!it) {
                            query = ""
                            onClearSearchQuery()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 64.dp),
                ) {
                }
            } else {
                TopAppBar(
                    title = { Text("SyncPlayer") },
                    actions = {
                        IconButton(onClick = {
                            active = !active
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(64.dp),
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = { Text(tab.label) },
                    )
                }
            }

            when {
                uiState is LibraryUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState is LibraryUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.message)
                            Button(onClick = onRetry) {
                                Text("Retry")
                            }
                        }
                    }
                }

                uiState is LibraryUiState.Loaded && metadataState is MetadataUiState.Loaded -> {
                    when (selectedTab) {
                        LibraryTab.SONGS -> SongsTab(uiState, onSongClick, onSortOrderChanged)
                        LibraryTab.ALBUMS -> AlbumsTab(uiState, onAlbumClick, onSortOrderChanged)
                        LibraryTab.ARTISTS -> ArtistsTab(uiState, onArtistClick)
                        LibraryTab.FAVORITES -> FavoriteTab(metadataState, onSongClick)
                        LibraryTab.MOST_PLAYED -> MostPlayedTab(metadataState, onSongClick)
                        LibraryTab.RECENTLY_PLAYED -> RecentlyPlayedTab(metadataState, onSongClick)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongsTab(
    state: LibraryUiState.Loaded,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onSortOrderChanged: (sortOrder: SortOrder) -> Unit,
) {
    if (state.songs.isEmpty()) {
        EmptyState("No songs found")
    } else {
        var expanded by remember { mutableStateOf(false) }
        var selectedSortOrder by remember { mutableStateOf(SortOrder.BY_TITLE) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier,
            content = {
                FilterChip(
                    selected = true,
                    onClick = { expanded = !expanded },
                    label = { Text(selectedSortOrder.label) },
                    leadingIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort") },
                    modifier = Modifier.widthIn(24.dp),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = {
                                selectedSortOrder = order
                                expanded = false
                                onSortOrderChanged(order)
                            },
                        )
                    }
                }
            },
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(state.songs, key = { _, song -> song.id }) { index, song ->
                SongListItem(
                    song,
                    onClick = { onSongClick(state.songs, index) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumsTab(
    state: LibraryUiState.Loaded,
    onAlbumClick: (albumId: Long, albumName: String) -> Unit,
    onSortOrderChanged: (sortOrder: SortOrder) -> Unit,
) {
    if (state.albums.isEmpty()) {
        EmptyState("No albums found")
    } else {
        var expanded by remember { mutableStateOf(false) }
        var selectedSortOrder by remember { mutableStateOf(SortOrder.BY_TITLE) }
        val albumSortOrder = listOf(SortOrder.BY_ALBUM, SortOrder.BY_ARTIST)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier,
            content = {
                FilterChip(
                    selected = true,
                    onClick = { expanded = !expanded },
                    label = { Text(selectedSortOrder.label) },
                    leadingIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort") },
                    modifier = Modifier.widthIn(24.dp),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    albumSortOrder.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = {
                                selectedSortOrder = order
                                expanded = false
                                onSortOrderChanged(order)
                            },
                        )
                    }
                }
            },
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.albums, key = { it.id }) { album ->
                AlbumGridItem(
                    album,
                    onClick = { onAlbumClick(album.id, album.name) },
                )
            }
        }
    }
}

@Composable
private fun ArtistsTab(
    state: LibraryUiState.Loaded,
    onArtistClick: (artistName: String) -> Unit,
) {
    if (state.artists.isEmpty()) {
        EmptyState("No artists found")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.artists, key = { it.name }) { artist ->
                ArtistListItem(artist, onArtistClick = { onArtistClick(artist.name) })
            }
        }
    }
}

@Composable
fun FavoriteTab(
    state: MetadataUiState.Loaded,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
) {
    if (state.favorites.isEmpty()) {
        EmptyState("No songs found")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(state.favorites, key = { _, song -> song.id }) { index, song ->
                SongListItem(
                    song,
                    onClick = { onSongClick(state.favorites, index) },
                )
            }
        }
    }
}

@Composable
fun MostPlayedTab(
    state: MetadataUiState.Loaded,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
) {
    if (state.mostPlayed.isEmpty()) {
        EmptyState("No songs found")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(state.mostPlayed, key = { _, song -> song.id }) { index, song ->
                SongListItem(
                    song,
                    onClick = { onSongClick(state.mostPlayed, index) },
                )
            }
        }
    }
}

@Composable
fun RecentlyPlayedTab(
    state: MetadataUiState.Loaded,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
) {
    if (state.recentlyPlayed.isEmpty()) {
        EmptyState("No songs found")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(state.recentlyPlayed, key = { _, song -> song.id }) { index, song ->
                SongListItem(
                    song,
                    onClick = { onSongClick(state.recentlyPlayed, index) },
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(message)
    }
}
