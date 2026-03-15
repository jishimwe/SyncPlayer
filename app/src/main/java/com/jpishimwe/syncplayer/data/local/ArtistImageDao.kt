package com.jpishimwe.syncplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jpishimwe.syncplayer.model.ArtistImage
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistImageDao {
    @Query("SELECT * FROM artist_images WHERE artistName = :name")
    suspend fun getByArtistName(name: String): ArtistImage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: ArtistImage)

    @Query("SELECT * FROM artist_images")
    fun getAll(): Flow<List<ArtistImage>>
}
