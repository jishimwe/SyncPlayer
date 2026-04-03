---
type: plan
feature: phase7
phase: 7
status: complete
tags:
  - type/plan
  - status/complete
  - feature/phase7
---

# Phase 7 — Plan

## Implementation Progress — Phase 7 COMPLETE ✅

### ✅ Task 1.1 — Null-safe `ConflictResolver.mergeHistoryEvent`
- Replaced `map { ... !! }` with `mapNotNull { ... ?: return@mapNotNull null }`
- Test: `unknown fingerprint in remote is silently dropped`

### ✅ Task 1.2 — Sign-in error snackbar
- `SettingsEvent.kt` — added `data object ClearSnackbar`
- `SettingsViewModel.kt` — separate `_snackbarMessage: MutableStateFlow<String?>`, handles `SignInError` and `ClearSnackbar`
- `SettingsScreenContent.kt` — `SnackbarHost` in `Scaffold`, `LaunchedEffect` in content block
- `SettingsScreen.kt` — collects `snackbarMessage`, passes `onSnackbarDismiss = { onEvent(ClearSnackbar) }`
- Tests: `SignInError event sets snackbarMessage`, `ClearSnackbar clears snackbarMessage`

### ✅ Task 1.3 — Sync Error state: "Retry" button
- `SettingsScreenContent.kt` — Retry `TextButton` in `trailingContent` when `syncStatus is SyncStatus.Error`

### ✅ Task 1.4 — Playlist soft-delete sync
- `MIGRATION_5_6` adds `deletedAt INTEGER NOT NULL DEFAULT 0`; version bumped to 6
- `getAllPlaylistsList()` returns all rows (no filter) — orchestrator needs deleted playlists to push
- `getAllPlaylists()` filters `WHERE deletedAt = 0` — UI never sees deleted playlists
- `deletePlaylist()` calls `softDeletePlaylist(playlistId, System.currentTimeMillis())`
- `pushPlaylist()` serializes `deletedAt` to Firestore
- Pull logic: skips creating playlists where `remote.deletedAt != null && remote.deletedAt > 0` and `local == null`; propagates `deletedAt` to local when remote wins
- Song replacement skipped for deleted playlists in remote-wins branch

### ✅ Task 2.1 — Bug 3: Loading state spinner
- `PlaylistsScreenContent.kt` — `CircularProgressIndicator` replaces `Error` icon in `Loading` state

### ✅ Task 2.2 — Bug 5: Empty state copy
- "No playlists found" → "No playlists yet"

### ✅ Task 2.3 — Defensive trim in `PlaylistViewModel`
- Both `CreatePlaylist` and `RenamePlaylist` handlers trim and reject blank names
- Existing tests confirmed coverage; no new tests needed

### ✅ Task 2.4 — Bug 1: songCount always 0
- `getAllPlaylistsWithCount()` DAO query joins `playlist_songs` and counts
- `PlaylistRepositoryImpl.getAllPlaylists()` delegates to new query
- `PlaylistListItem` updated with proper pluralization

### ✅ Task 2.5 — Bug 7: Move `NowPlayingScreenTest.kt`
- Moved from `ui.library` to `ui.player` package

### ✅ Task 2.6 — Bug 8: Missing playlist event tests
- `FakePlaylistRepository` — added `lastRemovedSongId: Long?` and `lastReorderedIds: List<Long>?`
- 4 tests added with `runTest` + `advanceUntilIdle()`: `AddSongsToPlaylist`, `RemoveSongFromPlaylist`, `RemoveSongsFromPlaylist`, `ReorderSongs`

### ✅ Task 3.1 — Search
- DAO: `searchSongs`, `searchAlbums`, `searchArtists` with `LIKE` queries
- Repository + interface updated
- `LibraryViewModel`: `_searchQuery` + `flatMapLatest` switching between full-list and search flows
- UI: `DockedSearchBar` (Material 3 1.4.0 API) replaces `TopAppBar` when active; clears on close

