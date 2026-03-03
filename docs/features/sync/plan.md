# Sync — Plan

## Context

Phases 1–5 built a fully functional local music player: library browsing, playback with Media3, playlist management, and metadata tracking (play counts, ratings, listening history). All data lives in a Room database on a single device. Phase 6 makes that metadata portable — when the user installs SyncPlayer on a second device, their play counts, ratings, playlists, and listening history follow them automatically.

The core constraint is that song files are **not** synced — only metadata. This means we cannot use file paths or MediaStore IDs as cross-device identifiers (they differ per device). We need a stable song fingerprint computed from metadata, and we need to handle cases where a song exists on one device but not the other.

---

## Scope

**Included:**
- Firebase Auth with Google Sign-In (via Android Credential Manager API)
- Firestore data model for song metadata (play counts, ratings, last played), playlists, and listening history
- `SyncOrchestrator`: push local changes to Firestore, pull remote changes, apply conflict resolution
- Conflict resolution: `playCount` = max-wins; `rating` = last-write-wins (by `lastModified` timestamp); `lastPlayed` = max-wins; playlists = last-write-wins per playlist; listening history = append-only union merge
- Offline support via Firestore's built-in offline persistence (enabled by default on Android)
- Song fingerprinting (`title + artist + album + duration` → SHA-256 hash) for cross-device song matching
- Room migration 4 → 5: add `lastModified` field to `Song` and `PlaylistEntity`; add `remoteId` to `PlaylistEntity`; switch from destructive to explicit migrations
- `lastModified` stamped on every Song metadata write (`setRating`, `incrementPlayCount`) and every playlist mutation (create, rename, add/remove/reorder songs)
- Settings screen: Google Sign-In / Sign-Out, sync status (idle / syncing / last-synced time / error), manual "Sync now" button
- Settings tab added to bottom navigation bar (Library | Playlists | Settings)
- Sync triggered on every app foreground (app comes back from background)

