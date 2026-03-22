package com.jpishimwe.syncplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.effect.frostedGlass
import com.jpishimwe.syncplayer.ui.effect.frostedGlassRendered
import com.jpishimwe.syncplayer.ui.theme.myAccentColor

/**
 * A fully-rounded pill with the frosted-glass background treatment.
 * Content is laid out in a [Row] so callers can place icons, text, or
 * icon+text combos side-by-side with standard [RowScope] modifiers.
 *
 * Usage:
 * ```
 * FrostedGlassPill {
 *     Icon(Icons.Default.Shuffle, contentDescription = null)
 *     Spacer(Modifier.width(4.dp))
 *     Text("Shuffle")
 * }
 * ```
 */
@Composable
fun FrostedGlassPill(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    shape: RoundedCornerShape = RoundedCornerShape(percent = 50),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier.clip(shape),
    ) {
        // Blurred background layer — frostedGlass only affects this Box, not the content
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .then(if (active) Modifier.frostedGlassRendered() else Modifier.frostedGlass()),
        )

        // Content layer — unblurred
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun FrostedGlassPillPreview() {
    SyncPlayerTheme(darkTheme = true) {
        FrostedGlassPill(modifier = Modifier.padding(24.dp)) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = null,
                tint = myAccentColor,
            )
            Spacer(Modifier.width(6.dp))
            Text(text = "Shuffle", color = myAccentColor)
        }
    }
}
