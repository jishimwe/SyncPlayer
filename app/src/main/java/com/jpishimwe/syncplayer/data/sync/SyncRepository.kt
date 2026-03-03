package com.jpishimwe.syncplayer.data.sync

import com.jpishimwe.syncplayer.data.local.ListeningHistoryEntity
import com.jpishimwe.syncplayer.data.local.PlaylistEntity
import com.jpishimwe.syncplayer.model.Song

interface SyncRepository {
    /** Push one song's metadata. Upserts the Firestore document. */
    suspend fun pushSongMetadata(
        userId: String,
        fingerprint: String,
        song: Song,
    )

    /** Pull all song metadata documents for the user. Returns fingerprint → metadata map. */
    suspend fun pullAllSongMetadata(userId: String): Map<String, FirestoreSongMetadata>

    /**
     * Push a playlist. Creates the Firestore document if [remoteId] is null (returns new remoteId).
     * Updates the document if [remoteId] is non-null (returns the same remoteId).
     */
    suspend fun pushPlaylist(
        userId: String,
        playlist: PlaylistEntity,
        songs: List<Song>,
    ): String

    /** Pull all playlist documents. Returns remoteId → playlist map. */
    suspend fun pullAllPlaylists(userId: String): Map<String, FirestorePlaylist>

    /** Push listening history events. Each event is a new document (append-only). */
    suspend fun pushHistoryEvents(
        userId: String,
        events: List<ListeningHistoryEntity>,
        fingerprintMap: Map<Long, String>,
    )

    /** Pull listening history events created after [since] (exclusive). */
    suspend fun pullHistoryEvents(
        userId: String,
        since: Long,
    ): List<FirestoreHistoryEvent>
}