### ✅ Task 3.2 — Sort (Songs + Albums tabs)
- `SortOrder` enum: `BY_TITLE`, `BY_ARTIST`, `BY_ALBUM`, `BY_DURATION`, `BY_DATE_ADDED`, `BY_PLAY_COUNT`
- `combine(_searchQuery, _sortOrder).flatMapLatest` — sort reacts to both query and order changes
- UI: `FilterChip` + `DropdownMenu` in `SongsTab` and `AlbumsTab` (extended beyond plan scope)

### ✅ Task 3.3 — Play count badge on `SongListItem`
- `trailingContent` shows `Column` with play count (when `> 0`) above duration

### ✅ Task 4.1 — Expose `clearQueue()` on `PlayerRepository`
- `PlayerRepository` + `PlayerRepositoryImpl`: `clearQueue()` calls `mediaController?.clearMediaItems()` + `queueDao.clearQueue()`
- `PlayerEvent.ClearQueue` added
- `PlayerViewModel` handles `ClearQueue`
- `QueueSheet`: "Clear" `TextButton` in header, `onClearQueue` parameter
- Test: `ClearQueue event calls playerRepository.clearQueue`

### ✅ Task 4.2 — Split `LibraryViewModel` into `LibraryViewModel` + `MetadataViewModel`
- `MetadataViewModel`: owns `getFavoriteSongs`, `getMostPlayedSongs`, `getRecentlyPlayed`; exposes `MetadataUiState`
- `LibraryUiState.Loaded`: removed `favorites`, `mostPlayed`, `recentlyPlayed`
- `LibraryViewModel`: removed `metadataFlows`, dead `songsFlowOld`/`albumsFlowOld` flows, duplicate sort in `combine` lambda
- `LibraryScreen`: collects both ViewModels separately; metadata tabs receive `metadataState`, library tabs receive `uiState`
- `when { uiState is Loaded && metadataState is Loaded }` pattern for combined rendering

---

## Context

Phases 1–6 are complete. The app plays music, manages playlists, tracks metadata, and syncs it across devices via Firebase. Phase 7 has four ordered sub-goals: close the Phase 6 sync gaps that affect correctness, fix the accumulated bugs from Phases 1–5, add search/sort/play-count UX to the library, and refactor `LibraryViewModel` before adding more flows.

Sub-goals are ordered by impact and risk: each section can be implemented and shipped independently; later sections do not depend on earlier ones except where noted.

**Pre-flight audit of the open-bugs list:** three of the eight listed bugs were already fixed during implementation:
- Bug 2 (date formatting): `PlaylistListItem` already formats `createdAt` with `DateTimeFormatter`.
- Bug 4 (name trimming): both `CreatePlaylistDialog` and `RenamePlaylistDialog` already call `.trim()` before firing events.
- Bug 6 (rating downgrade): `StarRating` already uses `if (rating == star) NONE else star`, so tapping a lower star sets it directly.

These are confirmed in code; no action required. Remaining bugs: 1, 3, 5, 7, 8.

---

## Scope

**Included:**

**7.1 — Sync gap closure**
- Playlist soft-delete sync (Room migration 5→6)
- Sign-in error snackbar in SettingsScreen
- "Retry" button in sync Error state
- Null-safe `ConflictResolver.mergeHistoryEvent`

**7.2 — Bug fixes**
- Bug 1: `PlaylistRepositoryImpl.getAllPlaylists()` drops `songCount` → always 0
- Bug 3: Playlist Loading state shows Error icon instead of `CircularProgressIndicator`
- Bug 5: Empty playlist copy "No playlists found" → "No playlists yet"
- Bug 7: Move `NowPlayingScreenTest.kt` from `ui.library` to `ui.player`
- Bug 8: Add 4 missing playlist event tests to `PlaylistViewModelTest`
- Hardening: add defensive `.trim()` in `PlaylistViewModel` (dialogs already trim, but ViewModel should not trust callers)

**7.3 — Library UX**
- Full-text search across songs, albums, artists (Room `LIKE` query + `flatMapLatest` in ViewModel)
- In-memory sort for Songs tab: by name (default), date added, play count
- Play count badge on `SongListItem` (shown only when `playCount > 0`)

