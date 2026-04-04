package com.jpishimwe.syncplayer.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.jpishimwe.syncplayer.ui.theme.MotionTokens
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
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
import com.jpishimwe.syncplayer.ui.albumdetail.AlbumDetailScreen
import com.jpishimwe.syncplayer.ui.artistdetail.ArtistDetailScreen
import com.jpishimwe.syncplayer.ui.components.MiniPlayer
import com.jpishimwe.syncplayer.ui.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.components.PlaylistPickerSheet
import com.jpishimwe.syncplayer.ui.effect.ScreenshotHolder
import com.jpishimwe.syncplayer.ui.home.HomeScreen
import com.jpishimwe.syncplayer.ui.player.NowPlayingScreen
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import com.jpishimwe.syncplayer.ui.player.rememberPlayerSheetState
import com.jpishimwe.syncplayer.ui.playlists.PlaylistDetailScreen
import com.jpishimwe.syncplayer.ui.playlists.PlaylistEvent
import com.jpishimwe.syncplayer.ui.playlists.PlaylistUiState
import com.jpishimwe.syncplayer.ui.playlists.PlaylistViewModel
import com.jpishimwe.syncplayer.ui.settings.SettingsScreen
import com.jpishimwe.syncplayer.ui.shared.LibraryViewModel
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch

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

    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    var searchActive by rememberSaveable { mutableStateOf(false) }

    // --- Playlist picker state (shared across detail screens) ---
    val playlistViewModel: PlaylistViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner)
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    var pendingSongIds by remember { mutableStateOf<List<Long>?>(null) }

    // Measured at runtime — no hardcoding
    var overlayHeightPx by remember { mutableIntStateOf(0) }
    val overlayHeightDp = with(LocalDensity.current) { overlayHeightPx.toDp() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.surface)) {
        val density = LocalDensity.current
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val miniPeekPx = with(density) { MiniPlayerPeek.toPx() }
        val sheetState = rememberPlayerSheetState(screenHeightPx, miniPeekPx)
        val scope = rememberCoroutineScope()

        // Flip status bar icon color at the 50% progress crossover.
        // Now Playing has a dark blurred background regardless of theme, so icons must be light.
        // On collapse, restore to the theme default.
        val activity = LocalActivity.current
        val darkTheme = isSystemInDarkTheme()
        LaunchedEffect(sheetState.isExpanded) {
            val window = activity?.window ?: return@LaunchedEffect
            WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars =
                if (sheetState.isExpanded) false else !darkTheme
        }

        // Expand helper used by all screens that have a "go to now playing" action.
        // Tap path: capture screenshot synchronously before animating.
        val expandNowPlaying: () -> Unit = {
            ScreenshotHolder.capture(view)
            scope.launch { sheetState.expand() }
        }

        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                enterTransition = { fadeIn(tween(MotionTokens.DurationMedium2)) },
                exitTransition = { fadeOut(tween(MotionTokens.DurationShort4)) },
                popEnterTransition = { fadeIn(tween(MotionTokens.DurationMedium2)) },
                popExitTransition = { fadeOut(tween(MotionTokens.DurationShort4)) },
                modifier = modifier
                    .padding(top = if (isOnTopLevelScreen) overlayHeightDp else 0.dp)
                    // Always reserve space for mini peek when a song is loaded
                    .padding(bottom = if (playerState.currentSong != null) MiniPlayerPeek else 0.dp),
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

                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }

        // Overlay: title bar + tab row, measures itself and reports height.
        // Hidden while the sheet is expanded.
        if (isOnTopLevelScreen && !sheetState.isExpanded) {
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

        // System back while sheet is expanded → collapse instead of nav pop
        BackHandler(enabled = sheetState.isExpanded) {
            scope.launch { sheetState.collapse() }
        }

        // --- Player sheet panel ---
        // Always composed when a song is loaded. Translates between mini (progress=0)
        // and full-screen (progress=1). Contains both MiniPlayer and NowPlayingScreen
        // as siblings, crossfading via their respective alphas.
        if (playerState.currentSong != null) {
            val velocityTracker = remember { VelocityTracker() }

            // Read derived values in composition scope so recomposition tracks progress changes
            val sheetTranslateY = sheetState.translateY
            val sheetCornerRadius = sheetState.cornerRadius
            val sheetPlayerAlpha = sheetState.playerAlpha
            val sheetMiniAlpha = sheetState.miniAlpha

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = sheetTranslateY
                        shape = RoundedCornerShape(sheetCornerRadius)
                        clip = true
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { _ ->
                                velocityTracker.resetTracking()
                                // Drag-path screenshot: only on first expand (progress == 0)
                                if (sheetState.progress.value == 0f) {
                                    ScreenshotHolder.capture(view)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                sheetState.onDragDelta(dragAmount.y)
                            },
                            onDragEnd = {
                                val velocity = velocityTracker.calculateVelocity()
                                sheetState.onDragEnd(velocity.y)
                            },
                            onDragCancel = {
                                sheetState.onDragEnd(0f)
                            },
                        )
                    },
            ) {
                // Layer 0: full player (NowPlayingScreen handles its own BlurredBackground)
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = sheetPlayerAlpha },
                ) {
                    NowPlayingScreen(
                        onNavigateBack = { scope.launch { sheetState.collapse() } },
                        onSheetDrag = { deltaY -> sheetState.onDragDelta(deltaY) },
                        onSheetDragEnd = { velocityY -> sheetState.onDragEnd(velocityY) },
                        isFullyExpanded = sheetState.isExpanded,
                    )
                }

                // Layer 1: mini-player strip at the bottom of the panel
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .graphicsLayer { alpha = sheetMiniAlpha },
                ) {
                    MiniPlayer(
                        uiState = playerState,
                        onEvent = playerViewModel::onEvent,
                        onClick = {
                            ScreenshotHolder.capture(view)
                            scope.launch { sheetState.expand() }
                        },
                    )
                }
            }
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
