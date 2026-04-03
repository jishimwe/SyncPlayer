---
type: analysis
feature: open-bugs
phase: 7
status: complete
tags:
  - type/analysis
  - status/complete
  - feature/open-bugs
---

# `data` Package Analysis

**Date**: 2026-03-22
**Scope**: All files in `app/src/main/java/com/jpishimwe/syncplayer/data/` (including `local/`, `sync/`, `remote/`)

---

## Bugs

### B1 — `ArtistImageRepositoryImpl` never caches failed lookups
**File**: `ArtistImageRepositoryImpl.kt:33-43`

When `service.fetchArtistImageUrl()` returns `null` (API has no result or network failed), the result is NOT cached. The 1-day TTL for null results on line 29 only works if a previous call cached a null `imageUrl`. But lines 34-42 only call `dao.insertOrReplace` when `url != null`. Every failed lookup hits the network again on the next call, ignoring the intended 1-day backoff.

**Fix**: Cache null results too:
```kotlin
dao.insertOrReplace(ArtistImage(artistName = artistName, imageUrl = url, fetchedAt = now))
return url
```

---

### B2 — `SyncRepositoryImpl` force-unwraps Firestore deserialization
**File**: `SyncRepositoryImpl.kt:57, 116`

```kotlin
doc.toObject(FirestoreSongMetadata::class.java)!!
doc.toObject(FirestorePlaylist::class.java)!!
```

If Firestore returns a document with unexpected schema (e.g., after a migration), `toObject` returns null and `!!` crashes the app. An entire sync fails because of one malformed document.

**Fix**: Use `mapNotNull` instead of `associate` + `!!`:
```kotlin
snapshot.documents.mapNotNull { doc ->
    val obj = doc.toObject(FirestoreSongMetadata::class.java) ?: return@mapNotNull null
    doc.id to obj
}.toMap()
```

---

### B3 — `SyncRepositoryImpl.pushHistoryEvents` exceeds Firestore batch limit
**File**: `SyncRepositoryImpl.kt:125-138`

Firestore batched writes have a hard limit of 500 operations per batch. If a user accumulates 500+ history events between syncs, `batch.commit()` will fail.

```kotlin
val batch = db.batch()
for (event in events) {  // Could exceed 500
    batch.set(ref, ...)
}
batch.commit().await()  // Crashes if > 500 operations
```

**Fix**: Chunk events into batches of 500:
```kotlin
events.chunked(500).forEach { chunk ->
    val batch = db.batch()
    for (event in chunk) { ... }
    batch.commit().await()
}
```

---

### B4 — `MediaStoreScanner` generates invalid album art URIs for albumId 0
**File**: `MediaStoreScanner.kt:66-71`

```kotlin
val albumArtUri = ContentUris.withAppendedId(
    Uri.parse("content://media/external/audio/albumart"),
    albumId,
).toString()
```

When `albumId` is 0 (no album metadata), this creates `content://media/external/audio/albumart/0` — an invalid URI. Coil will attempt to load it and fail silently, wasting resources.

**Fix**: Set `albumArtUri = null` when `albumId == 0L`.

---

## Gaps (Missing Functionality)

### G1 — Missing Room indices on frequently joined columns
**Files**: `Song.kt`, `PlaylistDao.kt` (PlaylistSongCrossRef), `ListeningHistoryDao.kt` (ListeningHistoryEntity)

No `@Index` annotations on columns used in WHERE/JOIN:
- `songs.artist` — used in `getSongsByArtist` (`WHERE artist = :artist`)
- `songs.albumArtist` — used in `getAllArtists` and `getArtistByName` (`GROUP BY`/`WHERE`)
- `songs.lastModified` — used in sync delta filtering
- `playlist_songs.playlistId` — used in every playlist query JOIN
- `listening_history.songId` — used in `getRecentlyPlayed` JOIN

Without indices, these queries do full table scans. Impact grows linearly with library size.

**Fix**: Add indices to `@Entity` annotations:
```kotlin
@Entity(tableName = "songs", indices = [
    Index("artist"), Index("albumArtist"), Index("albumId"), Index("lastModified")
])

@Entity(tableName = "playlist_songs", indices = [
    Index("playlistId"), Index("songId")
])

@Entity(tableName = "listening_history", indices = [
    Index("songId")
])
```

Requires a Room migration (version bump + `CREATE INDEX` statements).

---

### G2 — `PlaylistRepositoryImpl.renamePlaylist` silently fails on missing playlist
**File**: `PlaylistRepositoryImpl.kt:44-46`

```kotlin
val entity = playlistDao.getPlaylistById(playlistId).first()
playlistDao.updatePlaylist(entity?.copy(name = newName, ...) ?: return)
```

If the playlist doesn't exist, the function returns silently. The caller (PlaylistViewModel) has no way to know the rename failed. No error state is surfaced to the UI.

**Fix**: Throw or return a Result type so the ViewModel can surface an error.

---

