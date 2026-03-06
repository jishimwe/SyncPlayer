package com.jpishimwe.syncplayer.ui.settings

import app.cash.turbine.test
import com.jpishimwe.syncplayer.MainDispatcherRule
import com.jpishimwe.syncplayer.data.sync.AuthState
import com.jpishimwe.syncplayer.data.sync.FakeAuthRepository
import com.jpishimwe.syncplayer.data.sync.SyncOrchestrator
import com.jpishimwe.syncplayer.data.sync.SyncStatus
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var syncOrchestrator: SyncOrchestrator
    private val syncStatusFlow = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setup() {
        fakeAuthRepository = FakeAuthRepository()
        syncOrchestrator =
            mockk(relaxed = true) {
                every { syncStatus } returns syncStatusFlow.asStateFlow()
                every { lastSyncTime } returns 0L
            }
        viewModel = SettingsViewModel(fakeAuthRepository, syncOrchestrator)
    }

    @Test
    fun `initial state is signed out with idle sync status`() =
        runTest {
            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(AuthState.SignedOut, state.authState)
                assertEquals(SyncStatus.Idle, state.syncStatus)
            }
        }

    @Test
    fun `no previous sync — lastSyncTime is null`() =
        runTest {
            // lastSyncTime prefs = 0L, takeIf { > 0 } = null; syncStatus not Success → null
            viewModel.uiState.test {
                val state = awaitItem()
                assertNull(state.lastSyncTime)
            }
        }

    @Test
    fun `auth state change to signed in is reflected in uiState`() =
        runTest {
            viewModel.uiState.test {
                awaitItem() // initial state
                fakeAuthRepository.emitSignedIn(userId = "user-1", displayName = "Alice")
                val state = awaitItem()
                assertEquals(AuthState.SignedIn("user-1", "Alice", "test@example.com", null), state.authState)
            }
        }

    @Test
    fun `auth sign out is reflected in uiState`() =
        runTest {
            fakeAuthRepository.emitSignedIn()
            viewModel.uiState.test {
                awaitItem() // signed-in state
                fakeAuthRepository.emitSignedOut()
                val state = awaitItem()
                assertEquals(AuthState.SignedOut, state.authState)
            }
        }

    @Test
    fun `sync status Syncing is reflected in uiState`() =
        runTest {
            viewModel.uiState.test {
                awaitItem() // initial
                syncStatusFlow.value = SyncStatus.Syncing
                val state = awaitItem()
                assertEquals(SyncStatus.Syncing, state.syncStatus)
            }
        }

    @Test
    fun `sync status Success exposes syncedAt as lastSyncTime`() =
        runTest {
            val syncedAt = 1_700_000_000_000L
            viewModel.uiState.test {
                awaitItem() // initial
                syncStatusFlow.value = SyncStatus.Success(syncedAt = syncedAt)
                val state = awaitItem()
                assertEquals(SyncStatus.Success(syncedAt), state.syncStatus)
                assertEquals(syncedAt, state.lastSyncTime)
            }
        }

    @Test
    fun `sync status Error is reflected in uiState`() =
        runTest {
            viewModel.uiState.test {
                awaitItem() // initial
                syncStatusFlow.value = SyncStatus.Error("Firestore unavailable")
                val state = awaitItem()
                assertEquals(SyncStatus.Error("Firestore unavailable"), state.syncStatus)
            }
        }

    @Test
    fun `SignInWithToken event calls authRepository signInWithToken`() =
        runTest {
            viewModel.onEvent(SettingsEvent.SignInWithToken("id-token-abc"))
            advanceUntilIdle()
            assertEquals(1, fakeAuthRepository.signInCallCount)
            assertEquals("id-token-abc", fakeAuthRepository.lastSignInToken)
        }

    @Test
    fun `SignOut event calls authRepository signOut`() =
        runTest {
            fakeAuthRepository.emitSignedIn()
            viewModel.onEvent(SettingsEvent.SignOut)
            advanceUntilIdle()
            assertEquals(1, fakeAuthRepository.signOutCallCount)
        }

    @Test
    fun `SyncNow event triggers syncOrchestrator sync`() =
        runTest {
            viewModel.onEvent(SettingsEvent.SyncNow)
            advanceUntilIdle()
            coVerify(exactly = 1) { syncOrchestrator.sync() }
        }

    @Test
    fun `SignInError event sets snackbarMessage`() {
        viewModel.onEvent(SettingsEvent.SignInError("Sign-in failed"))
        assertEquals("Sign-in failed", viewModel.snackbarMessage.value)
    }

    @Test
    fun `ClearSnackbar clears snackbarMessage`() {
        viewModel.onEvent(SettingsEvent.SignInError("Sign-in failed"))
        viewModel.onEvent(SettingsEvent.ClearSnackbar)
        assertNull(viewModel.snackbarMessage.value)
    }
}
