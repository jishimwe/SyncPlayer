package com.jpishimwe.syncplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jpishimwe.syncplayer.ui.library.LibraryScreen
import com.jpishimwe.syncplayer.ui.player.NowPlayingScreen

sealed class Screen(
    val route: String,
) {
    data object Library : Screen("library")

    data object NowPlaying : Screen("now_playing")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
        modifier = modifier,
    ) {
        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                modifier,
            )
        }

        composable(Screen.NowPlaying.route) {
            NowPlayingScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }
    }
}
