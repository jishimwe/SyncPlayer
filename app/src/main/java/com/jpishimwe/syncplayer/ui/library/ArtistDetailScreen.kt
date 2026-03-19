package com.jpishimwe.syncplayer.ui.library

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.components.AlbumGridItem
import com.jpishimwe.syncplayer.ui.components.AlbumPlaybackState
import com.jpishimwe.syncplayer.ui.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.components.SongItem
import com.jpishimwe.syncplayer.ui.components.SongItemVariant
import com.jpishimwe.syncplayer.ui.components.SongMenuAction
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import com.jpishimwe.syncplayer.ui.theme.BlurredBackground
import com.jpishimwe.syncplayer.ui.theme.frostedGlassRendered

/** Portrait height — roughly square-ish on most phones. */
private val PortraitHeight = 360.dp

/** How much the glass panel overlaps the portrait bottom (the rounded lip). */
private val PanelOverlap = 24.dp

private val PanelTopShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val TopBarHeight = 64.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    // Defaulted so NavGraph doesn't need updating until Phase 6
    onNavigateToAlbumDetail: (Long, String) -> Unit = { _, _ -> },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
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
        onPlayNext = { playerViewModel.onEvent(PlayerEvent.PlayNext(it)) },
        onAddToQueue = { playerViewModel.onEvent(PlayerEvent.AddToQueue(it)) },
        onNavigateBack = onNavigateBack,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
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
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    var selectedSubTab by remember { mutableStateOf(ArtistSubTab.Songs) }
    val lazyListState = rememberLazyListState()

    // Parallax: compute how far the LazyColumn has scrolled from its rest position.
    val parallaxOffset by remember {
        derivedStateOf {
            val firstItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (firstItem == null) {
                0f
            } else {
                (-firstItem.offset.toFloat()).coerceAtLeast(0f)
            }
        }
    }

    // Content fade: layers 0/2/3 fade in independently, hero (layer 1) handled by shared element
    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, tween(350, delayMillis = 150, easing = FastOutSlowInEasing))
    }

// ── Root ─────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Layer 0 — blurred screenshot of previous screen
        Box(modifier = Modifier.alpha(contentAlpha.value)) { BlurredBackground() }

        // Layer 1 — fixed portrait pinned to top (parallax: scrolls slower than content)
        DetailHeroImage(
            imageModel = artist?.artUri,
            contentDescription = artistName,
            heroHeight = PortraitHeight,
            parallaxOffset = parallaxOffset,
            sharedElementModifier =
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    { mod ->
                        with(sharedTransitionScope) {
                            mod.sharedElement(
                                rememberSharedContentState(key = "artist_art_$artistName"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                        }
                    }
                } else {
                    null
                },
        )

        // Layer 2 — scrollable content: glass panel slides over portrait
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().padding(top = TopBarHeight).alpha(contentAlpha.value),
            contentPadding =
                PaddingValues(
                    top = PortraitHeight - PanelOverlap - TopBarHeight,
                    start = 8.dp,
                    end = 8.dp,
                    bottom = MiniPlayerPeek,
                ),
        ) {
            // ── Glass panel lip: rounded top edge (sticky) ──────────────
            stickyHeader(key = "panel_lip") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(PanelOverlap)
                            .clip(PanelTopShape)
                            .frostedGlassRendered(),
                )
            }

            // ── Sticky sub-tab row: Songs | Albums + shuffle/play ────────
            stickyHeader(key = "subtabs") {
                ArtistSubTabHeader(
                    selectedSubTab = selectedSubTab,
                    onSubTabChanged = { selectedSubTab = it },
                    songs = songs,
                    onSongClick = onSongClick,
                )
            }

            // ── Tab content (on the glass panel background) ──────────────
            when (selectedSubTab) {
                ArtistSubTab.Songs -> {
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

                                        SongMenuAction.GoToAlbum -> {
                                            onAlbumClick(song.albumId, song.album)
                                        }

                                        SongMenuAction.AddToPlaylist -> {}

                                        // Phase 3
                                        else -> {}
                                    }
                                },
                            )
                        }
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
                                    .background(MaterialTheme.colorScheme.background)
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

            // ── Glass panel lip: rounded bottom edge ─────────────────────
            item(key = "panel_lip_bottom") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(PanelOverlap)
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            .frostedGlassRendered(),
                )
            }
        }

        // Layer 3 — pinned top buttons: back + overflow (always visible)
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            ArtistDetailTopBar(
                artistName = artistName,
                albumCount = albums.size,
                songCount = songs.size,
                onNavigateBack = onNavigateBack,
                contentAlpha = contentAlpha.value,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    }
}