**7.4 — Architecture refactor**
- Split `LibraryViewModel` into `LibraryViewModel` (songs/albums/artists + scan) and `MetadataViewModel` (favorites/mostPlayed/recentlyPlayed)
- Expose `clearQueue()` on `PlayerRepository` interface + `ClearQueue` `PlayerEvent` + "Clear all" button in `QueueSheet`

**Excluded:**
- 7.5 playback polish items (custom notification layout, slide rating gesture, history screen) — low priority, no plan doc needed yet
- External integrations (Phase 8)
- Audio focus edge case verification (manual, device-only)

---

## Approach

Each sub-section is a self-contained batch of changes. Implement one section at a time, run `assembleDebug` + `test` after each, and commit before starting the next. Within a section, follow the standard layer order: DB → data → ViewModel → UI → tests.

---

## Tasks

### 7.1 — Sync gap closure

#### Task 1.1 — Null-safe `ConflictResolver.mergeHistoryEvent` (trivial)

**Why first:** one-liner, zero risk, fixes a latent crash.

**File:** `data/sync/ConflictResolver.kt`

Replace the `map { ... !! }` with `mapNotNull`:

```kotlin
// Before
val remoteHistoryEntity = remote.map {
    ListeningHistoryEntity(
        songId = fingerPrintToSongId[it.fingerprint]!!,
        playedAt = it.playedAt,
    )
}

// After
val remoteHistoryEntity = remote.mapNotNull {
    val songId = fingerPrintToSongId[it.fingerprint] ?: return@mapNotNull null
    ListeningHistoryEntity(songId = songId, playedAt = it.playedAt)
}
```

**Tests:** Update `ConflictResolverTest.MergeHistoryEvent` — add one test: `unknown fingerprint in remote is silently dropped`.

**Build check:** `assembleDebug`

---

#### Task 1.2 — Sign-in error snackbar

**Why:** `SettingsEvent.SignInError` is currently a no-op; users get no feedback on failed sign-in.

**Approach:** `snackbarMessage` is transient (consumed-once), so use a separate `StateFlow<String?>` rather than folding it into the `combine`. The composable reads it, shows the snackbar, then calls a `clearSnackbar` event to reset.

**Files:**
- `ui/settings/SettingsViewModel.kt` — add `private val _snackbarMessage = MutableStateFlow<String?>(null)` and `val snackbarMessage = _snackbarMessage.asStateFlow()`; handle `SignInError` by setting `_snackbarMessage.value = event.message`; add `ClearSnackbar` to `SettingsEvent` (or expose a `fun clearSnackbar()` method).
- `ui/settings/SettingsEvent.kt` — add `data object ClearSnackbar : SettingsEvent` (or keep as a method; discuss at plan time).
- `ui/settings/SettingsScreenContent.kt` — add `snackbarMessage: String?` and `onSnackbarDismiss: () -> Unit` parameters; add `SnackbarHost` to the `Scaffold`; show `LaunchedEffect(snackbarMessage) { snackbarMessage?.let { snackbarHostState.showSnackbar(it); onSnackbarDismiss() } }`.
- `ui/settings/SettingsScreen.kt` — pass `snackbarMessage` from ViewModel and `onSnackbarDismiss` to content.

**Tests:** Add to `SettingsViewModelTest`:
- `SignInError event sets snackbarMessage`
- `ClearSnackbar (or clearSnackbar()) clears snackbarMessage`

**Build check:** `assembleDebug` + `test`

---

#### Task 1.3 — Sync Error state: "Retry" button

**Why:** When sync fails, the user currently has to tap "Sync Now". A visible "Retry" button in the error card removes that friction.

**Approach:** No new logic needed in `SyncOrchestrator` — `syncIfSignedIn()` already retries on next `onResume`, and `SyncNow` event already triggers a manual sync. The fix is purely UI: render a "Retry" button when `syncStatus is SyncStatus.Error`.

