---
type: plan
feature: testing
phase: 7
status: complete
tags:
  - type/plan
  - status/complete
  - feature/testing
---

# Testing Master Plan

> **Status: Implemented** — All five tiers below were completed. See [`docs/features/testing/design.md`](design.md) for the design doc recording what was actually built.

## Context

This document audits every test in the project, evaluates its quality and relevance, identifies every gap by test type, and proposes a prioritized plan for closing them. It is written after Phase 7 is complete (Phases 1–7 all shipped).

The project had ~60 individual test methods across 8 test files at the time of writing. Of those, 7 were meaningful and 1 was an empty stub. All existing tests were JVM unit tests or Compose UI tests. No Room DAO tests and no SyncOrchestrator integration tests existed yet.

---

## Current Test Inventory

### Unit Tests (JVM — `app/src/test`)

| File | Class | Tests | What it covers |
|------|-------|-------|----------------|
| `data/sync/ConflictResolverTest.kt` | `ConflictResolverTest` | 13 | All resolution rules: playCount max-wins, rating LWW, lastPlayed max-wins, lastModified max; playlist LWW; history merge, dedup, fingerprint mapping, unknown fingerprint drop |
| `data/sync/SongFingerprintTest.kt` | `SongFingerprintTest` | 11 | Determinism, 16-char hex format, lowercase normalization, whitespace trimming, 2-second duration bucketing, differentiation by title/artist/album |
| `ui/settings/SettingsViewModelTest.kt` | `SettingsViewModelTest` | 12 | All `SettingsEvent` types, all `SyncStatus` transitions, `lastSyncTime` derivation, snackbar lifecycle |
| `ui/library/LibraryViewModelTest.kt` | `LibraryViewModelTest` | 10 | State → Loaded, empty state, tab selection, refresh (normal + error), isRefreshing, search-active state, search-cleared state, sort by playCount, sort by dateAdded |
| `ui/library/LibraryViewModelMetadataTest.kt` | `MetadataViewModelTest` | 3 | `MetadataViewModel.uiState` reflects favorites, mostPlayed, recentlyPlayed |
| `ui/playlists/PlaylistViewModelTest.kt` | `PlaylistViewModelTest` | 11 | State → Loaded, empty state, CreatePlaylist (valid + blank), RenamePlaylist (valid + blank), DeletePlaylist, AddSongsToPlaylist, RemoveSongFromPlaylist, RemoveSongsFromPlaylist, ReorderSongs |
| `ui/library/PlayerViewModelTest.kt` | `PlayerViewModelTest` | 17 | PlayPause (play and pause paths), SkipToNext, SkipToPrevious, SeekTo, ToggleShuffle, ToggleRepeat, PlaySongs (default + startIndex), SeekToQueueItem, formatTime (3 cases), uiState passthrough, SetRating (with song + without song), ClearQueue |

### Compose UI Tests (JVM — `app/src/test`)

| File | Class | Tests | What it covers |
|------|-------|-------|----------------|
| `ui/player/NowPlayingScreenContentTest.kt` | `NowPlayingScreenContentTest` | 4 | FavoriteButton: emits `SetRating(FAVORITE)` when unrated, emits `SetRating(NONE)` when rated, content description "Add to favorites" when NONE, content description "Remove from favorites" when FAVORITE |

### Empty / Stubs

| File | Status | Note |
|------|--------|------|
| `ui/library/PlayerRepositoryTest.kt` | Empty — no tests | Has been empty since Phase 2 |

### Test Infrastructure

| File | Purpose |
|------|---------|
| `MainDispatcherRule.kt` | JUnit 5 extension; swaps main dispatcher to `UnconfinedTestDispatcher` for deterministic coroutine testing |
| `data/FakeSongRepository.kt` | Fake implementing `SongRepository`: `MutableStateFlow` for all list data; call counters for `setRating`, `incrementPlayCount`, `recordListeningEvent`, `refreshLibrary`; stubs for `searchSongs/Albums/Artists` that return the base flows |
| `data/FakePlaylistRepository.kt` | Fake implementing `PlaylistRepository`: call counters + argument capture for all write operations |
| `data/FakePlayerRepository.kt` | Fake implementing `PlayerRepository`: call counters + argument capture for all player operations, `emitState()` helper |
| `data/sync/FakeAuthRepository.kt` | Fake implementing `AuthRepository`: call counters, `emitSignedIn()` / `emitSignedOut()` helpers |