**Excluded:**
- Syncing audio files themselves
- Background / periodic sync (WorkManager) — deferred; foreground-only for Phase 6
- Song matching for songs with mismatched metadata across devices (e.g., different artist tag formatting) — fingerprint must match exactly, mismatches are silently skipped
- Multi-account support (single signed-in account)
- Collaborative / shared playlists
- MusicBee or YouTube Music integration — those are separate External Integrations
- Sync of the playback queue (transient, not useful across devices)
- Sync of `lastScan` / MediaStore metadata (title, artist, etc. — those come from the device's local files)

---

## Approach

### Song fingerprinting

MediaStore IDs are device-specific: the same file gets different IDs on different devices. We need a stable cross-device identifier. We use a SHA-256 hash of four normalized fields:

```
fingerprint = SHA-256(lowercase(title) | lowercase(artist) | lowercase(album) | durationBucket)
```

`durationBucket = (durationMs / 2000) * 2` — rounds duration to the nearest 2 seconds to absorb minor encoding differences between rips of the same song.

**Known limitations** (see Known Gaps):
- Songs with identical title + artist + album + duration share a fingerprint. Extremely rare; acceptable for a first-pass.
- Songs where metadata differs across devices (e.g., "The Beatles" vs. "Beatles, The") produce different fingerprints and will not sync. A fuzzy-matching pass is a future improvement.

### Conflict resolution

| Data | Strategy | Rationale |
|------|----------|-----------|
| `playCount` | max-wins | You can't un-play a song. The device with the higher count has heard it more. |
| `rating` | last-write-wins by `lastModified` | Rating is a deliberate user preference. The most recent change is most likely what the user intended. On tie, local wins — avoids an unnecessary write. |
| `lastPlayed` | max-wins | Most recent play is the ground truth. |
| Playlist name + song list | last-write-wins per playlist by `lastModified` | Playlist is treated as a unit. Competing edits on two devices resolve to the most recently modified. Song-level merging is too complex for Phase 6. |
| Listening history | append-only union | History is immutable. Any event on any device is valid. Union merge with local deduplication (same `songId` + `playedAt` timestamp). |

### Room migration 4 → 5

Adds three columns without touching existing data:

| Table | Column | Type | Default |
|-------|--------|------|---------|
| `songs` | `lastModified` | `INTEGER NOT NULL` | `0` |
| `playlists` | `lastModified` | `INTEGER NOT NULL` | `0` |
| `playlists` | `remoteId` | `TEXT` | `NULL` |

`lastModified` defaults to `0` (meaning "never modified by the app — treat as older than any remote write"). This is safe: on first sync after migration, all remote data will have `lastModified > 0` and will win any conflict comparison, which is the desired outcome.

Phase 6 also removes `fallbackToDestructiveMigration` from `DatabaseModule.kt` and replaces it with explicit `Migration(4, 5)`. This is necessary because once sync is live, losing the local database (which destructive fallback would cause on an unhandled schema jump) means losing unsynced metadata.

### Sync trigger

Sync runs:
1. **On app foreground**: `MainActivity.onResume()` calls `SyncOrchestrator.syncIfSignedIn()`. This is a fire-and-forget `lifecycleScope.launch`.
2. **Manual trigger**: "Sync now" button in Settings calls `SyncOrchestrator.sync()` and updates `SyncStatus`.

The last successful sync timestamp is stored in `SharedPreferences` under key `"last_sync_time"`. On push, only records with `lastModified > lastSyncTime` (for song metadata and playlists) or `playedAt > lastSyncTime` (for history) are uploaded. On pull, all remote documents are fetched and resolved against local state (Firestore queries are cheap at this scale).

### Offline support

Firestore offline persistence is **enabled by default** on Android. All reads and writes succeed offline; Firestore queues writes locally and flushes when connectivity resumes. No additional configuration is required beyond initializing the Firestore instance. We explicitly set `CACHE_SIZE_UNLIMITED` to avoid eviction for users with large libraries.

Use the current settings DSL — `setPersistenceEnabled(true)` is deprecated:

```kotlin
db.firestoreSettings = firestoreSettings {
    setLocalCacheSettings(persistentCacheSettings {
        setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
    })
}
```

### Settings screen

A third tab in the bottom nav (alongside Library and Playlists). Shows:
- When signed out: "Sign in with Google" button
- When signed in: account name + email, "Sign out" button, sync status card, "Sync now" button

The Google Sign-In flow uses **Android Credential Manager** (`androidx.credentials`) rather than the legacy `GoogleSignInClient`. Since `minSdk = 34`, the Credential Manager is available without any fallback path.

**Sign-in responsibility split:**
- `SettingsScreen` composable: owns `CredentialManager`, launches the credential request coroutine, extracts the Google ID token from the result, fires `SettingsEvent.SignInWithToken(idToken)` or `SettingsEvent.SignInError(message)`.
- `SettingsViewModel`: calls `AuthRepository.signInWithToken(idToken)` (pure Firebase, no Context needed).
- `AuthRepositoryImpl`: exchanges the Google ID token for a Firebase credential and calls `FirebaseAuth.signInWithCredential(...)`.

This split keeps Context-dependent platform code in the composable layer and keeps the ViewModel and Repository testable without Android dependencies.

---

## Tasks

### Layer 1: Dependencies and build config

Add to `gradle/libs.versions.toml`:

```toml
[versions]
firebase-bom          = "34.10.0"
credentials           = "1.5.0"
googleid              = "1.1.1"
coroutines-play       = "1.10.1"            # matches existing coroutines version

[libraries]
firebase-bom          = { group = "com.google.firebase",   name = "firebase-bom",                 version.ref = "firebase-bom" }
firebase-auth         = { group = "com.google.firebase",   name = "firebase-auth" }
firebase-firestore    = { group = "com.google.firebase",   name = "firebase-firestore" }
credentials           = { group = "androidx.credentials",  name = "credentials",                  version.ref = "credentials" }
credentials-play      = { group = "androidx.credentials",  name = "credentials-play-services-auth", version.ref = "credentials" }
googleid              = { group = "com.google.android.libraries.identity.googleid", name = "googleid", version.ref = "googleid" }
coroutines-play       = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines-play" }

[plugins]
google-services       = { id = "com.google.gms.google-services", version = "4.4.3" }
```

> Note: `firebase-auth` and `firebase-firestore` have **no** `version.ref` — they inherit version from the Firebase BOM at runtime via `platform(libs.firebase.bom)`.

Update `app/build.gradle.kts`:

```kotlin
plugins {
    // ... existing plugins ...
    alias(libs.plugins.google.services)
}

dependencies {
    // Firebase (versions managed by BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Google Sign-In via Credential Manager
    implementation(libs.credentials)
    implementation(libs.credentials.play)
    implementation(libs.googleid)

    // Firebase Task → coroutine bridge
    implementation(libs.coroutines.play)

    // ... existing dependencies ...
}
```

Update `build.gradle.kts` (project-level) — add `google-services` plugin to `plugins {}` block with `apply false`.

**User action required before building:**
1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Add an Android app with package `com.jpishimwe.syncplayer`
3. Enable **Google** as a sign-in provider in Authentication → Sign-in method
4. Add your debug SHA-1 fingerprint to the app — get it via `./gradlew signingReport`
5. Download `google-services.json` and place it in `app/`

> **Critical:** The `oauth_client` array in `google-services.json` must be non-empty for Google Sign-In to work. An empty array means no Web Client ID was generated. If it's empty, the SHA-1 step above was missed — add it in Firebase Console and re-download the file.

6. Copy the **Web client ID** from Authentication → Sign-in method → Google → Web SDK configuration → Web client ID
7. Add to `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="default_web_client_id" translatable="false">YOUR_WEB_CLIENT_ID</string>
   ```
8. Ensure `app/google-services.json` is in `.gitignore` (it's sensitive; store as a CI secret for GitHub Actions)

Run `assembleDebug` and fix any classpath conflicts before proceeding.

---

### Layer 2: Room migration 4 → 5 + DAO updates

#### 2a: Entity changes

**Modified:** `model/Song.kt`

Add one field:
```kotlin
val lastModified: Long = 0,
```

**Modified:** `data/local/PlaylistDao.kt`

Add two fields to `PlaylistEntity`:
```kotlin
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val remoteId: String? = null,
    val lastModified: Long = 0,
)
```

#### 2b: Room migration

**Modified:** `data/local/SyncPlayerDatabase.kt`

Bump version to 5. Add `Migration(4, 5)`. Replace `fallbackToDestructiveMigration` with the explicit migration in `DatabaseModule.kt`:

```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE songs ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE playlists ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE playlists ADD COLUMN remoteId TEXT")
    }
}
```

**Modified:** `di/DatabaseModule.kt`

Replace `.fallbackToDestructiveMigration(dropAllTables = true)` with `.addMigrations(MIGRATION_4_5)`:

```kotlin
Room.databaseBuilder(
    context,
    SyncPlayerDatabase::class.java,
    "syncplayer.db",
).addMigrations(MIGRATION_4_5)
 .build()
```

> Note: Without `fallbackToDestructiveMigration`, any future unhandled schema change will throw `IllegalStateException` at runtime instead of silently wiping data. Future schema changes must include explicit `Migration` objects.

#### 2c: SongDao — stamp `lastModified` + add one-shot query

**Modified:** `data/local/SongDao.kt`

Update the two metadata-writing queries to also set `lastModified`:

```kotlin
@Query("""
    UPDATE songs SET
        playCount = playCount + 1,
        lastPlayed = :playedAt,
        lastModified = :modifiedAt
    WHERE id = :songId
""")
suspend fun incrementPlayCount(songId: Long, playedAt: Long, modifiedAt: Long = playedAt)

@Query("UPDATE songs SET rating = :rating, lastModified = :modifiedAt WHERE id = :songId")
suspend fun setRating(songId: Long, rating: Int, modifiedAt: Long)
```

Add a one-shot (non-Flow) query for sync use:

```kotlin
@Query("SELECT * FROM songs")
suspend fun getAllSongsList(): List<Song>
```

Add a batch update query for applying a resolved sync delta from the pull step:

```kotlin
@Query("""
    UPDATE songs SET
        playCount = :playCount,
        rating = :rating,
        lastPlayed = :lastPlayed,
        lastModified = :lastModified
    WHERE id = :songId
""")
suspend fun applySyncDelta(
    songId: Long,
    playCount: Int,
    rating: Int,
    lastPlayed: Long,
    lastModified: Long,
)
```

#### 2d: SongRepositoryImpl — pass `lastModified`

**Modified:** `data/SongRepositoryImpl.kt`

Update `setRating` to pass the current timestamp:

```kotlin
override suspend fun setRating(songId: Long, rating: Rating) {
    val now = System.currentTimeMillis()
    songDao.setRating(songId, rating.value, modifiedAt = now)
}
```

`incrementPlayCount` already uses `System.currentTimeMillis()` for `playedAt`; the DAO now uses the same value for `modifiedAt` via the default parameter — no change needed in the repository.

#### 2e: PlaylistDao — stamp `lastModified` + add one-shot queries

**Modified:** `data/local/PlaylistDao.kt`

Add a helper query to touch a playlist's `lastModified` without changing anything else:

```kotlin
@Query("UPDATE playlists SET lastModified = :modifiedAt WHERE id = :playlistId")
suspend fun touchPlaylist(playlistId: Long, modifiedAt: Long)
```

Add one-shot (non-Flow) queries for sync use:

```kotlin
@Query("SELECT * FROM playlists")
suspend fun getAllPlaylistsList(): List<PlaylistEntity>

@Query("""
    SELECT songs.* FROM songs
    INNER JOIN playlist_songs ON songs.id = playlist_songs.songId
    WHERE playlist_songs.playlistId = :playlistId
    ORDER BY playlist_songs.position ASC
""")
suspend fun getSongsForPlaylistList(playlistId: Long): List<Song>
```

#### 2f: PlaylistRepositoryImpl — stamp `lastModified` on all mutations

**Modified:** `data/PlaylistRepositoryImpl.kt`

Update every mutation to stamp the playlist's `lastModified`:

```kotlin
override suspend fun createPlaylist(name: String): Long {
    val now = System.currentTimeMillis()
    return playlistDao.insertPlaylist(
        PlaylistEntity(name = name.trim(), createdAt = now, lastModified = now)
    )
}

override suspend fun renamePlaylist(playlistId: Long, newName: String) {
    val entity = playlistDao.getPlaylistById(playlistId).first() ?: return
    playlistDao.updatePlaylist(entity.copy(name = newName.trim(), lastModified = System.currentTimeMillis()))
}

override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
    val position = playlistDao.getSongCountForPlaylist(playlistId).first()
    playlistDao.addSongToPlaylist(
        PlaylistSongCrossRef(playlistId = playlistId, songId = songId, position = position)
    )
    playlistDao.touchPlaylist(playlistId, System.currentTimeMillis())
}

override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
    playlistDao.removeSongFromPlaylist(playlistId, songId)
    playlistDao.touchPlaylist(playlistId, System.currentTimeMillis())
}

override suspend fun reorderSongs(playlistId: Long, orderedSongIds: List<Long>) {
    playlistDao.clearPlaylistSongs(playlistId)
    playlistDao.replacePlaylistSongs(
        orderedSongIds.mapIndexed { index, songId ->
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId, position = index)
        }
    )
    playlistDao.touchPlaylist(playlistId, System.currentTimeMillis())
}
```

> Note: `deletePlaylist` does not need to touch `lastModified` — deleted playlists are handled separately in the sync logic (Phase 6 defers playlist deletion sync; see Known Gaps).

#### 2g: ListeningHistoryDao — add range query for sync push

**Modified:** `data/local/ListeningHistoryDao.kt`

```kotlin
@Query("SELECT * FROM listening_history WHERE playedAt > :since ORDER BY playedAt ASC")
suspend fun getHistorySince(since: Long): List<ListeningHistoryEntity>
```

Run `assembleDebug` after Layer 2 before continuing.

---

### Layer 3: Firebase data models + SongFingerprint

#### 3a: SongFingerprint

**New file:** `data/sync/SongFingerprint.kt`

```kotlin
package com.jpishimwe.syncplayer.data.sync

import java.security.MessageDigest

/**
 * Generates a stable, cross-device identifier for a song based on normalized metadata.
 *
 * Duration is bucketed to ±2 seconds to absorb minor encoding-length differences
 * between rips of the same track (e.g., 3:22.1 vs 3:22.4).
 *
 * Limitations:
 * - Songs with identical title + artist + album + duration will share a fingerprint.
 * - Songs where metadata differs across devices (different tagging conventions)
 *   produce different fingerprints and will not be matched.
 */
object SongFingerprint {
    fun compute(title: String, artist: String, album: String, durationMs: Long): String {
        val durationBucket = (durationMs / 2000L) * 2L
        val input = "${title.trim().lowercase()}|${artist.trim().lowercase()}|${album.trim().lowercase()}|$durationBucket"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        // First 8 bytes = 64-bit fingerprint; negligible collision rate for music libraries
        return bytes.take(8).joinToString("") { "%02x".format(it) }
    }
}
```

> Note: `java.security.MessageDigest` is available in all Android API levels — no new dependency needed.

#### 3b: Firestore data model classes

**New file:** `data/sync/FirestoreModels.kt`

```kotlin
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
    val songs: List<FirestorePlaylistSong> = emptyList(),
)

/**
 * Firestore document: users/{userId}/history/{autoId}
 */
data class FirestoreHistoryEvent(
    val fingerprint: String = "",
    val playedAt: Long = 0,
)
```

**Firestore collection structure:**
```
users/{userId}/
  songs/{fingerprint}         ← one document per matched song
  playlists/{remoteId}        ← one document per playlist
  history/{autoId}            ← subcollection; one document per play event
```

> Note: Playlist songs are stored as an array inside the playlist document (not a subcollection). Arrays are fine here because a playlist is unlikely to exceed Firestore's 1 MB document limit in practice. If a playlist ever approaches that limit (thousands of songs), the alternative is a `songs` subcollection — deferred to a future improvement.

---

### Layer 4: AuthRepository

**New file:** `data/sync/AuthRepository.kt`

```kotlin
package com.jpishimwe.syncplayer.data.sync

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.StateFlow

sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(
        val uid: String,
        val displayName: String?,
        val email: String?,
        val photoUrl: String?,
    ) : AuthState
}

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val currentUserId: String?
    suspend fun signInWithToken(idToken: String)
    suspend fun signOut()
}
```

**New file:** `data/sync/AuthRepositoryImpl.kt`

```kotlin
package com.jpishimwe.syncplayer.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val externalScope: CoroutineScope,
) : AuthRepository {

    override val authState: StateFlow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            trySend(
                if (user == null) AuthState.SignedOut
                else AuthState.SignedIn(
                    uid = user.uid,
                    displayName = user.displayName,
                    email = user.email,
                    photoUrl = user.photoUrl?.toString(),
                )
            )
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.stateIn(externalScope, SharingStarted.Eagerly, currentStateFromAuth())

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override suspend fun signInWithToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    private fun currentStateFromAuth(): AuthState {
        val user = firebaseAuth.currentUser ?: return AuthState.SignedOut
        return AuthState.SignedIn(user.uid, user.displayName, user.email, user.photoUrl?.toString())
    }
}
```

> Note: `@param:ApplicationScope` on the constructor parameter suppresses the Kotlin warning about annotation targets. `externalScope` is an application-level `CoroutineScope` bound to the app's lifetime — injected via Hilt (see Layer 7). This allows `authState` to survive ViewModel recreation and stay active as long as the app process lives.

> Note: `kotlinx-coroutines-play-services` provides the `.await()` extension on Firebase `Task<T>`.

---

### Layer 5: SyncRepository + ConflictResolver

#### 5a: ConflictResolver

**New file:** `data/sync/ConflictResolver.kt`

```kotlin
package com.jpishimwe.syncplayer.data.sync

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
    fun resolveSongMetadata(local: Song, remote: FirestoreSongMetadata): SongMetadataDelta? {
        val resolvedPlayCount = maxOf(local.playCount, remote.playCount)
        val resolvedRating = if (local.lastModified >= remote.lastModified) local.rating else remote.rating
        val resolvedLastPlayed = maxOf(local.lastPlayed, remote.lastPlayed)
        val resolvedLastModified = maxOf(local.lastModified, remote.lastModified)

        val unchanged = resolvedPlayCount == local.playCount
            && resolvedRating == local.rating
            && resolvedLastPlayed == local.lastPlayed
        return if (unchanged) null
        else SongMetadataDelta(resolvedPlayCount, resolvedRating, resolvedLastPlayed, resolvedLastModified)
    }

    /**
     * Determines whether the remote playlist should overwrite the local one.
     * Last-write-wins by lastModified. Remote wins on tie (trusts server clock).
     */
    fun remotePlaylistWins(local: Long, remote: Long): Boolean = remote >= local
}
```

#### 5b: SyncRepository interface

**New file:** `data/sync/SyncRepository.kt`

```kotlin
package com.jpishimwe.syncplayer.data.sync

import com.jpishimwe.syncplayer.data.local.ListeningHistoryEntity
import com.jpishimwe.syncplayer.data.local.PlaylistEntity
import com.jpishimwe.syncplayer.model.Song

interface SyncRepository {
    /** Push one song's metadata. Upserts the Firestore document. */
    suspend fun pushSongMetadata(userId: String, fingerprint: String, song: Song)

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
    suspend fun pullHistoryEvents(userId: String, since: Long): List<FirestoreHistoryEvent>
}
```

#### 5c: SyncRepositoryImpl

**New file:** `data/sync/SyncRepositoryImpl.kt`

```kotlin
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
class SyncRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
) : SyncRepository {

    override suspend fun pushSongMetadata(userId: String, fingerprint: String, song: Song) {
        val data = mapOf(
            "playCount"    to song.playCount,
            "rating"       to song.rating,
            "lastPlayed"   to song.lastPlayed,
            "lastModified" to song.lastModified,
        )
        db.collection("users").document(userId)
            .collection("songs").document(fingerprint)
            .set(data, SetOptions.merge())
            .await()
    }

    override suspend fun pullAllSongMetadata(userId: String): Map<String, FirestoreSongMetadata> {
        val snapshot = db.collection("users").document(userId)
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
        val firestoreSongs = songs.mapIndexed { index, song ->
            FirestorePlaylistSong(
                fingerprint = SongFingerprint.compute(song.title, song.artist, song.album, song.duration),
                position = index,
            )
        }
        val data = FirestorePlaylist(
            name         = playlist.name,
            createdAt    = playlist.createdAt,
            lastModified = playlist.lastModified,
            songs        = firestoreSongs,
        )

        val remoteId = playlist.remoteId ?: db.collection("users").document(userId)
            .collection("playlists").document().id  // generate a new Firestore document ID

        db.collection("users").document(userId)
            .collection("playlists").document(remoteId)
            .set(data)
            .await()

        return remoteId
    }

    override suspend fun pullAllPlaylists(userId: String): Map<String, FirestorePlaylist> {
        val snapshot = db.collection("users").document(userId)
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
            val ref = db.collection("users").document(userId)
                .collection("history").document()
            batch.set(ref, FirestoreHistoryEvent(fingerprint = fingerprint, playedAt = event.playedAt))
        }
        batch.commit().await()
    }

    override suspend fun pullHistoryEvents(userId: String, since: Long): List<FirestoreHistoryEvent> {
        val snapshot = db.collection("users").document(userId)
            .collection("history")
            .whereGreaterThan("playedAt", since)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(FirestoreHistoryEvent::class.java) }
    }
}
```

> Note: `SetOptions.merge()` on `pushSongMetadata` means a partial Firestore update — only the four fields listed are written, preserving any other fields that might exist in the document from a future schema extension.

> Note: History events are pushed as a Firestore batch write (max 500 per batch). For Phase 6, a single batch is sufficient; if a user somehow has >500 history events to push in one sync, the batch will exceed Firestore's limit — add batching in a future improvement.

---

### Layer 6: SyncOrchestrator

**New file:** `data/sync/SyncOrchestrator.kt`

```kotlin
package com.jpishimwe.syncplayer.data.sync

import android.content.Context
import android.content.SharedPreferences
import com.jpishimwe.syncplayer.data.local.ListeningHistoryDao
import com.jpishimwe.syncplayer.data.local.ListeningHistoryEntity
import com.jpishimwe.syncplayer.data.local.PlaylistDao
import com.jpishimwe.syncplayer.data.local.SongDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Success(val syncedAt: Long) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

@Singleton
class SyncOrchestrator @Inject constructor(
    @ApplicationContext context: Context,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val listeningHistoryDao: ListeningHistoryDao,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    val lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)

    /** Runs a full sync only if the user is signed in. Called from MainActivity.onResume(). */
    suspend fun syncIfSignedIn() {
        if (authRepository.currentUserId != null) sync()
    }

    /** Full bi-directional sync: push local changes, then pull remote changes. */
    suspend fun sync() {
        val userId = authRepository.currentUserId ?: return
        _syncStatus.value = SyncStatus.Syncing
        try {
            push(userId)
            pull(userId)
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_SYNC_TIME, now).apply()
            _syncStatus.value = SyncStatus.Success(syncedAt = now)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
        }
    }

    // ── Push ──────────────────────────────────────────────────────────────

    private suspend fun push(userId: String) {
        val allSongs = songDao.getAllSongsList()
        val lastSync = lastSyncTime

        // Build fingerprint lookup (songId → fingerprint) for history push
        val fingerprintMap: Map<Long, String> = allSongs.associate { song ->
            song.id to SongFingerprint.compute(song.title, song.artist, song.album, song.duration)
        }

        // Push song metadata modified since last sync
        for (song in allSongs.filter { it.lastModified > lastSync }) {
            val fingerprint = fingerprintMap[song.id] ?: continue
            syncRepository.pushSongMetadata(userId, fingerprint, song)
        }

        // Push playlists modified since last sync
        val allPlaylists = playlistDao.getAllPlaylistsList()
        for (playlist in allPlaylists.filter { it.lastModified > lastSync }) {
            val songs = playlistDao.getSongsForPlaylistList(playlist.id)
            val remoteId = syncRepository.pushPlaylist(userId, playlist, songs)
            if (playlist.remoteId == null) {
                // Store the Firestore-assigned remoteId locally so future pushes update, not create
                playlistDao.updatePlaylist(playlist.copy(remoteId = remoteId))
            }
        }

        // Push listening history events since last sync
        val newHistory = listeningHistoryDao.getHistorySince(lastSync)
        if (newHistory.isNotEmpty()) {
            syncRepository.pushHistoryEvents(userId, newHistory, fingerprintMap)
        }
    }

    // ── Pull ──────────────────────────────────────────────────────────────

    private suspend fun pull(userId: String) {
        val allSongs = songDao.getAllSongsList()
        val fingerprintToSong: Map<String, com.jpishimwe.syncplayer.model.Song> =
            allSongs.associateBy { song ->
                SongFingerprint.compute(song.title, song.artist, song.album, song.duration)
            }

        // Pull song metadata and apply conflict resolution
        val remoteSongs = syncRepository.pullAllSongMetadata(userId)
        for ((fingerprint, remote) in remoteSongs) {
            val local = fingerprintToSong[fingerprint] ?: continue  // not on this device
            val delta = ConflictResolver.resolveSongMetadata(local, remote) ?: continue
            songDao.applySyncDelta(local.id, delta.playCount, delta.rating, delta.lastPlayed, delta.lastModified)
        }

        // Pull playlists
        val remotePlaylists = syncRepository.pullAllPlaylists(userId)
        val localPlaylists = playlistDao.getAllPlaylistsList()
        val remoteIdToLocal = localPlaylists.associateBy { it.remoteId }

        for ((remoteId, remote) in remotePlaylists) {
            val local = remoteIdToLocal[remoteId]
            if (local == null) {
                // New playlist from remote — create locally
                val newId = playlistDao.insertPlaylist(
                    com.jpishimwe.syncplayer.data.local.PlaylistEntity(
                        name = remote.name,
                        createdAt = remote.createdAt,
                        remoteId = remoteId,
                        lastModified = remote.lastModified,
                    )
                )
                val crossRefs = remote.songs.mapNotNull { s ->
                    fingerprintToSong[s.fingerprint]?.let {
                        com.jpishimwe.syncplayer.data.local.PlaylistSongCrossRef(
                            playlistId = newId,
                            songId = it.id,
                            position = s.position,
                        )
                    }
                }
                playlistDao.replacePlaylistSongs(crossRefs)
            } else if (ConflictResolver.remotePlaylistWins(local.lastModified, remote.lastModified)) {
                // Remote wins — overwrite local name and song list
                playlistDao.updatePlaylist(
                    local.copy(name = remote.name, lastModified = remote.lastModified)
                )
                val crossRefs = remote.songs.mapNotNull { s ->
                    fingerprintToSong[s.fingerprint]?.let {
                        com.jpishimwe.syncplayer.data.local.PlaylistSongCrossRef(
                            playlistId = local.id,
                            songId = it.id,
                            position = s.position,
                        )
                    }
                }
                playlistDao.clearPlaylistSongs(local.id)
                playlistDao.replacePlaylistSongs(crossRefs)
            }
            // else: local wins → no change needed
        }

        // Pull and merge listening history events
        val remoteHistory = syncRepository.pullHistoryEvents(userId, since = lastSyncTime)
        for (event in remoteHistory) {
            val song = fingerprintToSong[event.fingerprint] ?: continue
            listeningHistoryDao.insertListeningHistory(
                ListeningHistoryEntity(songId = song.id, playedAt = event.playedAt)
            )
        }
    }

    companion object {
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
    }
}
```

> Note: The `pull` step re-uses `lastSyncTime` from before `push()` ran (captured at the start of the sync cycle). This avoids a race condition where the history pull window would be set to "now" before the push completes, potentially missing the just-pushed events on a re-pull.

> Note: `SyncOrchestrator` does NOT observe Firestore in real-time (no `addSnapshotListener`). It does a one-shot read on each sync trigger. Real-time listeners are a future improvement — they add complexity (merge on every remote write) but enable near-instant sync across devices.

---

### Layer 7: DI modules

**New file:** `di/SyncModule.kt`

```kotlin
package com.jpishimwe.syncplayer.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.jpishimwe.syncplayer.data.sync.AuthRepository
import com.jpishimwe.syncplayer.data.sync.AuthRepositoryImpl
import com.jpishimwe.syncplayer.data.sync.SyncRepository
import com.jpishimwe.syncplayer.data.sync.SyncRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    companion object {
        /**
         * Application-level CoroutineScope for long-lived coroutines (e.g., authState flow).
         * Uses SupervisorJob so child failures don't cancel the scope.
         */
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob())

        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore {
            val db = FirebaseFirestore.getInstance()
            db.firestoreSettings = firestoreSettings {
                setLocalCacheSettings(persistentCacheSettings {
                    setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                })
            }
            return db
        }
    }
}
```

**Modified:** `di/AppModule.kt`

Inject `@ApplicationScope CoroutineScope` into `AuthRepositoryImpl` via the qualifier:

```kotlin
// AuthRepositoryImpl constructor should be:
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @param:ApplicationScope private val externalScope: CoroutineScope,
) : AuthRepository
```

Run `assembleDebug` after Layer 7.

---

### Layer 8: SettingsViewModel

**New file:** `ui/settings/SettingsEvent.kt`

```kotlin
package com.jpishimwe.syncplayer.ui.settings

sealed interface SettingsEvent {
    /** Fires after SettingsScreen completes the CredentialManager flow and has an ID token. */
    data class SignInWithToken(val idToken: String) : SettingsEvent
    /** Fires if CredentialManager throws or user cancels. */
    data class SignInError(val message: String) : SettingsEvent
    data object SignOut : SettingsEvent
    data object SyncNow : SettingsEvent
}
```

**New file:** `ui/settings/SettingsUiState.kt`

```kotlin
package com.jpishimwe.syncplayer.ui.settings

import com.jpishimwe.syncplayer.data.sync.AuthState
import com.jpishimwe.syncplayer.data.sync.SyncStatus

data class SettingsUiState(
    val authState: AuthState = AuthState.SignedOut,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val lastSyncTime: Long? = null,
)
```

**New file:** `ui/settings/SettingsViewModel.kt`

```kotlin
package com.jpishimwe.syncplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpishimwe.syncplayer.data.sync.AuthRepository
import com.jpishimwe.syncplayer.data.sync.SyncOrchestrator
import com.jpishimwe.syncplayer.data.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncOrchestrator: SyncOrchestrator,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        authRepository.authState,
        syncOrchestrator.syncStatus,
    ) { authState, syncStatus ->
        SettingsUiState(
            authState    = authState,
            syncStatus   = syncStatus,
            lastSyncTime = (syncStatus as? SyncStatus.Success)?.syncedAt
                ?: syncOrchestrator.lastSyncTime.takeIf { it > 0L },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SettingsUiState())

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SignInWithToken -> viewModelScope.launch {
                authRepository.signInWithToken(event.idToken)
            }
            is SettingsEvent.SignInError -> {
                // Surface error as a SyncStatus.Error so the UI can show a message
                // (auth errors reuse the sync status slot for simplicity)
                // No-op for Phase 6 — sign-in errors are visible via Logcat; a snackbar can be added in Phase 7
            }
            SettingsEvent.SignOut -> viewModelScope.launch {
                authRepository.signOut()
            }
            SettingsEvent.SyncNow -> viewModelScope.launch {
                syncOrchestrator.sync()
            }
        }
    }
}
```

> Note: `SettingsEvent.SignInError` is a no-op in Phase 6 (the error is swallowed). A Snackbar-based error surface is deferred to Phase 7 polish. Log the error at least with `Log.w` inside `AuthRepositoryImpl` or `SettingsScreen`.

---

### Layer 9: SettingsScreen + NavGraph update

**New file:** `ui/settings/SettingsScreen.kt`

Two composables following the testable Screen/ScreenContent pattern.

`SettingsScreen` — outer shell (handles CredentialManager, ViewModel):

```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // CredentialManager instance survives recomposition
    val credentialManager = remember { CredentialManager.create(context) }
    // Resolve before lambda — stringResource() cannot be called inside a non-composable lambda
    val clientId = stringResource(R.string.default_web_client_id)

    SettingsScreenContent(
        uiState = uiState,
        onSignIn = {
            scope.launch {
                try {
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(
                            GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(clientId)
                                .build()
                        )
                        .build()
                    val result = credentialManager.getCredential(context, request)
                    val googleIdToken = GoogleIdTokenCredential
                        .createFrom(result.credential.data)
                        .idToken
                    viewModel.onEvent(SettingsEvent.SignInWithToken(googleIdToken))
                } catch (e: GetCredentialException) {
                    viewModel.onEvent(SettingsEvent.SignInError(e.message ?: "Sign-in cancelled"))
                }
            }
        },
        onSignOut = { viewModel.onEvent(SettingsEvent.SignOut) },
        onSyncNow = { viewModel.onEvent(SettingsEvent.SyncNow) },
    )
}
```

`SettingsScreenContent` — testable inner (no ViewModel, no Context, no CredentialManager):

```kotlin
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                // Account section
                when (val auth = uiState.authState) {
                    is AuthState.SignedOut -> {
                        ListItem(
                            headlineContent = { Text("Sign in with Google") },
                            supportingContent = { Text("Sync metadata across your devices") },
                            trailingContent = {
                                Button(onClick = onSignIn) { Text("Sign in") }
                            }
                        )
                    }
                    is AuthState.SignedIn -> {
                        ListItem(
                            headlineContent = { Text(auth.displayName ?: "Signed in") },
                            supportingContent = { Text(auth.email ?: auth.uid) },
                            trailingContent = {
                                OutlinedButton(onClick = onSignOut) { Text("Sign out") }
                            }
                        )
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                // Sync section (only shown when signed in)
                if (uiState.authState is AuthState.SignedIn) {
                    SyncStatusCard(
                        syncStatus = uiState.syncStatus,
                        lastSyncTime = uiState.lastSyncTime,
                        onSyncNow = onSyncNow,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncStatusCard(
    syncStatus: SyncStatus,
    lastSyncTime: Long?,
    onSyncNow: () -> Unit,
) {
    ListItem(
        headlineContent = { Text("Sync") },
        supportingContent = {
            when (syncStatus) {
                SyncStatus.Idle -> if (lastSyncTime != null) {
                    Text("Last synced: ${formatSyncTime(lastSyncTime)}")
                } else {
                    Text("Never synced")
                }
                SyncStatus.Syncing -> Text("Syncing…")
                is SyncStatus.Success -> Text("Synced: ${formatSyncTime(syncStatus.syncedAt)}")
                is SyncStatus.Error -> Text("Sync error: ${syncStatus.message}")
            }
        },
        trailingContent = {
            val syncing = syncStatus is SyncStatus.Syncing
            if (syncing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                IconButton(onClick = onSyncNow) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync now")
                }
            }
        }
    )
}

private fun formatSyncTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
```

**Modified:** `ui/navigation/NavGraph.kt`

Add `Settings` to the `Screen` sealed class:

```kotlin
data object Settings : Screen("settings")
```

Add `Settings` to `BottomNavDestination` enum:

```kotlin
private enum class BottomNavDestination(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
) {
    LIBRARY(Screen.Library, "Library", Icons.Default.LibraryMusic),
    PLAYLISTS(Screen.Playlists, "Playlists", Icons.AutoMirrored.Filled.PlaylistPlay),
    SETTINGS(Screen.Settings, "Settings", Icons.Default.Settings),
}
```

Update `showBottomNav` to include `Screen.Settings.route`:

```kotlin
val showBottomNav = currentRoute in setOf(
    Screen.Library.route,
    Screen.Playlists.route,
    Screen.Settings.route,
)
```

Add a `composable` entry for the Settings route in `NavHost`:

```kotlin
composable(Screen.Settings.route) {
    SettingsScreen()
}
```

---

### Layer 10: Sync trigger wiring

**Modified:** `MainActivity.kt`

Wire `SyncOrchestrator.syncIfSignedIn()` into the activity lifecycle so sync fires every time the app comes to the foreground.

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncOrchestrator: SyncOrchestrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SyncPlayerApp() }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            syncOrchestrator.syncIfSignedIn()
        }
    }
}
```

> Note: `lifecycleScope.launch` launches a coroutine tied to the Activity lifecycle. If sync is still running when the app goes to the background, the coroutine will be cancelled when the activity is destroyed. This is acceptable for Phase 6 — sync is best-effort on foreground. A `WorkManager` periodic job would provide background durability (deferred).

---

### Layer 11: Tests

#### FakeAuthRepository

**New file:** `test/data/FakeAuthRepository.kt`

```kotlin
class FakeAuthRepository : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()
    override val currentUserId: String? get() = (_authState.value as? AuthState.SignedIn)?.uid

    var signInCallCount = 0
    var signOutCallCount = 0
    var lastIdToken: String? = null

    override suspend fun signInWithToken(idToken: String) {
        signInCallCount++
        lastIdToken = idToken
        _authState.value = AuthState.SignedIn(
            uid = "test-uid",
            displayName = "Test User",
            email = "test@example.com",
            photoUrl = null,
        )
    }

    override suspend fun signOut() {
        signOutCallCount++
        _authState.value = AuthState.SignedOut
    }

    fun setSignedIn() {
        _authState.value = AuthState.SignedIn("test-uid", "Test User", "test@example.com", null)
    }
}
```

#### FakeSyncRepository

**New file:** `test/data/FakeSyncRepository.kt`

```kotlin
class FakeSyncRepository : SyncRepository {
    var pushSongMetadataCallCount = 0
    var pushPlaylistCallCount = 0
    var pushHistoryCallCount = 0
    var remoteSongMetadata: Map<String, FirestoreSongMetadata> = emptyMap()
    var remotePlaylists: Map<String, FirestorePlaylist> = emptyMap()
    var remoteHistory: List<FirestoreHistoryEvent> = emptyList()
    var nextRemoteId = "remote-playlist-1"

