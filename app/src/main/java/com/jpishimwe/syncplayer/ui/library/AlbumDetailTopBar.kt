package com.jpishimwe.syncplayer.ui.library

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumDetailTopBar(
    albumName: String,
    artist: String,
    songCount: Int,
    onNavigateBack: () -> Unit,
    onAddAllToPlaylist: () -> Unit = {},
    contentAlpha: Float,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    DetailTopBar(
        title = albumName,
        subtitle = if (artist.isNotEmpty()) "$artist · $songCount songs" else "",
        onNavigateBack = onNavigateBack,
        onAddAllToPlaylist = onAddAllToPlaylist,
        contentAlpha = contentAlpha,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
}