**Files:**
- `ui/settings/SettingsScreenContent.kt` — in the sync status card, when state is `SyncStatus.Error`, show an error message and a "Retry" `TextButton` that fires `SettingsEvent.SyncNow`.

No ViewModel changes. No new tests needed (retry is wired through the existing `SyncNow` event path, already tested).

**Build check:** `assembleDebug`

---

#### Task 1.4 — Playlist soft-delete sync (Room migration 5→6)

**Why:** Deleted playlists are not synced. If a playlist is deleted on device A, it reappears on pull from device B.

**Approach:** Soft-delete — mark playlists as deleted (`deletedAt` timestamp) instead of removing the Room row. The orchestrator pushes the `deletedAt` value to Firestore; on pull, remote playlists with `deletedAt` set are either deleted locally or not created.

**Why not hard-delete with Firestore call at delete time:** it would require injecting `SyncRepository` into `PlaylistRepositoryImpl`, coupling data and sync layers. Soft-delete keeps the boundary clean.

**Layer 1 — DB migration:**
- `data/local/SyncPlayerDatabase.kt` — add `MIGRATION_5_6`:
  ```kotlin
  val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE playlists ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
      }
  }
  ```
  Bump version to 6 and add the migration.
- `di/DatabaseModule.kt` — add `MIGRATION_5_6` to the builder.

**Layer 2 — Entity + DAO:**
- `data/local/PlaylistDao.kt` — update `PlaylistEntity` to add `val deletedAt: Long = 0`. Update `getAllPlaylists()` query to `WHERE deletedAt = 0`. Update `getAllPlaylistsList()` similarly. Add `suspend fun softDeletePlaylist(playlistId: Long, deletedAt: Long)` query.

**Layer 3 — Repository:**
- `data/PlaylistRepositoryImpl.kt` — `deletePlaylist()`: call `playlistDao.softDeletePlaylist(playlistId, System.currentTimeMillis())` instead of `clearPlaylistSongs + deletePlaylist`. Keep `clearPlaylistSongs` for cases where we want to also remove cross-refs (they'll be filtered by the soft-delete anyway since the playlist won't appear in queries).

**Layer 4 — Orchestrator:**
- `data/sync/SyncOrchestrator.kt` — `push()`: include soft-deleted playlists (those with `deletedAt > 0`) in the push loop, using `pushPlaylist()` which should write `deletedAt` to Firestore. Filter: push any playlist where `lastModified > lastSync` regardless of `deletedAt` status.
- `SyncOrchestrator.pull()`: when iterating remote playlists, skip those with `remote.deletedAt != null && remote.deletedAt > 0` for creation; if the playlist exists locally, soft-delete it locally.
- `data/sync/SyncRepositoryImpl.kt` — `pushPlaylist()`: include `deletedAt` field when serializing to Firestore.
- `data/sync/FirestoreModels.kt` — `FirestorePlaylist.deletedAt` already exists (confirmed in design doc). No change needed.

**Build check:** `assembleDebug`

---

### 7.2 — Bug fixes

All bugs are self-contained. Order doesn't matter; do them smallest-to-largest.

#### Task 2.1 — Bug 3: Loading state spinner (trivial)

**File:** `ui/playlists/PlaylistsScreenContent.kt`

```kotlin
// Before
is PlaylistUiState.Loading -> {
    Icon(Icons.Default.Error, contentDescription = null)
}

// After
is PlaylistUiState.Loading -> {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
```

**Build check:** `assembleDebug`

---

#### Task 2.2 — Bug 5: Empty state copy (trivial)

**File:** `ui/playlists/PlaylistsScreenContent.kt`

Change `EmptyState("No playlists found")` → `EmptyState("No playlists yet")`.

**Build check:** `assembleDebug`

---

#### Task 2.3 — Bug 4 hardening: defensive trim in PlaylistViewModel (trivial)

The dialogs already trim, but the ViewModel should not trust callers.

**File:** `ui/playlists/PlaylistViewModel.kt`

