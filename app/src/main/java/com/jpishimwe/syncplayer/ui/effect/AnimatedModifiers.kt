package com.jpishimwe.syncplayer.ui.effect

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.jpishimwe.syncplayer.ui.theme.MotionTokens

/**
 * One-shot fade-in from 0f → 1f using [MotionTokens.DetailContentFadeIn].
 * Apply via `Modifier.alpha(contentAlpha.value)`.
 */
@Composable
fun rememberContentFadeIn(): Animatable<Float, AnimationVector1D> {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animatable.animateTo(1f, MotionTokens.DetailContentFadeIn)
    }
    return animatable
}
