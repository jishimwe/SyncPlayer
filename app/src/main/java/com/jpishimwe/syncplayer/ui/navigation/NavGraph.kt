package com.jpishimwe.syncplayer.ui.navigation

import android.net.Uri
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.jpishimwe.syncplayer.ui.home.HomeScreen
import com.jpishimwe.syncplayer.ui.library.AlbumDetailScreen
import com.jpishimwe.syncplayer.ui.library.ArtistDetailScreen
import com.jpishimwe.syncplayer.ui.library.LibraryViewModel
import com.jpishimwe.syncplayer.ui.player.NowPlayingScreen
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import com.jpishimwe.syncplayer.ui.player.components.MiniPlayer
import com.jpishimwe.syncplayer.ui.playlists.PlaylistDetailScreen
import com.jpishimwe.syncplayer.ui.settings.SettingsScreen
import com.jpishimwe.syncplayer.ui.theme.LocalExtendedColorScheme
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.myAccentColor
import com.jpishimwe.syncplayer.ui.theme.noRippleClickable

sealed class Screen(
    val route: String,
) {
    data object Home : Screen("home")

    data object NowPlaying : Screen("now_playing")

    data object AlbumDetail : Screen("album_detail/{albumId}/{albumName}") {
        fun createRoute(
            albumId: Long,
            albumName: String,
        ) = "album_detail/$albumId/$albumName"
    }

    data object ArtistDetail : Screen("artist_detail/{artistName}") {
        fun createRoute(artistName: String) = "artist_detail/$artistName"
    }

    data object PlaylistDetail : Screen("playlist_detail/{playlistId}/{playlistName}") {
        fun createRoute(
            playlistId: Long,
            playlistName: String,
        ) = "playlist_detail/$playlistId/${Uri.encode(playlistName)}"
    }

    data object Settings : Screen("settings")
}

