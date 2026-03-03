package com.jpishimwe.syncplayer.data.sync

import kotlinx.coroutines.flow.StateFlow

sealed interface AuthState {
    data object SignedOut : AuthState

    data class SignedIn(
        val userId: String,
        val displayName: String?,
        val email: String?,
        val photoUrl: String?,
    ) : AuthState
}

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val currentUserId: String?

    suspend fun signInWithToken(idToken: String)

    suspend fun signOut()
}