    override suspend fun pushSongMetadata(userId: String, fingerprint: String, song: Song) {
        pushSongMetadataCallCount++
    }

    override suspend fun pullAllSongMetadata(userId: String) = remoteSongMetadata

    override suspend fun pushPlaylist(
        userId: String,
        playlist: PlaylistEntity,
        songs: List<Song>,
    ): String {
        pushPlaylistCallCount++
        return playlist.remoteId ?: nextRemoteId
    }

    override suspend fun pullAllPlaylists(userId: String) = remotePlaylists

    override suspend fun pushHistoryEvents(
        userId: String,
        events: List<ListeningHistoryEntity>,
        fingerprintMap: Map<Long, String>,
    ) {
        pushHistoryCallCount++
    }

    override suspend fun pullHistoryEvents(userId: String, since: Long) = remoteHistory
}
```

#### ConflictResolverTest

**New file:** `test/data/sync/ConflictResolverTest.kt`

```kotlin
@ExtendWith(...)
class ConflictResolverTest {

    private fun song(playCount: Int, rating: Int, lastPlayed: Long, lastModified: Long): Song =
        Song(id = 1L, title = "T", artist = "A", album = "B", albumId = 0, duration = 180_000,
             trackNumber = 1, year = 2020, dateAdded = 0, contentUri = null, albumArtUri = null,
             playCount = playCount, rating = rating, lastPlayed = lastPlayed, lastModified = lastModified)

