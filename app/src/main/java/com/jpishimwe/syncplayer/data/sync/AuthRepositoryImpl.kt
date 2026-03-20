package com.jpishimwe.syncplayer.data.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.jpishimwe.syncplayer.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val firebaseAuth: FirebaseAuth,
        @param:ApplicationScope private val externalScope: CoroutineScope,
    ) : AuthRepository {
        override val authState: StateFlow<AuthState> =
            callbackFlow {
                val listener =
                    FirebaseAuth.AuthStateListener { auth ->
                        val user = auth.currentUser
                        val state =
                            if (user == null) {
                                Log.d(TAG, "AuthState: SignedOut")
                                AuthState.SignedOut
                            } else {
                                Log.d(TAG, "AuthState: SignedIn uid=${user.uid} email=${user.email}")
                                AuthState.SignedIn(
                                    userId = user.uid,
                                    displayName = user.displayName,
                                    email = user.email,
                                    photoUrl = user.photoUrl?.toString(),
                                )
                            }
                        trySend(state)
                    }
                firebaseAuth.addAuthStateListener(listener)
                awaitClose { firebaseAuth.removeAuthStateListener(listener) }
            }.stateIn(externalScope, SharingStarted.Eagerly, currentStateFromAuth())

        override val currentUserId: String?
            get() = firebaseAuth.currentUser?.uid

        override suspend fun signInWithToken(idToken: String) {
            Log.d(TAG, "signInWithToken: attempting sign-in")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            Log.d(TAG, "signInWithToken: success, uid=${firebaseAuth.currentUser?.uid}")
        }

        override suspend fun signOut() {
            firebaseAuth.signOut()
        }

        private fun currentStateFromAuth(): AuthState {
            val user = firebaseAuth.currentUser ?: return AuthState.SignedOut
            return AuthState.SignedIn(user.uid, user.displayName, user.email, user.photoUrl?.toString())
        }

        companion object {
            private const val TAG = "AuthRepository"
        }
    }
