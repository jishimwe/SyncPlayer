package com.jpishimwe.syncplayer.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.jpishimwe.syncplayer.R
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // CredentialManager instance survives recomposition
    val credentialManager = remember { CredentialManager.create(context) }
    val clientId = stringResource(R.string.default_web_client_id)

    SettingsScreenContent(
        uiState = uiState,
        onSignIn = {
            scope.launch {
                try {
                    val request =
                        GetCredentialRequest
                            .Builder()
                            .addCredentialOption(
                                GetGoogleIdOption
                                    .Builder()
                                    .setFilterByAuthorizedAccounts(
                                        false,
                                    ).setServerClientId(clientId)
                                    .build(),
                            ).build()

                    val result = credentialManager.getCredential(context, request)
                    val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken

                    viewModel.onEvent(SettingsEvent.SignInWithToken(googleIdToken))
                } catch (e: Exception) {
                    viewModel.onEvent(SettingsEvent.SignInError(e.message ?: "Sign-in cancelled"))
                }
            }
        },
        onSignOut = { viewModel.onEvent(SettingsEvent.SignOut) },
        onSyncNow = { viewModel.onEvent(SettingsEvent.SyncNow) },
    )
}
