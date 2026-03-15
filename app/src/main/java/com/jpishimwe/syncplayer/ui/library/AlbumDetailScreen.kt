package com.jpishimwe.syncplayer.ui.library

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
import com.jpishimwe.syncplayer.ui.player.components.BlurredBackground
import com.jpishimwe.syncplayer.ui.player.components.FrostedGlassPill
import com.jpishimwe.syncplayer.ui.player.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.player.components.SongItem
import com.jpishimwe.syncplayer.ui.player.components.SongItemVariant
import com.jpishimwe.syncplayer.ui.player.components.SongMenuAction
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.frostedGlassRendered
import com.jpishimwe.syncplayer.ui.theme.myAccentColor

/** Album art height — square-ish, matches ArtistDetailScreen portrait. */
private val AlbumArtHeight = 360.dp

/** How much the glass panel overlaps the album art bottom (the rounded lip). */
private val PanelOverlap = 24.dp

private val PanelTopShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val BarShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    albumName: String,
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    viewModel: LibraryViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    playerViewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val songs by viewModel.getSongsByAlbum(albumId).collectAsStateWithLifecycle(emptyList())
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val currentSongId = playerUiState.currentSong?.id

    AlbumDetailScreenContent(
        albumId = albumId,
        albumName = albumName,
        songs = songs,
        currentSongId = currentSongId,
        onSongClick = { songsToPlay, index ->
            playerViewModel.onEvent(PlayerEvent.PlaySongs(songsToPlay, index))
            onNavigateToNowPlaying()
        },
        onNavigateBack = onNavigateBack,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumDetailScreenContent(
    albumName: String,
    songs: List<Song>,
    currentSongId: Long?,
    // Accepts a list so shuffle can pass songs.shuffled()
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onNavigateBack: () -> Unit,
    albumId: Long = 0L,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    // Album art URI — all songs share the same album art; use the first non-null
    val albumArtUri = songs.firstOrNull { it.albumArtUri != null }?.albumArtUri
    val artist = songs.firstOrNull()?.artist.orEmpty()
    val songCount = songs.size
    val lazyListState = rememberLazyListState()

    // Parallax: hero scrolls at half speed
    val firstVisibleItemScrollOffset =
        if (lazyListState.firstVisibleItemIndex == 0) {
            lazyListState.firstVisibleItemScrollOffset.toFloat()
        } else {
            Float.MAX_VALUE
        }

    val accentBorderBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    myAccentColor.copy(alpha = 0.24f),
                    myAccentColor.copy(alpha = 0.75f),
                    myAccentColor.copy(alpha = 0.24f),
                ),
        )

    // ── Root ─────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Layer 0 — blurred screenshot of previous screen
        BlurredBackground()

        // Layer 1 — fixed album art pinned to top (parallax: scrolls at half speed)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(AlbumArtHeight)
                    .graphicsLayer {
                        translationY = firstVisibleItemScrollOffset * 0.5f
                    },
        ) {
            SubcomposeAsyncImage(
                model = albumArtUri,
                contentDescription = albumName,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .let { mod ->
                            if (sharedTransitionScope != null && animatedVisibilityScope != null && albumId != 0L) {
                                with(sharedTransitionScope) {
                                    mod.sharedElement(
                                        rememberSharedContentState(key = "album_art_$albumId"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                    )
                                }
                            } else {
                                mod
                            }
                        },
                error = {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                },
            )

            // Bottom gradient fade — album art fades into the dark panel
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f),
                                    ),
                            ),
                        ),
            )
        }

        // Layer 2 — scrollable content: glass panel slides over album art
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = AlbumArtHeight - PanelOverlap, start = 8.dp, end = 8.dp, bottom = MiniPlayerPeek),
        ) {
            // ── Glass panel lip: rounded top edge ────────────────────────
            item(key = "panel_lip") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(PanelOverlap)
                            .clip(PanelTopShape)
                            .frostedGlassRendered(),
                )
            }

            // ── Sticky bar: shuffle + play (no tabs needed for album) ────
            stickyHeader(key = "action_bar") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .border(BorderStroke(1.dp, accentBorderBrush), BarShape)
                                .clip(BarShape),
                    ) {
                        // Frosted glass background layer
                        Box(modifier = Modifier.matchParentSize().frostedGlassRendered())

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Song count label
                            Text(
                                text = "$songCount songs",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            )

                            Spacer(Modifier.weight(1f))

                            // Shuffle button
                            IconButton(onClick = { if (songs.isNotEmpty()) onSongClick(songs.shuffled(), 0) }) {
                                Icon(
                                    Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            // Play all button
                            IconButton(onClick = { if (songs.isNotEmpty()) onSongClick(songs, 0) }) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play all",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ── Song list ────────────────────────────────────────────────
            itemsIndexed(songs, key = { _, s -> "song_${s.id}" }) { index, song ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background),
                ) {
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
        }

        // Layer 3 — pinned top buttons: back + album info + overflow
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Back button
            FrostedGlassPill(
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // Album name + artist pill
            FrostedGlassPill(
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = albumName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (artist.isNotEmpty()) {
                        Text(
                            text = "$artist · $songCount songs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.width(4.dp))

            // Overflow menu button
            FrostedGlassPill(
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                IconButton(
                    onClick = { /* TODO: overflow menu */ },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(40.dp),
                    )
                }
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
            albumArtist = "The Weeknd",
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
            albumArtist = "The Weeknd",
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
            albumArtist = "The Weeknd",
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
            albumArtist = "The Weeknd",
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
