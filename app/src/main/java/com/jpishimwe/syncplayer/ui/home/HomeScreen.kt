package com.jpishimwe.syncplayer.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.home.tabs.AlbumsTabScreen
import com.jpishimwe.syncplayer.ui.home.tabs.ArtistsTabScreen
import com.jpishimwe.syncplayer.ui.home.tabs.FavoriteTabScreen
import com.jpishimwe.syncplayer.ui.home.tabs.HistoryTabScreen
import com.jpishimwe.syncplayer.ui.home.tabs.PlaylistsTabScreen
import com.jpishimwe.syncplayer.ui.home.tabs.SongsTabScreen
import com.jpishimwe.syncplayer.ui.library.LibraryUiState
import com.jpishimwe.syncplayer.ui.library.LibraryViewModel
import com.jpishimwe.syncplayer.ui.library.MetadataUiState
import com.jpishimwe.syncplayer.ui.library.MetadataViewModel
import com.jpishimwe.syncplayer.ui.library.PermissionHandler
import com.jpishimwe.syncplayer.ui.navigation.LibraryTab
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    selectedTab: LibraryTab,
    onSelectedTabChanged: (LibraryTab) -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbumDetail: (Long, String) -> Unit,
    onNavigateToArtistDetail: (String) -> Unit,
    onNavigateToPlaylistDetail: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    libraryViewModel: LibraryViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    metadataViewModel: MetadataViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    playerViewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    PermissionHandler {
        val libraryUiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
        val metadataUiState by metadataViewModel.uiState.collectAsStateWithLifecycle()
        val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            libraryViewModel.refreshLibrary()
        }

        val lifeCycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifeCycleOwner) {
            val observer =
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        libraryViewModel.onAppResumed()
                    }
                }
            lifeCycleOwner.lifecycle.addObserver(observer)
            onDispose { lifeCycleOwner.lifecycle.removeObserver(observer) }
        }
        val view = LocalView.current

        Column {
            HomeScreenContent(
                selectedTab = selectedTab,
                onSelectedTabChanged = onSelectedTabChanged,
                libraryUiState = libraryUiState,
                metadataUiState = metadataUiState,
                playerUiState = playerUiState,
                onRetry = libraryViewModel::refreshLibrary,
                onSongClick = { songs, index ->
                    playerViewModel.onEvent(PlayerEvent.PlaySongs(songs, index))
//                    onNavigateToNowPlaying()
                },
                onAlbumClick = { albumId, albumName ->
                    onNavigateToAlbumDetail(albumId, albumName)
                },
                onArtistClick = { artistName ->
                    onNavigateToArtistDetail(artistName)
                },
                onPlaylistClick = { playlistId, playlistName ->
                    onNavigateToPlaylistDetail(playlistId, playlistName)
                },
                onPlayNext = { playerViewModel.onEvent(PlayerEvent.PlayNext(it)) },
                onAddToQueue = { playerViewModel.onEvent(PlayerEvent.AddToQueue(it)) },
                onPlayNow = { playerViewModel.onEvent(PlayerEvent.PlaySongs(listOf(it), 0)) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreenContent(
    selectedTab: LibraryTab,
    onSelectedTabChanged: (LibraryTab) -> Unit,
    libraryUiState: LibraryUiState,
    metadataUiState: MetadataUiState,
    playerUiState: PlayerUiState,
    onRetry: () -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onAlbumClick: (Long, String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (Long, String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onPlayNow: (Song) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier,
) {
    val tab = LibraryTab.entries
    val pagerState =
        rememberPagerState(
            tab.indexOf(selectedTab),
            pageCount = { tab.size },
        )

    // Pager swipe → update selected tab in NavGraph
    // IMPORTANT: use settledPage, NOT currentPage.
    // currentPage updates mid-animation (passing through intermediate pages),
    // which causes a race: tap PLAYLISTS → animates → passes ARTISTS →
    // currentPage fires → selectedTab overwritten to ARTISTS.
    // settledPage only fires once the pager has fully come to rest.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.targetPage }.collect { page ->
            onSelectedTabChanged(tab[page])
        }
    }

    // Tab tap → scroll pager to match
    LaunchedEffect(selectedTab) {
        val targetPage = tab.indexOf(selectedTab)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            libraryUiState is LibraryUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            libraryUiState is LibraryUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(libraryUiState.message)
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }

            libraryUiState is LibraryUiState.Loaded && metadataUiState is MetadataUiState.Loaded -> {
                val currentSongId = playerUiState.currentSong?.id
                val currentAlbumId = playerUiState.currentSong?.albumId
                val currentArtistName = playerUiState.currentSong?.artist

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (tab[page]) {
                        LibraryTab.SONGS -> {
                            SongsTabScreen(
                                libraryUiState = libraryUiState,
                                currentSongId = currentSongId,
                                onSongClick = onSongClick,
                                onNavigateToArtist = onArtistClick,
                                onNavigateToAlbum = onAlbumClick,
                                onPlayNext = onPlayNext,
                                onAddToQueue = onAddToQueue,
                                onPlayNow = onPlayNow,
                            )
                        }

                        LibraryTab.ALBUMS -> {
                            AlbumsTabScreen(
                                libraryUiState = libraryUiState,
                                currentAlbumId = currentAlbumId,
                                onAlbumClick = onAlbumClick,
                                onSongClick = onSongClick,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                        }

                        LibraryTab.ARTISTS -> {
                            ArtistsTabScreen(
                                libraryUiState = libraryUiState,
                                currentArtistName = currentArtistName,
                                onArtistClick = onArtistClick,
                                onSongClick = onSongClick,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                        }

                        LibraryTab.FAVORITES -> {
                            FavoriteTabScreen(
                                metadataUiState = metadataUiState,
                                onSongClick = onSongClick,
                                onPlayNext = onPlayNext,
                                onAddToQueue = onAddToQueue,
                                onNavigateToArtist = onArtistClick,
                                onNavigateToAlbum = onAlbumClick,
                            )
                        }

                        LibraryTab.PLAYLISTS -> {
                            PlaylistsTabScreen(
                                onPlaylistClick = onPlaylistClick,
                                onPlayPlaylist = onPlaylistClick,
                                isPlaying = false,
                            )
                        }

                        LibraryTab.HISTORY -> {
                            HistoryTabScreen(
                                metadataUiState = metadataUiState,
                                onSongClick = onSongClick,
                                onAlbumClick = onAlbumClick,
                                onArtistClick = onArtistClick,
                                onPlayNext = onPlayNext,
                                onAddToQueue = onAddToQueue,
                            )
                        }
                    }
                }
            }
        }
    }
}
