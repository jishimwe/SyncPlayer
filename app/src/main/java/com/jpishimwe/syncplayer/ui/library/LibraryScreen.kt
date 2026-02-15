package com.jpishimwe.syncplayer.ui.library

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.jpishimwe.syncplayer.ui.player.components.MiniPlayer

@Composable
fun LibraryScreen(
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbumDetail: (albumId: Long, albumName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    playerViewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    PermissionHandler {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                    onNavigateToAlbumDetail(artistName.hashCode().toLong(), artistName)
                },
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenContent(
    uiState: LibraryUiState,
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    onRetry: () -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onAlbumClick: (albumId: Long, albumName: String) -> Unit,
    onArtistClick: (artistName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("SyncPlayer") })
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            when (uiState) {
                is LibraryUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is LibraryUiState.Error -> {
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

                is LibraryUiState.Loaded -> {
                    when (selectedTab) {
                        LibraryTab.SONGS -> SongsTab(uiState, onSongClick)
                        LibraryTab.ALBUMS -> AlbumsTab(uiState, onAlbumClick)
                        LibraryTab.ARTISTS -> ArtistsTab(uiState, onArtistClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun SongsTab(
    state: LibraryUiState.Loaded,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
) {
    if (state.songs.isEmpty()) {
        EmptyState("No songs found")
    } else {
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

@Composable
private fun AlbumsTab(
    state: LibraryUiState.Loaded,
    onAlbumClick: (albumId: Long, albumName: String) -> Unit,
) {
    if (state.albums.isEmpty()) {
        EmptyState("No albums found")
    } else {
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
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(message)
    }
}