    @Test
    fun `resolveSongMetadata returns null when local is already merged state`() {
        val local = song(5, 3, 1000, 2000)
        val remote = FirestoreSongMetadata(playCount = 5, rating = 3, lastPlayed = 1000, lastModified = 2000)
        assertNull(ConflictResolver.resolveSongMetadata(local, remote))
    }

    @Test
    fun `playCount takes max`() {
        val local = song(3, 0, 0, 1000)
        val remote = FirestoreSongMetadata(playCount = 7, rating = 0, lastPlayed = 0, lastModified = 900)
        val delta = ConflictResolver.resolveSongMetadata(local, remote)!!
        assertEquals(7, delta.playCount)
    }

    @Test
    fun `rating uses last-write-wins by lastModified - remote wins`() {
        val local = song(0, 3, 0, 1000)  // local modified at 1000
        val remote = FirestoreSongMetadata(playCount = 0, rating = 5, lastPlayed = 0, lastModified = 2000)
        val delta = ConflictResolver.resolveSongMetadata(local, remote)!!
        assertEquals(5, delta.rating)
    }

    @Test
    fun `rating uses last-write-wins by lastModified - local wins`() {
        val local = song(0, 5, 0, 2000)  // local modified at 2000
        val remote = FirestoreSongMetadata(playCount = 0, rating = 3, lastPlayed = 0, lastModified = 1000)
        val delta = ConflictResolver.resolveSongMetadata(local, remote)!!
        assertEquals(5, delta.rating)
    }

