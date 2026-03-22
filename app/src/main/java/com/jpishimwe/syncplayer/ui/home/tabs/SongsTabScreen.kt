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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.jpishimwe.syncplayer.R
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.components.AlphabeticalIndexSidebar
import com.jpishimwe.syncplayer.ui.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.components.SongItem
import com.jpishimwe.syncplayer.ui.components.SongItemVariant
import com.jpishimwe.syncplayer.ui.components.SongMenuAction
import com.jpishimwe.syncplayer.ui.components.SortFilterBar
import com.jpishimwe.syncplayer.ui.shared.LibraryUiState
import com.jpishimwe.syncplayer.ui.shared.SortOrder
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import kotlinx.coroutines.launch

@Composable
fun SongsTabScreen(
    libraryUiState: LibraryUiState.Loaded,
    currentSongId: Long?,
    sortOrder: SortOrder,
    onSortOrderChanged: (SortOrder) -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (Long, String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onPlayNow: (Song) -> Unit,
    onAddToPlaylist: (songIds: List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    SongsTabScreenContent(
        songs = libraryUiState.songs,
        currentSongId = currentSongId,
        sortOrder = sortOrder,
        onSortOrderChanged = onSortOrderChanged,
        onSongClick = onSongClick,
        onNavigateToArtist = onNavigateToArtist,
        onNavigateToAlbum = onNavigateToAlbum,
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        onPlayNow = onPlayNow,
        onAddToPlaylist = onAddToPlaylist,
        modifier = modifier,
    )
}

@Composable
fun SongsTabScreenContent(
    songs: List<Song>,
    currentSongId: Long?,
    sortOrder: SortOrder,
    onSortOrderChanged: (SortOrder) -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (Long, String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onPlayNow: (Song) -> Unit,
    onAddToPlaylist: (songIds: List<Long>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (songs.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.empty_songs))
        }
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var sortBarHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val sortBarHeightDp = with(density) { sortBarHeightPx.toDp() }

    // Build letter index based on active sort field.
    // Numeric sorts (duration, date, play count) get an empty map → sidebar hidden.
    val letterIndexMap: Map<Char, Int> =
        remember(songs, sortOrder) {
            val keySelector: ((Song) -> Char)? =
                when (sortOrder) {
                    SortOrder.BY_TITLE -> { song -> song.title.first().uppercaseChar() }

                    SortOrder.BY_ARTIST -> { song -> song.artist.first().uppercaseChar() }

                    SortOrder.BY_ALBUM -> { song -> song.album.first().uppercaseChar() }

                    SortOrder.BY_DURATION,
                    SortOrder.BY_DATE_ADDED,
                    SortOrder.BY_PLAY_COUNT,
                    -> null
                }
            if (keySelector == null) {
                emptyMap()
            } else {
                buildMap {
                    songs.forEachIndexed { index, song ->
                        val letter = keySelector(song)
                        if (!containsKey(letter)) put(letter, index)
                    }
                }
            }
        }
    val letters = remember(letterIndexMap) { letterIndexMap.keys.sorted() }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = sortBarHeightDp, bottom = MiniPlayerPeek),
        ) {
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
                            SongMenuAction.PlayNext -> {
                                onPlayNext(song)
                            }

                            SongMenuAction.PlayNow -> {
                                onPlayNow(song)
                            }

                            SongMenuAction.AddToQueue -> {
                                onAddToQueue(song)
                            }

                            SongMenuAction.GoToArtist -> {
                                onNavigateToArtist(song.artist)
                            }

                            SongMenuAction.AddToPlaylist -> {
                                onAddToPlaylist(listOf(song.id))
                            }

                            SongMenuAction.GoToAlbum -> {
                                onNavigateToAlbum(song.albumId, song.album)
                            }

                            else -> {}
                        }
                    },
                )
            }
        }

        // A–Z sidebar on right edge
        AlphabeticalIndexSidebar(
            letters = letters,
            onLetterSelected = { letter ->
                val targetIndex = letterIndexMap[letter]
                if (targetIndex != null) {
                    scope.launch { listState.scrollToItem(targetIndex) }
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        )

        // Overlay sort bar on top so its dropdown popup is not clipped by LazyColumn bounds
        SortFilterBar(
            selectedSort = sortOrder,
            onSortClick = onSortOrderChanged,
            onShuffle = { onSongClick(songs.shuffled(), 0) },
            onPlayAll = { onSongClick(songs, 0) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .onSizeChanged { sortBarHeightPx = it.height },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun SongsTabScreenPreview() {
    SyncPlayerTheme(darkTheme = true) {
        SongsTabScreenContent(
            songs = emptyList(),
            currentSongId = null,
            sortOrder = SortOrder.BY_TITLE,
            onSortOrderChanged = {},
            onSongClick = { _, _ -> },
            onNavigateToArtist = {},
            onNavigateToAlbum = { _, _ -> },
            onPlayNext = {},
            onAddToQueue = {},
            onPlayNow = {},
        )
    }
}
