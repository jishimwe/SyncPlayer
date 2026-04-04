package com.jpishimwe.syncplayer.ui.home.tabs

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
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
import com.jpishimwe.syncplayer.ui.shared.MetadataUiState
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.myAccentColor

private const val SONGS_PREVIEW = 5
private const val GRID_PREVIEW = 4 // 2 rows × 2 columns

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

    // Hoist derived lists — computed once, not inside stickyHeader lambdas
    val albumSongs = recentSongs.filter { song -> recentAlbums.any { it.id == song.albumId } }
    val artistSongs = recentSongs.filter { song -> recentArtists.any { it.name == song.artist } }

    var songsExpanded by rememberSaveable { mutableStateOf(false) }
    var albumsExpanded by rememberSaveable { mutableStateOf(false) }
    var artistsExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = MiniPlayerPeek)) {
        // ── Songs ──────────────────────────────────────────────────────────
        if (recentSongs.isNotEmpty()) {
            stickyHeader(key = "header_songs") {
                CollapsibleSectionHeader(
                    title = stringResource(R.string.history_recently_played),
                    count = recentSongs.size,
                    isExpanded = songsExpanded,
                    onToggle = { songsExpanded = !songsExpanded },
                    onShuffle = { onSongClick(recentSongs.shuffled(), 0) },
                    onPlayAll = { onSongClick(recentSongs, 0) },
                )
            }

            val visibleSongs = if (songsExpanded) recentSongs else recentSongs.take(SONGS_PREVIEW)
            itemsIndexed(visibleSongs, key = { _, song -> song.id }) { index, song ->
                SongItem(
                    song = song,
                    onClick = { onSongClick(recentSongs, index) },
                    isPlaying = song.id == currentSongId && isPlaying,
                    menuActions = listOf(
                        SongMenuAction.PlayNext,
                        SongMenuAction.AddToQueue,
                        SongMenuAction.AddToPlaylist,
                        SongMenuAction.GoToArtist,
                        SongMenuAction.GoToAlbum,
                    ),
                    onMenuAction = { action ->
                        when (action) {
                            SongMenuAction.PlayNext -> onPlayNext(song)
                            SongMenuAction.AddToQueue -> onAddToQueue(song)
                            SongMenuAction.AddToPlaylist -> onAddToPlaylist(listOf(song.id))
                            SongMenuAction.GoToArtist -> onArtistClick(song.artist)
                            SongMenuAction.GoToAlbum -> onAlbumClick(song.albumId, song.album)
                            else -> {}
                        }
                    },
                    modifier = Modifier.animateItem(),
                )
            }
            if (!songsExpanded && recentSongs.size > SONGS_PREVIEW) {
                item(key = "songs_show_more") {
                    ShowMoreItem(
                        remaining = recentSongs.size - SONGS_PREVIEW,
                        onClick = { songsExpanded = true },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        // ── Albums ─────────────────────────────────────────────────────────
        if (recentAlbums.isNotEmpty()) {
            stickyHeader(key = "header_albums") {
                CollapsibleSectionHeader(
                    title = stringResource(R.string.history_recently_played_albums),
                    count = recentAlbums.size,
                    isExpanded = albumsExpanded,
                    onToggle = { albumsExpanded = !albumsExpanded },
                    onShuffle = { if (albumSongs.isNotEmpty()) onSongClick(albumSongs.shuffled(), 0) },
                    onPlayAll = { if (albumSongs.isNotEmpty()) onSongClick(albumSongs, 0) },
                )
            }

            val albumRows = recentAlbums.chunked(2)
            val visibleAlbumRows = if (albumsExpanded) albumRows else albumRows.take(GRID_PREVIEW / 2)
            items(visibleAlbumRows.size, key = { "album_row_$it" }) { rowIndex ->
                val rowAlbums = visibleAlbumRows[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .animateItem(),
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
                    if (rowAlbums.size < 2) Spacer(Modifier.weight(1f))
                }
            }
            if (!albumsExpanded && albumRows.size > GRID_PREVIEW / 2) {
                item(key = "albums_show_more") {
                    ShowMoreItem(
                        remaining = recentAlbums.size - GRID_PREVIEW,
                        onClick = { albumsExpanded = true },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        // ── Artists ────────────────────────────────────────────────────────
        if (recentArtists.isNotEmpty()) {
            stickyHeader(key = "header_artists") {
                CollapsibleSectionHeader(
                    title = stringResource(R.string.history_recently_played_artists),
                    count = recentArtists.size,
                    isExpanded = artistsExpanded,
                    onToggle = { artistsExpanded = !artistsExpanded },
                    onShuffle = { if (artistSongs.isNotEmpty()) onSongClick(artistSongs.shuffled(), 0) },
                    onPlayAll = { if (artistSongs.isNotEmpty()) onSongClick(artistSongs, 0) },
                )
            }

            val artistRows = recentArtists.chunked(2)
            val visibleArtistRows = if (artistsExpanded) artistRows else artistRows.take(GRID_PREVIEW / 2)
            items(visibleArtistRows.size, key = { "artist_row_$it" }) { rowIndex ->
                val rowArtists = visibleArtistRows[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .animateItem(),
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
            if (!artistsExpanded && artistRows.size > GRID_PREVIEW / 2) {
                item(key = "artists_show_more") {
                    ShowMoreItem(
                        remaining = recentArtists.size - GRID_PREVIEW,
                        onClick = { artistsExpanded = true },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowMoreItem(
    remaining: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.history_show_more, remaining) + " →",
            style = MaterialTheme.typography.labelLarge,
            color = myAccentColor,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun HistoryTabScreenPreview() {
    val songs = listOf(
        Song(id = 1, title = "Bohemian Rhapsody", artist = "Queen", albumArtist = "Queen", album = "A Night at the Opera", albumId = 1, duration = 355000, trackNumber = 1, year = 1975, dateAdded = 0, contentUri = null, albumArtUri = null),
        Song(id = 2, title = "Enter Sandman", artist = "Metallica", albumArtist = "Metallica", album = "Metallica", albumId = 2, duration = 331000, trackNumber = 1, year = 1991, dateAdded = 0, contentUri = null, albumArtUri = null),
        Song(id = 3, title = "Back in Black", artist = "AC/DC", albumArtist = "AC/DC", album = "Back in Black", albumId = 3, duration = 255000, trackNumber = 1, year = 1980, dateAdded = 0, contentUri = null, albumArtUri = null),
        Song(id = 4, title = "Smells Like Teen Spirit", artist = "Nirvana", albumArtist = "Nirvana", album = "Nevermind", albumId = 4, duration = 301000, trackNumber = 1, year = 1991, dateAdded = 0, contentUri = null, albumArtUri = null),
        Song(id = 5, title = "Hotel California", artist = "Eagles", albumArtist = "Eagles", album = "Hotel California", albumId = 5, duration = 391000, trackNumber = 1, year = 1977, dateAdded = 0, contentUri = null, albumArtUri = null),
        Song(id = 6, title = "Sweet Child O' Mine", artist = "Guns N' Roses", albumArtist = "Guns N' Roses", album = "Appetite for Destruction", albumId = 6, duration = 356000, trackNumber = 1, year = 1987, dateAdded = 0, contentUri = null, albumArtUri = null),
        Song(id = 7, title = "Stairway to Heaven", artist = "Led Zeppelin", albumArtist = "Led Zeppelin", album = "Led Zeppelin IV", albumId = 7, duration = 482000, trackNumber = 1, year = 1971, dateAdded = 0, contentUri = null, albumArtUri = null),
    )
    SyncPlayerTheme(darkTheme = true) {
        HistoryTabScreen(
            metadataUiState = MetadataUiState.Loaded(
                favorites = emptyList(),
                mostPlayed = emptyList(),
                recentlyPlayed = songs,
                recentlyPlayedAlbums = emptyList(),
                recentlyPlayedArtists = emptyList(),
            ),
            currentSongId = 2L,
            isPlaying = true,
            onSongClick = { _, _ -> },
            onAlbumClick = { _, _ -> },
            onArtistClick = {},
            onPlayNext = {},
            onAddToQueue = {},
            onAddToPlaylist = {},
        )
    }
}