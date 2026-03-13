package com.jpishimwe.syncplayer.ui.home.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.library.MetadataUiState
import com.jpishimwe.syncplayer.ui.player.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.player.components.SongItem
import com.jpishimwe.syncplayer.ui.player.components.SongItemVariant
import com.jpishimwe.syncplayer.ui.player.components.SongMenuAction
import com.jpishimwe.syncplayer.ui.player.components.SortFilterBar

private val faveSortOptions = listOf("Title", "Artist", "Rating")

@Composable
fun FavoriteTabScreen(
    metadataUiState: MetadataUiState.Loaded,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val songs = metadataUiState.favorites

    if (songs.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No favourites yet — rate a song 4 stars or higher")
        }
        return
    }

    var selectedSort by remember { mutableStateOf(faveSortOptions.first()) }

    // Sort locally so the list stays reactive without a ViewModel round-trip
    val sorted =
        remember(songs, selectedSort) {
            when (selectedSort) {
                "Artist" -> songs.sortedBy { it.artist }
                "Rating" -> songs.sortedByDescending { it.rating }
                else -> songs.sortedBy { it.title }
            }
        }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = MiniPlayerPeek)) {
        stickyHeader {
            SortFilterBar(
                sortLabel = selectedSort,
                sortOptions = faveSortOptions,
                onSortClick = { selectedSort = it.label },
                onShuffle = { onSongClick(sorted.shuffled(), 0) },
                onPlayAll = { onSongClick(sorted, 0) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        itemsIndexed(sorted, key = { _, song -> song.id }) { index, song ->
            SongItem(
                song = song,
                onClick = { onSongClick(sorted, index) },
                isPlaying = false, // wire currentSongId if desired
                variant = SongItemVariant.Default,
                showRating = true,
                menuActions =
                    listOf(
                        SongMenuAction.PlayNext,
                        SongMenuAction.AddToQueue,
                        SongMenuAction.GoToArtist,
                        SongMenuAction.GoToAlbum,
                    ),
                onMenuAction = {}, // TODO: wire via PlayerViewModel
            )
        }
    }
}
