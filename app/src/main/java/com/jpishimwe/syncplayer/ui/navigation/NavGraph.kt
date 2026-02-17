package com.jpishimwe.syncplayer.ui.navigation

import android.net.Uri
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.jpishimwe.syncplayer.ui.library.AlbumDetailScreen
import com.jpishimwe.syncplayer.ui.library.ArtistDetailScreen
import com.jpishimwe.syncplayer.ui.library.LibraryScreen
import com.jpishimwe.syncplayer.ui.player.NowPlayingScreen
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import com.jpishimwe.syncplayer.ui.player.components.MiniPlayer
import com.jpishimwe.syncplayer.ui.playlists.PlaylistDetailScreen
import com.jpishimwe.syncplayer.ui.playlists.PlaylistsScreen

sealed class Screen(
    val route: String,
) {
    data object Library : Screen("library")

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

    data object Playlists : Screen("playlists")

    data object PlaylistDetail : Screen("playlist_detail/{playlistId}/{playlistName}") {
        fun createRoute(
            playlistId: Long,
            playlistName: String,
        ) = "playlist_detail/$playlistId/${Uri.encode(playlistName)}"
    }
}

private enum class BottomNavDestination(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
) {
    LIBRARY(Screen.Library, "Library", Icons.Default.LibraryMusic),
    PLAYLISTS(Screen.Playlists, "Playlists", Icons.AutoMirrored.Filled.PlaylistPlay),
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val isOnNowPlayingScreen: Boolean =
        navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route == Screen.NowPlaying.route

    Scaffold(
        bottomBar = {
            val currentRoute =
                navController
                    .currentBackStackEntryAsState()
                    .value
                    ?.destination
                    ?.route
            val showBottomNav = currentRoute in setOf(Screen.Library.route, Screen.Playlists.route)
            Log.d("NavGraph", "showBottomNav: $showBottomNav $currentRoute")

            Column {
                Log.d("NavGraph", "showBottomNav > Column: $showBottomNav")
                if (showBottomNav) {
                    NavigationBar {
                        BottomNavDestination.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.screen.route,
                                onClick = {
                                    navController.navigate(destination.screen.route) {
                                        popUpTo(Screen.Library.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }

                if (playerState.currentSong != null && !isOnNowPlayingScreen) {
                    MiniPlayer(
                        uiState = playerState,
                        onEvent = playerViewModel::onEvent,
                        onClick = { navController.navigate(Screen.NowPlaying.route) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = modifier.padding(padding),
        ) {
            composable(Screen.Library.route) {
                LibraryScreen(
                    onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                    onNavigateToAlbumDetail = { id, name ->
                        navController.navigate(Screen.AlbumDetail.createRoute(id, name))
                    },
                    onNavigateToArtistDetail = { name ->
                        navController.navigate(Screen.ArtistDetail.createRoute(name))
                    },
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

            composable(Screen.Playlists.route) {
                PlaylistsScreen(
                    onNavigateToPlaylistDetail = { id, name ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(id, name))
                    },
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
                NowPlayingScreen(
                    onNavigateBack = { navController.navigateUp() },
                )
            }
        }
    }
}
