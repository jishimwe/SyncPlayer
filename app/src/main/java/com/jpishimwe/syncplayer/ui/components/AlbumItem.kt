package com.jpishimwe.syncplayer.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jpishimwe.syncplayer.R
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.myAccentColor
import com.jpishimwe.syncplayer.ui.effect.noRippleClickable

sealed interface AlbumPlaybackState {
    data object Default : AlbumPlaybackState

    data object Playing : AlbumPlaybackState

    data object Paused : AlbumPlaybackState
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumGridItem(
    album: Album,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    playbackState: AlbumPlaybackState = AlbumPlaybackState.Default,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val isActive = playbackState != AlbumPlaybackState.Default
    val textColor = if (isActive) myAccentColor else MaterialTheme.colorScheme.onSurface

    val albumArtShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    val albumInfoShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 8.dp, bottomEnd = 8.dp)

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .noRippleClickable(
                    onClick = onClick,
                ),
    ) {
        Column {
            Box(
                Modifier
                    .clip(albumArtShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .wrapContentSize(),
            ) {
                SubcomposeAsyncImage(
                    model = album.albumArtUri,
                    contentDescription = stringResource(R.string.cd_album_art, album.name),
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .let { mod ->
                                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                    with(sharedTransitionScope) {
                                        mod.sharedElement(
                                            rememberSharedContentState(key = "album_art_${album.id}"),
                                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(8.dp)),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            boundsTransform = { _, _ -> tween(300, easing = FastOutSlowInEasing) },
                                        )
                                    }
                                } else {
                                    mod
                                }
                            },
                    error = {
                        Icon(
                            Icons.Default.Album,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .padding(24.dp),
                        )
                    },
                )

                val icon =
                    when (playbackState) {
                        AlbumPlaybackState.Playing -> Icons.Default.PlayArrow
                        AlbumPlaybackState.Paused -> Icons.Default.Pause
                        AlbumPlaybackState.Default -> Icons.Default.PlayArrow // unreachable
                    }
                val playDesc = if (playbackState is AlbumPlaybackState.Playing) {
                    stringResource(R.string.cd_pause_album, album.name)
                } else {
                    stringResource(R.string.cd_play_album, album.name)
                }
                Box(
                    modifier =
                        Modifier
                            .padding(4.dp)
                            .size(48.dp)
                            .align(Alignment.BottomStart)
                            .noRippleClickable { onPlayClick() }
                            .semantics { contentDescription = playDesc },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isActive) myAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Menu — top-right, always visible
                val menuDesc = stringResource(R.string.cd_album_options, album.name)
                Box(
                    modifier =
                        Modifier
                            .padding(4.dp)
                            .size(48.dp)
                            .align(Alignment.TopEnd)
                            .noRippleClickable { onMenuClick() }
                            .semantics { contentDescription = menuDesc },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = if (isActive) myAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            FrostedGlassPill(modifier = Modifier.fillMaxWidth(), shape = albumInfoShape) {
                Column {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor,
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Light),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color =
                            if (isActive) {
                                myAccentColor.copy(alpha = 0.75f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }
        if (isActive) {
            Box(
                Modifier
                    .matchParentSize()
                    .border(BorderStroke(1.5.dp, myAccentColor), RoundedCornerShape(8.dp)),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun AlbumGridItemPreview() {
    SyncPlayerTheme(darkTheme = true) {
        val album = Album(id = 1L, name = "Fe3O4: FORWARD", artist = "NMIXX", songCount = 6, albumArtUri = null)
        Row(modifier = Modifier.padding(8.dp)) {
            AlbumGridItem(
                album = album,
                onClick = {},
                onMenuClick = {},
                onPlayClick = {},
                playbackState = AlbumPlaybackState.Default,
                modifier = Modifier.weight(1f).padding(4.dp),
            )
            AlbumGridItem(
                album = album,
                onClick = {},
                onMenuClick = {},
                onPlayClick = {},
                playbackState = AlbumPlaybackState.Playing,
                modifier = Modifier.weight(1f).padding(4.dp),
            )
            AlbumGridItem(
                album = album,
                onClick = {},
                onMenuClick = {},
                onPlayClick = {},
                playbackState = AlbumPlaybackState.Paused,
                modifier = Modifier.weight(1f).padding(4.dp),
            )
        }
    }
}
