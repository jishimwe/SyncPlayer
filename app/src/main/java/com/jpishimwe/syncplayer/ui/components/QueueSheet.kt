package com.jpishimwe.syncplayer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.R
import com.jpishimwe.syncplayer.model.QueueItem
import com.jpishimwe.syncplayer.ui.theme.myFrostedGlassSurface
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<QueueItem>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSongClick: (index: Int) -> Unit,
    onRemove: (String) -> Unit,
    onReorder: (String, Int) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = myFrostedGlassSurface.copy(alpha = 0.85f),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        QueueSheetContent(
            queue = queue,
            currentIndex = currentIndex,
            onDismiss = onDismiss,
            onSongClick = onSongClick,
            onRemove = onRemove,
            onReorder = onReorder,
            onClearQueue = onClearQueue,
        )
    }
}

@Composable
fun QueueSheetContent(
    queue: List<QueueItem>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSongClick: (index: Int) -> Unit,
    onRemove: (String) -> Unit,
    onReorder: (String, Int) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (currentIndex >= 0) {
            lazyListState.animateScrollToItem(currentIndex)
        }
    }

    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            onReorder(from.key.toString(), to.index)
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
    ) {
        // --- Header: Queue title + trash + collapse ---
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    if (queue.isEmpty()) {
                        stringResource(R.string.queue_title)
                    } else {
                        stringResource(R.string.queue_title_count, queue.size)
                    },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            // Clear all
            IconButton(onClick = onClearQueue) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_clear_queue),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Collapse
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = stringResource(R.string.cd_collapse_queue),
                )
            }
        }

        // --- Queue items ---
        if (queue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.queue_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(state = lazyListState) {
                itemsIndexed(
                    items = queue,
                    key = { _, item -> item.song.id },
                ) { index, item ->
                    ReorderableItem(reorderableLazyListState, key = item.song.id) { isDragging ->
                        val dismissState =
                            rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        onRemove(item.song.id.toString())
                                        true
                                    } else {
                                        false
                                    }
                                },
                            )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {},
                        ) {
                            SongItem(
                                song = item.song,
                                onClick = { onSongClick(index) },
                                isPlaying = index == currentIndex,
                                isDragging = isDragging,
                                variant = SongItemVariant.Reorderable,
                                reorderableScope = this@ReorderableItem,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113)
@Composable
fun QueueSheetPreview() {
    QueueSheetContent(
        queue = emptyList(),
        currentIndex = 0,
        onDismiss = {},
        onSongClick = {},
        onRemove = {},
        onReorder = { _, _ -> },
        onClearQueue = {},
    )
}
