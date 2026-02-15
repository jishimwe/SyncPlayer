package com.jpishimwe.syncplayer.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jpishimwe.syncplayer.ui.library.AlbumDetailScreen
import com.jpishimwe.syncplayer.ui.library.ArtistDetailScreen
import com.jpishimwe.syncplayer.ui.library.LibraryScreen
import com.jpishimwe.syncplayer.ui.player.NowPlayingScreen
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import com.jpishimwe.syncplayer.ui.player.components.MiniPlayer

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
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (playerState.currentSong != null) {
                MiniPlayer(
                    uiState = playerState,
                    onEvent = playerViewModel::onEvent,
                    onClick = { navController.navigate(Screen.NowPlaying.route) },
                )
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

            composable(Screen.NowPlaying.route) {
                NowPlayingScreen(
                    onNavigateBack = { navController.navigateUp() },
                )
            }
        }
    }
}