    @Test
    fun `lastPlayed takes max`() {
        val local = song(0, 0, 5000, 0)
        val remote = FirestoreSongMetadata(playCount = 0, rating = 0, lastPlayed = 9000, lastModified = 0)
        val delta = ConflictResolver.resolveSongMetadata(local, remote)!!
        assertEquals(9000, delta.lastPlayed)
    }

    @Test
    fun `remotePlaylistWins returns true when remote is newer`() {
        assertTrue(ConflictResolver.remotePlaylistWins(local = 1000, remote = 2000))
    }

    @Test
    fun `remotePlaylistWins returns false when local is newer`() {
        assertFalse(ConflictResolver.remotePlaylistWins(local = 2000, remote = 1000))
    }

    @Test
    fun `remotePlaylistWins returns true on tie (server clock trusted)`() {
        assertTrue(ConflictResolver.remotePlaylistWins(local = 1000, remote = 1000))
    }
}
```

#### SongFingerprintTest

**New file:** `test/data/sync/SongFingerprintTest.kt`

```kotlin
class SongFingerprintTest {
    @Test
    fun `same song produces same fingerprint`() {
        val a = SongFingerprint.compute("Bohemian Rhapsody", "Queen", "A Night at the Opera", 354_000)
        val b = SongFingerprint.compute("Bohemian Rhapsody", "Queen", "A Night at the Opera", 354_000)
        assertEquals(a, b)
    }