enum class LibraryTab(
    val label: String,
) {
    HISTORY("History"),
    FAVORITES("Faves"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
}

data class TabBounds(
    val x: Int,
    val width: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
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
    val isOnNowPlayingScreen = currentRoute == Screen.NowPlaying.route
    val isOnTopLevelScreen = currentRoute == Screen.Home.route

    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    var searchActive by rememberSaveable { mutableStateOf(false) }

    // Measured at runtime — no hardcoding
    var overlayHeightPx by remember { mutableIntStateOf(0) }
    val overlayHeightDp = with(LocalDensity.current) { overlayHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.surface)) {
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
                    onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                    onNavigateToAlbumDetail = { id, name ->
                        navController.navigate(Screen.AlbumDetail.createRoute(id, name))
                    },
                    onNavigateToArtistDetail = { name ->
                        navController.navigate(Screen.ArtistDetail.createRoute(name))
                    },
                    onNavigateToPlaylistDetail = { id, name ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(id, name))
                    },
                    playerViewModel = playerViewModel,
                    libraryViewModel = libraryViewModel,
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
                    onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
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
                    onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
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
                    onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                )
            }

            composable(Screen.NowPlaying.route) {
                NowPlayingScreen(onNavigateBack = { navController.navigateUp() })
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }

        // Overlay: title bar + tab row, measures itself and reports height
        if (isOnTopLevelScreen) {
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

        if (playerState.currentSong != null && !isOnNowPlayingScreen) {
            MiniPlayer(
                uiState = playerState,
                onEvent = playerViewModel::onEvent,
                onClick = { navController.navigate(Screen.NowPlaying.route) },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockedSearchBarContent(
    query: String = "",
    active: Boolean = true,
    onQueryChanged: (String) -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onClearSearchQuery: () -> Unit,
) {
    DockedSearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = { onQueryChanged(it) },
                onSearch = {},
                expanded = active,
                onExpandedChange = { onActiveChanged(it) },
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            )
        },
        expanded = active,
        onExpandedChange = {
            onActiveChanged(it)
            if (!it) {
                onQueryChanged("")
                onClearSearchQuery()
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 64.dp)
                .statusBarsPadding(),
    ) {}
}

@Composable
fun TopAppBarContent(
    searchActive: Boolean,
    onSearchActiveChanged: (Boolean) -> Unit,
    onSettingsClicked: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .statusBarsPadding()
                .background(
                    Brush.verticalGradient(
                        colorStops =
                            arrayOf(
                                0f to MaterialTheme.colorScheme.background,
                                0.5f to MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                                1f to Color.Transparent,
                            ),
                    ),
                ).padding(bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SyncPlayer",
                maxLines = 1,
                style = MaterialTheme.typography.displayMedium,
                color = LocalExtendedColorScheme.current.accentColor.color,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onSearchActiveChanged(!searchActive) }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onSettingsClicked) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun CustomTabRow(
    selectedTab: LibraryTab,
    onSelectedTabChanged: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val activeFontSize = MaterialTheme.typography.displaySmall.fontSize
    val inactiveFontSize = MaterialTheme.typography.headlineMedium.fontSize
    val textHeight = MaterialTheme.typography.displaySmall.lineHeight.value.dp
    val tabBounds = remember { mutableStateMapOf<LibraryTab, TabBounds>() }

    val density = LocalDensity.current

    var viewPortWidth by remember { mutableIntStateOf(0) }
    val halfViewportDp = with(density) { (viewPortWidth / 4).toDp() }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .onGloballyPositioned { viewPortWidth = it.size.width }
                .horizontalScroll(scrollState)
                .height(with(LocalDensity.current) { activeFontSize.toDp() + 24.dp })
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // ── Leading spacer: allows the FIRST tab to scroll to center ──
        Spacer(Modifier.width(halfViewportDp))

        LibraryTab.entries.forEach { tab ->
            val fontSize by animateFloatAsState(
                targetValue = if (selectedTab == tab) activeFontSize.value else inactiveFontSize.value,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "tabFontSize",
            )
            Text(
                text = tab.label,
                modifier =
                    Modifier
                        .onGloballyPositioned { coordinates ->
                            // Position relative to the Row (excludes the spacer?—no:
                            // coordinates are relative to the parent Row, so tabX already
                            // accounts for the leading spacer in the layout. But we
                            // want the offset relative to the first real tab, so we
                            // record positionInParent which IS the Row-local offset.)
                            val pos = coordinates.positionInParent()
                            tabBounds[tab] = TabBounds(pos.x.toInt(), coordinates.size.width)
                        }.alignByBaseline()
                        .noRippleClickable { onSelectedTabChanged(tab) }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .height(textHeight),
                style =
                    if (selectedTab == tab) {
                        MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize.sp,
                        )
                    } else {
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Thin,
                            fontSize = fontSize.sp,
                        )
                    },
                color =
                    if (selectedTab == tab) {
                        myAccentColor
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
        // ── Trailing spacer: allows the LAST tab to scroll to center ──
        Spacer(Modifier.width(halfViewportDp))
    }

    // Sync scroll when tab changes from pager swipe (not tap)
    LaunchedEffect(selectedTab, viewPortWidth) {
        Log.e("CustomTabRow", "LaunchedEffect: $selectedTab")
        val bounds = tabBounds[selectedTab] ?: return@LaunchedEffect
        if (viewPortWidth == 0) return@LaunchedEffect

        val (tabX, tabWidth) = bounds
        val targetScroll = tabX + (tabWidth / 2) - viewPortWidth / 2
        tabBounds[selectedTab]?.let { bounds ->
            Log.e(
                "LaunchedEffect",
                "onSelectedTabChanged: $selectedTab  | bounds -> ${tabBounds[selectedTab]} | $bounds.x scrollState ${scrollState.maxValue} | viewPortWidth $viewPortWidth",
            )
            scrollState.animateScrollTo(targetScroll.coerceIn(0, scrollState.maxValue))
        }
    }
}

@Preview
@Composable
fun TopAppBarContentPreview() {
    SyncPlayerTheme(darkTheme = true) {
        TopAppBarContent(
            searchActive = false,
            onSearchActiveChanged = {},
            onSettingsClicked = {},
        )
    }
}

@Preview
@Composable
fun CustomTabRowPreview() {
    SyncPlayerTheme(darkTheme = true) {
        CustomTabRow(selectedTab = LibraryTab.SONGS, onSelectedTabChanged = {})
    }
}
