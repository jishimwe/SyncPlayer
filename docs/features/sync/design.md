# Sync — Design

## Overview

Phase 6 adds cross-device metadata sync to SyncPlayer. Users sign in with Google via Firebase Auth and their play counts, ratings, last-played timestamps, playlists, and listening history are synced to Firestore. Songs themselves are not transferred — only metadata. Sync is triggered automatically on each app resume (if signed in) and manually from the Settings screen. Conflict resolution rules keep data consistent without user intervention.

---

## What was built

### Data layer
- `data/sync/AuthRepository.kt` — interface exposing `authState: StateFlow<AuthState>` and `signInWithToken`/`signOut` suspend functions.
- `data/sync/AuthRepositoryImpl.kt` — Firebase Auth implementation; wraps `FirebaseAuth.AuthStateListener` in a `StateFlow` scoped to the application lifecycle.
- `data/sync/SyncRepository.kt` — interface for all Firestore I/O: push/pull song metadata, playlists, and history events.
- `data/sync/SyncRepositoryImpl.kt` — Firestore implementation; uses set-with-merge for song metadata upserts and auto-generated document IDs for history (append-only).
- `data/sync/SyncOrchestrator.kt` — singleton that drives the full sync cycle; emits `SyncStatus` (Idle / Syncing / Success / Error) as a `StateFlow`; stores `lastSyncTime` in `SharedPreferences` to enable incremental pushes.
- `data/sync/ConflictResolver.kt` — pure object with three functions covering song metadata merge, playlist last-write-wins, and history append-only union.
- `data/sync/SongFingerprint.kt` — pure object computing an 8-byte SHA-256 hex fingerprint from normalised `title|artist|album|durationBucket`.
- `data/sync/FirestoreModels.kt` — `FirestoreSongMetadata`, `FirestorePlaylist`, `FirestorePlaylistSong`, `FirestoreHistoryEvent`; all have no-arg constructors for Firestore deserialization.

### Database
- `data/local/SyncPlayerDatabase.kt` — bumped to version 5; added `MIGRATION_4_5` that adds `lastModified INTEGER NOT NULL DEFAULT 0` to `songs` and `playlists`, and `remoteId TEXT` to `playlists`. Switched from `fallbackToDestructiveMigration` to explicit migrations.
- `model/Song.kt` — added `lastModified: Long = 0`.
- `data/local/PlaylistEntity` — added `remoteId: String?` and `lastModified: Long = 0`.
- `data/local/SongDao.kt` — added `applySyncDelta` to update play count, rating, last-played, and last-modified without touching unrelated fields.
- `data/local/ListeningHistoryDao.kt` — added `getHistorySince(since: Long)` for incremental history push.
- `data/local/PlaylistDao.kt` — added `getAllPlaylistsList`, `getSongsForPlaylistList`, `replacePlaylistSongs`, `clearPlaylistSongs` for sync operations.

### DI
- `di/SyncModule.kt` — provides `FirebaseAuth`, `FirebaseFirestore` (with persistent cache enabled), `ApplicationScope` (`SupervisorJob + Dispatchers.IO`), and binds both repository interfaces to their implementations.
- `di/DatabaseModule.kt` — updated to supply `MIGRATION_4_5` and all new DAOs.

### UI
- `ui/settings/SettingsUiState.kt` — `data class` combining `AuthState`, `SyncStatus`, and nullable `lastSyncTime`.
- `ui/settings/SettingsEvent.kt` — sealed interface with `SignInWithToken`, `SignInError`, `SignOut`, `SyncNow`.
- `ui/settings/SettingsViewModel.kt` — combines `authRepository.authState` and `syncOrchestrator.syncStatus` into a single `StateFlow<SettingsUiState>` using `WhileSubscribed(5_000)`.
- `ui/settings/SettingsScreen.kt` — owns the `CredentialManager` / `GetGoogleIdOption` / `GoogleIdTokenCredential` flow; dispatches the resulting ID token (or error) to the ViewModel as an event.
- `ui/settings/SettingsScreenContent.kt` — stateless composable showing sign-in / sign-out button, account info, sync status card, last sync timestamp, and "Sync Now" button.
- `MainActivity.kt` — injects `SyncOrchestrator` and calls `syncIfSignedIn()` from `onResume`.

