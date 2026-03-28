package com.jpishimwe.syncplayer.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpishimwe.syncplayer.ui.effect.gradientBorderStroke
import com.jpishimwe.syncplayer.ui.theme.LocalExtendedColorScheme
import com.jpishimwe.syncplayer.ui.theme.myAccentColor

// Colors matching the Glance widget
private val WidgetBackground = Color(0xE6111113)
private val WidgetTextPrimary = Color.White
private val WidgetTextSecondary = Color.White.copy(alpha = 0.7f)
private val WidgetControlBg = Color.White.copy(alpha = 0.1f)
private val WidgetPlayBtnBg = myAccentColor.copy(alpha = 0.2f)
private val WidgetAlbumBg = Color.White.copy(alpha = 0.1f)

// ── Compact preview ─────────────────────────────────────────────────────────

@Composable
private fun WidgetCompactPreview(
    title: String?,
    subtitle: String?,
    isPlaying: Boolean,
    hasContent: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .gradientBorderStroke(shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(WidgetBackground)
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art placeholder
            AlbumArtPreview(size = 56.dp, hasArt = hasContent)

            Spacer(Modifier.width(8.dp))

            // Song info
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title ?: "Not playing",
                    color = if (hasContent) LocalExtendedColorScheme.current.accentColor.color else MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = WidgetTextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Controls
            if (hasContent) {
                ControlsPreview(isPlaying = isPlaying)
            }
        }
    }
}

// ── Expanded preview ────────────────────────────────────────────────────────

@Composable
private fun WidgetExpandedPreview(
    title: String?,
    subtitle: String?,
    isPlaying: Boolean,
    hasContent: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .gradientBorderStroke(shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(WidgetBackground)
            .padding(12.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top: art + info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtPreview(size = 80.dp, hasArt = hasContent)

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title ?: "Not playing",
                        color = if (hasContent) LocalExtendedColorScheme.current.accentColor.color else WidgetTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = WidgetTextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Bottom: controls centered
            if (hasContent) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ControlsPreview(isPlaying = isPlaying)
                }
            }
        }
    }
}

// ── Shared composables ──────────────────────────────────────────────────────

@Composable
private fun AlbumArtPreview(size: Dp, hasArt: Boolean) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(WidgetAlbumBg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(if (hasArt) size * 0.5f else 32.dp),
            tint = WidgetTextSecondary,
        )
    }
}

@Composable
private fun ControlsPreview(isPlaying: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Previous
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(WidgetControlBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(22.dp),
                tint = WidgetTextPrimary,
            )
        }

        Spacer(Modifier.width(6.dp))

        // Play/Pause
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(WidgetPlayBtnBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(28.dp),
                tint = LocalExtendedColorScheme.current.accentColor.color,
            )
        }

        Spacer(Modifier.width(6.dp))

        // Next
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(WidgetControlBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(22.dp),
                tint = WidgetTextPrimary,
            )
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun CompactPlayingPreview() {
    WidgetCompactPreview(
        title = "KNOW ABOUT ME",
        subtitle = "Fe3O4: FORWARD · NMIXX",
        isPlaying = true,
        hasContent = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun CompactPausedPreview() {
    WidgetCompactPreview(
        title = "KNOW ABOUT ME",
        subtitle = "Fe3O4: FORWARD · NMIXX",
        isPlaying = false,
        hasContent = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun CompactIdlePreview() {
    WidgetCompactPreview(
        title = null,
        subtitle = null,
        isPlaying = false,
        hasContent = false,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun ExpandedPlayingPreview() {
    WidgetExpandedPreview(
        title = "KNOW ABOUT ME",
        subtitle = "Fe3O4: FORWARD · NMIXX",
        isPlaying = true,
        hasContent = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun ExpandedPausedPreview() {
    WidgetExpandedPreview(
        title = "KNOW ABOUT ME",
        subtitle = "Fe3O4: FORWARD · NMIXX",
        isPlaying = false,
        hasContent = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun ExpandedIdlePreview() {
    WidgetExpandedPreview(
        title = null,
        subtitle = null,
        isPlaying = false,
        hasContent = false,
    )
}
