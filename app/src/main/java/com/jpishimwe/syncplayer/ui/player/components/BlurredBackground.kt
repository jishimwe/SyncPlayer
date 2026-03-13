package com.jpishimwe.syncplayer.ui.player.components

import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.view.drawToBitmap
import com.jpishimwe.syncplayer.ui.theme.frostedGlassRendered

/**
 * Singleton that holds a screenshot bitmap captured before navigating to a detail screen.
 * Not in a ViewModel — bitmaps are large and should not survive configuration changes.
 * The detail screen consumes it as a blurred background, then it can be cleared.
 */
object ScreenshotHolder {
    var bitmap: Bitmap? = null

    fun capture(view: View) {
        bitmap =
            try {
                view.drawToBitmap()
            } catch (e: Exception) {
                // If capture fails (e.g., hardware-accelerated layer issues), silently skip.
                // The detail screen will show a dark fallback instead.
                null
            }
    }

    fun clear() {
        bitmap?.recycle()
        bitmap = null
    }
}

/**
 * Blurred screenshot background for detail screens.
 * Reads from [ScreenshotHolder] and displays the bitmap with a GPU blur + dark scrim.
 * Falls back to a plain dark background if no screenshot is available.
 *
 * Usage: Place as Layer 0 inside a `Box(Modifier.fillMaxSize())`.
 */
@Composable
fun BlurredBackground(
    modifier: Modifier = Modifier,
    blurRadiusX: Float = 25f,
    blurRadiusY: Float = 25f,
    scrimAlpha: Float = 0.55f,
) {
    val screenshot = ScreenshotHolder.bitmap

    Box(modifier = modifier.fillMaxSize()) {
        if (screenshot != null && !screenshot.isRecycled) {
            // Blurred screenshot layer — GPU-accelerated RenderEffect (API 31+, minSdk 34)
            Image(
                bitmap = screenshot.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .frostedGlassRendered(),
            )
        }

        // Dark scrim on top — makes the blurred content subtle
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha)),
        )
    }
}
