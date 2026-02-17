package com.jpishimwe.syncplayer.di

import android.content.Context
import androidx.room.Room
import com.jpishimwe.syncplayer.data.local.PlaylistDao
import com.jpishimwe.syncplayer.data.local.QueueDao
import com.jpishimwe.syncplayer.data.local.SongDao
import com.jpishimwe.syncplayer.data.local.SyncPlayerDatabase
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
            ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideSongDao(database: SyncPlayerDatabase): SongDao = database.songDao()

    @Provides
    fun provideQueueDao(database: SyncPlayerDatabase): QueueDao = database.queueDao()

    @Provides
    fun providePlaylistDao(database: SyncPlayerDatabase): PlaylistDao = database.playlistDao()
}
