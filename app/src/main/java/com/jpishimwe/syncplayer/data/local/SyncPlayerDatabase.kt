package com.jpishimwe.syncplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jpishimwe.syncplayer.model.RatingConverter
import com.jpishimwe.syncplayer.model.Song

@Database(
    entities = [
        Song::class, QueueEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        ListeningHistoryEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(RatingConverter::class)
abstract class SyncPlayerDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    abstract fun queueDao(): QueueDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun listeningHistoryDao(): ListeningHistoryDao
}
