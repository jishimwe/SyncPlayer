package com.jpishimwe.syncplayer.ui.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MotionTokensTest {

    @Test
    fun `duration tokens match M3 spec values`() {
        assertEquals(150, MotionTokens.DurationShort3)
        assertEquals(200, MotionTokens.DurationShort4)
        assertEquals(300, MotionTokens.DurationMedium2)
        assertEquals(350, MotionTokens.DurationMedium3)
        assertEquals(400, MotionTokens.DurationMedium4)
        assertEquals(600, MotionTokens.DurationLong4)
    }

    @Test
    fun `all duration tokens are positive`() {
        val durations = listOf(
            MotionTokens.DurationShort3,
            MotionTokens.DurationShort4,
            MotionTokens.DurationMedium2,
            MotionTokens.DurationMedium3,
            MotionTokens.DurationMedium4,
            MotionTokens.DurationLong4,
        )
        durations.forEach { duration ->
            assert(duration > 0) { "Duration must be positive, was $duration" }
        }
    }

    @Test
    fun `easing constants are not null`() {
        // Verify all easing curves are properly initialized
        requireNotNull(MotionTokens.EasingStandard)
        requireNotNull(MotionTokens.EasingStandardDecelerate)
        requireNotNull(MotionTokens.EasingStandardAccelerate)
        requireNotNull(MotionTokens.EasingEmphasizedDecelerate)
        requireNotNull(MotionTokens.EasingEmphasizedAccelerate)
    }

    @Test
    fun `animation spec objects are not null`() {
        requireNotNull(MotionTokens.SharedElementBoundsSpec)
        requireNotNull(MotionTokens.DetailContentFadeIn)
        requireNotNull(MotionTokens.BackgroundColorCrossFade)
        requireNotNull(MotionTokens.SwipeExitFling)
        requireNotNull(MotionTokens.SnapBackSpring)
        requireNotNull(MotionTokens.TabSizeTransition)
        requireNotNull(MotionTokens.OverlaySlide)
        requireNotNull(MotionTokens.FadeInSpec)
        requireNotNull(MotionTokens.FadeOutSpec)
    }

    @Test
    fun `transition combinations are not null`() {
        requireNotNull(MotionTokens.MiniPlayerEnter)
        requireNotNull(MotionTokens.MiniPlayerExit)
        requireNotNull(MotionTokens.NowPlayingEnter)
        requireNotNull(MotionTokens.NowPlayingExit)
    }
}
