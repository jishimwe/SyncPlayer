package com.jpishimwe.syncplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsList(): List<Song>

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
        SELECT artist AS name, COUNT(*) AS songCount, COUNT(DISTINCT albumId) AS albumCount,
               (SELECT albumArtUri FROM songs s2 WHERE s2.artist = songs.artist AND s2.albumArtUri IS NOT NULL ORDER BY s2.dateAdded DESC LIMIT 1) AS artUri
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

    /** Inserts only songs that don't already exist — preserves rating/playCount on existing rows. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(songs: List<Song>)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE songs SET
            title        = :title,
            artist       = :artist,
            album        = :album,
            albumId      = :albumId,
            duration     = :duration,
            trackNumber  = :trackNumber,
            year         = :year,
            dateAdded    = :dateAdded,
            contentUri   = :contentUri,
            albumArtUri  = :albumArtUri
        WHERE id = :id
        """,
    )
    suspend fun updateAudioMetadata(
        id: Long,
        title: String,
        artist: String,
        album: String,
        albumId: Long,
        duration: Long,
        trackNumber: Int,
        year: Int,
        dateAdded: Long,
        contentUri: String?,
        albumArtUri: String?,
    )

    /**
     * Upserts songs from a MediaStore scan:
     * - New songs are inserted with default rating = 0.
     * - Existing songs have only their audio metadata updated;
     *   rating, playCount, lastPlayed and lastModified are preserved.
     */
    @Transaction
    suspend fun upsertSongs(songs: List<Song>) {
        insertAllIgnore(songs)
        songs.forEach { song ->
            updateAudioMetadata(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                albumId = song.albumId,
                duration = song.duration,
                trackNumber = song.trackNumber,
                year = song.year,
                dateAdded = song.dateAdded,
                contentUri = song.contentUri,
                albumArtUri = song.albumArtUri,
            )
        }
    }

    @Query(
        """
        UPDATE songs SET 
            playCount = playCount + 1, 
            lastPlayed = :playedAt, 
            lastModified = :modifiedAt 
        WHERE id = :songId
        """,
    )
    suspend fun incrementPlayCount(
        songId: Long,
        playedAt: Long,
        modifiedAt: Long = playedAt,
    )

    @Query("UPDATE songs SET rating = :rating, lastModified = :modifiedAt WHERE id = :songId")
    suspend fun setRating(
        songId: Long,
        rating: Int,
        modifiedAt: Long,
    )

    fun getFavoriteSongs(): Flow<List<Song>> = getSongsByMinRating()

    @Query("SELECT * FROM songs WHERE rating >= :minRating ORDER BY rating DESC, title ASC")
    fun getSongsByMinRating(minRating: Int = 4): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedSongs(limit: Int = 50): Flow<List<Song>>

    @Query("SELECT rating FROM songs WHERE id = :songId")
    fun getRating(songId: Long): Flow<Int?>

    @Query(
        """
        UPDATE songs SET
            playCount = :playCount,
            rating = :rating,
            lastPlayed = :playedAt,
            lastModified = :modifiedAt
        WHERE id = :songId
    """,
    )
    suspend fun applySyncDelta(
        songId: Long,
        playCount: Int,
        rating: Int,
        playedAt: Long,
        modifiedAt: Long,
    )

    @Query(
        """
            SELECT * FROM songs 
            WHERE title LIKE '%' || :query || '%' OR 
                 artist LIKE '%' || :query || '%' OR
                  album LIKE '%' || :query || '%' 
            ORDER BY title ASC
            """,
    )
    fun searchSongs(query: String): Flow<List<Song>>

    @Query(
        """
        SELECT albumId AS id, album AS name, artist, COUNT(*) AS songCount, albumArtUri
        FROM songs
        WHERE album LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'
        GROUP BY albumId
        ORDER BY album ASC
    """,
    )
    fun searchAlbums(query: String): Flow<List<Album>>

    @Query(
        """
            SELECT artist AS name, COUNT(*) AS songCount, COUNT(DISTINCT albumId) AS albumCount,
                   (SELECT albumArtUri FROM songs s2 WHERE s2.artist = songs.artist AND s2.albumArtUri IS NOT NULL ORDER BY s2.dateAdded DESC LIMIT 1) AS artUri
            FROM songs
            WHERE artist LIKE '%' || :query || '%'
            GROUP BY artist
            ORDER BY artist ASC
        """,
    )
    fun searchArtists(query: String): Flow<List<Artist>>
}
