package com.jpishimwe.syncplayer.ui.library

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import com.jpishimwe.syncplayer.ui.player.components.AlbumGridItem
import com.jpishimwe.syncplayer.ui.player.components.AlbumPlaybackState
import com.jpishimwe.syncplayer.ui.player.components.FrostedGlassPill
import com.jpishimwe.syncplayer.ui.player.components.SongItem
import com.jpishimwe.syncplayer.ui.player.components.SongItemVariant
import com.jpishimwe.syncplayer.ui.player.components.SongMenuAction
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.myAccentColor
import com.jpishimwe.syncplayer.ui.theme.noRippleClickable

private enum class ArtistSubTab { Songs, Albums }

@Composable
fun ArtistDetailScreen(
    artistName: String,
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    // Defaulted so NavGraph doesn't need updating until Phase 6
    onNavigateToAlbumDetail: (Long, String) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    playerViewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val songs by viewModel.getSongsByArtist(artistName).collectAsStateWithLifecycle(emptyList())
    val albums by viewModel.getAlbumsByArtist(artistName).collectAsStateWithLifecycle(emptyList())
    val artist by viewModel.getArtistByName(artistName).collectAsStateWithLifecycle(null)
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val currentSongId = playerUiState.currentSong?.id
    val currentAlbumId = playerUiState.currentSong?.albumId

    ArtistDetailScreenContent(
        artist = artist,
        artistName = artistName,
        songs = songs,
        albums = albums,
        currentSongId = currentSongId,
        currentAlbumId = currentAlbumId,
        onSongClick = { songsToPlay, index ->
            playerViewModel.onEvent(PlayerEvent.PlaySongs(songsToPlay, index))
            onNavigateToNowPlaying()
        },
        onAlbumClick = onNavigateToAlbumDetail,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun ArtistDetailScreenContent(
    artist: Artist?,
    artistName: String,
    songs: List<Song>,
    albums: List<Album>,
    currentSongId: Long?,
    currentAlbumId: Long?,
    // Accepts a list so callers can pass songs.shuffled() for the shuffle action
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onAlbumClick: (Long, String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var selectedSubTab by remember { mutableStateOf(ArtistSubTab.Songs) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Hero: circular portrait ────────────────────────────────────
            item(key = "hero") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // 240dp circle centred in the hero box — same shape as grid items
                    SubcomposeAsyncImage(
                        model = artist?.artUri,
                        contentDescription = artistName,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(240.dp),
                        error = {
                            Box(
                                modifier =
                                    Modifier
                                        .size(240.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                        },
                    )

                    // Top-left frosted strip: artist name + album/song counts
                    FrostedGlassPill(
                        modifier =
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp),
                    ) {
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${albums.size} albums · ${songs.size} songs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Top-right frosted strip: overflow (actions deferred to Phase 6+)
                    FrostedGlassPill(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // ── Sticky sub-tab row: Songs | Albums + shuffle/play ──────────
            stickyHeader(key = "subtabs") {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtistSubTab.entries.forEach { tab ->
                        val isSelected = tab == selectedSubTab
                        Text(
                            text = tab.name,
                            style =
                                if (isSelected) {
                                    MaterialTheme.typography.titleLarge
                                } else {
                                    MaterialTheme.typography.titleMedium
                                },
                            color = if (isSelected) myAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier
                                    .noRippleClickable { selectedSubTab = tab }
                                    .padding(end = 16.dp),
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    IconButton(onClick = { if (songs.isNotEmpty()) onSongClick(songs.shuffled(), 0) }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { if (songs.isNotEmpty()) onSongClick(songs, 0) }) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play all",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // ── Tab content ────────────────────────────────────────────────
            when (selectedSubTab) {
                ArtistSubTab.Songs -> {
                    itemsIndexed(songs, key = { _, s -> "song_${s.id}" }) { index, song ->
                        SongItem(
                            song = song,
                            onClick = { onSongClick(songs, index) },
                            isPlaying = song.id == currentSongId,
                            variant = SongItemVariant.Default,
                            menuActions =
                                listOf(
                                    SongMenuAction.PlayNext,
                                    SongMenuAction.AddToQueue,
                                    SongMenuAction.AddToPlaylist,
                                    SongMenuAction.GoToAlbum,
                                ),
                            onMenuAction = { action ->
                                if (action == SongMenuAction.GoToAlbum) {
                                    onAlbumClick(song.albumId, song.album)
                                }
                                // TODO: wire PlayNext, AddToQueue, AddToPlaylist via PlayerViewModel
                            },
                        )
                    }
                }

                ArtistSubTab.Albums -> {
                    // chunked(2) — LazyVerticalGrid cannot be nested inside LazyColumn
                    val albumRows = albums.chunked(2)
                    items(albumRows.size, key = { "album_row_$it" }) { rowIndex ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            albumRows[rowIndex].forEach { album ->
                                AlbumGridItem(
                                    album = album,
                                    playbackState =
                                        if (album.id == currentAlbumId) {
                                            AlbumPlaybackState.Playing
                                        } else {
                                            AlbumPlaybackState.Default
                                        },
                                    onClick = { onAlbumClick(album.id, album.name) },
                                    onPlayClick = { onAlbumClick(album.id, album.name) },
                                    onMenuClick = {},
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            // Pad to 2 columns when last row has only 1 item
                            if (albumRows[rowIndex].size < 2) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Back button — overlaid outside LazyColumn so it stays visible on scroll
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ── Preview data ───────────────────────────────────────────────────────────────

private val previewSongs =
    listOf(
        Song(
            id = 1,
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            duration = 200_000L,
            albumArtUri = null,
            playCount = 12,
            rating = Rating.GREAT.value,
            lastPlayed = 0L,
            albumId = 1,
            trackNumber = 1,
            year = 2020,
            dateAdded = 1000L,
            contentUri = null,
            lastModified = 0L,
        ),
        Song(
            id = 2,
            title = "Save Your Tears",
            artist = "The Weeknd",
            album = "After Hours",
            duration = 215_000L,
            albumArtUri = null,
            playCount = 8,
            rating = Rating.GOOD.value,
            lastPlayed = 0L,
            albumId = 1,
            trackNumber = 2,
            year = 2020,
            dateAdded = 1001L,
            contentUri = null,
            lastModified = 0L,
        ),
        Song(
            id = 3,
            title = "Starboy",
            artist = "The Weeknd",
            album = "Starboy",
            duration = 230_000L,
            albumArtUri = null,
            playCount = 5,
            rating = Rating.NONE.value,
            lastPlayed = 0L,
            albumId = 2,
            trackNumber = 1,
            year = 2016,
            dateAdded = 1002L,
            contentUri = null,
            lastModified = 0L,
        ),
    )

private val previewAlbums =
    listOf(
        Album(id = 1L, name = "After Hours", artist = "The Weeknd", songCount = 14, albumArtUri = null),
        Album(id = 2L, name = "Starboy", artist = "The Weeknd", songCount = 18, albumArtUri = null),
        Album(id = 3L, name = "Beauty Behind the Madness", artist = "The Weeknd", songCount = 14, albumArtUri = null),
    )

private val previewArtist = Artist(name = "The Weeknd", songCount = 42, albumCount = 5, artUri = null)

/** Songs sub-tab, first song playing. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Songs sub-tab")
@Composable
private fun ArtistDetailSongsPreview() {
    SyncPlayerTheme(darkTheme = true) {
        ArtistDetailScreenContent(
            artist = previewArtist,
            artistName = "The Weeknd",
            songs = previewSongs,
            albums = previewAlbums,
            currentSongId = 1L,
            currentAlbumId = null,
            onSongClick = { _, _ -> },
            onAlbumClick = { _, _ -> },
            onNavigateBack = {},
        )
    }
}

/** Empty state — no songs or albums loaded yet. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Empty state")
@Composable
private fun ArtistDetailEmptyPreview() {
    SyncPlayerTheme(darkTheme = true) {
        ArtistDetailScreenContent(
            artist = null,
            artistName = "Unknown Artist",
            songs = emptyList(),
            albums = emptyList(),
            currentSongId = null,
            currentAlbumId = null,
            onSongClick = { _, _ -> },
            onAlbumClick = { _, _ -> },
            onNavigateBack = {},
        )
    }
}