```kotlin
is PlaylistEvent.CreatePlaylist -> {
    val trimmed = event.name.trim()       // add
    if (trimmed.isNotBlank()) {           // change
        playlistRepository.createPlaylist(trimmed)
    }
}
is PlaylistEvent.RenamePlaylist -> {
    val trimmed = event.newName.trim()    // add
    if (trimmed.isNotBlank()) {           // change
        playlistRepository.renamePlaylist(event.playlistId, trimmed)
    }
}
```

**Tests:** Update `PlaylistViewModelTest` — update blank-name tests to use whitespace-only strings (e.g., `"   "`) to confirm the VM trims and rejects them.

**Build check:** `assembleDebug` + `test`

---

#### Task 2.4 — Bug 1: songCount always 0

**Root cause:** `PlaylistRepositoryImpl.getAllPlaylists()` maps `PlaylistEntity` to `Playlist(it.id, it.name, it.createdAt)` — drops `songCount`.

**Fix:** Add a DAO query that joins and counts in a single round-trip.

**File:** `data/local/PlaylistDao.kt` — read the `Playlist` model first to confirm field names, then add:

```kotlin
@Query("""
    SELECT p.id, p.name, p.createdAt, COUNT(ps.songId) AS songCount
    FROM playlists p
    LEFT JOIN playlist_songs ps ON p.id = ps.playlistId
    WHERE p.deletedAt = 0
    GROUP BY p.id
    ORDER BY p.name ASC
""")
fun getAllPlaylistsWithCount(): Flow<List<Playlist>>
```

**File:** `data/PlaylistRepositoryImpl.kt` — replace `getAllPlaylists()` body with `playlistDao.getAllPlaylistsWithCount()`.

**Note:** Confirm `Playlist` model field names before writing the query. Room maps query result columns to constructor params by name; if `Playlist` uses different field names, add column aliases.

**Also update `PlaylistListItem`:** the supporting text currently shows `playlist.songCount.toString()` — add proper pluralization: `"${playlist.songCount} ${if (playlist.songCount == 1) "song" else "songs"}"`.

**Build check:** `assembleDebug`

---

#### Task 2.5 — Bug 7: Move `NowPlayingScreenTest.kt` to correct package

**Current location:** `app/src/test/java/com/jpishimwe/syncplayer/ui/library/NowPlayingScreenTest.kt`
**Correct location:** `app/src/test/java/com/jpishimwe/syncplayer/ui/player/NowPlayingScreenTest.kt`

Steps:
1. Create new file at the correct path.
2. Update `package` declaration from `com.jpishimwe.syncplayer.ui.library` to `com.jpishimwe.syncplayer.ui.player`.
3. Copy content (all imports and test cases are already correct since they import from `ui.player`).
4. Delete the old file.

**Build check:** `assembleDebug` + `test` — confirm tests appear under `ui.player` in results.

---

#### Task 2.6 — Bug 8: Missing playlist event tests

**File:** `app/src/test/java/com/jpishimwe/syncplayer/ui/playlists/PlaylistViewModelTest.kt`

Add four tests:

1. `AddSongsToPlaylist event calls addSongToPlaylist for each song id` — verify `FakePlaylistRepository.addSongCallCount == event.songIds.size`.
2. `RemoveSongFromPlaylist event calls removeSongFromPlaylist with correct ids` — verify `removeSongCallCount == 1` and correct `playlistId`/`songId`.
3. `RemoveSongsFromPlaylist event calls removeSongFromPlaylist for each song` — verify call count equals list size.
4. `ReorderSongs event calls reorderSongs with correct playlistId and ordered list` — verify `reorderCallCount == 1` and `lastReorderedIds` matches.

**Note:** Check `FakePlaylistRepository` has the needed counters (`addSongCallCount`, `removeSongCallCount`, `reorderCallCount`). From the existing code, `addSongCallCount` and `removeSongCallCount` exist; verify `lastRemovedSongId` and `lastReorderedIds` are present or add them.

**Build check:** `test`

---

### 7.3 — Library UX

#### Task 3.1 — Search

