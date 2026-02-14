package com.jpishimwe.syncplayer.ui.player

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
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
