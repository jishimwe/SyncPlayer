package com.jpishimwe.syncplayer.ui.library

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ArtistDetailTopBar(
    artistName: String,
    albumCount: Int,
    songCount: Int,
    onNavigateBack: () -> Unit,
    onAddAllToPlaylist: () -> Unit = {},
    contentAlpha: Float,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    DetailTopBar(
        title = artistName,
        subtitle = "$albumCount albums · $songCount songs",
        onNavigateBack = onNavigateBack,
        onAddAllToPlaylist = onAddAllToPlaylist,
        contentAlpha = contentAlpha,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
}