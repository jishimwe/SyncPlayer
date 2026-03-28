package com.jpishimwe.syncplayer.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.FixedColorProvider
import com.jpishimwe.syncplayer.MainActivity
import com.jpishimwe.syncplayer.R
import com.jpishimwe.syncplayer.ui.theme.LocalExtendedColorScheme
import java.io.InputStream

/** Accent color matching the app theme (#FF1D58) */
private val AccentColor = "#FFFF1D58".toColorInt()
private val TextPrimary = "#FFFFFFFF".toColorInt()
private val TextSecondary = "#B3FFFFFF".toColorInt()

private fun openAppIntent(context: Context) =
    actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
    )

class NowPlayingWidget : GlanceAppWidget() {
    companion object {
        val COMPACT = DpSize(250.dp, 72.dp)
        val EXPANDED = DpSize(250.dp, 140.dp)
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, EXPANDED))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            val prefs = currentState<Preferences>()
            val title = prefs[WidgetStateKeys.TITLE]
            val artist = prefs[WidgetStateKeys.ARTIST]
            val album = prefs[WidgetStateKeys.ALBUM]
            val albumArtUri = prefs[WidgetStateKeys.ALBUM_ART_URI]
            val isPlaying = prefs[WidgetStateKeys.IS_PLAYING] ?: false
            val hasContent = title != null
            val subtitle = buildSubtitle(album, artist)

            GlanceTheme {
                // Entire widget — dark background with rounded corners
                Box(
                    modifier =
                        GlanceModifier
                            .fillMaxSize()
                            .background(ImageProvider(R.drawable.widget_background))
                            .clickable(openAppIntent(context)),
                ) {
                    val size = LocalSize.current
                    if (size.height >= EXPANDED.height) {
                        ExpandedContent(context, title, subtitle, albumArtUri, isPlaying, hasContent)
                    } else {
                        CompactContent(context, title, subtitle, albumArtUri, isPlaying, hasContent)
                    }
                }
            }
        }
    }
}

// ── Compact (4×1): album art | title + subtitle | controls ──────────────────

@SuppressLint("RestrictedApi")
@Composable
private fun CompactContent(
    context: Context,
    title: String?,
    subtitle: String?,
    albumArtUri: String?,
    isPlaying: Boolean,
    hasContent: Boolean,
) {
    Row(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Album art
        AlbumArt(context, albumArtUri, size = 56.dp)

        Spacer(modifier = GlanceModifier.width(12.dp))

        // Song info
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Title in accent color (like the reference)
            Text(
                text = title ?: context.getString(R.string.widget_not_playing),
                style =
                    TextStyle(
                        color =
                            FixedColorProvider(
                                if (hasContent) {
                                    LocalExtendedColorScheme.current.accentColor.color
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            ),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                maxLines = 1,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style =
                        TextStyle(
                            color =
                                FixedColorProvider(
                                    MaterialTheme.colorScheme.primary,
                                ),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                        ),
                    maxLines = 1,
                )
            }
        }

        // Controls
        if (hasContent) {
            MediaControls(isPlaying = isPlaying)
        }
    }
}

// ── Expanded (4×2): album art + info on top, controls below ─────────────────

@SuppressLint("RestrictedApi")
@Composable
private fun ExpandedContent(
    context: Context,
    title: String?,
    subtitle: String?,
    albumArtUri: String?,
    isPlaying: Boolean,
    hasContent: Boolean,
) {
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
    ) {
        // Top: album art + song info
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(context, albumArtUri, size = 80.dp)

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title ?: context.getString(R.string.widget_not_playing),
                    style =
                        TextStyle(
                            color =
                                FixedColorProvider(
                                    if (hasContent) {
                                        LocalExtendedColorScheme.current.accentColor.color
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                ),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                    maxLines = 1,
                )
                if (subtitle != null) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style =
                            TextStyle(
                                color =
                                    FixedColorProvider(
                                        MaterialTheme.colorScheme.primary,
                                    ),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                            ),
                        maxLines = 1,
                    )
                }
            }
        }

        // Bottom: controls centered
        if (hasContent) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaControls(isPlaying = isPlaying)
            }
        }
    }
}

// ── Media controls row ──────────────────────────────────────────────────────

@Composable
private fun MediaControls(isPlaying: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Previous — subtle background
        ControlButton(
            iconRes = R.drawable.ic_widget_skip_previous,
            contentDescription = "Previous",
            btnSize = 40.dp,
            iconSize = 22.dp,
            action = actionRunCallback<SkipPreviousCallback>(),
            backgroundRes = R.drawable.widget_control_bg,
            tintColor = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = GlanceModifier.width(6.dp))

        // Play/Pause — accent background, larger
        ControlButton(
            iconRes = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            contentDescription = if (isPlaying) "Pause" else "Play",
            btnSize = 52.dp,
            iconSize = 28.dp,
            action = actionRunCallback<PlayPauseCallback>(),
            backgroundRes = R.drawable.widget_play_btn_bg,
            tintColor = LocalExtendedColorScheme.current.accentColor.color,
        )

        Spacer(modifier = GlanceModifier.width(6.dp))

        // Next — subtle background
        ControlButton(
            iconRes = R.drawable.ic_widget_skip_next,
            contentDescription = "Next",
            btnSize = 40.dp,
            iconSize = 22.dp,
            action = actionRunCallback<SkipNextCallback>(),
            backgroundRes = R.drawable.widget_control_bg,
            tintColor = MaterialTheme.colorScheme.primary,
        )
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun ControlButton(
    iconRes: Int,
    contentDescription: String,
    btnSize: Dp,
    iconSize: Dp,
    action: Action,
    backgroundRes: Int,
    tintColor: Color,
) {
    Box(
        modifier =
            GlanceModifier
                .size(btnSize)
                .background(ImageProvider(backgroundRes))
                .clickable(action, rippleOverride = 0),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(iconSize),
            colorFilter =
                ColorFilter.tint(
                    FixedColorProvider(
                        tintColor,
                    ),
                ),
        )
    }
}

// ── Album art ───────────────────────────────────────────────────────────────

@SuppressLint("RestrictedApi")
@Composable
private fun AlbumArt(
    context: Context,
    albumArtUri: String?,
    size: Dp,
) {
    val bitmap = albumArtUri?.let { loadAlbumArt(context, it) }
    Box(
        modifier =
            GlanceModifier
                .size(size)
                .background(ImageProvider(R.drawable.widget_album_art_clip)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "Album art",
                modifier = GlanceModifier.fillMaxSize().cornerRadius(8.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.album_default_foreground),
                contentDescription = "No album art",
                modifier = GlanceModifier.size(32.dp),
                colorFilter =
                    ColorFilter.tint(
                        FixedColorProvider(
                            MaterialTheme.colorScheme.secondary,
                        ),
                    ),
            )
        }
    }
}

// ── Utilities ───────────────────────────────────────────────────────────────

private fun buildSubtitle(
    album: String?,
    artist: String?,
): String? {
    val parts =
        listOfNotNull(
            album?.takeIf { it.isNotBlank() },
            artist?.takeIf { it.isNotBlank() },
        )
    return parts.joinToString(" · ").takeIf { it.isNotEmpty() }
}

private fun loadAlbumArt(
    context: Context,
    uriString: String,
): Bitmap? =
    try {
        val uri = uriString.toUri()
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        Log.e("NowPlayingWidget", "Failed to load album art", e)
        null
    }
