package com.jpishimwe.syncplayer.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset

/**
 * Centralized motion tokens for SyncPlayer, aligned with Material 3 motion guidelines.
 * Use these instead of inline animation specs to keep animations consistent and tunable.
 */
object MotionTokens {

    // ── Duration (milliseconds) ─────────────────────────────────────────
    const val DurationShort3 = 150
    const val DurationShort4 = 200
    const val DurationMedium2 = 300
    const val DurationMedium3 = 350
    const val DurationMedium4 = 400
    const val DurationLong4 = 600

    // ── Easing ──────────────────────────────────────────────────────────
    val EasingStandard: Easing = FastOutSlowInEasing
    val EasingStandardDecelerate: Easing = LinearOutSlowInEasing
    val EasingStandardAccelerate: Easing = FastOutLinearInEasing
    val EasingEmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EasingEmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    // ── Reusable AnimationSpec instances ─────────────────────────────────

    /** Shared-element bounds transform (AlbumItem, ArtistItem, AlbumDetailScreen). */
    val SharedElementBoundsSpec: FiniteAnimationSpec<Rect> =
        tween(DurationMedium2, easing = EasingStandard)

    /** Detail screen content fade-in (blurred bg, panels, top bar). */
    val DetailContentFadeIn: AnimationSpec<Float> =
        tween(DurationMedium3, delayMillis = DurationShort3, easing = EasingStandard)

    /** Album-art background color cross-fade in Now Playing. */
    val BackgroundColorCrossFade: AnimationSpec<Color> =
        tween(DurationLong4, easing = LinearEasing)

    /** Swipe-triggered exit fling on the album art. Spring handles interruption naturally. */
    val SwipeExitFling: AnimationSpec<Offset> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

    /** Snap-back spring when drag is cancelled or below threshold. */
    val SnapBackSpring: AnimationSpec<Offset> =
        spring(dampingRatio = 0.6f, stiffness = 400f)

    /** Tab font-size transition in CustomTabRow. */
    val TabSizeTransition: AnimationSpec<Float> =
        tween(DurationShort4, easing = EasingStandard)

    /** Tab label color transition — same duration as font-size so they stay in sync. */
    val TabColorTransition: AnimationSpec<Color> =
        tween(DurationShort4, easing = EasingStandard)

    /** Now Playing overlay slide (enter & exit). */
    val OverlaySlide: FiniteAnimationSpec<IntOffset> =
        tween(DurationMedium4, easing = EasingStandard)

    /** Standard fade-in for overlays. */
    val FadeInSpec: FiniteAnimationSpec<Float> =
        tween(DurationMedium2)

    /** Standard fade-out for overlays. */
    val FadeOutSpec: FiniteAnimationSpec<Float> =
        tween(DurationShort4)

    // ── Transition combinations ─────────────────────────────────────────

    /** MiniPlayer appear — slides up from 50% below with fade. */
    val MiniPlayerEnter: EnterTransition =
        slideInVertically(
            animationSpec = tween(DurationMedium2, easing = EasingEmphasizedDecelerate),
        ) { it / 2 } + fadeIn(animationSpec = FadeInSpec)

    /** MiniPlayer disappear — slides down and fades. */
    val MiniPlayerExit: ExitTransition =
        slideOutVertically(
            animationSpec = tween(DurationShort4, easing = EasingStandardAccelerate),
        ) { it / 3 } + fadeOut(animationSpec = FadeOutSpec)

    /** Now Playing fade-in: spans the full slide duration so they finish together. */
    private val NowPlayingFadeIn: FiniteAnimationSpec<Float> =
        tween(DurationMedium4, easing = EasingStandard)

    /** Now Playing fade-out: spans the full slide duration so the ghost doesn't linger. */
    private val NowPlayingFadeOut: FiniteAnimationSpec<Float> =
        tween(DurationMedium4, easing = EasingStandardAccelerate)

    /** Now Playing full-screen overlay enter. */
    val NowPlayingEnter: EnterTransition =
        slideInVertically(animationSpec = OverlaySlide) { it } +
            fadeIn(animationSpec = NowPlayingFadeIn)

    /** Now Playing full-screen overlay exit. */
    val NowPlayingExit: ExitTransition =
        slideOutVertically(animationSpec = OverlaySlide) { it } +
            fadeOut(animationSpec = NowPlayingFadeOut)

    // ── Player sheet ────────────────────────────────────────────────────

    /** Sheet expand spring — responsive, overshoot suppressed. */
    val SheetExpandSpring: AnimationSpec<Float> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Sheet collapse spring — snappy, no bounce going down. */
    val SheetCollapseSpring: AnimationSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 450f)

    // ── Song-change transition ───────────────────────────────────────────

    /** Song slide-in (direction = +1 for next, -1 for previous). */
    fun songSlideEnter(direction: Int): EnterTransition =
        slideInHorizontally(
            animationSpec = tween(DurationMedium2, easing = EasingEmphasizedDecelerate),
        ) { fullWidth -> fullWidth * direction } +
            fadeIn(animationSpec = tween(DurationMedium2, easing = EasingEmphasizedDecelerate))

    /** Song slide-out (direction = +1 for next, -1 for previous). */
    fun songSlideExit(direction: Int): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(DurationMedium2, easing = EasingEmphasizedDecelerate),
        ) { fullWidth -> -fullWidth * direction } +
            fadeOut(animationSpec = tween(DurationShort4, easing = EasingStandardAccelerate))
}
