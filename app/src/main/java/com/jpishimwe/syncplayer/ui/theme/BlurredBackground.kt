package com.jpishimwe.syncplayer.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
                val window =
                    view.context.getActivityWindow() ?: run {
                        Log.e("ScreenshotHolder", "capture FAILED: no window")
                        return
                    }
                val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

                // Use a background HandlerThread for the PixelCopy callback.
                // PixelCopy.request posts the callback to the provided Handler.
                // If that Handler is on the main thread, and we block the main thread
                // with latch.await(), the callback can never run → deadlock.
                val handlerThread = HandlerThread("PixelCopyThread")
                handlerThread.start()
                val handler = Handler(handlerThread.looper)

                val latch = CountDownLatch(1)
                var copyResult = PixelCopy.ERROR_UNKNOWN
                PixelCopy.request(
                    window,
                    bmp,
                    { result ->
                        copyResult = result
                        latch.countDown()
                    },
                    handler,
                )
                latch.await(1000, TimeUnit.MILLISECONDS)
                handlerThread.quitSafely()

                if (copyResult == PixelCopy.SUCCESS) {
                    Log.e("ScreenshotHolder", "capture SUCCESS: ${bmp.width}x${bmp.height} | view ${view.width}x${view.height}")
                    bmp
                } else {
                    Log.e("ScreenshotHolder", "capture FAILED: PixelCopy result=$copyResult | view ${view.width}x${view.height}")
                    bmp.recycle()
                    null
                }
            } catch (e: Exception) {
                Log.e("ScreenshotHolder", "capture FAILED: ${e.message}")
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

/** Helper to get the Activity's Window from a Context */
private fun Context.getActivityWindow(): Window? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx.window
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Blurs a bitmap using Android's hardware-accelerated RenderEffect (API 31+).
 * Returns a new software bitmap with the blur baked in, or the original on failure.
 */
private fun blurBitmap(
    source: Bitmap,
    radiusPx: Float,
): Bitmap =
    try {
        val width = source.width
        val height = source.height

        val renderNode =
            RenderNode("blur").apply {
                setPosition(0, 0, width, height)
                setRenderEffect(
                    RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP),
                )
            }

        // Draw the source bitmap into the render node
        val canvas = renderNode.beginRecording()
        canvas.drawBitmap(source, 0f, 0f, null)
        renderNode.endRecording()

        // Render to an ImageReader via HardwareRenderer
        val imageReader =
            ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                1,
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
            )
        val renderer =
            HardwareRenderer().apply {
                setSurface(imageReader.surface)
                setContentRoot(renderNode)
            }

        renderer
            .createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()

        val image = imageReader.acquireLatestImage()
        val hardwareBuffer = image?.hardwareBuffer

        val result =
            if (hardwareBuffer != null) {
                val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                // Convert to software bitmap so Compose Image can draw it
                hardwareBitmap?.copy(Bitmap.Config.ARGB_8888, false).also {
                    hardwareBitmap?.recycle()
                }
            } else {
                null
            }

        // Cleanup
        image?.close()
        imageReader.close()
        renderer.destroy()
        renderNode.discardDisplayList()

        result ?: source
    } catch (e: Exception) {
        // If hardware blur fails, return original unblurred bitmap
        source
    }

/**
 * Blurred screenshot background for detail screens and Now Playing.
 * Reads from [ScreenshotHolder], blurs the bitmap via hardware RenderEffect,
 * and displays it with a dark scrim overlay.
 * Falls back to a plain dark background if no screenshot is available.
 *
 * Usage: Place as Layer 0 inside a `Box(Modifier.fillMaxSize())`.
 */
@Composable
fun BlurredBackground(
    modifier: Modifier = Modifier,
    blurRadiusPx: Float = 80f,
    scrimAlpha: Float = 0.55f,
) {
    val screenshot = ScreenshotHolder.bitmap
    Log.e("BlurredBackground", "screenshot=${screenshot?.let { "${it.width}x${it.height}, recycled=${it.isRecycled}" } ?: "NULL"}")

    // Blur once and cache — recomputes only when the screenshot identity changes
    val blurredBitmap =
        remember(screenshot) {
            if (screenshot != null && !screenshot.isRecycled) {
                blurBitmap(screenshot, blurRadiusPx)
            } else {
                null
            }
        }

    Box(modifier = modifier.fillMaxSize()) {
        if (blurredBitmap != null && !blurredBitmap.isRecycled) {
            Image(
                bitmap = blurredBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Dark scrim on top
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha)),
        )
    }
}
