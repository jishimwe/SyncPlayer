package com.jpishimwe.syncplayer.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.ui.theme.MotionTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Drives the Now Playing bottom sheet.
 *
 * progress = 0f → only the mini-player strip is visible (panel translated off-screen below)
 * progress = 1f → full-screen expanded player
 *
 * All drag, fling, and anchor-snap logic lives here.
 * Derived visual values (translateY, cornerRadius, alphas) are computed from [progress].
 */
class PlayerSheetState(
    private val screenHeightPx: Float,
    private val miniPeekPx: Float,
    private val scope: CoroutineScope,
) {
    val progress = Animatable(0f)

    // ── Derived layout values ────────────────────────────────────────────

    /** TranslateY to apply to the sheet panel via graphicsLayer. */
    val translateY: Float
        get() = (screenHeightPx - miniPeekPx) * (1f - progress.value)

    /** Corner radius — morphs from pill (16dp) to full-screen (0dp). */
    val cornerRadius: Dp
        get() = (16f * (1f - progress.value)).dp

    /** Alpha for the mini-player content layer (fades out as sheet opens). */
    val miniAlpha: Float
        get() = (1f - progress.value * 3f).coerceIn(0f, 1f)

    /** Alpha for the full player content layer (fades in as sheet opens). */
    val playerAlpha: Float
        get() = ((progress.value - 0.2f) / 0.8f).coerceIn(0f, 1f)

    /**
     * True when the sheet is more than halfway open.
     * Used to flip the status bar appearance and enable full-player gestures.
     */
    val isExpanded: Boolean
        get() = progress.value > 0.5f

    // ── Drag handling ────────────────────────────────────────────────────

    /**
     * Called on every drag delta from the sheet's pointerInput or the nested scroll connection.
     * [deltaY] is positive when dragging downward (collapsing direction).
     */
    fun onDragDelta(deltaY: Float) {
        // Convert pixel delta to progress delta (dragging down = decreasing progress)
        val progressDelta = -deltaY / (screenHeightPx - miniPeekPx)
        val newProgress = (progress.value + progressDelta).coerceIn(0f, 1f)
        scope.launch { progress.snapTo(newProgress) }
    }

    /**
     * Called when the drag gesture ends.
     * A strong fling (above [velocityThresholdPxPerSec]) wins over position;
     * otherwise the sheet snaps to whichever anchor [progress] is closest to.
     *
     * [velocityY] is in px/s, positive = downward (collapsing direction).
     */
    fun onDragEnd(velocityY: Float, velocityThresholdPxPerSec: Float = 800f) {
        scope.launch {
            val targetAnchor = when {
                velocityY < -velocityThresholdPxPerSec -> 1f  // strong upward fling → expand
                velocityY > velocityThresholdPxPerSec -> 0f   // strong downward fling → collapse
                progress.value >= 0.5f -> 1f                  // position-based
                else -> 0f
            }
            val spec = if (targetAnchor == 1f) MotionTokens.SheetExpandSpring
                       else MotionTokens.SheetCollapseSpring
            progress.animateTo(targetAnchor, spec)
        }
    }

    // ── Programmatic expand / collapse ───────────────────────────────────

    suspend fun expand() {
        progress.animateTo(1f, MotionTokens.SheetExpandSpring)
    }

    suspend fun collapse() {
        progress.animateTo(0f, MotionTokens.SheetCollapseSpring)
    }
}

@Composable
fun rememberPlayerSheetState(
    screenHeightPx: Float,
    miniPeekPx: Float,
): PlayerSheetState {
    val scope = rememberCoroutineScope()
    return remember(screenHeightPx, miniPeekPx) {
        PlayerSheetState(
            screenHeightPx = screenHeightPx,
            miniPeekPx = miniPeekPx,
            scope = scope,
        )
    }
}
