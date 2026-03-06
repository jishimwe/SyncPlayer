package com.jpishimwe.syncplayer.data.sync

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
            db
                .collection("users")
                .document(userId)
                .collection("songs")
                .document(fingerprint)
                .set(data, SetOptions.merge())
                .await()
        }

        override suspend fun pullAllSongMetadata(userId: String): Map<String, FirestoreSongMetadata> {
            val snapshot =
                db
                    .collection("users")
                    .document(userId)
                    .collection("songs")
                    .get()
                    .await()
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

            db
                .collection("users")
                .document(userId)
                .collection("playlists")
                .document(remoteId)
                .set(data)
                .await()

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
            batch.commit().await()
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
            return snapshot.documents.mapNotNull { it.toObject(FirestoreHistoryEvent::class.java) }
        }
    }
