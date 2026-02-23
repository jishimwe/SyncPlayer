package com.jpishimwe.syncplayer.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "listening_history")
data class ListeningHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
)

@Dao
interface ListeningHistoryDao {
    @Insert
    suspend fun insertListeningHistory(history: ListeningHistoryEntity)

    @Query(
        """
        SELECT songs.* FROM listening_history
        INNER JOIN songs ON songs.id = listening_history.songId
        GROUP BY songs.id
        ORDER BY MAX(listening_history.playedAt) DESC
        LIMIT :limit
    """,
    )
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<Song>>

    @Query("DELETE FROM listening_history")
    suspend fun clearAll()
}
