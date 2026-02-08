package com.jpishimwe.syncplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jpishimwe.syncplayer.ui.library.LibraryScreen
import com.jpishimwe.syncplayer.ui.library.PermissionHandler
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme

@Composable
fun SyncPlayerApp(modifier: Modifier = Modifier) {
    SyncPlayerTheme {
        PermissionHandler {
            LibraryScreen(modifier = modifier)
        }
    }
}