---

## Evaluation of Current Tests

### ✅ Excellent — keep as-is

**`ConflictResolverTest`** — Pure business logic, no dependencies, 13 well-named tests. Covers every resolution rule and edge case including the exact scenarios that cause data divergence across devices. The most valuable tests in the project because they protect the correctness invariants that are almost impossible to debug manually on two real devices. Nothing missing.

**`SongFingerprintTest`** — Pure function, 11 tests. Determinism, format, all normalization cases, and the duration-bucketing boundary (the trickiest part). Well-scoped. Nothing missing.

**`SettingsViewModelTest`** — 12 tests covering every event, every `SyncStatus` variant, derived `lastSyncTime`, and the snackbar consume-once pattern. Uses `FakeAuthRepository` (correct) and `mockk` for `SyncOrchestrator` (necessary — it's a concrete class with Android dependencies). Good use of Turbine for state flow assertions.

---

### ✅ Good — minor gaps

**`PlaylistViewModelTest`** — 11 tests, covers all `PlaylistEvent` types. The fakes are well-structured with call counters and argument capture. Two minor gaps:
- `CreatePlaylist with blank name does NOT call repository` uses `"   "` — good; but `RenamePlaylist with blank name` uses `""` (empty, not whitespace-only). The `.trim()` guard in the ViewModel also rejects whitespace-only strings; the test should use `"  "` to verify the trim path specifically.
- `getSongsForPlaylist(playlistId)` and `getAllSongs()` are untested delegation methods. Low value to add since they're one-liners, but worth noting.

**`MetadataViewModelTest`** — 3 tests, appropriately minimal for a simple 3-flow combine. Each flow is tested independently. Nothing essential missing given the simplicity of the ViewModel.

---

### ⚠️ Adequate but incomplete

**`LibraryViewModelTest`** — 10 tests, good coverage of the primary paths. Gaps:
- Sort orders `BY_ARTIST`, `BY_ALBUM`, `BY_DURATION` are never tested — only `BY_PLAY_COUNT` and `BY_DATE_ADDED` are. `BY_TITLE` is tested implicitly as the default. The three untested sorts are real code paths (4 of 6 `when` branches in the sort lambda).
- `onAppResumed()` — the 24-hour auto-refresh logic is completely untested. This guards against unnecessary scans on every resume.
- Search tests use `FakeSongRepository.searchSongs` which returns `songsFlow` unfiltered. The tests verify that the ViewModel doesn't break when a query is set, but do not verify that it actually switches to the search code path. The `FakeSongRepository` could be extended to track `searchCallCount` vs `getAllSongsCallCount` to make this assertion meaningful.
- `LibraryUiState.Error` branch is tested structurally but the commented-out assertions in `refreshLibrary handles error gracefully` and `refreshLibrary calls repository` weaken the test — both tests call the repository but never assert on the resulting state.

**`PlayerViewModelTest`** — 17 tests (the most comprehensive ViewModel test in the project), but four `PlayerEvent` types have zero coverage:
- `AddToQueue` — uncovered
- `PlayNext` — uncovered
- `RemoveFromQueue` — uncovered
- `ReorderQueue` — uncovered
- `currentSongRating: StateFlow<Rating>` (the `flatMapLatest` derived from the current song's rating) — untested. This is real logic: it switches the rating flow when the song changes mid-playback.
- The file is in package `ui.library` — it should be in `ui.player` (same issue as `NowPlayingScreenTest` that was fixed in Phase 7 Bug 7).

---

### ❌ Weak or problematic

**`NowPlayingScreenContentTest`** — 4 tests, all for the `FavoriteButton`. This is the only Compose UI test in the project, and it covers one of the smallest interactive elements on the most important screen. The screen also contains: a seek bar, play/pause button, skip controls, star rating bar, shuffle and repeat buttons, a queue sheet toggle — none of which are tested.

**`PlayerRepositoryTest`** — Empty file. Has existed since Phase 2 with zero tests. Should either be populated or deleted. `PlayerRepositoryImpl` is legitimately difficult to test without a running `MediaController`, so the file is likely empty for a valid reason, but it misleads readers into thinking testing is planned.

---

## Gaps by Test Type

### 1. Unit Tests (JVM) — gaps

#### 1a. PlayerViewModel — 4 missing events

The `AddToQueue`, `PlayNext`, `RemoveFromQueue`, and `ReorderQueue` events are wired in the ViewModel but untested. `FakePlayerRepository` already has counters for all four (`lastQueuedSong`, `lastPlayNextSong`, `lastRemovedId`, `lastReorderedId/Position`).

**Tests to add** in `PlayerViewModelTest`:
```
AddToQueue event calls playerRepository.addToQueue with correct song
PlayNext event calls playerRepository.playNext with correct song
RemoveFromQueue event calls playerRepository.removeFromQueue with correct id
ReorderQueue event calls playerRepository.reorderQueue with correct id and position
```

**Also add:**
```
currentSongRating reflects rating of current song
currentSongRating updates when song changes mid-playback
```
This requires `FakeSongRepository.setRatingForSong(id, rating)` (already present) + `FakePlayerRepository.emitState()` to change the current song and verify the rating flow switches.

**Also fix:** move `PlayerViewModelTest.kt` from `ui.library` to `ui.player` package (same fix applied to `NowPlayingScreenTest` in Phase 7 Bug 7).

#### 1b. LibraryViewModel — 3 missing sort orders + 1 missing method

**Tests to add** in `LibraryViewModelTest`:
```
sort by title orders songs alphabetically (currently implicit in default — make it explicit)
sort by artist orders songs by artist name
sort by album orders songs by album name
sort by duration orders songs ascending by duration
onAppResumed triggers refresh if more than 24 hours have passed
onAppResumed does not trigger refresh if less than 24 hours have passed
```

The `onAppResumed` tests need a way to control `System.currentTimeMillis()`. The simplest approach: call `refreshLibrary()` first to set `lastScanTimestamp`, then call `onAppResumed()` immediately — the 24-hour window will not have elapsed, so no extra refresh should fire.

**Also restore the commented-out assertions** in the two `refreshLibrary` tests — they were commented out after a refactor and weaken the tests.

#### 1c. PlaylistViewModel — 1 test to sharpen

In `RenamePlaylist with blank name does NOT call repository`, change the name from `""` to `"  "` to specifically exercise the `.trim()` path that was added in Phase 7.

#### 1d. FakeSongRepository — search call tracking

Add `getAllSongsCallCount` and `searchSongsCallCount` counters to `FakeSongRepository.getAllSongs()` and `.searchSongs()`. Then update the two search tests in `LibraryViewModelTest` to assert that the correct method was called:
- When query is blank → `getAllSongsCallCount > 0`, `searchSongsCallCount == 0`
- When query is non-blank → `searchSongsCallCount > 0`, `getAllSongsCallCount stays at its previous count`

This turns "search doesn't crash" into "search switches code paths correctly".

---

### 2. Room DAO Tests (Instrumented) — entirely missing

All queries in `SongDao` and `PlaylistDao` are untested. These tests run on the JVM using Room's in-memory database (`Room.inMemoryDatabaseBuilder`), so they're fast and don't require a device. They catch SQL errors, wrong join logic, and column-mapping bugs that JVM unit tests with fakes cannot catch.

**Priority: High** — DAO tests have protected several bugs in this project already (Bug 1 was a DAO query bug found manually). Writing them after the fact is cheap and prevents regressions.

#### SongDao — tests to write

```
insertAll and getAllSongs returns songs sorted by title
getAllAlbums groups songs by albumId and counts correctly
getAllArtists groups songs by artist and counts correctly
searchSongs returns songs matching title LIKE query
searchSongs returns songs matching artist LIKE query
searchSongs returns nothing for non-matching query
searchAlbums returns albums matching album or artist LIKE query
searchArtists returns artists matching artist LIKE query
getFavoriteSongs returns only songs with rating >= 4
getMostPlayedSongs returns songs ordered by playCount descending
getRecentlyPlayed returns latest listened songs deduped by id
incrementPlayCount increases playCount by 1 and sets lastPlayed
setRating writes correct rating and updates lastModified
applySyncDelta applies max playCount and updates timestamp
upsertSongs preserves metadata fields on conflict (insertAllIgnore behavior)
getSongsByAlbum returns songs for albumId ordered by trackNumber
getSongsByArtist returns songs for artist
```

#### PlaylistDao — tests to write

```
insertPlaylist returns generated ID
getAllPlaylists excludes soft-deleted playlists (deletedAt > 0)
getAllPlaylistsList includes soft-deleted playlists
softDeletePlaylist sets deletedAt timestamp
getAllPlaylistsWithCount returns correct song count per playlist
getAllPlaylistsWithCount returns 0 count for playlist with no songs
getSongsForPlaylist returns songs in correct order (by position)
addSongToPlaylist and removeSongFromPlaylist update membership
replacePlaylistSongs overwrites existing songs in correct order
touchPlaylist updates lastModified
```

**Implementation note:** All DAO tests share a setup that creates an in-memory `SyncPlayerDatabase`, inserts required rows, and tears down in `@AfterEach`. Use JUnit 5 + `runBlocking` for suspend DAOs. These tests live in `app/src/androidTest` (instrumented) or can run on JVM with Robolectric, but Room's in-memory builder works without Robolectric on Android test runner.

---

### 3. Compose UI Tests — severely undercovered

Only `NowPlayingScreenContentTest` exists (4 tests, all for `FavoriteButton`). Every other screen has zero UI test coverage. Compose UI tests catch wiring bugs — callbacks not connected, wrong state displayed, missing nodes — that ViewModel tests can't catch.

**Priority order: high-traffic screens first.**

#### NowPlayingScreenContent — expand existing tests

```
PlayPause button emits PlayPause event
SkipToNext button emits SkipToNext event
SkipToPrevious button emits SkipToPrevious event
SeekBar interaction emits SeekTo event with correct position
StarRating tap emits SetRating with correct rating
StarRating tap on active star emits SetRating(NONE)
Track title and artist are displayed from PlayerUiState
Queue sheet opens when queue button tapped
Queue sheet close emits no event (state change only)
Clear queue button emits ClearQueue event
```

#### LibraryScreenContent — new test class

```
Songs tab is selected by default
Tab click switches to Albums tab
Songs list renders song titles
Search bar appears when search icon tapped
Search query change triggers onSearchQueryChanged callback
Sort dropdown shows sort options
Selecting sort option triggers onSortOrder callback
Play count badge visible for songs with playCount > 0
Play count badge hidden for songs with playCount == 0
```

#### PlaylistsScreenContent — new test class

```
Loading state renders CircularProgressIndicator (not error icon)
Empty state shows "No playlists yet"
Playlist names are shown in list
Create playlist dialog appears on FAB click
Create button is disabled when name is blank
```

#### SettingsScreenContent — new test class

```
Signed-out state shows sign-in button
Signed-in state shows display name and sign-out button
SyncStatus.Idle shows idle text
SyncStatus.Syncing shows progress indicator
SyncStatus.Error shows Retry button
Retry button emits SyncNow event
Snackbar appears when snackbarMessage is non-null
Snackbar dismiss calls onSnackbarDismiss
```

#### AlbumDetailScreen / ArtistDetailScreen — new test classes

```
// AlbumDetailScreenContentTest
Song list is displayed
Tap on song fires onSongClick with correct song

// ArtistDetailScreenContentTest
Song list is displayed
Tap on song fires onSongClick with correct song
```

---

### 4. Integration / Orchestrator Tests — entirely missing

`SyncOrchestrator` contains the most complex business logic in the codebase — it coordinates fingerprinting, `ConflictResolver`, `SyncRepository`, `PlaylistRepository`, `SongRepository`, and `ListeningHistoryDao` to execute a bi-directional sync. None of this is directly tested.

This is a difficult area to test because `SyncOrchestrator` has many dependencies. The approach is to fake all of them:

```kotlin
class SyncOrchestratorTest {
    // Fakes needed:
    // FakeSyncRepository (implements SyncRepository — new fake needed)
    // FakeSongRepository (exists)
    // FakePlaylistRepository (exists)
    // FakeListeningHistoryRepository or use FakeSongRepository
    // FakeAuthRepository (exists)
    // SyncOrchestrator constructed directly with fakes
}
```

**Tests to write** (after creating `FakeSyncRepository`):

```
// Push phase
syncIfSignedIn does nothing when user is signed out
syncIfSignedIn calls sync when user is signed in
push writes song metadata delta for songs modified since lastSync
push skips songs not modified since lastSync
push includes soft-deleted playlists in push loop
push writes playlist with deletedAt when soft-deleted
push writes listening history events

// Pull phase
pull applies ConflictResolver result to local song when remote wins
pull ignores remote song with no local fingerprint match
pull creates new local playlist when remote has one not present locally
pull soft-deletes local playlist when remote playlist has deletedAt > 0
pull skips song replacement for remote-deleted playlists
pull merges history via ConflictResolver.mergeHistoryEvent

// State transitions
sync sets syncStatus to Syncing during sync, Success after
sync sets syncStatus to Error when exception thrown
sync updates lastSyncTime on success
```

**Note on `FakeSyncRepository`:** This is the primary missing fake. It needs to return configurable remote data for songs, playlists, and history, and record what was pushed. It should implement `SyncRepository` with `MutableList` backing stores.

---

### 5. Missing Infrastructure

#### 5a. `PlayerRepositoryTest.kt` — delete or populate

The empty file at `ui/library/PlayerRepositoryTest.kt` should be deleted. `PlayerRepositoryImpl` requires a bound `MediaController` (obtained from a running `MediaSessionService`), which makes unit testing impractical without an instrumented test that starts the full `PlaybackService`. If a real test is written, it belongs in `androidTest` not `test`, and the empty JVM file serves no purpose. Keeping it implies tests are coming; they aren't.

#### 5b. `PlayerViewModelTest.kt` package

Move from `ui.library` to `ui.player`. Same fix as `NowPlayingScreenTest` in Phase 7 Bug 7. The class itself is correct — only the `package` declaration and directory path need updating.

#### 5c. Room test module

DAO tests require `testImplementation("androidx.room:room-testing")` (Room testing artifact). The in-memory builder is already in Room's core API; the testing artifact adds migration test helpers. Verify `libs.versions.toml` has `room-testing` before writing DAO tests.

---

## Prioritized Work Plan

Work is ordered by **risk protected per effort unit**. Fix infrastructure first, then fill in the highest-risk gaps.

### Tier 1 — Quick fixes (< 1 hour each)

| Item | Why now |
|------|---------|
| Delete `PlayerRepositoryTest.kt` | Removes misleading empty file |
| Move `PlayerViewModelTest.kt` to `ui.player` | Consistency with Phase 7 Bug 7 fix |
| Sharpen `RenamePlaylist with blank name` test | Uses `""` not `"  "` — doesn't exercise `.trim()` |
| Restore commented-out `LibraryViewModel` error assertions | Two tests with no assertions on state are noise |

### Tier 2 — High-value unit test gaps (1–2 hours)

| Item | Risk protected |
|------|----------------|
| `PlayerViewModel` — 4 missing events | `AddToQueue`, `PlayNext`, `RemoveFromQueue`, `ReorderQueue` are untested wiring |
| `PlayerViewModel` — `currentSongRating` flow | Real `flatMapLatest` logic with song-change switching — untested |
| `LibraryViewModel` — 3 missing sort orders | 3 of 6 `when` branches in sort lambda are dead code from the test perspective |
| `LibraryViewModel` — `onAppResumed` (2 tests) | 24-hour guard logic could regress silently |
| `FakeSongRepository` — search call counters | Turns search tests from "doesn't crash" into "switches code paths correctly" |

### Tier 3 — Room DAO tests (2–4 hours total, requires androidTest setup)

| File | Priority tests |
|------|----------------|
| `SongDaoTest` | LIKE search queries, grouping/aggregation for albums and artists, `applySyncDelta`, `getFavoriteSongs`, `getMostPlayedSongs`, `upsertSongs` conflict behavior |
| `PlaylistDaoTest` | `getAllPlaylistsWithCount` join, `softDeletePlaylist`, `getAllPlaylists` filter, `getSongsForPlaylist` order |

These protect the queries most likely to break silently on schema changes: the JOIN queries, the LIKE predicates, and the soft-delete filter.

### Tier 4 — Compose UI tests (2–3 hours per screen)

| Screen | Priority |
|--------|----------|
| `NowPlayingScreenContent` — expand existing tests | High — most-used screen, only FavoriteButton tested |
| `PlaylistsScreenContent` — new | High — three states (Loading, Empty, Loaded); Loading spinner was a Phase 7 bug |
| `SettingsScreenContent` — new | Medium — Error retry button and snackbar are new Phase 7 additions with no UI test |
| `LibraryScreenContent` — new | Medium — search and sort are new Phase 7 additions |
| `AlbumDetailScreen` / `ArtistDetailScreen` — new | Low — simple screens, but currently zero coverage |

### Tier 5 — SyncOrchestrator integration tests (4–6 hours, requires FakeSyncRepository)

| Item | Priority |
|------|----------|
| Create `FakeSyncRepository` | Prerequisite for all orchestrator tests |
| Push phase: delta detection, modified-since filter | High — the most likely source of silent data loss |
| Pull phase: conflict resolution integration, playlist soft-delete propagation | High — correctness-critical, already caused a bug (Phase 7 Task 1.4) |
| State transitions: Syncing → Success / Error | Medium — already tested indirectly in SettingsViewModelTest |

---

## What Not to Test

These are explicitly out of scope and should not consume effort:

- **`PlayerRepositoryImpl`** — Requires a running `MediaSessionService` and `MediaController`. Covered by manual testing and the Media3 library's own test suite.
- **Composables with no logic** — `PlaylistListItem`, `SongListItem`, `AlbumGridItem`, `ArtistListItem`, `MiniPlayer` display data but have no event logic. Snapshot tests would add maintenance cost with little protection.
- **`NavGraph`** — Navigation routes are tested implicitly by integration testing the app manually. Route string testing is brittle and adds no protection.
- **`DurationFormatter`** — Tested implicitly via `PlayerViewModel.formatTime` tests.
- **100% ViewModel coverage** — Simple delegating methods (`getSongsByAlbum`, `getSongsByArtist`) that are one-liner pass-throughs to the repository do not need tests; the repository fake already validates the path at the ViewModel boundary.

---

## Summary

State at time of writing (pre-implementation):

| Test type | Files | Tests | Primary gaps |
|-----------|-------|-------|-------------|
| JVM unit | 7 | ~60 | Missing sort orders, onAppResumed, 4 PlayerVM events, search routing |
| Room DAO | 0 | 0 | All DAO queries untested |
| Compose UI | 1 | 4 | Only FavoriteButton on one screen |
| Orchestrator integration | 0 | 0 | SyncOrchestrator push/pull entirely untested |
| **Total** | **8** | **~64** | |

**Achieved (post-implementation):**

| Test type | Files | Tests |
|-----------|-------|-------|
| JVM unit (src/test) | 9 | 110 |
| Room DAO (src/androidTest) | 2 | 46 |
| Compose UI (src/androidTest) | 6 | 32 |
| **Total** | **17** | **188** |

The existing tests did a good job protecting pure business logic (conflict resolution, fingerprinting) and ViewModel event wiring. The primary risk surfaces that were unprotected — DAO query correctness, `SyncOrchestrator` push/pull logic, and UI wiring on the three most-used screens — are now covered.

---

**Design doc**: [[testing-design]]