**Layer 1 — DAO:**
- `data/local/SongDao.kt` — add:
  ```kotlin
  @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY title ASC")
  fun searchSongs(query: String): Flow<List<Song>>

  @Query("""SELECT albumId AS id, album AS name, artist, COUNT(*) AS songCount, albumArtUri
            FROM songs WHERE album LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'
            GROUP BY albumId ORDER BY album ASC""")
  fun searchAlbums(query: String): Flow<List<Album>>

  @Query("""SELECT artist AS name, COUNT(*) AS songCount, COUNT(DISTINCT albumId) AS albumCount
            FROM songs WHERE artist LIKE '%' || :query || '%'
            GROUP BY artist ORDER BY artist ASC""")
  fun searchArtists(query: String): Flow<List<Artist>>
  ```

**Layer 2 — Repository:**
- `data/SongRepository.kt` — add `fun searchSongs(query: String): Flow<List<Song>>`, `searchAlbums`, `searchArtists`.
- `data/SongRepositoryImpl.kt` — implement by delegating to DAO.
- `data/FakeSongRepository.kt` (test) — add stub implementations returning `songsFlow`, `albumsFlow`, `artistsFlow` (existing flows, search filtering handled in DAO in real impl).

**Layer 3 — ViewModel:**
- `ui/library/LibraryViewModel.kt` — add `private val _searchQuery = MutableStateFlow("")` and `val searchQuery = _searchQuery.asStateFlow()`. Add `fun onSearchQueryChanged(query: String)`.

  Replace the three source flows in the `uiState` combine with `flatMapLatest` to switch between full-list and search-result flows:
  ```kotlin
  private val songsFlow = _searchQuery.flatMapLatest { q ->
      if (q.isBlank()) songRepository.getAllSongs() else songRepository.searchSongs(q)
  }
  private val albumsFlow = _searchQuery.flatMapLatest { q ->
      if (q.isBlank()) songRepository.getAllAlbums() else songRepository.searchAlbums(q)
  }
  private val artistsFlow = _searchQuery.flatMapLatest { q ->
      if (q.isBlank()) songRepository.getAllArtists() else songRepository.searchArtists(q)
  }
  ```
  Then use these private flows in the existing `combine(songsFlow, albumsFlow, artistsFlow, refreshError, metadataFlows)` — no change to the combine signature.

  Add `fun clearSearch()` convenience method.

**Layer 4 — UI:**
- `ui/library/LibraryScreen.kt` — add a search icon (`Icons.Default.Search`) to `TopAppBar` actions. Clicking expands a `DockedSearchBar` (Material 3) or an `OutlinedTextField` below the top bar. Observe `viewModel.searchQuery` and call `viewModel.onSearchQueryChanged(it)` on value change. When search is active, show results across the currently-selected tab (songs/albums/artists). Clear on close.

**Tests:**
- `LibraryViewModelTest` (existing) — add: `search query filters songs list`, `clearing search restores full list`.

**Build check:** `assembleDebug` + `test`

---

#### Task 3.2 — Sort (Songs tab)

**Layer 3 — ViewModel:**
- `ui/library/LibraryViewModel.kt` — add:
  ```kotlin
  enum class SortOrder { BY_TITLE, BY_DATE_ADDED, BY_PLAY_COUNT }
  private val _sortOrder = MutableStateFlow(SortOrder.BY_TITLE)
  val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()
  fun setSortOrder(order: SortOrder)
  ```
  Apply sort in the `combine` lambda before constructing `LibraryUiState.Loaded`:
  ```kotlin
  val sortedSongs = when (sortOrder) { // sortOrder passed via combine or read from _sortOrder directly
      SortOrder.BY_TITLE -> songs.sortedBy { it.title.lowercase() }
      SortOrder.BY_DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
      SortOrder.BY_PLAY_COUNT -> songs.sortedByDescending { it.playCount }
  }
  ```
  Add `_sortOrder` as a 6th source to the combine, or read `_sortOrder.value` inside the lambda (simpler; avoids exceeding combine param limit).

