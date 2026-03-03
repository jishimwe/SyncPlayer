package com.jpishimwe.syncplayer.data.sync

/**
 * Firestore document: users/{userId}/songs/{fingerprint}
 *
 * Firestore requires a no-arg constructor → all fields have default values.
 */
data class FirestoreSongMetadata(
    val playCount: Int = 0,
    val rating: Int = 0,
    val lastPlayed: Long = 0,
    val lastModified: Long = 0,
)

/**
 * Nested inside FirestorePlaylist.songs array.
 * Identifies a song in a playlist by fingerprint + position.
 */
data class FirestorePlaylistSong(
    val fingerprint: String = "",
    val position: Int = 0,
)

/**
 * Firestore document: users/{userId}/playlists/{remoteId}
 */
data class FirestorePlaylist(
    val name: String = "",
    val createdAt: Long = 0,
    val lastModified: Long = 0,
    val deletedAt: Long? = null,
    val songs: List<FirestorePlaylistSong> = emptyList(),
)

/**
 * Firestore document: users/{userId}/history/{autoId}
 */
data class FirestoreHistoryEvent(
    val fingerprint: String = "",
    val playedAt: Long = 0,
)
