package com.jpishimwe.syncplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.R
import com.jpishimwe.syncplayer.model.Playlist

/**
 * Modal bottom sheet that lists all playlists for the user to pick one.
 * Includes a "Create new playlist" option at the top.
 *
 * @param playlists The available playlists to choose from.
 * @param onPlaylistSelected Called with the playlist ID when the user picks one.
 * @param onCreatePlaylist Called with the new playlist name when the user creates one.
 * @param onDismiss Called when the sheet is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerSheet(
    playlists: List<Playlist>,
    onPlaylistSelected: (playlistId: Long) -> Unit,
    onCreatePlaylist: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var showCreateField by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.playlist_picker_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Create new playlist
            if (showCreateField) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text(stringResource(R.string.playlist_picker_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val trimmed = newPlaylistName.trim()
                            if (trimmed.isNotBlank()) {
                                onCreatePlaylist(trimmed)
                                newPlaylistName = ""
                                showCreateField = false
                            }
                        },
                        enabled = newPlaylistName.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.playlist_picker_create))
                    }
                }
            } else {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.playlist_picker_create_new)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    modifier = Modifier.clickable { showCreateField = true },
                )
            }

            Spacer(Modifier.height(4.dp))

            // Existing playlists
            LazyColumn {
                items(playlists, key = { it.id }) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        supportingContent = {
                            Text(stringResource(R.string.playlist_song_count, playlist.songCount))
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        modifier = Modifier.clickable { onPlaylistSelected(playlist.id) },
                    )
                }
            }
        }
    }
}