**Layer 4 — UI:**
- `ui/library/LibraryScreen.kt` — add a sort dropdown (three-dot menu or `ExposedDropdownMenuBox`) in `LibraryScreenContent`, visible only on the Songs tab. Calls `onTabSelected`-style callback to set sort order.

**Tests:** `LibraryViewModelTest` — add: `sort by play count orders songs descending`, `sort by date added orders songs descending`.

**Build check:** `assembleDebug` + `test`

---

#### Task 3.3 — Play count badge on `SongListItem`

Show play count only when `playCount > 0`. Add it to the `SongListItem` trailing content (small `Text` with muted color).

**File:** `ui/library/SongListItem.kt`

Read the file first to see current structure, then add a `trailingContent` (or append to existing) that shows `"${song.playCount}×"` or a count badge. Show only in the Top Plays tab context, or always when > 0 — decide based on existing `SongListItem` signature (if it takes a `Song`, use `song.playCount`; if it does not expose the count, expose it).

**Build check:** `assembleDebug`

---

### 7.4 — Architecture refactor

#### Task 4.1 — Expose `clearQueue()` on `PlayerRepository`

**Layer 2 — Interface + implementation:**
- `data/PlayerRepository.kt` — add `suspend fun clearQueue()`.
- `data/PlayerRepositoryImpl.kt` — the existing private `clearQueue()` method (if present); if not, implement it as `mediaController?.clearMediaItems()` or equivalent Media3 call. Make it public.

**Layer 3 — ViewModel + event:**
- `ui/player/PlayerEvent.kt` — add `data object ClearQueue : PlayerEvent`.
- `ui/player/PlayerViewModel.kt` — handle `ClearQueue` in `onEvent`: `viewModelScope.launch { playerRepository.clearQueue() }`.

**Layer 4 — UI:**
- `ui/player/components/QueueSheet.kt` — add a "Clear all" `TextButton` in the sheet header row (next to the "Queue" title). Calls a new `onClearQueue: () -> Unit` parameter passed from `NowPlayingScreenContent`.
- `ui/player/NowPlayingScreenContent.kt` — pass `onClearQueue = { onEvent(PlayerEvent.ClearQueue) }` to `QueueSheet`.

**Tests:** `PlayerViewModelTest` — add: `ClearQueue event calls playerRepository.clearQueue`.

**Build check:** `assembleDebug` + `test`

---

#### Task 4.2 — Split `LibraryViewModel` into `LibraryViewModel` + `MetadataViewModel`

**Why now:** Adding search (Task 3.1) and sort (Task 3.2) adds more state to `LibraryViewModel`. Before it grows further, split it cleanly. The current 2-level `combine` workaround works but is harder to test.

**New structure:**

`MetadataViewModel` (new file: `ui/library/MetadataViewModel.kt`):
```kotlin
@HiltViewModel
class MetadataViewModel @Inject constructor(
    private val songRepository: SongRepository,
) : ViewModel() {
    val uiState: StateFlow<MetadataUiState> = combine(
        songRepository.getFavoriteSongs(),
        songRepository.getMostPlayedSongs(),
        songRepository.getRecentlyPlayed(),
    ) { favorites, mostPlayed, recentlyPlayed ->
        MetadataUiState.Loaded(favorites, mostPlayed, recentlyPlayed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetadataUiState.Loading)
}

sealed interface MetadataUiState {
    data object Loading : MetadataUiState
    data class Loaded(
        val favorites: List<Song>,
        val mostPlayed: List<Song>,
        val recentlyPlayed: List<Song>,
    ) : MetadataUiState
}
```

`LibraryViewModel` (simplified):
- Remove `metadataFlows`, `getFavoriteSongs`, `getMostPlayedSongs`, `getRecentlyPlayed` from the combine.
- `LibraryUiState.Loaded` no longer carries metadata lists — it only has `songs`, `albums`, `artists`.
- Keep `selectedTab`, `isRefreshing`, search, sort.

