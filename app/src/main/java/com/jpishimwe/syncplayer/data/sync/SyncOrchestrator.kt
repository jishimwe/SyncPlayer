package com.jpishimwe.syncplayer.data.sync

import android.content.Context
import android.content.SharedPreferences
import com.jpishimwe.syncplayer.data.local.ListeningHistoryDao
import com.jpishimwe.syncplayer.data.local.ListeningHistoryEntity
import com.jpishimwe.syncplayer.data.local.PlaylistDao
import com.jpishimwe.syncplayer.data.local.PlaylistEntity
import com.jpishimwe.syncplayer.data.local.PlaylistSongCrossRef
import com.jpishimwe.syncplayer.data.local.SongDao
import com.jpishimwe.syncplayer.model.Song
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncStatus {
    data object Idle : SyncStatus

    data object Syncing : SyncStatus

    data class Success(
        val syncedAt: Long,
    ) : SyncStatus

    data class Error(
        val message: String,
    ) : SyncStatus
}

@Singleton
class SyncOrchestrator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val authRepository: AuthRepository,
        private val syncRepository: SyncRepository,
        private val songDao: SongDao,
        private val playlistDao: PlaylistDao,
        private val listeningHistoryDao: ListeningHistoryDao,
    ) {
        private val prefs: SharedPreferences = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

        private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
        val syncStatus = _syncStatus.asStateFlow()

        val lastSyncTime: Long get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)

        /** Runs a full sync only if the user is signed in. Called from MainActivity.onResume(). */
        suspend fun syncIfSignedIn() {
            if (authRepository.currentUserId != null) sync()
        }

        /** Full bi-directional sync: push local changes, then pull remote changes. */
        suspend fun sync() {
            val userId = authRepository.currentUserId
            if (userId == null) {
                Log.d(TAG, "sync() aborted — userId is null (not signed in)")
                return
            }
            Log.d(TAG, "sync() starting for userId=$userId, lastSyncTime=$lastSyncTime")
            _syncStatus.value = SyncStatus.Syncing

            try {
                push(userId)
                pull(userId)
                val now = System.currentTimeMillis()
                prefs.edit().putLong(KEY_LAST_SYNC_TIME, now).apply()
                Log.d(TAG, "sync() completed successfully at $now")
                _syncStatus.value = SyncStatus.Success(syncedAt = now)
            } catch (e: Exception) {
                Log.e(TAG, "sync() failed", e)
                _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
            }
        }

        // ── Push ──────────────────────────────────────────────────────────────
        private suspend fun push(userId: String) {
            val allSongs = songDao.getAllSongsList()
            val lastSync = lastSyncTime
            Log.d(TAG, "push(): ${allSongs.size} songs total, lastSync=$lastSync")

            // Build fingerprint lookup (songId → fingerprint) for history push
            val fingerprintMap: Map<Long, String> =
                allSongs.associate { song ->
                    song.id to SongFingerprint.compute(song.title, song.artist, song.album, song.duration)
                }

            // Push song metadata modified since last sync
            // On first sync (lastSync == 0), also push songs with lastModified == 0
            // (pre-migration songs that were never touched)
            val songsToSync =
                allSongs.filter { it.lastModified > lastSync || (lastSync == 0L && it.lastModified == 0L) }
            Log.d(TAG, "push(): ${songsToSync.size} songs to push")
            for (song in songsToSync) {
                val fingerprint = fingerprintMap[song.id] ?: continue
                syncRepository.pushSongMetadata(userId, fingerprint, song)
            }

            // Push playlists modified since last sync
            val allPlaylists = playlistDao.getAllPlaylistsList()
            val playlistsToSync =
                allPlaylists.filter { it.lastModified > lastSync || (lastSync == 0L && it.lastModified == 0L) }
            Log.d(TAG, "push(): ${playlistsToSync.size} playlists to push")
            for (playlist in playlistsToSync) {
                val songs = playlistDao.getSongsForPlaylistList(playlist.id)
                val remoteId = syncRepository.pushPlaylist(userId, playlist, songs)
                if (playlist.remoteId == null) {
                    // Store the Firestore-assigned remoteId locally so future pushes update, not create
                    playlistDao.updatePlaylist(playlist.copy(remoteId = remoteId))
                }
            }
            // Push listening history events since last sync
            val newHistory = listeningHistoryDao.getHistorySince(lastSync)
            Log.d(TAG, "push(): ${newHistory.size} history events to push")
            if (newHistory.isNotEmpty()) {
                syncRepository.pushHistoryEvents(userId, newHistory, fingerprintMap)
            }
        }

        // ── Pull ──────────────────────────────────────────────────────────────
        private suspend fun pull(userId: String) {
            Log.d(TAG, "pull(): starting for userId=$userId")
            val allSongs = songDao.getAllSongsList()
            val fingerprintToSong: Map<String, Song> =
                allSongs.associateBy { song -> SongFingerprint.compute(song.title, song.artist, song.album, song.duration) }

            // Pull song metadata and apply conflict resolution
            val remoteSongs = syncRepository.pullAllSongMetadata(userId)
            for ((fingerprint, remote) in remoteSongs) {
                val local = fingerprintToSong[fingerprint] ?: continue // not on this device
                val delta = ConflictResolver.resolveSongMetadata(local, remote) ?: continue
                songDao.applySyncDelta(local.id, delta.playCount, delta.rating, delta.lastPlayed, delta.lastModified)
            }

            // Pull playlists
            val remotePlaylists = syncRepository.pullAllPlaylists(userId)
            val localPlaylists = playlistDao.getAllPlaylistsList()
            val remoteIdToLocal = localPlaylists.associateBy { it.remoteId }

            for ((remoteId, remote) in remotePlaylists) {
                val local = remoteIdToLocal[remoteId]
                if (local == null && (remote.deletedAt == null || remote.deletedAt == 0L)) {
                    // New playlist from remote — create locally
                    val newId =
                        playlistDao.insertPlaylist(
                            PlaylistEntity(
                                name = remote.name,
                                createdAt = remote.createdAt,
                                remoteId = remoteId,
                                lastModified = remote.lastModified,
                            ),
                        )

                    val crossRefs =
                        remote.songs.mapNotNull { s ->
                            fingerprintToSong[s.fingerprint]?.let {
                                PlaylistSongCrossRef(
                                    playlistId = newId,
                                    songId = it.id,
                                    position = s.position,
                                )
                            }
                        }

                    playlistDao.replacePlaylistSongs(crossRefs)
                } else if (local != null && ConflictResolver.remotePlaylistWins(local.lastModified, remote.lastModified)) {
                    // Remote wins — overwrite local name and song list
                    val deletedAt = remote.deletedAt ?: 0
                    playlistDao.updatePlaylist(
                        local.copy(name = remote.name, lastModified = remote.lastModified, deletedAt = deletedAt),
                    )

                    val crossRefs =
                        remote.songs.mapNotNull { s ->
                            fingerprintToSong[s.fingerprint]?.let {
                                PlaylistSongCrossRef(
                                    playlistId = local.id,
                                    songId = it.id,
                                    position = s.position,
                                )
                            }
                        }
                    if (deletedAt == 0L) {
                        playlistDao.clearPlaylistSongs(local.id)
                        playlistDao.replacePlaylistSongs(crossRefs)
                    }
                }
                // else: local wins → no change needed
            }

            // Pull and merge listening history events
            val remoteHistory = syncRepository.pullHistoryEvents(userId, lastSyncTime)
            for (event in remoteHistory) {
                val song = fingerprintToSong[event.fingerprint] ?: continue
                listeningHistoryDao.insertListeningHistory(
                    ListeningHistoryEntity(songId = song.id, playedAt = event.playedAt),
                )
            }
        }

        companion object {
            private const val TAG = "SyncOrchestrator"
            private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        }
    }
