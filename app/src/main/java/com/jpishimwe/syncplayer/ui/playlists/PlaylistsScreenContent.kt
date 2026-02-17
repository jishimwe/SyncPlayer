package com.jpishimwe.syncplayer.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jpishimwe.syncplayer.model.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreenContent(
    uiState: PlaylistUiState,
    onCreatePlaylist: (name: String) -> Unit,
    onRenamePlaylist: (id: Long, name: String) -> Unit,
    onDeletePlaylist: (id: Long) -> Unit,
    onPlaylistClick: (id: Long, name: String) -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Playlists") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
    ) { padding ->

        when (uiState) {
            is PlaylistUiState.Loading -> {
                Icon(Icons.Default.Error, contentDescription = null)
            }

            is PlaylistUiState.Loaded -> {
                if (uiState.playlists.isEmpty()) {
                    EmptyState("No playlists found")
                    return@Scaffold
                } else {
                    LazyColumn(modifier = Modifier.padding(padding)) {
                        itemsIndexed(
                            uiState.playlists,
                            key = { _, playlist -> playlist.id },
                        ) { _, playlist ->
                            PlaylistListItem(
                                playlist,
                                onPlaylistClick = { onPlaylistClick(playlist.id, playlist.name) },
                                onRenamePlaylist = { renameTarget = playlist },
                                onDeletePlaylist = { deleteTarget = playlist },
                            )
                        }
                    }
                }
            }
        }
        if (showCreateDialog) {
            CreatePlaylistDialog(
                onCreatePlaylist = { name ->
                    onCreatePlaylist(name)
                    showCreateDialog = false
                },
                onDismiss = { showCreateDialog = false },
            )
        }

        renameTarget?.let { playlistToRename ->
            RenamePlaylistDialog(
                playlist = playlistToRename,
                onRenamePlaylist = { id, name -> onRenamePlaylist(id, name) },
                onDismiss = { renameTarget = null },
            )
        }
        deleteTarget?.let { playlistToDelete ->
            DeletePlaylistDialog(
                playlist = playlistToDelete,
                onDeletePlaylist = { onDeletePlaylist(playlistToDelete.id) },
                onDismiss = { deleteTarget = null },
            )
        }
    }
}

@Composable
fun PlaylistListItem(
    playlist: Playlist,
    onPlaylistClick: () -> Unit,
    onRenamePlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = { onPlaylistClick() }),
        leadingContent = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
        overlineContent = {
            Text(playlist.createdAt.toString())
        },
        headlineContent = { Text(playlist.name) },
        supportingContent = { Text(playlist.songCount.toString()) },
        trailingContent = {
            Box {
                var isExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { isExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            onRenamePlaylist()
                            isExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDeletePlaylist()
                            isExpanded = false
                        },
                    )
                }
            }
        },
    )
}

@Composable
fun CreatePlaylistDialog(
    onCreatePlaylist: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { newName -> name = newName },
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreatePlaylist(name)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun RenamePlaylistDialog(
    playlist: Playlist,
    onRenamePlaylist: (id: Long, name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(playlist.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { newName -> name = newName },
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onRenamePlaylist(playlist.id, name)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun DeletePlaylistDialog(
    playlist: Playlist,
    onDeletePlaylist: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Playlist") },
        text = { Text("Are you sure you want to delete ${playlist.name}?") },
        confirmButton = {
            TextButton(
                onClick = {
                    onDeletePlaylist()
                    onDismiss()
                },
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(message)
    }
}
