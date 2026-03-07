# Phase 7 — Design

## Overview

Phase 7 closed four categories of work: sync correctness gaps carried forward from Phase 6, accumulated UI bugs from Phases 1–5, library search/sort/play-count UX, and a ViewModel architecture refactor before the library grows further. No new libraries were added; all changes use Room, Compose, Material 3, and kotlinx-coroutines already in the project. The phase shipped as four self-contained sub-goals, each verifiable independently.

---

## What was built

### 7.1 — Sync gap closure

- **`data/sync/ConflictResolver.kt`** — replaced `map { ... !! }` with `mapNotNull` in `mergeHistoryEvent`; unknown remote fingerprints are silently dropped instead of throwing a `NullPointerException`.

- **`ui/settings/SettingsEvent.kt`** — added `data object ClearSnackbar` and `data class SignInError(val message: String)`.

- **`ui/settings/SettingsViewModel.kt`** — added a separate `_snackbarMessage: MutableStateFlow<String?>` (not folded into the main `combine`) so snackbar messages are consumed-once. `SignInError` sets it; `ClearSnackbar` nulls it. Exposed as `val snackbarMessage: StateFlow<String?>`.

- **`ui/settings/SettingsScreenContent.kt`** — added `snackbarMessage: String?` and `onSnackbarDismiss: () -> Unit` parameters; `SnackbarHost` in `Scaffold`; `LaunchedEffect(snackbarMessage)` shows the snackbar then calls dismiss. Also added a "Retry" `TextButton` in the trailing content of the sync-status card when `syncStatus is SyncStatus.Error` — fires `SettingsEvent.SyncNow`.

- **`ui/settings/SettingsScreen.kt`** — collects `snackbarMessage` and passes it with `onSnackbarDismiss = { onEvent(ClearSnackbar) }` to `SettingsScreenContent`.

- **`data/local/SyncPlayerDatabase.kt`** — `MIGRATION_5_6` adds `deletedAt INTEGER NOT NULL DEFAULT 0` to `playlists`; version bumped to 6.

- **`di/DatabaseModule.kt`** — `MIGRATION_5_6` added to the builder chain.

- **`data/local/PlaylistDao.kt`** — `PlaylistEntity` gets `val deletedAt: Long = 0`. `getAllPlaylists()` and `getAllPlaylistsList()` filter `WHERE deletedAt = 0`; `getAllPlaylistsList()` variant returns all rows for the orchestrator push loop. Added `softDeletePlaylist(playlistId, deletedAt)` update query.

- **`data/PlaylistRepositoryImpl.kt`** — `deletePlaylist()` calls `softDeletePlaylist(id, System.currentTimeMillis())` instead of hard-deleting the row.

- **`data/sync/SyncOrchestrator.kt`** — push loop includes soft-deleted playlists (those with `deletedAt > 0`) so deletions propagate. Pull logic: remote playlists where `deletedAt > 0` are not created locally if absent; if present, their local row is soft-deleted.

- **`data/sync/SyncRepositoryImpl.kt`** — `pushPlaylist()` serializes `deletedAt` to Firestore.

### 7.2 — Bug fixes

- **`ui/playlists/PlaylistsScreenContent.kt`** — `Loading` branch now renders `CircularProgressIndicator` (was `Icon(Icons.Default.Error)`). Empty-state copy changed from `"No playlists found"` to `"No playlists yet"`.

- **`ui/playlists/PlaylistViewModel.kt`** — `CreatePlaylist` and `RenamePlaylist` handlers trim the name and reject blank strings, so the ViewModel does not trust callers even if dialogs already trim.

- **`data/local/PlaylistDao.kt`** — added `getAllPlaylistsWithCount()` that joins `playlist_songs` and uses `COUNT(ps.songId)` so Room maps directly to `Playlist(id, name, createdAt, songCount)`.

- **`data/PlaylistRepositoryImpl.kt`** — `getAllPlaylists()` delegates to `getAllPlaylistsWithCount()` instead of the bare entity query that dropped `songCount`.

- **`ui/playlists/PlaylistListItem.kt`** — trailing text uses `"${count} song"` / `"${count} songs"` pluralization.

- **`app/src/test/.../ui/player/NowPlayingScreenTest.kt`** — moved from `ui.library` to `ui.player` package with corrected `package` declaration.

- **`app/src/test/.../ui/playlists/PlaylistViewModelTest.kt`** — four new tests covering `AddSongsToPlaylist`, `RemoveSongFromPlaylist`, `RemoveSongsFromPlaylist`, and `ReorderSongs` events. `FakePlaylistRepository` gained `lastRemovedSongId`, `lastReorderedIds`, and a `reorderCallCount`.

### 7.3 — Library UX

- **`data/local/SongDao.kt`** — added `searchSongs(query)`, `searchAlbums(query)`, `searchArtists(query)` with `LIKE '%' || :query || '%'` predicates.

- **`data/SongRepository.kt` / `SongRepositoryImpl.kt`** — interface and implementation extended with the three search methods.

- **`data/FakeSongRepository.kt`** (test) — search methods return the existing `songsFlow`, `albumsFlow`, `artistsFlow` (filtering is a DAO concern in the real implementation).

- **`ui/library/LibraryViewModel.kt`** — added `_searchQuery: MutableStateFlow<String>` and `_sortOrder: MutableStateFlow<SortOrder>`. The `songsFlow` and `albumsFlow` private flows combine `_searchQuery` and `_sortOrder` via `flatMapLatest`, switching between full-list and search-result flows and applying in-memory sort. `artistsFlow` switches on query only. Exposed `onSearchQueryChanged()`, `onClearSearchQuery()`, `onSortOrder()`. The `SortOrder` enum lives in this file: `BY_TITLE`, `BY_ARTIST`, `BY_ALBUM`, `BY_DURATION`, `BY_DATE_ADDED`, `BY_PLAY_COUNT`.

