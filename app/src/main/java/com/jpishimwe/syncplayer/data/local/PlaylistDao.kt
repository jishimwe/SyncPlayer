package com.jpishimwe.syncplayer.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.jpishimwe.syncplayer.model.Playlist
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val remoteId: String? = null,
    val lastModified: Long = 0,
    val deletedAt: Long = 0,
)

@Entity(tableName = "playlist_songs")
data class PlaylistSongCrossRef(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val songId: Long,
    val position: Int,
)

@Dao
interface PlaylistDao {
    // Playlist CRUD

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlists WHERE deletedAt = 0 ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    // Song membership
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(
        playlistId: Long,
        songId: Long,
    )

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replacePlaylistSongs(songs: List<PlaylistSongCrossRef>)

    @Query(
        """
        SELECT songs.* FROM songs
        INNER JOIN playlist_songs ON songs.id = playlist_songs.songId
        WHERE playlist_songs.playlistId = :playlistId
        ORDER BY playlist_songs.position ASC
    """,
    )
    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    fun getSongCountForPlaylist(playlistId: Long): Flow<Int>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistById(playlistId: Long): Flow<PlaylistEntity?>

    @Query("UPDATE playlists SET lastModified = :modifiedAT WHERE id = :playlistId")
    suspend fun touchPlaylist(
        playlistId: Long,
        modifiedAT: Long,
    )

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylistsList(): List<PlaylistEntity>

    @Query(
        """
        SELECT songs.* FROM songs
        INNER JOIN playlist_songs ON songs.id = playlist_songs.songId
        WHERE playlist_songs.playlistId = :playlistId
        ORDER BY playlist_songs.position ASC
    """,
    )
    suspend fun getSongsForPlaylistList(playlistId: Long): List<Song>

    @Query("UPDATE playlists SET deletedAt = :deletedAt, lastModified = :deletedAt WHERE id = :playlistId")
    suspend fun softDeletePlaylist(
        playlistId: Long,
        deletedAt: Long,
    )

    @Query(
        """
        SELECT p.id, p.name, p.createdAt, 
               COUNT(ps.songId) AS songCount,
               COALESCE(SUM(s.duration), 0) AS totalDuration
        FROM playlists p
        LEFT JOIN playlist_songs ps ON p.id = ps.playlistId
        LEFT JOIN songs s ON ps.songId = s.id
        WHERE p.deletedAt = 0
        GROUP BY p.id
        ORDER BY p.name ASC
    """,
    )
    fun getAllPlaylistsWithCount(): Flow<List<Playlist>>
}
