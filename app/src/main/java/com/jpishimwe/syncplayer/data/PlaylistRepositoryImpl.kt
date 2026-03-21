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
            playlistDao.getAllPlaylistsWithCount()

        override fun getPlaylistById(playlistId: Long): Flow<Playlist?> =
            playlistDao.getPlaylistByIdWithCount(playlistId)

        override fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> = playlistDao.getSongsForPlaylist(playlistId)

        override fun getArtUrisForPlaylist(playlistId: Long): Flow<List<String>> = playlistDao.getArtUrisForPlaylist(playlistId)

        override suspend fun createPlaylist(name: String): Long {
            val now = System.currentTimeMillis()
            return playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = name,
                    createdAt = now,
                    lastModified = now,
                ),
            )
        }

        override suspend fun renamePlaylist(
            playlistId: Long,
            newName: String,
        ) {
            val now = System.currentTimeMillis()
            val entity = playlistDao.getPlaylistById(playlistId).first()
            playlistDao.updatePlaylist(entity?.copy(name = newName, lastModified = now) ?: return)
        }

        override suspend fun deletePlaylist(playlistId: Long) {
            playlistDao.softDeletePlaylist(playlistId, System.currentTimeMillis())
        }

        override suspend fun addSongToPlaylist(
            playlistId: Long,
            songId: Long,
        ) {
            if (playlistDao.isSongInPlaylist(playlistId, songId)) return
            val position = playlistDao.getSongCountForPlaylist(playlistId).first()
            playlistDao.addSongToPlaylist(
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = songId,
                    position = position,
                ),
            )
            playlistDao.touchPlaylist(playlistId, System.currentTimeMillis())
        }

        override suspend fun removeSongFromPlaylist(
            playlistId: Long,
            songId: Long,
        ) {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
            playlistDao.touchPlaylist(playlistId, System.currentTimeMillis())
        }

        override suspend fun reorderSongs(
            playlistId: Long,
            orderedSongsId: List<Long>,
        ) {
            playlistDao.reorderPlaylistSongs(
                playlistId,
                orderedSongsId.mapIndexed { index, songId ->
                    PlaylistSongCrossRef(
                        playlistId = playlistId,
                        songId = songId,
                        position = index,
                    )
                },
            )
            playlistDao.touchPlaylist(playlistId, System.currentTimeMillis())
        }
    }
