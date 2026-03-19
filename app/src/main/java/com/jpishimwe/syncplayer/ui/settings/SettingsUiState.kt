package com.jpishimwe.syncplayer.ui.settings

import com.jpishimwe.syncplayer.data.sync.AuthState
import com.jpishimwe.syncplayer.data.sync.SyncStatus

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Loaded(
        val authState: AuthState = AuthState.SignedOut,
        val syncStatus: SyncStatus = SyncStatus.Idle,
        val lastSyncTime: Long? = 0L,
    ) : SettingsUiState
}
