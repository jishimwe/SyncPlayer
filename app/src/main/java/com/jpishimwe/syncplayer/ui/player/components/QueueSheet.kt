package com.jpishimwe.syncplayer.ui.player.components

import android.widget.Button
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jpishimwe.syncplayer.model.QueueItem
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
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            onReorder(from.key.toString(), to.index)
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            Text(
                text = "Queue (${queue.size})",
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(state = lazyListState) {
                itemsIndexed(
                    items = queue,
                    key = { _, item -> item.song.id },
                ) { index, item ->
                    ReorderableItem(reorderableLazyListState, key = item.song.id) { isDragging ->
                        QueueItemRow(
                            item = item,
                            isPlaying = index == currentIndex,
                            onSongClick = { onSongClick(index) },
                            onRemove = { onRemove(item.song.id.toString()) },
                            formatTime = formatTime,
                            isDragging = isDragging,
                            modifier = Modifier.draggableHandle(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueueItemRow(
    item: QueueItem,
    isPlaying: Boolean,
    onSongClick: (index: Int) -> Unit,
    onRemove: (id: String) -> Unit,
    formatTime: (Long) -> String,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            Modifier
                .padding(
                    8.dp,
                ).height(48.dp)
                .background(
                    if (isDragging) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        Color.Transparent
                    },
                ).clickable(onClick = { onSongClick(item.index) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Reorder,
            contentDescription = "Reorder",
            modifier = modifier,
        )

        AsyncImage(
            model = item.song.albumArtUri,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = item.song.title,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = item.song.artist,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = formatTime(item.song.duration),
            style = MaterialTheme.typography.bodySmall,
        )

        if (isPlaying) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Playing",
            )
        }

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = { onRemove(item.song.id.toString()) },
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove",
            )
        }
    }
}
