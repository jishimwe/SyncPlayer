package com.jpishimwe.syncplayer.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.jpishimwe.syncplayer.data.sync.AuthRepository
import com.jpishimwe.syncplayer.data.sync.AuthRepositoryImpl
import com.jpishimwe.syncplayer.data.sync.SyncRepository
import com.jpishimwe.syncplayer.data.sync.SyncRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    companion object {
        /**
         * Application-level CoroutineScope for long-lived coroutines (e.g., authState flow).
         * Uses SupervisorJob so child failures don't cancel the scope.
         */

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob())

        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore {
            val db = FirebaseFirestore.getInstance()
            db.firestoreSettings =
                firestoreSettings {
                    setLocalCacheSettings(
                        persistentCacheSettings {
                            setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        },
                    )
                }
            return db
        }
    }
}