`LibraryScreen.kt`:
- Obtain both ViewModels via `hiltViewModel(LocalActivity.current as ViewModelStoreOwner)` (activity-scoped so they survive tab switches).
- Combine `uiState` from `LibraryViewModel` and `MetadataViewModel` in the composable for the tabs that need metadata.
- OR observe them separately and pass the relevant list to each tab composable.

`LibraryScreenContent` signature update:
- Accept a `metadataState: MetadataUiState` parameter in addition to `uiState: LibraryUiState`.
- Each metadata tab (`FavoriteTab`, `MostPlayedTab`, `RecentlyPlayedTab`) reads from `metadataState`.

**Impact on tests:**
- `LibraryViewModelTest` — remove metadata-related test cases (move to new `MetadataViewModelTest`).
- New `MetadataViewModelTest` — covers all three metadata flows using `FakeSongRepository`.

**Build check:** `assembleDebug` + `test`

---

## Dependencies

No new libraries required for any task in Phase 7. All features use existing Room, Compose, Material 3, and kotlinx-coroutines APIs already in the project.

---

## Open questions

1. **Search UI placement:** `DockedSearchBar` (anchored below TopAppBar) or a search icon that expands inline in the TopAppBar? `DockedSearchBar` is more discoverable; inline is more compact. Read the Material 3 search guidelines and pick one at implementation time.

2. **Sort scope:** Sort for Songs tab only, or also Albums (by name / song count) and Artists (by name / song count)? Start with Songs only; extend later if needed.

3. **Play count display:** Always show on `SongListItem` when > 0 (all tabs), or only in Top Plays tab? Showing everywhere gives more context; showing only in Top Plays keeps the list clean. Recommendation: show on all tabs but only when > 0.

4. **`MetadataViewModel` activity scope:** After the split, both ViewModels are activity-scoped. Confirm that `hiltViewModel(LocalActivity.current as ViewModelStoreOwner)` in both `LibraryScreen` and any screen that needs metadata (e.g., `NowPlayingScreen` if it ever shows "similar songs") works correctly with Hilt.

5. **Soft-delete and query performance:** The `WHERE deletedAt = 0` filter on the playlists queries will be evaluated on every read. Confirm there is an index on `deletedAt` or that the playlist table is small enough that a full-scan is acceptable (it is — playlist counts are in the tens, not millions).

---

## Verification

- `gradlew.bat assembleDebug` after every task
- `gradlew.bat test` after Tasks 1.1, 1.2, 2.3, 2.5, 2.6, 3.1, 3.2, 4.1, 4.2

### Manual verification checklist

**7.1 — Sync gap closure**
- [ ] Delete a playlist on device A → sync → reopen on device B → playlist is gone
- [ ] Sign in with invalid token → snackbar appears with error message
- [ ] Sync fails → "Retry" button visible → tap → sync retries
- [ ] Sync with a remote history event using unknown fingerprint → no crash, event is silently dropped

**7.2 — Bugs**
- [ ] Playlist list shows correct song count per playlist (not 0)
- [ ] Song count shows "1 song" / "3 songs" (correct pluralization)
- [ ] Playlist loading state shows spinner (not error icon)
- [ ] Empty playlist screen shows "No playlists yet"
- [ ] Creating playlist with `"  My List  "` saves as `"My List"` (VM trim hardening)
- [ ] `gradlew.bat test` — NowPlaying tests appear under `ui.player` package
- [ ] `gradlew.bat test` — 4 new playlist event tests pass

**7.3 — Library UX**
- [ ] Searching "Beatles" in Songs tab filters to matching songs
- [ ] Clearing search restores full song list
- [ ] Sorting by Play Count orders songs highest-first
- [ ] Songs with playCount > 0 show the count on the list item

**7.4 — Architecture**
- [ ] "Clear all" button in QueueSheet clears the queue
- [ ] After LibraryViewModel split, all 6 tabs (Songs, Albums, Artists, Faves, Top Plays, Recent) render correctly
- [ ] Navigating between Library and other screens and back preserves tab state
---

**Design doc**: [[phase7-design]]