    @Test
    fun `fingerprint is case-insensitive`() {
        val lower = SongFingerprint.compute("bohemian rhapsody", "queen", "a night at the opera", 354_000)
        val mixed = SongFingerprint.compute("Bohemian Rhapsody", "QUEEN", "A Night at the Opera", 354_000)
        assertEquals(lower, mixed)
    }

    @Test
    fun `durations within 2 seconds produce same fingerprint`() {
        val a = SongFingerprint.compute("Track", "Artist", "Album", 180_000)
        val b = SongFingerprint.compute("Track", "Artist", "Album", 181_500)  // +1.5 s
        assertEquals(a, b)
    }

    @Test
    fun `durations more than 2 seconds apart produce different fingerprints`() {
        val a = SongFingerprint.compute("Track", "Artist", "Album", 180_000)
        val b = SongFingerprint.compute("Track", "Artist", "Album", 184_000)  // +4 s
        assertNotEquals(a, b)
    }

    @Test
    fun `different songs produce different fingerprints`() {
        val a = SongFingerprint.compute("Song A", "Artist", "Album", 200_000)
        val b = SongFingerprint.compute("Song B", "Artist", "Album", 200_000)
        assertNotEquals(a, b)
    }
}
```

#### SettingsViewModelTest

**New file:** `test/ui/settings/SettingsViewModelTest.kt`

```kotlin
@ExtendWith(MainDispatcherExtension::class)
class SettingsViewModelTest {

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var syncOrchestrator: FakeSyncOrchestrator  // or spy on real instance
    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setup() {
        authRepository = FakeAuthRepository()
        // FakeSyncOrchestrator: wraps SyncOrchestrator with no-op implementations
        syncOrchestrator = FakeSyncOrchestrator()
        viewModel = SettingsViewModel(authRepository, syncOrchestrator)
    }

