package com.jpishimwe.syncplayer.ui.home.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.jpishimwe.syncplayer.R
import androidx.compose.ui.Modifier
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.components.SongItem
import com.jpishimwe.syncplayer.ui.components.SongItemVariant
import com.jpishimwe.syncplayer.ui.components.SongMenuAction
import com.jpishimwe.syncplayer.ui.components.SortFilterBar
import com.jpishimwe.syncplayer.ui.shared.MetadataUiState
import com.jpishimwe.syncplayer.ui.shared.SortOrder
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import androidx.compose.ui.tooling.preview.Preview

private val faveSortOptions = listOf(SortOrder.BY_TITLE, SortOrder.BY_ARTIST, SortOrder.BY_PLAY_COUNT)

@Composable
fun FavoriteTabScreen(
    metadataUiState: MetadataUiState.Loaded,
    currentSongId: Long?,
    isPlaying: Boolean,
    sortOrder: SortOrder,
    onSortOrderChanged: (SortOrder) -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (songIds: List<Long>) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val songs = metadataUiState.favorites

    if (songs.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.empty_favorites))
        }
        return
    }

    val sorted =
        remember(songs, sortOrder) {
            when (sortOrder) {
                SortOrder.BY_ARTIST -> songs.sortedBy { it.artist }
                SortOrder.BY_PLAY_COUNT -> songs.sortedByDescending { it.rating }
                else -> songs.sortedBy { it.title }
            }
        }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = MiniPlayerPeek)) {
        stickyHeader {
            SortFilterBar(
                selectedSort = sortOrder,
                sortOptions = faveSortOptions,
                onSortClick = onSortOrderChanged,
                onShuffle = { onSongClick(sorted.shuffled(), 0) },
                onPlayAll = { onSongClick(sorted, 0) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        itemsIndexed(sorted, key = { _, song -> song.id }) { index, song ->
            SongItem(
                song = song,
                onClick = { onSongClick(sorted, index) },
                isPlaying = song.id == currentSongId && isPlaying,
                variant = SongItemVariant.Default,
                showRating = true,
                menuActions =
                    listOf(
                        SongMenuAction.PlayNext,
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

                        SongMenuAction.AddToQueue -> {
                            onAddToQueue(song)
                        }

                        SongMenuAction.AddToPlaylist -> {
                            onAddToPlaylist(listOf(song.id))
                        }

                        SongMenuAction.GoToArtist -> {
                            onNavigateToArtist(song.artist)
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
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun FavoriteTabScreenPreview() {
    SyncPlayerTheme(darkTheme = true) {
        FavoriteTabScreen(
            metadataUiState = MetadataUiState.Loaded(
                favorites = listOf(
                    Song(
                        id = 1, title = "Imagine", artist = "John Lennon", albumArtist = "John Lennon",
                        album = "Imagine", albumId = 1, duration = 183000, trackNumber = 1,
                        year = 1971, dateAdded = 0, contentUri = null, albumArtUri = null, rating = 5,
                    ),
                ),
                mostPlayed = emptyList(),
                recentlyPlayed = emptyList(),
                recentlyPlayedAlbums = emptyList(),
                recentlyPlayedArtists = emptyList(),
            ),
            currentSongId = null,
            isPlaying = false,
            sortOrder = SortOrder.BY_TITLE,
            onSortOrderChanged = {},
            onSongClick = { _, _ -> },
            onPlayNext = {},
            onAddToQueue = {},
            onAddToPlaylist = {},
            onNavigateToArtist = {},
            onNavigateToAlbum = { _, _ -> },
        )
    }
}
