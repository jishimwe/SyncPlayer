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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.R
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
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import androidx.compose.ui.tooling.preview.Preview

/**
 * History tab: three collapsible sections for recently played songs, albums, and artists.
 *
 * Albums and artists use .chunked(2) rows inside LazyColumn items to avoid nested
 * lazy layouts (LazyVerticalGrid cannot be nested inside LazyColumn).
 */
@Composable
fun HistoryTabScreen(
    metadataUiState: MetadataUiState.Loaded,
    currentSongId: Long?,
    isPlaying: Boolean,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onAlbumClick: (albumId: Long, albumName: String) -> Unit,
    onArtistClick: (artistName: String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (songIds: List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recentSongs = metadataUiState.recentlyPlayed
    val recentAlbums = metadataUiState.recentlyPlayedAlbums
    val recentArtists = metadataUiState.recentlyPlayedArtists

    if (recentSongs.isEmpty() && recentAlbums.isEmpty() && recentArtists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.empty_history), style = MaterialTheme.typography.titleLarge)
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
                    title = stringResource(R.string.history_recently_played),
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
                        isPlaying = song.id == currentSongId && isPlaying,
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
        if (recentAlbums.isNotEmpty()) {
            stickyHeader(key = "header_albums") {
                val albumSongs = recentSongs.filter { song -> recentAlbums.any { it.id == song.albumId } }
                CollapsibleSectionHeader(
                    title = stringResource(R.string.history_recently_played_albums),
                    isExpanded = albumsExpanded,
                    onToggle = { albumsExpanded = !albumsExpanded },
                    onShuffle = { if (albumSongs.isNotEmpty()) onSongClick(albumSongs.shuffled(), 0) },
                    onPlayAll = { if (albumSongs.isNotEmpty()) onSongClick(albumSongs, 0) },
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
                                onMenuClick = { onAlbumClick(album.id, album.name) },
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
                val artistSongs = recentSongs.filter { song -> recentArtists.any { it.name == song.artist } }
                CollapsibleSectionHeader(
                    title = stringResource(R.string.history_recently_played_artists),
                    isExpanded = artistsExpanded,
                    onToggle = { artistsExpanded = !artistsExpanded },
                    onShuffle = { if (artistSongs.isNotEmpty()) onSongClick(artistSongs.shuffled(), 0) },
                    onPlayAll = { if (artistSongs.isNotEmpty()) onSongClick(artistSongs, 0) },
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
                                onMenuClick = { onArtistClick(artist.name) },
                            )
                        }
                        if (rowArtists.size < 2) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun HistoryTabScreenPreview() {
    SyncPlayerTheme(darkTheme = true) {
        HistoryTabScreen(
            metadataUiState = MetadataUiState.Loaded(
                favorites = emptyList(),
                mostPlayed = emptyList(),
                recentlyPlayed = listOf(
                    Song(
                        id = 1, title = "Bohemian Rhapsody", artist = "Queen", albumArtist = "Queen",
                        album = "A Night at the Opera", albumId = 1, duration = 355000, trackNumber = 1,
                        year = 1975, dateAdded = 0, contentUri = null, albumArtUri = null,
                    ),
                ),
                recentlyPlayedAlbums = emptyList(),
                recentlyPlayedArtists = emptyList(),
            ),
            currentSongId = null,
            isPlaying = false,
            onSongClick = { _, _ -> },
            onAlbumClick = { _, _ -> },
            onArtistClick = {},
            onPlayNext = {},
            onAddToQueue = {},
            onAddToPlaylist = {},
        )
    }
}
