package com.jpishimwe.syncplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE id = :id")
    fun getSongById(id: Long): Flow<Song?>

    @Query("SELECT * FROM songs WHERE id in (:idList)")
    fun getSongsByIds(idList: List<Long>): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query(
        """
        SELECT albumId AS id, album AS name, artist, COUNT(*) AS songCount, albumArtUri
        FROM songs
        GROUP BY albumId
        ORDER BY album ASC
        """,
    )
    fun getAllAlbums(): Flow<List<Album>>

    @Query(
        """
        SELECT artist AS name, COUNT(*) AS songCount, COUNT(DISTINCT albumId) AS albumCount
        FROM songs
        GROUP BY artist
        ORDER BY artist ASC
        """,
    )
    fun getAllArtists(): Flow<List<Artist>>

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY trackNumber ASC")
    fun getSongsByAlbum(albumId: Long): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album ASC, trackNumber ASC")
    fun getSongsByArtist(artist: String): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()
}
