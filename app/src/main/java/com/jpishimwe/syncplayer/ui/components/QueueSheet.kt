package com.jpishimwe.syncplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jpishimwe.syncplayer.R
import com.jpishimwe.syncplayer.model.QueueItem
import com.jpishimwe.syncplayer.ui.theme.myAccentColor
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
    formatTime: (Long) -> String,
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
            formatTime = formatTime,
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
    formatTime: (Long) -> String,
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
                text = if (queue.isEmpty()) stringResource(R.string.queue_title) else stringResource(R.string.queue_title_count, queue.size),
                style = MaterialTheme.typography.titleLarge,
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
                            backgroundContent = {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.error),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cd_remove_from_queue),
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.padding(end = 16.dp),
                                    )
                                }
                            },
                        ) {
                            QueueItemRow(
                                item = item,
                                isPlaying = index == currentIndex,
                                onSongClick = { onSongClick(index) },
                                formatTime = formatTime,
                                isDragging = isDragging,
                                dragModifier = Modifier.draggableHandle(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueItemRow(
    item: QueueItem,
    isPlaying: Boolean,
    onSongClick: () -> Unit,
    formatTime: (Long) -> String,
    isDragging: Boolean,
    dragModifier: Modifier = Modifier,
) {
    val accentBorder =
        if (isPlaying) {
            Modifier.border(1.5.dp, myAccentColor, RoundedCornerShape(8.dp))
        } else {
            Modifier
        }

    val bgColor =
        when {
            isDragging -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            isPlaying -> myAccentColor.copy(alpha = 0.08f)
            else -> Color.Transparent
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(accentBorder)
                .background(bgColor)
                .clickable(onClick = onSongClick)
                .padding(8.dp)
                .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Album art
        AsyncImage(
            model = item.song.albumArtUri,
            contentDescription = null,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
        )

        Spacer(Modifier.width(12.dp))

        // Track info
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = item.song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPlaying) myAccentColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = if (isPlaying) myAccentColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        // Duration
        Text(
            text = formatTime(item.song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Playing indicator
        if (isPlaying) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = stringResource(R.string.cd_playing),
                tint = myAccentColor,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        // Drag handle (replaces overflow menu per spec)
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = stringResource(R.string.cd_reorder),
            modifier = dragModifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
