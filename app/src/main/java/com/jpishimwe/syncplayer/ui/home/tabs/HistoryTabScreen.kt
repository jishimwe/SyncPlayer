package com.jpishimwe.syncplayer.ui.home.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.components.AlbumGridItem
import com.jpishimwe.syncplayer.ui.components.AlbumPlaybackState
import com.jpishimwe.syncplayer.ui.components.ArtistItem
import com.jpishimwe.syncplayer.ui.components.ArtistPlaybackState
import com.jpishimwe.syncplayer.ui.components.CollapsibleSectionHeader
import com.jpishimwe.syncplayer.ui.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.components.SongItem
import com.jpishimwe.syncplayer.ui.components.SongMenuAction
import com.jpishimwe.syncplayer.ui.library.MetadataUiState

/**
 * History tab: three collapsible sections for recently played songs, albums, and artists.
 *
 * Albums and artists use .chunked(2) rows inside LazyColumn items to avoid nested
 * lazy layouts (LazyVerticalGrid cannot be nested inside LazyColumn).
 */
@Composable
fun HistoryTabScreen(
    metadataUiState: MetadataUiState.Loaded,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onAlbumClick: (albumId: Long, albumName: String) -> Unit,
    onArtistClick: (artistName: String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recentSongs = metadataUiState.recentlyPlayed
    val recentAlbums = metadataUiState.recentlyPlayedAlbums
    val recentArtists = metadataUiState.recentlyPlayedArtists

    if (recentSongs.isEmpty() && recentAlbums.isEmpty() && recentArtists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No listening history yet", style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    var songsExpanded by rememberSaveable { mutableStateOf(true) }
    var albumsExpanded by rememberSaveable { mutableStateOf(true) }
    var artistsExpanded by rememberSaveable { mutableStateOf(true) }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = MiniPlayerPeek)) {
        // ── Songs ──────────────────────────────────────────────────────────
        if (recentSongs.isNotEmpty()) {
            stickyHeader(key = "header_songs") {
                CollapsibleSectionHeader(
                    title = "Recently played",
                    isExpanded = songsExpanded,
                    onToggle = { songsExpanded = !songsExpanded },
                    onShuffle = { onSongClick(recentSongs.shuffled(), 0) },
                    onPlayAll = { onSongClick(recentSongs, 0) },
                )
            }

            if (songsExpanded) {
                itemsIndexed(recentSongs, key = { _, song -> song.id }) { index, song ->
                    SongItem(
                        song = song,
                        onClick = { onSongClick(recentSongs, index) },
                        isPlaying = false,
                        menuActions =
                            listOf(
                                SongMenuAction.PlayNext,
                                SongMenuAction.AddToQueue,
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

                                SongMenuAction.GoToArtist -> {
                                    onArtistClick(song.artist)
                                }

                                SongMenuAction.GoToAlbum -> {
                                    onAlbumClick(song.albumId, song.album)
                                }

                                else -> {}
                            }
                        },
                    )
                }
            }
        }

        // ── Albums ─────────────────────────────────────────────────────────
        if (recentSongs.isNotEmpty()) {
            stickyHeader(key = "header_albums") {
                CollapsibleSectionHeader(
                    title = "Recently played albums",
                    isExpanded = albumsExpanded,
                    onToggle = { albumsExpanded = !albumsExpanded },
                    onShuffle = { },
                    onPlayAll = { },
                )
            }

            if (albumsExpanded) {
                // Pair albums into 2-column rows — avoids nested LazyVerticalGrid
                val albumRows = recentAlbums.chunked(2)
                items(albumRows.size, key = { "album_row_$it" }) { rowIndex ->
                    val rowAlbums = albumRows[rowIndex]
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowAlbums.forEach { album ->
                            AlbumGridItem(
                                album = album,
                                playbackState = AlbumPlaybackState.Default,
                                onClick = { onAlbumClick(album.id, album.name) },
                                onPlayClick = { onAlbumClick(album.id, album.name) },
                                onMenuClick = {},
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Fill the second column with empty space if row has only one item
                        if (rowAlbums.size < 2) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Artists ────────────────────────────────────────────────────────
        if (recentArtists.isNotEmpty()) {
            stickyHeader(key = "header_artists") {
                CollapsibleSectionHeader(
                    title = "Recently Played Artists",
                    isExpanded = artistsExpanded,
                    onToggle = { artistsExpanded = !artistsExpanded },
                    onShuffle = {},
                    onPlayAll = {},
                )
            }
            if (artistsExpanded) {
                val artistRows = recentArtists.chunked(2)
                items(artistRows.size, key = { "artist_row_$it" }) { rowIndex ->
                    val rowArtists = artistRows[rowIndex]
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowArtists.forEach { artist ->
                            ArtistItem(
                                artistName = artist.name,
                                playbackState = ArtistPlaybackState.Default,
                                onClick = { onArtistClick(artist.name) },
                                modifier = Modifier.weight(1f),
                                imageUri = artist.artUri,
                                onPlayPause = { onArtistClick(artist.name) },
                                onMenuClick = {},
                            )
                        }
                        if (rowArtists.size < 2) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
