package com.jpishimwe.syncplayer.ui.library

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/**
 * Parallax hero image with gradient fade overlay, used by both
 * [ArtistDetailScreen] and [AlbumDetailScreen].
 *
 * @param imageModel      Coil model (URI / URL) for the hero image.
 * @param contentDescription  Accessibility description for the image.
 * @param heroHeight      Height of the hero section.
 * @param parallaxOffset  Current scroll offset; image translates at 35 % speed.
 * @param sharedElementModifier Optional lambda that applies shared-element
 *   modifiers to the image. Receives the base [Modifier] and returns the
 *   decorated one. When `null`, no shared-element transition is applied.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailHeroImage(
    imageModel: Any?,
    contentDescription: String,
    heroHeight: Dp,
    parallaxOffset: Float,
    sharedElementModifier: (@Composable (Modifier) -> Modifier)? = null,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .graphicsLayer {
                    translationY = -parallaxOffset * 0.35f
                },
    ) {
        SubcomposeAsyncImage(
            model = imageModel,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxSize()
                    .let { mod -> sharedElementModifier?.invoke(mod) ?: mod },
            error = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            },
        )

        // Bottom gradient fade — image fades into the dark panel
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
}
