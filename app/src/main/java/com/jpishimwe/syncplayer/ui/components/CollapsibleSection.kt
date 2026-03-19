package com.jpishimwe.syncplayer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.myAccentColor
import com.jpishimwe.syncplayer.ui.theme.noRippleClickable

/**
 * A section header with Shuffle + Play All actions and an animated
 * expand/collapse chevron. Content is shown/hidden via [AnimatedVisibility].
 *
 * Caller controls state via [isExpanded] / [onToggle] for full hoisting.
 */
@Composable
fun CollapsibleSectionHeader(
    title: String,
    isExpanded: Boolean,
    onShuffle: () -> Unit,
    onPlayAll: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        label = "chevron_rotation",
    )

    val borderBrush =
        Brush.linearGradient(
            colors = listOf(myAccentColor.copy(alpha = 0.24f), myAccentColor.copy(alpha = 0.75f), myAccentColor.copy(alpha = 0.24f)),
        )
    val barShape = RoundedCornerShape(8.dp)

    Row(
        modifier =
            Modifier
                .border(BorderStroke(1.dp, borderBrush), barShape)
                .fillMaxWidth()
                .noRippleClickable { onToggle() }
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        // Shuffle
        IconButton(onClick = onShuffle, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle $title",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }

        // Play all
        IconButton(onClick = onPlayAll, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play $title",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }

        // Collapse chevron
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier =
                Modifier
                    .size(36.dp)
                    .padding(8.dp)
                    .rotate(chevronRotation),
        )
    }

/*    // Animated content
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        content()
    }*/
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun CollapsibleSectionHeaderExpandedPreview() {
    val fakeSongs = listOf("Bohemian Rhapsody", "Enter Sandman", "Back in Black")
    SyncPlayerTheme(darkTheme = true) {
        Column {
            CollapsibleSectionHeader(
                title = "Rock",
                isExpanded = true,
                onToggle = {},
                onShuffle = {},
                onPlayAll = {},
            )
            fakeSongs.forEach { title ->
                HorizontalDivider()
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun CollapsibleSectionHeaderCollapsedPreview() {
    SyncPlayerTheme(darkTheme = true) {
        CollapsibleSectionHeader(
            title = "Rock",
            isExpanded = false,
            onToggle = {},
            onShuffle = {},
            onPlayAll = {},
        )
    }
}
