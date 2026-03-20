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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jpishimwe.syncplayer.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.components.AlbumGridItem
import com.jpishimwe.syncplayer.ui.components.AlbumPlaybackState
import com.jpishimwe.syncplayer.ui.components.AlphabeticalIndexSidebar
import com.jpishimwe.syncplayer.ui.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.components.SortFilterBar
import com.jpishimwe.syncplayer.ui.library.LibraryUiState
import com.jpishimwe.syncplayer.ui.library.SortOrder
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumsTabScreen(
    libraryUiState: LibraryUiState.Loaded,
    currentAlbumId: Long?,
    sortOrder: SortOrder,
    onSortOrderChanged: (SortOrder) -> Unit,
    onAlbumClick: (Long, String) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val albums = libraryUiState.albums

    if (albums.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.empty_albums))
        }
        return
    }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    var barHeightPx by remember { mutableIntStateOf(0) }
    val barHeightDp = with(LocalDensity.current) { barHeightPx.toDp() }

    val letters =
        remember(albums) {
            albums.map { it.name.first().uppercaseChar() }.distinct().sorted()
        }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = barHeightDp + 8.dp, bottom = MiniPlayerPeek),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(albums, key = { it.id }) { album ->
                val playbackState =
                    when {
                        album.id != currentAlbumId -> AlbumPlaybackState.Default
                        album.id == currentAlbumId -> AlbumPlaybackState.Playing
                        else -> AlbumPlaybackState.Paused
                    }
                AlbumGridItem(
                    album = album,
                    onClick = { onAlbumClick(album.id, album.name) },
                    playbackState = playbackState,
                    onMenuClick = { onAlbumClick(album.id, album.name) },
                    onPlayClick = { onAlbumClick(album.id, album.name) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        }

        AlphabeticalIndexSidebar(
            letters = letters,
            onLetterSelected = { letter ->
                val targetIndex =
                    albums.indexOfFirst {
                        it.name.first().uppercaseChar() == letter
                    }
                if (targetIndex >= 0) {
                    scope.launch { gridState.scrollToItem(targetIndex) }
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        )

        // Sticky sort bar spanning full width
        SortFilterBar(
            selectedSort = sortOrder,
            sortOptions = listOf(SortOrder.BY_ALBUM, SortOrder.BY_ARTIST),
            onSortClick = onSortOrderChanged,
            onShuffle = { onSongClick(libraryUiState.songs.shuffled(), 0) },
            onPlayAll = { onSongClick(libraryUiState.songs, 0) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { barHeightPx = it.height },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun AlbumsTabScreenPreview() {
    SyncPlayerTheme(darkTheme = true) {
        AlbumsTabScreen(
            libraryUiState = LibraryUiState.Loaded(
                songs = emptyList(),
                albums = listOf(
                    Album(id = 1, name = "Abbey Road", artist = "The Beatles", songCount = 17, albumArtUri = null),
                    Album(id = 2, name = "Dark Side of the Moon", artist = "Pink Floyd", songCount = 10, albumArtUri = null),
                ),
                artists = emptyList(),
            ),
            currentAlbumId = null,
            sortOrder = SortOrder.BY_ALBUM,
            onSortOrderChanged = {},
            onAlbumClick = { _, _ -> },
            onSongClick = { _, _ -> },
        )
    }
}
