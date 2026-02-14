package com.jpishimwe.syncplayer.ui.player

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NowPlaingScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    NowPlayingScreenContent(
        uiState = uiState.value,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        formatTime = viewModel::formatTime,
    )
}