### Tests
- `data/sync/SongFingerprintTest.kt` — 10 unit tests covering determinism, 16-char hex output, case and whitespace normalisation, 2-second duration bucketing, and uniqueness per field.
- `data/sync/ConflictResolverTest.kt` — 14 unit tests across three `@Nested` classes covering all resolution rules and edge cases.
- `data/sync/FakeAuthRepository.kt` — in-memory `AuthRepository` fake with observable call counts and a `MutableStateFlow` for auth state.
- `ui/settings/SettingsViewModelTest.kt` — 10 unit tests covering all `SettingsEvent` types and all `SyncStatus` transitions; uses `FakeAuthRepository` + mockk for `SyncOrchestrator`.

---

## Design decisions

- **Song fingerprint instead of file path** — file paths and MediaStore IDs differ across devices for the same physical track. SHA-256 of normalised `title|artist|album|durationBucket` is device-agnostic and computable offline.

- **Duration bucketed to 2-second windows** — minor encoding-length differences between rips of the same track (e.g. 3:22.1 vs 3:22.4) are absorbed by `(durationMs / 2000) * 2`. This avoids false mismatches at the cost of theoretically merging tracks that differ by less than 2 seconds; in practice such tracks also share title/artist/album and are the same song.

- **Conflict resolution rules chosen per field semantics**
  - `playCount`: max-wins — play count can only grow; taking the higher value from either side is always correct.
  - `rating`: last-write-wins by `lastModified` — a user actively sets a rating; the most recent intent should win.
  - `lastPlayed`: max-wins — the most recent play is ground truth regardless of which device recorded it.
  - Playlists: last-write-wins by `lastModified` — playlist shape is user-intent; the most recent edit wins. Remote wins on tie to trust the server clock.
  - Listening history: append-only union, deduplicated by `(songId, playedAt)` — history is immutable log data; merging preserves all events without double-counting.

- **`SyncOrchestrator` is a concrete singleton, not an interface** — it was not extracted to an interface because only `SettingsViewModel` and `MainActivity` consume it. Tests mock it with mockk rather than a fake to avoid needing to subclass through an Android `Context` constructor.

- **Google Sign-In via Credential Manager, not `GoogleSignInClient`** — `GoogleSignInClient` is deprecated. Credential Manager (`androidx.credentials`) is the current recommendation for Min SDK 34 and integrates with passkeys in future.

- **Context-dependent sign-in code lives in the composable, not the ViewModel** — `CredentialManager`, `ActivityResultLauncher`, and `GetGoogleIdOption` all need Android `Context` or `Activity`. The composable does the work and passes only the resulting ID token (or error string) to the ViewModel as a sealed event. This keeps the ViewModel fully unit-testable.

- **`lastSyncTime` stored in `SharedPreferences`** — only the timestamp needs to persist across process death. Using Preferences is simpler than a Room table for a single scalar value and avoids a schema dependency.

- **Firestore offline persistence** — enabled by default on Android via `FirebaseFirestore.getInstance()`. No explicit configuration needed; documents written while offline are queued and flushed automatically on reconnect.

- **Incremental push via `lastSyncTime`** — only songs and playlists modified since the last successful sync are pushed, keeping Firestore write costs proportional to activity rather than library size.

- **Listening history pull without conflict resolver** — the `SyncOrchestrator.pull()` inserts remote history events directly via `listeningHistoryDao.insertListeningHistory`, relying on the DAO to deduplicate. `ConflictResolver.mergeHistoryEvent` was built but is not called in the current push/pull path; it is available for a batch-reconcile use case in a future phase.

---

## Known gaps

- **Playlist deletion is not synced** — `deletedAt` field exists on `FirestorePlaylist` but delete events are never written or acted on. A deleted playlist on one device will reappear on pull from another.
- **`ConflictResolver.mergeHistoryEvent` uses `!!`** — unknown fingerprints cause a `NullPointerException`. The calling code in `SyncOrchestrator` filters unknowns with `?: continue` before inserting; the resolver itself does not guard against this, so callers must ensure the fingerprint map is complete.
- **Sign-in errors are Logcat-only** — `SettingsEvent.SignInError` is a no-op in the ViewModel. A snackbar or error state slot is deferred to Phase 7.
- **No retry logic on sync failure** — `SyncStatus.Error` is emitted and the user must tap "Sync Now" to retry. Exponential back-off retry is deferred.
- **No per-song merge for playlists** — when remote wins, the full song list is replaced. Songs present locally but not on the remote snapshot (e.g. due to the song not existing on the remote device) are dropped from the playlist.