    @Test
    fun `uiState reflects SignedOut by default`() = runTest {
        val state = viewModel.uiState.value
        assertIs<AuthState.SignedOut>(state.authState)
    }

    @Test
    fun `uiState reflects SignedIn after signInWithToken event`() = runTest {
        viewModel.onEvent(SettingsEvent.SignInWithToken("fake-id-token"))
        val state = viewModel.uiState.value
        assertIs<AuthState.SignedIn>(state.authState)
        assertEquals("test@example.com", (state.authState as AuthState.SignedIn).email)
    }

    @Test
    fun `SignOut event calls authRepository signOut`() = runTest {
        authRepository.setSignedIn()
        viewModel.onEvent(SettingsEvent.SignOut)
        assertEquals(1, authRepository.signOutCallCount)
    }

    @Test
    fun `SyncNow event calls syncOrchestrator sync`() = runTest {
        viewModel.onEvent(SettingsEvent.SyncNow)
        assertEquals(1, syncOrchestrator.syncCallCount)
    }
}
```

---

## Files summary

### New files (15)

| File | Purpose |
|------|---------|
| `data/sync/SongFingerprint.kt` | Stable cross-device song identifier |
| `data/sync/FirestoreModels.kt` | Firestore document data classes |
| `data/sync/AuthRepository.kt` | Auth interface + `AuthState` sealed interface |
| `data/sync/AuthRepositoryImpl.kt` | Firebase Auth + callbackFlow authState |
| `data/sync/SyncRepository.kt` | Firestore read/write interface |
| `data/sync/SyncRepositoryImpl.kt` | Firestore operations |
| `data/sync/ConflictResolver.kt` | Merge logic for all data types |
| `data/sync/SyncOrchestrator.kt` | Push/pull coordination + `SyncStatus` StateFlow |
| `di/SyncModule.kt` | Firebase + auth + sync DI bindings; `@ApplicationScope` |
| `ui/settings/SettingsEvent.kt` | Event sealed interface |
| `ui/settings/SettingsUiState.kt` | UI state data class |
| `ui/settings/SettingsViewModel.kt` | ViewModel |
| `ui/settings/SettingsScreen.kt` | Screen + ScreenContent + SyncStatusCard |
| `test/data/FakeAuthRepository.kt` | Test double |
| `test/data/FakeSyncRepository.kt` | Test double |

### New test files (3)

| File | Purpose |
|------|---------|
| `test/data/sync/ConflictResolverTest.kt` | Conflict resolution logic for all strategies |
| `test/data/sync/SongFingerprintTest.kt` | Fingerprint stability and edge cases |
| `test/ui/settings/SettingsViewModelTest.kt` | ViewModel state transitions and event routing |

### Modified files (8)

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Add Firebase BOM, Auth, Firestore, Credential Manager, Google ID, coroutines-play-services, google-services plugin |
| `app/build.gradle.kts` | Apply `google-services` plugin; add Firebase, Credential Manager, coroutines-play deps |
| `build.gradle.kts` (project) | Add `google-services` plugin with `apply false` |
| `model/Song.kt` | Add `lastModified: Long = 0` field |
| `data/local/PlaylistDao.kt` | Add `remoteId`, `lastModified` to `PlaylistEntity`; add `touchPlaylist`, `getAllPlaylistsList`, `getSongsForPlaylistList` |
| `data/local/ListeningHistoryDao.kt` | Add `getHistorySince(since: Long)` |
| `data/local/SongDao.kt` | Add `lastModified` to `incrementPlayCount` and `setRating` queries; add `getAllSongsList`, `applySyncDelta` |
| `data/local/SyncPlayerDatabase.kt` | Bump to version 5; add `MIGRATION_4_5` constant |
| `di/DatabaseModule.kt` | Replace `fallbackToDestructiveMigration` with `.addMigrations(MIGRATION_4_5)` |
| `data/SongRepositoryImpl.kt` | Pass `modifiedAt` to `songDao.setRating` |
| `data/PlaylistRepositoryImpl.kt` | Stamp `lastModified` on all mutations; call `touchPlaylist` after song add/remove/reorder |
| `di/AppModule.kt` | (No change needed — `SyncModule` handles new bindings) |
| `ui/navigation/NavGraph.kt` | Add `Screen.Settings`; add Settings to `BottomNavDestination`; add `composable` entry; update `showBottomNav` set |
| `MainActivity.kt` | Inject `SyncOrchestrator`; call `syncIfSignedIn()` in `onResume` |
| `app/src/main/res/values/strings.xml` | Add `default_web_client_id` (user-provided value) |

---

## Dependencies

Versions verified March 2026. Confirmed compatible with AGP 9.0.0 + Kotlin 2.2.10.

> **Note:** Do not use `-ktx` variants for Firebase — as of BOM 34.0.0 they were removed. KTX APIs now live in the main modules.

| Library | Group ID | Notes |
|---------|----------|-------|
| Firebase BOM | `com.google.firebase:firebase-bom` | Manages versions of all Firebase libraries |
| Firebase Auth | `com.google.firebase:firebase-auth` | Version from BOM — no explicit version in catalog |
| Cloud Firestore | `com.google.firebase:firebase-firestore` | Version from BOM — no explicit version in catalog |
| Credential Manager | `androidx.credentials:credentials` | Android Credential Manager API (Jetpack) |
| Credentials Play Services Auth | `androidx.credentials:credentials-play-services-auth` | Google Play Services bridge for CredentialManager |
| Google ID | `com.google.android.libraries.identity.googleid:googleid` | `GoogleIdTokenCredential` type |
| `kotlinx-coroutines-play-services` | `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | `.await()` on Firebase `Task<T>` — same version as existing coroutines (`1.10.1`) |
| Google Services Gradle Plugin | `com.google.gms:google-services` | Processes `google-services.json` at build time |

