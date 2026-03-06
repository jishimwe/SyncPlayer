package com.jpishimwe.syncplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpishimwe.syncplayer.data.sync.AuthRepository
import com.jpishimwe.syncplayer.data.sync.SyncOrchestrator
import com.jpishimwe.syncplayer.data.sync.SyncRepository
import com.jpishimwe.syncplayer.data.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val syncOrchestrator: SyncOrchestrator,
    ) : ViewModel() {
        private val _snackbarMessage = MutableStateFlow<String?>(null)
        val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

        val uiState: StateFlow<SettingsUiState> =
            combine(
                authRepository.authState,
                syncOrchestrator.syncStatus,
            ) { authState, syncStatus ->
                SettingsUiState(
                    authState = authState,
                    syncStatus = syncStatus,
                    lastSyncTime = (syncStatus as? SyncStatus.Success)?.syncedAt ?: syncOrchestrator.lastSyncTime.takeIf { it > 0L },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SettingsUiState())

        fun onEvent(event: SettingsEvent) {
            when (event) {
                is SettingsEvent.SignInWithToken -> {
                    viewModelScope.launch { authRepository.signInWithToken(event.idToken) }
                }

                is SettingsEvent.SignInError -> {
                    _snackbarMessage.value = event.message
                }

                SettingsEvent.SignOut -> {
                    viewModelScope.launch { authRepository.signOut() }
                }

                SettingsEvent.SyncNow -> {
                    viewModelScope.launch { syncOrchestrator.sync() }
                }

                SettingsEvent.ClearSnackbar -> {
                    _snackbarMessage.value = null
                }
            }
        }
    }