- **`ui/library/LibraryScreen.kt`** — `DockedSearchBar` (Material 3) replaces `TopAppBar` when active, providing a search icon in the top bar that expands inline. Query changes call `viewModel.onSearchQueryChanged()`; closing clears the query. Sort order exposed via `FilterChip` + `DropdownMenu` in both the Songs and Albums tabs.

- **`ui/library/SongListItem.kt`** — trailing content shows play count as `"${song.playCount}×"` in muted color when `playCount > 0`, stacked above the duration.

### 7.4 — Architecture refactor

- **`data/PlayerRepository.kt`** — added `suspend fun clearQueue()` to the interface.

- **`data/PlayerRepositoryImpl.kt`** — implemented `clearQueue()` as `mediaController?.clearMediaItems()` + `queueDao.clearQueue()`.

- **`ui/player/PlayerEvent.kt`** — added `data object ClearQueue`.

- **`ui/player/PlayerViewModel.kt`** — handles `ClearQueue` by launching `playerRepository.clearQueue()`.

- **`ui/player/components/QueueSheet.kt`** — "Clear" `TextButton` in the sheet header fires `onClearQueue: () -> Unit`.

- **`ui/player/NowPlayingScreenContent.kt`** — passes `onClearQueue = { onEvent(PlayerEvent.ClearQueue) }` to `QueueSheet`.

- **`ui/library/MetadataViewModel.kt`** _(new)_ — `@HiltViewModel` that owns `getFavoriteSongs`, `getMostPlayedSongs`, `getRecentlyPlayed`; exposes `MetadataUiState` (`Loading` | `Loaded(favorites, mostPlayed, recentlyPlayed)`).

- **`ui/library/LibraryViewModel.kt`** — stripped of all metadata flows. `LibraryUiState.Loaded` now carries only `songs`, `albums`, `artists`.

- **`ui/library/LibraryScreen.kt`** — collects both `LibraryViewModel` and `MetadataViewModel` separately. Library tabs (Songs, Albums, Artists) read `uiState`; metadata tabs (Faves, Top Plays, Recent) read `metadataState`.

---

## Design decisions

- **Separate `snackbarMessage` flow instead of folding into `uiState` combine** — snackbar messages are consumed-once; if they lived inside the combine, the UI would need to ack them via a state delta, complicating the combine lambda. A standalone `MutableStateFlow<String?>` with a `ClearSnackbar` event keeps the pattern simple and testable.

- **Soft-delete over hard-delete for playlist sync** — hard-deleting a playlist would require calling Firestore at delete time, coupling `PlaylistRepositoryImpl` to the sync layer. Soft-delete (`deletedAt` timestamp) keeps the data boundary clean: the orchestrator's normal push loop reads `deletedAt` and propagates it; the UI queries filter `WHERE deletedAt = 0` and never see deleted rows.

- **`getAllPlaylistsList()` returns all rows (including soft-deleted) for the push loop** — the push loop needs to see deleted playlists to push their `deletedAt` to Firestore. The UI-facing `getAllPlaylists()` (Flow variant) applies `WHERE deletedAt = 0`.

- **In-memory sort via `flatMapLatest` + `map` instead of DAO ORDER BY** — sort order is a runtime preference that can change without a new database query. Sorting in the `map` transform after the `flatMapLatest` avoids extra DAO queries and keeps the sort reactive to `_sortOrder` changes via the `combine(_searchQuery, _sortOrder)` upstream.

- **`songsFlow` and `albumsFlow` both react to `_sortOrder`; `artistsFlow` does not** — albums have two meaningful sort axes (by name, by artist). Artists have no sort axes planned for Phase 7. Extending sort to artists is a one-liner when needed.

- **`MetadataViewModel` split** — `LibraryViewModel` had a 5-source `combine` workaround for metadata that made it harder to test and reason about. Splitting into two ViewModels keeps each `combine` under four sources (the practical limit for readable lambdas) and makes `MetadataViewModelTest` independent. Both ViewModels are activity-scoped so they survive tab switches.

- **`DockedSearchBar` over inline `OutlinedTextField`** — `DockedSearchBar` is the Material 3 recommended pattern for search anchored below the top bar; it provides a standard dismiss gesture and integrates with the system back press. The `OutlinedTextField` alternative would require manual focus management.

---

## Known gaps

- **`songsFlowOld` and `albumsFlowOld` dead code in `LibraryViewModel`** — two private flows left from an intermediate refactor step. They are not wired into any `combine` or exposed, so they do not affect behavior, but they add noise. Removal is a one-line cleanup deferred to Phase 8 housekeeping.

- **Sort for Artists tab** — not implemented (out of scope per plan). The sort dropdown is only shown on Songs and Albums tabs.

- **Search filtering in `FakeSongRepository`** — the fake returns the same flow for `searchSongs` / `searchAlbums` / `searchArtists` as for their full-list counterparts. Tests therefore verify that the ViewModel switches code paths and produces a `Loaded` state, but do not assert on filtered results. True filtering is covered at the DAO layer (SQLite `LIKE` query), which requires an in-memory Room database to test.

- **Playback polish items (7.5)** — custom notification layout, slide-to-rate gesture, listening history screen — deferred to Phase 8.
