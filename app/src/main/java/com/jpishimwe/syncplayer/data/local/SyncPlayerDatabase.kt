package com.jpishimwe.syncplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jpishimwe.syncplayer.model.RatingConverter
import com.jpishimwe.syncplayer.model.Song

@Database(
    entities = [
        Song::class, QueueEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        ListeningHistoryEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(RatingConverter::class)
abstract class SyncPlayerDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    abstract fun queueDao(): QueueDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun listeningHistoryDao(): ListeningHistoryDao

    companion object {
        val MIGRATION_4_5 =
            object :
                Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE songs ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE playlists ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE playlists ADD COLUMN remoteId TEXT")
                }
            }

        val MIGRATION_5_6 =
            object :
                Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE playlists ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
                }
            }

        val MIGRATION_6_7 =
            object :
                Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE songs ADD COLUMN albumArtist TEXT NOT NULL DEFAULT ''")
                }
            }
    }
}
