package com.jpishimwe.syncplayer.ui.home.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.jpishimwe.syncplayer.ui.library.AlbumGridItem
import com.jpishimwe.syncplayer.ui.library.LibraryUiState
import com.jpishimwe.syncplayer.ui.library.SortOrder
import com.jpishimwe.syncplayer.ui.player.components.AlphabeticalIndexSidebar
import com.jpishimwe.syncplayer.ui.player.components.SortFilterBar
import com.jpishimwe.syncplayer.ui.theme.frostedGlassRendered
import kotlinx.coroutines.launch

private val albumSortOptions = listOf(SortOrder.BY_ALBUM.label, SortOrder.BY_ARTIST.label)

@Composable
fun AlbumsTabScreen(
    libraryUiState: LibraryUiState.Loaded,
    currentAlbumId: Long?,
    onAlbumClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val albums = libraryUiState.albums

    if (albums.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No albums found")
        }
        return
    }

    var selectedSort by remember { mutableStateOf(SortOrder.BY_ALBUM) }
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
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = barHeightDp + 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(albums, key = { it.id }) { album ->
                AlbumGridItem(
                    album = album,
                    onClick = { onAlbumClick(album.id, album.name) },
                    isPlaying = album.id == currentAlbumId,
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
                    scope.launch { gridState.scrollToItem(targetIndex + 1) } // +1 for sort bar
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        )

        // Sticky sort bar spanning full width
        SortFilterBar(
            sortLabel = selectedSort.label,
            sortOptions = albumSortOptions,
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
