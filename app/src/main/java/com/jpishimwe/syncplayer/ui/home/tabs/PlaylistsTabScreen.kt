package com.jpishimwe.syncplayer.ui.home.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jpishimwe.syncplayer.ui.library.MetadataUiState

@Composable
fun PlaylistsTabScreen(metadataUiState: MetadataUiState.Loaded) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Playlists Tab Screen")
    }
}
