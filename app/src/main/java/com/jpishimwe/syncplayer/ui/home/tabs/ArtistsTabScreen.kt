package com.jpishimwe.syncplayer.ui.home.tabs

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.ui.library.LibraryUiState
import com.jpishimwe.syncplayer.ui.library.SortOrder
import com.jpishimwe.syncplayer.ui.player.components.AlphabeticalIndexSidebar
import com.jpishimwe.syncplayer.ui.player.components.ArtistItem
import com.jpishimwe.syncplayer.ui.player.components.ArtistPlaybackState
import com.jpishimwe.syncplayer.ui.player.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.player.components.SortFilterBar
import kotlinx.coroutines.launch

private val artistSortOptions = listOf(SortOrder.BY_ARTIST.label)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ArtistsTabScreen(
    libraryUiState: LibraryUiState.Loaded,
    currentArtistName: String?,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val artists = libraryUiState.artists

    if (artists.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No artists found")
        }
        return
    }

    var selectedSort by remember { mutableStateOf(SortOrder.BY_ARTIST) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    var barHeightPx by remember { mutableIntStateOf(0) }
    val barHeightDp = with(LocalDensity.current) { barHeightPx.toDp() }

    val letters =
        remember(artists) {
            artists.map { it.name.first().uppercaseChar() }.distinct().sorted()
        }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = barHeightDp + 8.dp,
                    bottom = MiniPlayerPeek,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
            }

            items(artists, key = { it.name }) { artist ->
                val playbackState =
                    when (artist.name) {
                        currentArtistName -> ArtistPlaybackState.Playing
                        else -> ArtistPlaybackState.Default
                    }
                ArtistItem(
                    artistName = artist.name,
                    imageUri = artist.artUri,
                    playbackState = playbackState,
                    onClick = { onArtistClick(artist.name) },
                    onPlayPause = { onArtistClick(artist.name) },
                    onMenuClick = {},
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        }

        AlphabeticalIndexSidebar(
            letters = letters,
            onLetterSelected = { letter ->
                val targetIndex =
                    artists.indexOfFirst {
                        it.name.first().uppercaseChar() == letter
                    }
                if (targetIndex >= 0) {
                    scope.launch { gridState.scrollToItem(targetIndex + 1) }
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        )

        // Sticky sort bar spanning full width
        SortFilterBar(
            sortLabel = selectedSort.label,
            sortOptions = artistSortOptions,
            onSortClick = { selectedSort = it },
            onShuffle = { /* TODO */ },
            onPlayAll = { /* TODO */ },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { barHeightPx = it.height },
        )
    }
}
