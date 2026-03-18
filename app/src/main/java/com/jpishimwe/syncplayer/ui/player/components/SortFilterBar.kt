package com.jpishimwe.syncplayer.ui.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.ui.library.SortOrder
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.frostedGlassRendered
import com.jpishimwe.syncplayer.ui.theme.myAccentColor

/**
 * Sticky row shown above a song list.
 * Left side: tappable sort label (e.g. "Title ▾") that opens a sort picker.
 * Right side: Shuffle + Play All icon buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortFilterBar(
    selectedSort: SortOrder,
    sortOptions: List<SortOrder> = SortOrder.entries.toList(),
    onSortClick: (sortOrder: SortOrder) -> Unit,
    onShuffle: () -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    myAccentColor.copy(alpha = 0.24f),
                    myAccentColor.copy(alpha = 0.75f),
                    myAccentColor.copy(alpha = 0.24f),
                ),
        )
    val barShape = RoundedCornerShape(8.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .border(BorderStroke(1.dp, borderBrush), barShape)
                .clip(barShape),
    ) {
        Box(
            modifier = Modifier.matchParentSize().frostedGlassRendered(),
        )

        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .wrapContentWidth()
                    .padding(horizontal = 0.dp, vertical = 0.dp)
                    .border(BorderStroke(1.dp, borderBrush), barShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var expanded by remember { mutableStateOf(false) }
            // Sort label — TextButton keeps touch target large, no ripple bleed
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = { Text(selectedSort.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Transparent,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    border = null,
                    modifier =
                        Modifier
                            .widthIn(min = 24.dp)
                            .menuAnchor(),
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    sortOptions.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = {
                                onSortClick(order)
                                expanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Shuffle button
            IconButton(onClick = onShuffle) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Play all button
            IconButton(onClick = onPlayAll) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play all",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
private fun SortFilterBarPreview() {
    SyncPlayerTheme(darkTheme = true) {
        SortFilterBar(
            selectedSort = SortOrder.BY_TITLE,
            onSortClick = {},
            onShuffle = {},
            onPlayAll = {},
        )
    }
}
