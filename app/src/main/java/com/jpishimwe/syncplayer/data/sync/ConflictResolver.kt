package com.jpishimwe.syncplayer.data.sync

import com.jpishimwe.syncplayer.data.local.ListeningHistoryEntity
import com.jpishimwe.syncplayer.model.Song

/** Resolved values to apply to the local Song row after a pull. */
data class SongMetadataDelta(
    val playCount: Int,
    val rating: Int,
    val lastPlayed: Long,
    val lastModified: Long,
)

object ConflictResolver {
    /**
     * Merges local song metadata with a remote snapshot.
     * Returns null if the local record is already up-to-date (no write needed).
     *
     * Rules:
     *   - playCount: max-wins (can only grow)
     *   - rating: last-write-wins by lastModified
     *   - lastPlayed: max-wins (most recent play is ground truth)
     *   - lastModified: max of both (tracks the winning write time)
     */
    fun resolveSongMetadata(
        local: Song,
        remote: FirestoreSongMetadata,
    ): SongMetadataDelta? {
        val resolvedPlayCount = maxOf(local.playCount, remote.playCount)
        val resolvedRating =
            if (local.lastModified < remote.lastModified) {
                remote.rating
            } else {
                local.rating
            }
        val resolvedLastPlayed = maxOf(local.lastPlayed, remote.lastPlayed)
        val resolvedLastModified = maxOf(local.lastModified, remote.lastModified)

        val unchanged =
            resolvedPlayCount == local.playCount &&
                resolvedRating == local.rating &&
                resolvedLastPlayed == local.lastPlayed
        return if (unchanged) null else SongMetadataDelta(resolvedPlayCount, resolvedRating, resolvedLastPlayed, resolvedLastModified)
    }

    /**
     * Determines whether the remote playlist should overwrite the local one.
     * Last-write-wins by lastModified. Remote wins on tie (trusts server clock).
     */
    fun remotePlaylistWins(
        local: Long,
        remote: Long,
    ): Boolean = remote >= local

    /**
     * Merges local listening history with remote history events.
     *
     * This function combines both sets of history, maps remote fingerprints to local song IDs,
     * and removes duplicates based on the combination of song ID and timestamp.
     *
     * @param local The list of listening history records currently stored locally.
     * @param remote The list of history events retrieved from the remote source.
     * @param fingerPrintToSongId A mapping used to resolve remote song fingerprints to local database IDs.
     * @return A consolidated list of unique [ListeningHistoryEntity] records.
     */
    fun mergeHistoryEvent(
        local: List<ListeningHistoryEntity>,
        remote: List<FirestoreHistoryEvent>,
        fingerPrintToSongId: Map<String, Long>,
    ): List<ListeningHistoryEntity> {
        val remoteHistoryEntity =
            remote.mapNotNull {
                ListeningHistoryEntity(
                    songId = fingerPrintToSongId[it.fingerprint] ?: return@mapNotNull null,
                    playedAt = it.playedAt,
                )
            }

        return (local + remoteHistoryEntity).distinctBy { Pair(it.songId, it.playedAt) }
    }
}
