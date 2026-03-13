package com.jpishimwe.syncplayer.ui.home.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.library.LibraryUiState
import com.jpishimwe.syncplayer.ui.library.SortOrder
import com.jpishimwe.syncplayer.ui.player.components.AlphabeticalIndexSidebar
import com.jpishimwe.syncplayer.ui.player.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.player.components.SongItem
import com.jpishimwe.syncplayer.ui.player.components.SongItemVariant
import com.jpishimwe.syncplayer.ui.player.components.SongMenuAction
import com.jpishimwe.syncplayer.ui.player.components.SortFilterBar
import kotlinx.coroutines.launch

private val songSortOptions = SortOrder.entries.map { it.label }

@Composable
fun SongsTabScreen(
    libraryUiState: LibraryUiState.Loaded,
    currentSongId: Long?,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val songs = libraryUiState.songs

    if (songs.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No songs found")
        }
        return
    }

    var selectedSort: SortOrder by remember { mutableStateOf(SortOrder.BY_TITLE) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Build A–Z index from first letters of song titles
    val letters =
        remember(songs) {
            songs.map { it.title.first().uppercaseChar() }.distinct().sorted()
        }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = MiniPlayerPeek),
        ) {
            // Sticky sort bar
            stickyHeader {
                SortFilterBar(
                    sortLabel = selectedSort.label,
                    sortOptions = songSortOptions,
                    onSortClick = { selectedSort = it },
                    onShuffle = { onSongClick(songs.shuffled(), 0) },
                    onPlayAll = { onSongClick(songs, 0) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                SongItem(
                    song = song,
                    onClick = { onSongClick(songs, index) },
                    isPlaying = song.id == currentSongId,
                    variant = SongItemVariant.Default,
                    menuActions =
                        listOf(
                            SongMenuAction.PlayNext,
                            SongMenuAction.PlayNow,
                            SongMenuAction.AddToQueue,
                            SongMenuAction.AddToPlaylist,
                            SongMenuAction.GoToArtist,
                            SongMenuAction.GoToAlbum,
                        ),
                    onMenuAction = { action ->
                        when (action) {
                            SongMenuAction.GoToArtist -> {
                                onNavigateToArtist(song.artist)
                            }

                            SongMenuAction.GoToAlbum -> {
                                onNavigateToAlbum(song.albumId, song.album)
                            }

                            else -> {} // TODO: wire PlayNext, PlayNow, AddToQueue, AddToPlaylist via PlayerViewModel
                        }
                    },
                )
            }
        }

        // A–Z sidebar on right edge
        AlphabeticalIndexSidebar(
            letters = letters,
            onLetterSelected = { letter ->
                val targetIndex =
                    songs.indexOfFirst {
                        it.title.first().uppercaseChar() == letter
                    }
                if (targetIndex >= 0) {
                    scope.launch { listState.scrollToItem(targetIndex + 1) } // +1 for sticky header
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}
