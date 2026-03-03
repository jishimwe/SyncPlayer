package com.jpishimwe.syncplayer.ui.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.jpishimwe.syncplayer.data.sync.AuthState
import com.jpishimwe.syncplayer.data.sync.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                // Account section
                when (val auth = uiState.authState) {
                    is AuthState.SignedOut -> {
                        ListItem(
                            headlineContent = { Text("Sign in with Google") },
                            supportingContent = { Text("Sign in to sync your music library") },
                            trailingContent = {
                                Button(onClick = onSignIn) { Text("Sign in") }
                            },
                        )
                    }

                    is AuthState.SignedIn -> {
                        ListItem(
                            headlineContent = { Text(auth.displayName ?: "Signed in") },
                            supportingContent = { Text(auth.email ?: auth.userId) },
                            trailingContent = {
                                Button(onClick = onSignOut) { Text("Sign out") }
                            },
                        )
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                // Sync section (only shown when signed in)
                if (uiState.authState is AuthState.SignedIn) {
                    SyncStatusCard(
                        syncStatus = uiState.syncStatus,
                        lastSyncTime = uiState.lastSyncTime,
                        onSyncNow = onSyncNow,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncStatusCard(
    syncStatus: SyncStatus,
    lastSyncTime: Long?,
    onSyncNow: () -> Unit,
) {
    ListItem(
        headlineContent = { Text("Sync") },
        supportingContent = {
            when (syncStatus) {
                SyncStatus.Idle -> if (lastSyncTime != null) Text("Last sync: ${formatSyncTime(lastSyncTime)}") else Text("No sync history")
                SyncStatus.Syncing -> Text("Syncing...")
                is SyncStatus.Success -> Text("Last sync: ${formatSyncTime(syncStatus.syncedAt)}")
                is SyncStatus.Error -> Text(syncStatus.message)
            }
        },
        trailingContent = {
            val syncing = syncStatus is SyncStatus.Syncing
            if (syncing) {
                IconButton(onClick = onSyncNow) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync now")
                }
            }
        },
    )
}

private fun formatSyncTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
