package com.jpishimwe.syncplayer.data

import com.jpishimwe.syncplayer.data.local.PlaylistDao
import com.jpishimwe.syncplayer.data.local.PlaylistEntity
import com.jpishimwe.syncplayer.data.local.PlaylistSongCrossRef
import com.jpishimwe.syncplayer.model.Playlist
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlaylistRepositoryImpl
    @Inject
    constructor(
        private val playlistDao: PlaylistDao,
    ) : PlaylistRepository {
        override fun getAllPlaylists(): Flow<List<Playlist>> =
            playlistDao.getAllPlaylists().map { entities ->
                entities.map { Playlist(it.id, it.name, it.createdAt) }
            }

        override fun getPlaylistById(playlistId: Long): Flow<Playlist?> =
            playlistDao.getPlaylistById(playlistId).map { entity ->
                entity?.let { Playlist(it.id, it.name, it.createdAt, playlistDao.getSongCountForPlaylist(it.id).first()) }
            }

        override fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> = playlistDao.getSongsForPlaylist(playlistId)

        override suspend fun createPlaylist(name: String) =
            playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = name,
                    createdAt = System.currentTimeMillis(),
                ),
            )

        override suspend fun renamePlaylist(
            playlistId: Long,
            newName: String,
        ) {
            val entity = playlistDao.getPlaylistById(playlistId).first()
            playlistDao.updatePlaylist(entity?.copy(name = newName) ?: return)
        }

        override suspend fun deletePlaylist(playlistId: Long) {
            playlistDao.clearPlaylistSongs(playlistId)
            playlistDao.deletePlaylist(playlistId)
        }

        override suspend fun addSongToPlaylist(
            playlistId: Long,
            songId: Long,
        ) {
            val position = playlistDao.getSongCountForPlaylist(playlistId).first()
            playlistDao.addSongToPlaylist(
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = songId,
                    position = position,
                ),
            )
        }

        override suspend fun removeSongFromPlaylist(
            playlistId: Long,
            songId: Long,
        ) = playlistDao.removeSongFromPlaylist(playlistId, songId)

        override suspend fun reorderSongs(
            playlistId: Long,
            orderedSongsId: List<Long>,
        ) {
            playlistDao.clearPlaylistSongs(playlistId)
            playlistDao.replacePlaylistSongs(
                orderedSongsId.mapIndexed { index, songId ->
                    PlaylistSongCrossRef(
                        playlistId = playlistId,
                        songId = songId,
                        position = index,
                    )
                },
            )
        }
    }
