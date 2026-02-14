package com.jpishimwe.syncplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.jpishimwe.syncplayer.ui.navigation.NavGraph
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme

@Composable
fun SyncPlayerApp(modifier: Modifier = Modifier) {
    SyncPlayerTheme {
        val navController = rememberNavController()
        NavGraph(
            navController = navController,
            modifier = modifier,
        )
    }
}
