package com.jpishimwe.syncplayer.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jpishimwe.syncplayer.data.local.ListeningHistoryEntity
import com.jpishimwe.syncplayer.data.local.PlaylistEntity
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl
    @Inject
    constructor(
        private val db: FirebaseFirestore,
    ) : SyncRepository {
        companion object {
            private const val TAG = "SyncRepository"
        }

        override suspend fun pushSongMetadata(
            userId: String,
            fingerprint: String,
            song: Song,
        ) {
            val data =
                mapOf(
                    "playCount" to song.playCount,
                    "rating" to song.rating,
                    "lastPlayed" to song.lastPlayed,
                    "lastModified" to song.lastModified,
                )
            val path = "users/$userId/songs/$fingerprint"
            Log.d(TAG, "pushSongMetadata: $path data=$data")
            db
                .collection("users")
                .document(userId)
                .collection("songs")
                .document(fingerprint)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "pushSongMetadata: $path written successfully")
        }

        override suspend fun pullAllSongMetadata(userId: String): Map<String, FirestoreSongMetadata> {
            val snapshot =
                db
                    .collection("users")
                    .document(userId)
                    .collection("songs")
                    .get()
                    .await()
            Log.d(TAG, "pullAllSongMetadata: fetched ${snapshot.documents.size} documents")
            return snapshot.documents.associate { doc ->
                doc.id to doc.toObject(FirestoreSongMetadata::class.java)!!
            }
        }

        override suspend fun pushPlaylist(
            userId: String,
            playlist: PlaylistEntity,
            songs: List<Song>,
        ): String {
            val firestoreSongs =
                songs.map { song ->
                    FirestorePlaylistSong(
                        fingerprint = SongFingerprint.compute(song.title, song.artist, song.album, song.duration),
                        position = songs.indexOf(song),
                    )
                }

            val data =
                FirestorePlaylist(
                    name = playlist.name,
                    createdAt = playlist.createdAt,
                    lastModified = playlist.lastModified,
                    songs = firestoreSongs,
                    deletedAt = playlist.deletedAt,
                )

            val remoteId =
                playlist.remoteId ?: db
                    .collection("users")
                    .document(userId)
                    .collection("playlists")
                    .document()
                    .id

            val path = "users/$userId/playlists/$remoteId"
            Log.d(TAG, "pushPlaylist: $path name=${playlist.name}")
            db
                .collection("users")
                .document(userId)
                .collection("playlists")
                .document(remoteId)
                .set(data)
                .await()
            Log.d(TAG, "pushPlaylist: $path written successfully")

            return remoteId
        }

        override suspend fun pullAllPlaylists(userId: String): Map<String, FirestorePlaylist> {
            val snapshot =
                db
                    .collection("users")
                    .document(userId)
                    .collection("playlists")
                    .get()
                    .await()

            Log.d(TAG, "pullAllPlaylists: fetched ${snapshot.documents.size} documents")
            return snapshot.documents.associate { doc ->
                doc.id to doc.toObject(FirestorePlaylist::class.java)!!
            }
        }

        override suspend fun pushHistoryEvents(
            userId: String,
            events: List<ListeningHistoryEntity>,
            fingerprintMap: Map<Long, String>,
        ) {
            val batch = db.batch()
            for (event in events) {
                val fingerprint = fingerprintMap[event.songId] ?: continue
                val ref =
                    db
                        .collection("users")
                        .document(userId)
                        .collection("history")
                        .document()
                batch.set(ref, FirestoreHistoryEvent(fingerprint, event.playedAt))
            }
            Log.d(TAG, "pushHistoryEvents: committing batch of ${events.size} events")
            batch.commit().await()
            Log.d(TAG, "pushHistoryEvents: batch committed successfully")
        }

        override suspend fun pullHistoryEvents(
            userId: String,
            since: Long,
        ): List<FirestoreHistoryEvent> {
            val snapshot =
                db
                    .collection("users")
                    .document(userId)
                    .collection("history")
                    .whereGreaterThanOrEqualTo("playedAt", since)
                    .get()
                    .await()
            Log.d(TAG, "pullHistoryEvents: fetched ${snapshot.documents.size} events since $since")
            return snapshot.documents.mapNotNull { it.toObject(FirestoreHistoryEvent::class.java) }
        }
    }