### G3 — `ArtistImageRepositoryImpl` doesn't distinguish network errors from "not found"
**File**: `ArtistImageRepositoryImpl.kt:33`

Both network timeouts and "API has no result" return `null`. Network timeouts should retry sooner (minutes, not a full day), while "not found" results are correctly cached for 1 day. But since null results aren't cached (see B1), this distinction doesn't matter yet — fixing B1 makes this gap actionable.

---

### G4 — `SyncRepositoryImpl` logs sensitive data in production
**File**: `SyncRepositoryImpl.kt:36, 44, 55, 114, 136`

```kotlin
Log.d(TAG, "pushSongMetadata: $path data=$data")
```

Logs contain user song metadata, playlist names, and sync payloads. These logs are readable by other apps via `adb logcat` and persist on device.

**Fix**: Guard with `BuildConfig.DEBUG` or remove entirely.

---

### G5 — `QueueEntity.position` is mutable (`var`)
**File**: `QueueDao.kt:14`

```kotlin
data class QueueEntity(
    @PrimaryKey val id: String,
    val songId: Long,
    var position: Int,  // mutable
)
```

Room entities should be immutable (`val`). Mutable fields create confusion about whether in-memory mutations persist to the database (they don't automatically). The current code in `PlayerRepositoryImpl.playNext()` works because it mutates then re-inserts, but this pattern is fragile.

**Fix**: Change to `val position: Int` and rebuild entities with `.copy(position = newPosition)` instead of `it.position++`.

---

## Improvements

### I1 — `SongDao` artist queries should filter empty names
**File**: `SongDao.kt:38-48`

```sql
SELECT albumArtist AS name, ...
FROM songs
LEFT JOIN artist_images ai ON ai.artistName = songs.albumArtist
GROUP BY name
```

If songs have empty `albumArtist` values, they create a blank artist entry. Should add `WHERE songs.albumArtist IS NOT NULL AND songs.albumArtist != ''`.

---

### I2 — `ArtistImageRepositoryImpl` cache logic can be simplified
**File**: `ArtistImageRepositoryImpl.kt:21-31`

The two `if` branches can be collapsed:
```kotlin
if (cached != null) {
    val ttl = if (cached.imageUrl != null) sevenDays else oneDay
    if (now - cached.fetchedAt < ttl) return cached.imageUrl
}
```

---

### I3 — `PlaylistRepositoryImpl.addSongToPlaylist` uses count as position
**File**: `PlaylistRepositoryImpl.kt:57`

```kotlin
val position = playlistDao.getSongCountForPlaylist(playlistId).first()
```

Uses song count as the new position. If songs are removed mid-playlist, positions can have gaps, and count != max position. Should query `MAX(position) + 1` instead.

**Fix**: Add a DAO method `getMaxPositionForPlaylist(playlistId: Long): Flow<Int?>` and use `(maxPosition ?: -1) + 1`.

---

### I4 — `ConflictResolver.mergeHistoryEvent` is unused in current sync flow
**File**: `ConflictResolver.kt:66`

The function exists and is tested (7 tests) but never called by `SyncOrchestrator`. The design doc explicitly notes this: "available for a batch-reconcile use case in a future phase." Not dead code per se, but should be documented in-file to prevent accidental deletion.

---

### I5 — `SyncOrchestrator` builds full fingerprint map on every sync
**File**: `SyncOrchestrator.kt:127`

```kotlin
val fingerprintToSong: Map<String, Song> =
    allSongs.associateBy { song -> SongFingerprint.compute(...) }
```

For a 50,000-song library, this allocates a large map on every sync. Could be filtered to only songs modified since `lastSyncTime` for the push path, and lazily computed for the pull path.

---

### I6 — `PlayerRepositoryImpl` position update polling
**File**: `PlayerRepositoryImpl.kt:157-189`

Position updates poll every 500ms unconditionally while playing. For a 4-minute song, that's ~480 state updates. The seek bar UI only needs updates when visible. Consider:
- Pausing updates when the NowPlaying screen is not visible
- Using a larger interval (1s is typical for music players)

---

## Summary

| Category     | Total | Remaining |
|--------------|-------|-----------|
| Bugs         | 4     | 4         |
| Gaps         | 5     | 5         |
| Improvements | 6     | 6         |

### Priority order
1. **B1** — Cache failed artist image lookups (causes repeated network hits)
2. **B3** — Firestore batch limit (will crash with heavy usage)
3. **G1** — Add Room indices (performance at scale)
4. **B2** — Null-safe Firestore deserialization (crash prevention)
5. **B4** — Invalid album art URI for albumId 0
6. **G5** — Immutable QueueEntity
7. **G4** — Remove production logging of sensitive data
8. **G2** — Surface rename errors
9. **I3** — Use max position for playlist song ordering
10. **I1** — Filter empty artist names
11. **I2** — Simplify cache TTL logic
12. **I5** — Optimize fingerprint map allocation
13. **I6** — Reduce position update frequency
14. **G3** — Distinguish network error from not-found
15. **I4** — Document `mergeHistoryEvent` intent in-file