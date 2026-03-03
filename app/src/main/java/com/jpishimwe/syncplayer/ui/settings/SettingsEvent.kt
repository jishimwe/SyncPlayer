package com.jpishimwe.syncplayer.ui.settings

sealed interface SettingsEvent {
    /** Fires after SettingsScreen completes the CredentialManager flow and has an ID token. */
    data class SignInWithToken(
        val idToken: String,
    ) : SettingsEvent

    /** Fires if CredentialManager throws or user cancels. */
    data class SignInError(
        val message: String,
    ) : SettingsEvent

    data object SignOut : SettingsEvent

    data object SyncNow : SettingsEvent
}
