package com.jpishimwe.syncplayer.di

import android.content.ContentResolver
import android.content.Context
import com.jpishimwe.syncplayer.data.PlayerRepository
import com.jpishimwe.syncplayer.data.PlayerRepositoryImpl
import com.jpishimwe.syncplayer.data.SongRepository
import com.jpishimwe.syncplayer.data.SongRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindSongRepository(impl: SongRepositoryImpl): SongRepository

    @Binds
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository

    companion object {
        @Provides
        fun provideContentResolver(
            @ApplicationContext context: Context,
        ): ContentResolver = context.contentResolver
    }
}
