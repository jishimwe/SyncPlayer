package com.jpishimwe.syncplayer.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
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

    @Query("SELECT * FROM listening_history WHERE playedAt > :since ORDER BY playedAt DESC")
    suspend fun getHistorySince(since: Long): List<ListeningHistoryEntity>

    /**
     * Returns the most recently played distinct albums, ordered by last play time.
     * albumArtUri is resolved via a correlated subquery so we always pick a non-null
     * URI from the group rather than relying on SQLite's undefined GROUP BY column
     * selection for non-aggregated fields.
     */
    @Query(
        """
        SELECT s.albumId AS id, s.album AS name, s.artist,
               COUNT(DISTINCT s.id) AS songCount,
               (SELECT s2.albumArtUri FROM songs s2
                WHERE s2.albumId = s.albumId
                AND s2.albumArtUri IS NOT NULL
                LIMIT 1) AS albumArtUri
        FROM listening_history h
        INNER JOIN songs s ON s.id = h.songId
        GROUP BY s.albumId
        ORDER BY MAX(h.playedAt) DESC
        LIMIT :limit
    """,
    )
    fun getRecentlyPlayedAlbums(limit: Int = 20): Flow<List<Album>>

    /**
     * Returns the most recently played distinct artists, ordered by last play time.
     * artUri mirrors SongDao.getAllArtists(): prefers Deezer image from artist_images,
     * falls back to the most recently added album art for the artist.
     */
    @Query(
        """
        SELECT s.artist AS name,
               COUNT(DISTINCT s.id) AS songCount,
               COUNT(DISTINCT s.albumId) AS albumCount,
               COALESCE(ai.imageUrl,
                   (SELECT s2.albumArtUri FROM songs s2
                    WHERE s2.artist = s.artist
                    AND s2.albumArtUri IS NOT NULL
                    ORDER BY s2.dateAdded DESC LIMIT 1)
               ) AS artUri
        FROM listening_history h
        INNER JOIN songs s ON s.id = h.songId
        LEFT JOIN artist_images ai ON ai.artistName = s.artist
        GROUP BY s.artist
        ORDER BY MAX(h.playedAt) DESC
        LIMIT :limit
        """,
    )
    fun getRecentlyPlayedArtists(limit: Int = 20): Flow<List<Artist>>
}
