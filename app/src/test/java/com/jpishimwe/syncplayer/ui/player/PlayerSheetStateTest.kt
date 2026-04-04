package com.jpishimwe.syncplayer.ui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// Note: expand(), collapse(), and onDragEnd() call animateTo() which requires a
// MonotonicFrameClock not available in plain JVM unit tests. Those paths are covered
// by manual testing. Tests here cover onDragDelta (uses snapTo — no frame clock needed)
// and all pure derived properties.

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerSheetStateTest {

    private val screenHeightPx = 1920f
    private val miniPeekPx = 240f // representative peek height in px

    private lateinit var state: PlayerSheetState

    @BeforeEach
    fun setUp() {
        state = PlayerSheetState(
            screenHeightPx = screenHeightPx,
            miniPeekPx = miniPeekPx,
            scope = CoroutineScope(UnconfinedTestDispatcher()),
        )
    }

    // ── translateY ─────────────────────────────────────────────────────────────

    @Test
    fun `translateY is 0 when fully expanded`() = runTest {
        state.progress.snapTo(1f)
        assertEquals(0f, state.translateY, 0.001f)
    }

    @Test
    fun `translateY equals screenHeight minus peek when fully collapsed`() = runTest {
        state.progress.snapTo(0f)
        assertEquals(screenHeightPx - miniPeekPx, state.translateY, 0.001f)
    }

    @Test
    fun `translateY interpolates linearly between anchors`() = runTest {
        state.progress.snapTo(0.5f)
        val expected = (screenHeightPx - miniPeekPx) * 0.5f
        assertEquals(expected, state.translateY, 0.001f)
    }

    // ── cornerRadius ───────────────────────────────────────────────────────────

    @Test
    fun `cornerRadius is 16dp when collapsed`() = runTest {
        state.progress.snapTo(0f)
        assertEquals(16f, state.cornerRadius.value, 0.001f)
    }

    @Test
    fun `cornerRadius is 0dp when expanded`() = runTest {
        state.progress.snapTo(1f)
        assertEquals(0f, state.cornerRadius.value, 0.001f)
    }

    // ── alphas ─────────────────────────────────────────────────────────────────

    @Test
    fun `miniAlpha is 1 when collapsed`() = runTest {
        state.progress.snapTo(0f)
        assertEquals(1f, state.miniAlpha, 0.001f)
    }

    @Test
    fun `miniAlpha is 0 when expanded`() = runTest {
        state.progress.snapTo(1f)
        assertEquals(0f, state.miniAlpha, 0.001f)
    }

    @Test
    fun `playerAlpha is 0 when collapsed`() = runTest {
        state.progress.snapTo(0f)
        assertEquals(0f, state.playerAlpha, 0.001f)
    }

    @Test
    fun `playerAlpha is 1 when expanded`() = runTest {
        state.progress.snapTo(1f)
        assertEquals(1f, state.playerAlpha, 0.001f)
    }

    // ── isExpanded ──────────────────────────────────────────────────────────────

    @Test
    fun `isExpanded is true when progress exceeds 0_5`() = runTest {
        state.progress.snapTo(0.51f)
        assertTrue(state.isExpanded)
    }

    @Test
    fun `isExpanded is false when progress is below 0_5`() = runTest {
        state.progress.snapTo(0.49f)
        assertFalse(state.isExpanded)
    }

    @Test
    fun `isExpanded is false at exactly 0_5`() = runTest {
        state.progress.snapTo(0.5f)
        assertFalse(state.isExpanded)
    }

    // ── onDragDelta ─────────────────────────────────────────────────────────────

    @Test
    fun `onDragDelta upward drag increases progress`() = runTest {
        state.progress.snapTo(0.5f)
        state.onDragDelta(-200f) // negative = upward = expanding
        assertTrue(state.progress.value > 0.5f)
    }

    @Test
    fun `onDragDelta downward drag decreases progress`() = runTest {
        state.progress.snapTo(0.5f)
        state.onDragDelta(200f) // positive = downward = collapsing
        assertTrue(state.progress.value < 0.5f)
    }

    @Test
    fun `onDragDelta clamps progress to 0 when dragging far downward`() = runTest {
        state.progress.snapTo(0.5f)
        state.onDragDelta(100_000f) // far beyond screen height
        assertEquals(0f, state.progress.value, 0.001f)
    }

    @Test
    fun `onDragDelta clamps progress to 1 when dragging far upward`() = runTest {
        state.progress.snapTo(0.5f)
        state.onDragDelta(-100_000f) // far beyond screen height
        assertEquals(1f, state.progress.value, 0.001f)
    }

    @Test
    fun `onDragDelta has no effect when already at 0 and dragging down`() = runTest {
        state.progress.snapTo(0f)
        state.onDragDelta(500f)
        assertEquals(0f, state.progress.value, 0.001f)
    }

    @Test
    fun `onDragDelta has no effect when already at 1 and dragging up`() = runTest {
        state.progress.snapTo(1f)
        state.onDragDelta(-500f)
        assertEquals(1f, state.progress.value, 0.001f)
    }
}
