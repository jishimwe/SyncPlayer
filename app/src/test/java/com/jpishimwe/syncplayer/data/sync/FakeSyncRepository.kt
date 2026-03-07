package com.jpishimwe.syncplayer.data.sync

import com.jpishimwe.syncplayer.data.local.ListeningHistoryEntity
import com.jpishimwe.syncplayer.data.local.PlaylistEntity
import com.jpishimwe.syncplayer.model.Song

class FakeSyncRepository : SyncRepository {
    // ── Captured calls ────────────────────────────────────────────────────────
    var pushSongCallCount = 0
    val pushedSongs = mutableListOf<Pair<String, Song>>() // fingerprint → song

    var pushPlaylistCallCount = 0
    val pushedPlaylists = mutableListOf<PlaylistEntity>()
    var pushPlaylistRemoteId = "remote-id-1"

    var pushHistoryCallCount = 0
    val pushedHistoryEvents = mutableListOf<ListeningHistoryEntity>()

    // ── Stubbed return values ─────────────────────────────────────────────────
    var remoteSongs: Map<String, FirestoreSongMetadata> = emptyMap()
    var remotePlaylists: Map<String, FirestorePlaylist> = emptyMap()
    var remoteHistory: List<FirestoreHistoryEvent> = emptyList()

    var throwOnPush: Exception? = null
    var throwOnPull: Exception? = null

    // ── SyncRepository impl ───────────────────────────────────────────────────

    override suspend fun pushSongMetadata(userId: String, fingerprint: String, song: Song) {
        throwOnPush?.let { throw it }
        pushSongCallCount++
        pushedSongs.add(fingerprint to song)
    }

    override suspend fun pullAllSongMetadata(userId: String): Map<String, FirestoreSongMetadata> {
        throwOnPull?.let { throw it }
        return remoteSongs
    }

    override suspend fun pushPlaylist(userId: String, playlist: PlaylistEntity, songs: List<Song>): String {
        throwOnPush?.let { throw it }
        pushPlaylistCallCount++
        pushedPlaylists.add(playlist)
        return pushPlaylistRemoteId
    }

    override suspend fun pullAllPlaylists(userId: String): Map<String, FirestorePlaylist> {
        throwOnPull?.let { throw it }
        return remotePlaylists
    }

    override suspend fun pushHistoryEvents(
        userId: String,
        events: List<ListeningHistoryEntity>,
        fingerprintMap: Map<Long, String>,
    ) {
        throwOnPush?.let { throw it }
        pushHistoryCallCount++
        pushedHistoryEvents.addAll(events)
    }

    override suspend fun pullHistoryEvents(userId: String, since: Long): List<FirestoreHistoryEvent> {
        throwOnPull?.let { throw it }
        return remoteHistory
    }
}
