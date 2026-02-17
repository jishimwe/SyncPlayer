package com.jpishimwe.syncplayer.data

import com.jpishimwe.syncplayer.model.Playlist
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>

    fun getPlaylistById(playlistId: Long): Flow<Playlist?>

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>>

    suspend fun createPlaylist(name: String): Long

    suspend fun renamePlaylist(
        playlistId: Long,
        newName: String,
    )

    suspend fun deletePlaylist(playlistId: Long)

    suspend fun addSongToPlaylist(
        playlistId: Long,
        songId: Long,
    )

    suspend fun removeSongFromPlaylist(
        playlistId: Long,
        songId: Long,
    )

    suspend fun reorderSongs(
        playlistId: Long,
        orderedSongsId: List<Long>,
    )
}
