package com.jpishimwe.syncplayer.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey val id: String,
    val songId: Long,
    var position: Int,
)

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY position ASC")
    suspend fun getQueue(): List<QueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToQueue(queueEntity: QueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(queueEntities: List<QueueEntity>)

    @Query("DELETE FROM queue WHERE id = :id")
    suspend fun deleteFromQueue(id: String)

    @Query("DELETE FROM queue")
    suspend fun clearQueue()
}
