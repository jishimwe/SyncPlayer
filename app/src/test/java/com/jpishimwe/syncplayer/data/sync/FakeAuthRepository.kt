package com.jpishimwe.syncplayer.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()
    override val currentUserId: String?
        get() = (_authState.value as? AuthState.SignedIn)?.userId

    var signInCallCount = 0
    var lastSignInToken: String? = null
    var signOutCallCount = 0

    fun emitSignedIn(
        userId: String = "user123",
        displayName: String? = "Test User",
        email: String? = "test@example.com",
    ) {
        _authState.value = AuthState.SignedIn(userId, displayName, email, photoUrl = null)
    }

    fun emitSignedOut() {
        _authState.value = AuthState.SignedOut
    }

    override suspend fun signInWithToken(idToken: String) {
        signInCallCount++
        lastSignInToken = idToken
    }

    override suspend fun signOut() {
        signOutCallCount++
        _authState.value = AuthState.SignedOut
    }
}