> **Compatibility note:** Firebase Android SDK requires `google-services.json` to initialize. Without it, `FirebaseApp.initializeApp()` will throw at runtime. Verify `google-services.json` is present in `app/` before first build after adding the plugin.

> **AGP 9 note:** `google-services` `4.4.3` is confirmed compatible with AGP 9.

---

## Open questions

1. **Playlist deletion sync**: If the user deletes a playlist on Device A and both devices sync, Device B will re-create the playlist on pull (since it still exists in Firestore). Phase 6 defers deletion sync — deleted playlists on Firestore are never removed. A tombstone-based deletion strategy is a future improvement. **Decision needed**: Is it acceptable in Phase 6 for deleted playlists to reappear after sync? If not, add a `deletedAt` tombstone to `FirestorePlaylist` and skip creating local playlists where `deletedAt` is set.

2. **History deduplication on pull**: The pull step inserts remote history events into the local `listening_history` table unconditionally. If the same event is pulled twice (e.g., after a failed sync that partially committed), duplicate rows are inserted. A `(songId, playedAt)` uniqueness constraint on `listening_history` would prevent this but requires a migration. **Decision needed**: Add unique constraint in migration 4→5, or accept rare duplicates and add deduplication in a later phase?

3. **Batch size for history push**: `SyncRepositoryImpl.pushHistoryEvents` uses a single Firestore `WriteBatch`. Firestore batches are limited to 500 operations. A user who has been offline for a long time and accumulated >500 history events will hit this limit and see a sync error. **Decision needed**: Add chunking (split into batches of 500), or document as a known limitation?

4. **Web Client ID in source control**: The `default_web_client_id` string is not secret (it's a public OAuth2 client ID), but it's Firebase-project-specific. Committing it in `strings.xml` is fine. However, it will be different for every developer who creates their own Firebase project. **Decision needed**: Hard-code a placeholder in `strings.xml` and document setup steps, or use a `local.properties`-based approach?

5. **`google-services.json` in CI**: GitHub Actions will need this file to build a release APK. It should be stored as a base64-encoded GitHub Secret and written to `app/google-services.json` in the workflow. This is standard practice (already noted in `docs/PLAN.md` under CI/CD). No blocking decision required — just needs to be done when setting up CI.

---

## Known bugs

| # | Bug | Likely cause |
|---|-----|-------------|
| 1 | No sync button visible in Settings | Settings tab navigation not wired, or `SyncStatusCard` not rendering when signed in |
| 2 | Metadata (play counts, ratings) not persisting across sessions | `lastModified` not being stamped on write — `setRating` or `incrementPlayCount` not passing timestamp to DAO, or migration not applied |
| 3 | Top plays list showing wrong songs | `playCount` sort query incorrect, or `applySyncDelta` overwriting play counts with stale remote values |
| 4 | Favourites not populating with starred songs | `rating` filter query not matching persisted values, or rating not surviving sync |
| 5 | Lists resetting when navigating back | `SharingStarted.WhileSubscribed` stopping flow too aggressively, or ViewModel being recreated on navigation |

---

## Verification

- `assembleDebug` succeeds after each layer
- `test` passes all unit tests
- Manual:
  - App opens without crash (no `google-services.json` errors)
  - Settings tab visible in bottom nav — navigates to SettingsScreen
  - "Sign in with Google" button launches Google account picker
  - After sign-in: account name and email displayed, "Sync now" button visible
  - Tap "Sync now" → spinner shows while syncing → "Last synced: [time]" appears
  - Rate a song → sign into second device → sync → rating visible on second device
  - Play a song past threshold on Device A → sync → play count incremented on Device B
  - Create a playlist on Device A → sync → playlist appears on Device B
  - Sign out → Settings shows "Sign in" button; sync status is hidden
  - Kill network → tap "Sync now" → error state shown; app does not crash
  - Restore network → sync succeeds from Settings