package com.jpishimwe.syncplayer.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
        private val externalScope: CoroutineScope,
    ) : AuthRepository {
        override val authState: StateFlow<AuthState> =
            callbackFlow {
                val listener =
                    FirebaseAuth.AuthStateListener { auth ->
                        val user = auth.currentUser
                        trySend(
                            if (user == null) {
                                AuthState.SignedOut
                            } else {
                                AuthState.SignedIn(
                                    userId = user.uid,
                                    displayName = user.displayName,
                                    email = user.email,
                                    photoUrl = user.photoUrl?.toString(),
                                )
                            },
                        )
                    }
                firebaseAuth.addAuthStateListener(listener)
                awaitClose { firebaseAuth.removeAuthStateListener(listener) }
            }.stateIn(externalScope, SharingStarted.Eagerly, currentStateFromAuth())

        override val currentUserId: String?
            get() = firebaseAuth.currentUser?.uid

        override suspend fun signInWithToken(idToken: String) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
        }

        override suspend fun signOut() {
            firebaseAuth.signOut()
        }

        private fun currentStateFromAuth(): AuthState {
            val user = firebaseAuth.currentUser ?: return AuthState.SignedOut
            return AuthState.SignedIn(user.uid, user.displayName, user.email, user.photoUrl?.toString())
        }
    }
