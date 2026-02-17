package com.jpishimwe.syncplayer.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAddCheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jpishimwe.syncplayer.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongPickerSheet(
    allSongs: List<Song>,
    selectedSongs: List<Song>,
    onDismiss: () -> Unit,
    onAddSongs: (songsIdsToAdd: List<Long>) -> Unit,
    onRemoveSongs: (songsIdsToRemove: List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    var songsToAdd by remember { mutableStateOf(selectedSongs.map { it.id }) }
    val selectedSongIds = selectedSongs.map { it.id }.toSet()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column {
            LazyColumn(state = lazyListState, modifier = Modifier.weight(1f)) {
                itemsIndexed(
                    items = allSongs,
                    key = { _, item -> item.id },
                ) { _, item ->
                    SongSelectorItem(
                        song = item,
                        isSelected = songsToAdd.contains(item.id),
                        onSelected = { isChecked ->
                            songsToAdd = if (isChecked) songsToAdd + (item.id) else songsToAdd - (item.id)
                        },
                    )
                }
            }

            Button(
                onClick = {
                    onAddSongs(
                        (songsToAdd - selectedSongIds).toSet().toList(),
                    )
                    onRemoveSongs(
                        (selectedSongIds - songsToAdd.toSet()).toSet().toList(),
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Icon(Icons.Default.PlaylistAddCheckCircle, contentDescription = "Add songs")
                Spacer(Modifier.width(8.dp))
                Text("Confirm")
            }
        }
    }
}

@Composable
fun SongSelectorItem(
    song: Song,
    isSelected: Boolean,
    onSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        leadingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.clickable(
                        onClick = { onSelected(!isSelected) },
                    ),
            ) {
                Checkbox(
                    isSelected,
                    onCheckedChange = { onSelected(it) },
                )
                Spacer(Modifier.width(8.dp))
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                )
            }
        },
        headlineContent = {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}
