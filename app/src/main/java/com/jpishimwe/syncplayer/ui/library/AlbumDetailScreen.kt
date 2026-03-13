package com.jpishimwe.syncplayer.ui.library

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import com.jpishimwe.syncplayer.ui.player.components.FrostedGlassPill
import com.jpishimwe.syncplayer.ui.player.components.SongItem
import com.jpishimwe.syncplayer.ui.player.components.SongItemVariant
import com.jpishimwe.syncplayer.ui.player.components.SongMenuAction
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    albumName: String,
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    playerViewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val songs by viewModel.getSongsByAlbum(albumId).collectAsStateWithLifecycle(emptyList())
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val currentSongId = playerUiState.currentSong?.id

    AlbumDetailScreenContent(
        albumName = albumName,
        songs = songs,
        currentSongId = currentSongId,
        onSongClick = { songsToPlay, index ->
            playerViewModel.onEvent(PlayerEvent.PlaySongs(songsToPlay, index))
            onNavigateToNowPlaying()
        },
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun AlbumDetailScreenContent(
    albumName: String,
    songs: List<Song>,
    currentSongId: Long?,
    // Accepts a list so shuffle can pass songs.shuffled()
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onNavigateBack: () -> Unit,
) {
    // Album art URI — all songs share the same album art; use the first non-null
    val albumArtUri = songs.firstOrNull { it.albumArtUri != null }?.albumArtUri
    val artist = songs.firstOrNull()?.artist.orEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Hero: full-width album art ─────────────────────────────────
            item(key = "hero") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                ) {
                    SubcomposeAsyncImage(
                        model = albumArtUri,
                        contentDescription = albumName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                        },
                    )

                    // Top-left frosted strip: album name + artist
                    FrostedGlassPill(
                        modifier =
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp),
                    ) {
                        Text(
                            text = albumName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (artist.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Bottom-right frosted strip: shuffle + play
                    FrostedGlassPill(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                    ) {
                        Row {
                            IconButton(
                                onClick = { if (songs.isNotEmpty()) onSongClick(songs.shuffled(), 0) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { if (songs.isNotEmpty()) onSongClick(songs, 0) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play all",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            // ── Song list ──────────────────────────────────────────────────
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
                            SongMenuAction.GoToArtist,
                        ),
                    onMenuAction = {
                        // TODO: wire PlayNext, AddToQueue, AddToPlaylist, GoToArtist
                        //       via PlayerViewModel / navigation in Phase 6+
                    },
                )
            }
        }

        // Back button — overlaid so it stays visible while scrolling
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

private val previewAlbumSongs =
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
            title = "Heartless",
            artist = "The Weeknd",
            album = "After Hours",
            duration = 198_000L,
            albumArtUri = null,
            playCount = 3,
            rating = Rating.NONE.value,
            lastPlayed = 0L,
            albumId = 1,
            trackNumber = 3,
            year = 2020,
            dateAdded = 1002L,
            contentUri = null,
            lastModified = 0L,
        ),
        Song(
            id = 4,
            title = "Faith",
            artist = "The Weeknd",
            album = "After Hours",
            duration = 412_000L,
            albumArtUri = null,
            playCount = 1,
            rating = Rating.NONE.value,
            lastPlayed = 0L,
            albumId = 1,
            trackNumber = 4,
            year = 2020,
            dateAdded = 1003L,
            contentUri = null,
            lastModified = 0L,
        ),
    )

/** Default — no song currently playing. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Default")
@Composable
private fun AlbumDetailDefaultPreview() {
    SyncPlayerTheme(darkTheme = true) {
        AlbumDetailScreenContent(
            albumName = "After Hours",
            songs = previewAlbumSongs,
            currentSongId = null,
            onSongClick = { _, _ -> },
            onNavigateBack = {},
        )
    }
}

/** Playing state — first track highlighted with accent color. */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Song playing")
@Composable
private fun AlbumDetailPlayingPreview() {
    SyncPlayerTheme(darkTheme = true) {
        AlbumDetailScreenContent(
            albumName = "After Hours",
            songs = previewAlbumSongs,
            currentSongId = 1L,
            onSongClick = { _, _ -> },
            onNavigateBack = {},
        )
    }
}

/** Empty state — album has no songs yet (e.g. still loading). */
@Preview(showBackground = true, backgroundColor = 0xFF111113, name = "Empty state")
@Composable
private fun AlbumDetailEmptyPreview() {
    SyncPlayerTheme(darkTheme = true) {
        AlbumDetailScreenContent(
            albumName = "Unknown Album",
            songs = emptyList(),
            currentSongId = null,
            onSongClick = { _, _ -> },
            onNavigateBack = {},
        )
    }
}
