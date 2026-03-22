package com.jpishimwe.syncplayer.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.jpishimwe.syncplayer.ui.components.MiniPlayer
import com.jpishimwe.syncplayer.ui.components.PlaylistPickerSheet
import com.jpishimwe.syncplayer.ui.home.HomeScreen
import com.jpishimwe.syncplayer.ui.albumdetail.AlbumDetailScreen
import com.jpishimwe.syncplayer.ui.artistdetail.ArtistDetailScreen
import com.jpishimwe.syncplayer.ui.shared.LibraryViewModel
import com.jpishimwe.syncplayer.ui.player.NowPlayingScreen
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import com.jpishimwe.syncplayer.ui.playlists.PlaylistDetailScreen
import com.jpishimwe.syncplayer.ui.playlists.PlaylistEvent
import com.jpishimwe.syncplayer.ui.playlists.PlaylistUiState
import com.jpishimwe.syncplayer.ui.playlists.PlaylistViewModel
import com.jpishimwe.syncplayer.ui.settings.SettingsScreen
import com.jpishimwe.syncplayer.ui.effect.ScreenshotHolder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    libraryViewModel: LibraryViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val currentRoute =
        navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route
    val isOnTopLevelScreen = currentRoute == Screen.Home.route

    val view = LocalView.current

    // --- Now Playing expansion state (replaces navigation) ---
    var isNowPlayingExpanded by rememberSaveable { mutableStateOf(false) }
    val expandNowPlaying: () -> Unit = {
        ScreenshotHolder.capture(view)
        isNowPlayingExpanded = true
    }
    val collapseNowPlaying = { isNowPlayingExpanded = false }

    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    var searchActive by rememberSaveable { mutableStateOf(false) }

    // --- Playlist picker state (shared across detail screens) ---
    val playlistViewModel: PlaylistViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner)
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    var pendingSongIds by remember { mutableStateOf<List<Long>?>(null) }

    // Measured at runtime — no hardcoding
    var overlayHeightPx by remember { mutableIntStateOf(0) }
    val overlayHeightDp = with(LocalDensity.current) { overlayHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.surface)) {
        SharedTransitionLayout {
            // Content sits below the overlay, padded by its actual measured height
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = modifier.padding(top = if (isOnTopLevelScreen) overlayHeightDp else 0.dp),
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        selectedTab = selectedTab,
                        onSelectedTabChanged = { selectedTab = it },
                        onNavigateToNowPlaying = expandNowPlaying,
                        onNavigateToAlbumDetail = { id, name ->
                            ScreenshotHolder.capture(view)
                            navController.navigate(Screen.AlbumDetail.createRoute(id, name))
                        },
                        onNavigateToArtistDetail = { name ->
                            ScreenshotHolder.capture(view)
                            navController.navigate(Screen.ArtistDetail.createRoute(name))
                        },
                        onNavigateToPlaylistDetail = { id, name ->
                            ScreenshotHolder.capture(view)
                            navController.navigate(Screen.PlaylistDetail.createRoute(id, name))
                        },
                        playerViewModel = playerViewModel,
                        libraryViewModel = libraryViewModel,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                        modifier = modifier,
                    )
                }

                composable(
                    Screen.AlbumDetail.route,
                    arguments =
                        listOf(
                            navArgument("albumId") { type = NavType.LongType },
                            navArgument("albumName") { type = NavType.StringType },
                        ),
                ) { backStackEntry ->
                    AlbumDetailScreen(
                        albumId = backStackEntry.arguments?.getLong("albumId") ?: 0L,
                        albumName = backStackEntry.arguments?.getString("albumName") ?: "",
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToNowPlaying = expandNowPlaying,
                        onNavigateToArtistDetail = { name ->
                            ScreenshotHolder.capture(view)
                            navController.navigate(Screen.ArtistDetail.createRoute(name))
                        },
                        onAddToPlaylist = { songIds -> pendingSongIds = songIds },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                    )
                }

                composable(
                    Screen.ArtistDetail.route,
                    arguments =
                        listOf(
                            navArgument("artistName") { type = NavType.StringType },
                        ),
                ) { backStackEntry ->
                    ArtistDetailScreen(
                        artistName = backStackEntry.arguments?.getString("artistName") ?: "",
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToNowPlaying = expandNowPlaying,
                        onAddToPlaylist = { songIds -> pendingSongIds = songIds },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                    )
                }

                composable(
                    Screen.PlaylistDetail.route,
                    arguments =
                        listOf(
                            navArgument("playlistId") { type = NavType.LongType },
                            navArgument("playlistName") { type = NavType.StringType },
                        ),
                ) { backStackEntry ->
                    PlaylistDetailScreen(
                        playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L,
                        playlistName = backStackEntry.arguments?.getString("playlistName") ?: "",
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToNowPlaying = expandNowPlaying,
                    )
                }

                // NowPlaying route removed — handled by AnimatedContent overlay below

                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }

        // Overlay: title bar + tab row, measures itself and reports height
        if (isOnTopLevelScreen && !isNowPlayingExpanded) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .onGloballyPositioned { overlayHeightPx = it.size.height },
            ) {
                if (searchActive) {
                    DockedSearchBarContent(
                        query = libraryViewModel.searchQuery.collectAsState().value,
                        active = searchActive,
                        onQueryChanged = libraryViewModel::onSearchQueryChanged,
                        onActiveChanged = { searchActive = it },
                        onClearSearchQuery = libraryViewModel::onClearSearchQuery,
                    )
                } else {
                    TopAppBarContent(
                        searchActive = searchActive,
                        onSearchActiveChanged = { searchActive = it },
                        onSettingsClicked = { navController.navigate(Screen.Settings.route) },
                    )
                    CustomTabRow(
                        selectedTab = selectedTab,
                        onSelectedTabChanged = { selectedTab = it },
                    )
                }
            }
        }

        // System back while Now Playing is expanded → collapse instead of nav pop
        BackHandler(enabled = isNowPlayingExpanded) {
            collapseNowPlaying()
        }

        // --- MiniPlayer: visible when song loaded and NOT expanded ---
        AnimatedVisibility(
            visible = playerState.currentSong != null && !isNowPlayingExpanded,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
        ) {
            MiniPlayer(
                uiState = playerState,
                onEvent = playerViewModel::onEvent,
                onClick = {
                    expandNowPlaying()
                },
            )
        }

        // --- Now Playing: full-screen overlay when expanded ---
        AnimatedVisibility(
            visible = isNowPlayingExpanded,
            enter =
                slideInVertically(
                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                    initialOffsetY = { fullHeight -> fullHeight },
                ) + fadeIn(animationSpec = tween(300)),
            exit =
                slideOutVertically(
                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                    targetOffsetY = { fullHeight -> fullHeight },
                ) + fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize(),
        ) {
            NowPlayingScreen(
                onNavigateBack = collapseNowPlaying,
            )
        }

        // --- Playlist picker bottom sheet (shared across detail screens) ---
        val playlists = (playlistUiState as? PlaylistUiState.Loaded)?.playlists ?: emptyList()
        pendingSongIds?.let { songIds ->
            PlaylistPickerSheet(
                playlists = playlists,
                onPlaylistSelected = { playlistId ->
                    playlistViewModel.onEvent(PlaylistEvent.AddSongsToPlaylist(playlistId, songIds))
                    pendingSongIds = null
                },
                onCreatePlaylist = { name ->
                    playlistViewModel.onEvent(PlaylistEvent.CreatePlaylist(name))
                },
                onDismiss = { pendingSongIds = null },
            )
        }
    }
}
