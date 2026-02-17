package com.jpishimwe.syncplayer.data

import com.jpishimwe.syncplayer.model.Playlist
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePlaylistRepository : PlaylistRepository {
    val playlistsFlow = MutableStateFlow<List<Playlist>>(emptyList())
    val songsForPlaylistFlow = MutableStateFlow<List<Song>>(emptyList())

    var createCallCount = 0
    var lastCreatedName: String? = null
    var renameCallCount = 0
    var lastRenamedId: Long? = null
    var lastRenamedName: String? = null
    var deleteCallCount = 0
    var lastDeletedId: Long? = null
    var addSongCallCount = 0
    var removeSongCallCount = 0
    var reorderCallCount = 0

    private var nextId = 1L

    override fun getAllPlaylists(): Flow<List<Playlist>> = playlistsFlow

    override fun getPlaylistById(playlistId: Long): Flow<Playlist?> = playlistsFlow.map { list -> list.find { it.id == playlistId } }

    override fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> = songsForPlaylistFlow

    override suspend fun createPlaylist(name: String): Long {
        createCallCount++
        lastCreatedName = name
        val id = nextId++
        val playlist = Playlist(id = id, name = name, createdAt = System.currentTimeMillis())
        playlistsFlow.value += playlist
        return id
    }

    override suspend fun renamePlaylist(
        playlistId: Long,
        newName: String,
    ) {
        renameCallCount++
        lastRenamedId = playlistId
        lastRenamedName = newName
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        deleteCallCount++
        lastDeletedId = playlistId
    }

    override suspend fun addSongToPlaylist(
        playlistId: Long,
        songId: Long,
    ) {
        addSongCallCount++
    }

    override suspend fun removeSongFromPlaylist(
        playlistId: Long,
        songId: Long,
    ) {
        removeSongCallCount++
    }

    override suspend fun reorderSongs(
        playlistId: Long,
        orderedSongsId: List<Long>,
    ) {
        reorderCallCount++
    }
}
