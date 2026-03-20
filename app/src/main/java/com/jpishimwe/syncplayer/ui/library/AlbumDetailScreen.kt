package com.jpishimwe.syncplayer.ui.library

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.components.MiniPlayerPeek
import com.jpishimwe.syncplayer.ui.components.SongItem
import com.jpishimwe.syncplayer.ui.components.SongItemVariant
import com.jpishimwe.syncplayer.ui.components.SongMenuAction
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import com.jpishimwe.syncplayer.ui.theme.BlurredBackground
import com.jpishimwe.syncplayer.ui.theme.frostedGlassRendered
import com.jpishimwe.syncplayer.ui.theme.myAccentColor

/** Album art height — square-ish, matches ArtistDetailScreen portrait. */
private val AlbumArtHeight = 360.dp

/** How much the glass panel overlaps the album art bottom (the rounded lip). */
private val PanelOverlap = 24.dp

private val PanelTopShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val TopBarHeight = 64.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    albumName: String,
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToArtistDetail: (String) -> Unit = {},
    onAddToPlaylist: (songIds: List<Long>) -> Unit = {},
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
        onPlayNext = { playerViewModel.onEvent(PlayerEvent.PlayNext(it)) },
        onAddToQueue = { playerViewModel.onEvent(PlayerEvent.AddToQueue(it)) },
        onAddToPlaylist = onAddToPlaylist,
        onNavigateToArtist = onNavigateToArtistDetail,
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
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (songIds: List<Long>) -> Unit = {},
    onNavigateToArtist: (String) -> Unit,
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

    // Parallax: compute how far the LazyColumn has scrolled from its rest position.
    // The first item (panel_lip) starts at contentPadding.top = 336dp.
    // As the user scrolls, firstItem.offset goes from 0 → negative.
    // Total scroll ≈ -firstItem.offset (when index 0) or beyond hero once index > 0.
    val parallaxOffset by remember {
        derivedStateOf {
            val firstItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (firstItem == null) {
                0f
            } else {
                // firstItem.offset starts at the contentPadding top edge and goes negative
                // as the user scrolls down. The magnitude is how far we've scrolled.
                (-firstItem.offset.toFloat()).coerceAtLeast(0f)
            }
        }
    }

    // Content fade: layers 0/2/3 fade in independently, hero (layer 1) handled by shared element
    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, tween(350, delayMillis = 150, easing = FastOutSlowInEasing))
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
        Box(modifier = Modifier.alpha(contentAlpha.value)) { BlurredBackground() }

        // Layer 1 — fixed album art pinned to top (parallax: scrolls at half speed)
        DetailHeroImage(
            imageModel = albumArtUri,
            contentDescription = albumName,
            heroHeight = AlbumArtHeight,
            parallaxOffset = parallaxOffset,
            sharedElementModifier =
                if (sharedTransitionScope != null && animatedVisibilityScope != null && albumId != 0L) {
                    { mod ->
                        with(sharedTransitionScope) {
                            mod.sharedElement(
                                rememberSharedContentState(key = "album_art_$albumId"),
                                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(8.dp)),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> tween(300, easing = FastOutSlowInEasing) },
                            )
                        }
                    }
                } else {
                    null
                },
        )

        // Layer 2 — scrollable content: glass panel slides over album art
        LazyColumn(
            state = lazyListState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = TopBarHeight)
                    .alpha(contentAlpha.value)
                    .clip(PanelTopShape),
            contentPadding =
                PaddingValues(
                    top = AlbumArtHeight - PanelOverlap - TopBarHeight,
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

            // ── Sticky bar: shuffle + play (no tabs needed for album) ────
            stickyHeader(key = "action_bar") {
                AlbumActionBar(
                    songCount = songCount,
                    songs = songs,
                    accentBorderBrush = accentBorderBrush,
                    onSongClick = onSongClick,
                )
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
                        onMenuAction = { action ->
                            when (action) {
                                SongMenuAction.PlayNext -> {
                                    onPlayNext(song)
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

                                else -> {}
                            }
                        },
                    )
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

        // Layer 3 — pinned top buttons: back + album info + overflow
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            AlbumDetailTopBar(
                albumName = albumName,
                artist = artist,
                songCount = songCount,
                onNavigateBack = onNavigateBack,
                onAddAllToPlaylist = { onAddToPlaylist(songs.map { it.id }) },
                contentAlpha = contentAlpha.value,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    }
}
