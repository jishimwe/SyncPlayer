package com.jpishimwe.syncplayer.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jpishimwe.syncplayer.data.sync.AuthState
import com.jpishimwe.syncplayer.data.sync.SyncStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenContentTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // ── signed out ────────────────────────────────────────────────────────────

    @Test
    fun signedOut_showsSignInButton() {
        composeTestRule.setContent {
            SettingsScreenContent(
                uiState = SettingsUiState(authState = AuthState.SignedOut),
                onSignIn = {},
                onSignOut = {},
                onSyncNow = {},
                snackbarMessage = null,
                onSnackbarDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("Sign in").assertIsDisplayed()
    }

    @Test
    fun signedOut_clickSignIn_invokesCallback() {
        var signInCalled = false
        composeTestRule.setContent {
            SettingsScreenContent(
                uiState = SettingsUiState(authState = AuthState.SignedOut),
                onSignIn = { signInCalled = true },
                onSignOut = {},
                onSyncNow = {},
                snackbarMessage = null,
                onSnackbarDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("Sign in").performClick()
        assertTrue(signInCalled)
    }

    // ── signed in ─────────────────────────────────────────────────────────────

    @Test
    fun signedIn_showsUserEmailAndSignOutButton() {
        composeTestRule.setContent {
            SettingsScreenContent(
                uiState = SettingsUiState(
                    authState = AuthState.SignedIn(
                        userId = "uid-1",
                        displayName = "Alice",
                        email = "alice@example.com",
                        photoUrl = null,
                    )
                ),
                onSignIn = {},
                onSignOut = {},
                onSyncNow = {},
                snackbarMessage = null,
                onSnackbarDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("alice@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign out").assertIsDisplayed()
    }

    @Test
    fun signedIn_clickSignOut_invokesCallback() {
        var signOutCalled = false
        composeTestRule.setContent {
            SettingsScreenContent(
                uiState = SettingsUiState(
                    authState = AuthState.SignedIn("uid-1", "Alice", "alice@example.com", null)
                ),
                onSignIn = {},
                onSignOut = { signOutCalled = true },
                onSyncNow = {},
                snackbarMessage = null,
                onSnackbarDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("Sign out").performClick()
        assertTrue(signOutCalled)
    }

    // ── sync status ───────────────────────────────────────────────────────────

    @Test
    fun syncError_showsRetryButton() {
        composeTestRule.setContent {
            SettingsScreenContent(
                uiState = SettingsUiState(
                    authState = AuthState.SignedIn("uid-1", null, null, null),
                    syncStatus = SyncStatus.Error("Network unavailable"),
                ),
                onSignIn = {},
                onSignOut = {},
                onSyncNow = {},
                snackbarMessage = null,
                onSnackbarDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun syncError_clickRetry_invokesOnSyncNow() {
        var syncNowCalled = false
        composeTestRule.setContent {
            SettingsScreenContent(
                uiState = SettingsUiState(
                    authState = AuthState.SignedIn("uid-1", null, null, null),
                    syncStatus = SyncStatus.Error("Timeout"),
                ),
                onSignIn = {},
                onSignOut = {},
                onSyncNow = { syncNowCalled = true },
                snackbarMessage = null,
                onSnackbarDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("Retry").performClick()
        assertTrue(syncNowCalled)
    }

    @Test
    fun syncing_showsSyncIcon() {
        composeTestRule.setContent {
            SettingsScreenContent(
                uiState = SettingsUiState(
                    authState = AuthState.SignedIn("uid-1", null, null, null),
                    syncStatus = SyncStatus.Syncing,
                ),
                onSignIn = {},
                onSignOut = {},
                onSyncNow = {},
                snackbarMessage = null,
                onSnackbarDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("Syncing...").assertIsDisplayed()
    }

    @Test
    fun syncSectionNotShown_whenSignedOut() {
        composeTestRule.setContent {
            SettingsScreenContent(
                uiState = SettingsUiState(authState = AuthState.SignedOut),
                onSignIn = {},
                onSignOut = {},
                onSyncNow = {},
                snackbarMessage = null,
                onSnackbarDismiss = {},
            )
        }

        // "Sync" section label should not be visible when signed out
        composeTestRule.onNodeWithText("Sync").assertDoesNotExist()
    }
}
