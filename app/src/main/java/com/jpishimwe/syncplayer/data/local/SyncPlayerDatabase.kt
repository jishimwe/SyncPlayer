package com.jpishimwe.syncplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jpishimwe.syncplayer.model.Song

@Database(entities = [Song::class, QueueEntity::class], version = 2, exportSchema = false)
abstract class SyncPlayerDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    abstract fun queueDao(): QueueDao
}
