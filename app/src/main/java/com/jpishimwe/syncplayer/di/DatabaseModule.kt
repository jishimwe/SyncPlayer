package com.jpishimwe.syncplayer.di

import android.content.Context
import androidx.room.Room
import com.jpishimwe.syncplayer.data.local.ListeningHistoryDao
import com.jpishimwe.syncplayer.data.local.PlaylistDao
import com.jpishimwe.syncplayer.data.local.QueueDao
import com.jpishimwe.syncplayer.data.local.SongDao
import com.jpishimwe.syncplayer.data.local.SyncPlayerDatabase
import com.jpishimwe.syncplayer.data.local.SyncPlayerDatabase.Companion.MIGRATION_4_5
import com.jpishimwe.syncplayer.data.local.SyncPlayerDatabase.Companion.MIGRATION_5_6
import com.jpishimwe.syncplayer.data.local.ArtistImageDao
import com.jpishimwe.syncplayer.data.local.SyncPlayerDatabase.Companion.MIGRATION_6_7
import com.jpishimwe.syncplayer.data.local.SyncPlayerDatabase.Companion.MIGRATION_7_8
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SyncPlayerDatabase =
        Room
            .databaseBuilder(
                context,
                SyncPlayerDatabase::class.java,
                "syncplayer.db",
            ).addMigrations(MIGRATION_4_5)
            .addMigrations(MIGRATION_5_6)
            .addMigrations(MIGRATION_6_7)
            .addMigrations(MIGRATION_7_8)
            .build()

    @Provides
    fun provideSongDao(database: SyncPlayerDatabase): SongDao = database.songDao()

    @Provides
    fun provideQueueDao(database: SyncPlayerDatabase): QueueDao = database.queueDao()

    @Provides
    fun providePlaylistDao(database: SyncPlayerDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideListeningHistoryDao(database: SyncPlayerDatabase): ListeningHistoryDao = database.listeningHistoryDao()

    @Provides
    fun provideArtistImageDao(database: SyncPlayerDatabase): ArtistImageDao = database.artistImageDao()
}
