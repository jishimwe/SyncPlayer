# Testing — Design

## Overview

After Phase 7 shipped, a dedicated testing pass audited every existing test, identified gaps across all four test types, and closed them in five ordered tiers. The work added 46 unit tests (110 total JVM), 27 Room DAO tests for `SongDao`, 19 Room DAO tests for `PlaylistDao`, 7 Compose UI tests for `NowPlayingScreenContent`, and coverage for `PlaylistsScreenContent`, `SettingsScreenContent`, `AlbumDetailScreenContent`, and `ArtistDetailScreenContent`. A `FakeSyncRepository` was also written and used to build 14 integration tests for `SyncOrchestrator`.

**Verification status**: All 110 JVM unit tests pass (`./gradlew test`). The 78 instrumented tests (`./gradlew connectedAndroidTest`) compile cleanly and have been audited against the production source for correctness — content descriptions and text labels were verified against the actual composable source before writing assertions. Running them requires a connected device or emulator.

---

## What was built

### Tier 1 — Infrastructure fixes

- **Deleted `app/src/test/.../ui/library/PlayerRepositoryTest.kt`** — empty stub since Phase 2; removed to eliminate the false impression tests were planned for `PlayerRepositoryImpl` (which requires a running `MediaController`).
- **Deleted `app/src/test/.../ui/library/PlayerViewModelTest.kt`** — misplaced in `ui.library` instead of `ui.player`; replaced by the correctly-packaged file below.
- **`app/src/test/.../ui/playlists/PlaylistViewModelTest.kt`** — sharpened `RenamePlaylist with blank name does NOT call repository` to use `"   "` (whitespace-only) instead of `""`, actually exercising the `.trim()` path added in Phase 7.
- **`app/src/test/.../ui/library/LibraryViewModelTest.kt`** — restored the commented-out error-state assertion in `refreshLibrary handles error gracefully` using Turbine, so the test now asserts `LibraryUiState.Error` is emitted rather than just counting repository calls.

### Tier 2 — Unit test gaps

- **`app/src/test/.../data/FakeSongRepository.kt`** — added `getAllSongsCallCount` and `searchSongsCallCount` integer counters, incremented in `getAllSongs()` and `searchSongs()` respectively, so tests can assert which code path the ViewModel uses.

- **`app/src/test/.../data/FakeSongRepository.kt`** — added `refreshGate: CompletableDeferred<Unit>?` field; if non-null, `refreshLibrary()` suspends on it before returning. Enables tests to observe intermediate `isRefreshing = true` state without polling or mocking time.

- **`app/src/test/.../ui/player/PlayerViewModelTest.kt`** *(new, correct package)* — 26 tests total. New tests added:
  - `AddToQueue event calls playerRepository addToQueue with correct song`
  - `PlayNext event calls playerRepository playNext with correct song`
  - `RemoveFromQueue event calls playerRepository removeFromQueue with correct id`
  - `ReorderQueue event calls playerRepository reorderQueue with correct id and position`
  - `currentSongRating is NONE when no song is playing`
  - `currentSongRating reflects rating of current song`
  - `currentSongRating updates when current song changes`

- **`app/src/test/.../ui/library/LibraryViewModelTest.kt`** — 17 tests total. Added:
  - 3 search routing tests (`blank query uses getAllSongs`, `non-blank uses searchSongs`, `clearing switches back`) — assert on call counters, not just state shape.
  - 6 sort order tests: `BY_TITLE`, `BY_ARTIST`, `BY_ALBUM`, `BY_DURATION`, `BY_PLAY_COUNT`, `BY_DATE_ADDED`.
  - 2 `onAppResumed` tests: triggers refresh when `lastScanTimestamp == 0`; skips refresh when called immediately after a refresh.
  - Fixed `isRefreshing is true during refresh and false after`: added `CompletableDeferred` gate to `FakeSongRepository` so `refreshLibrary()` actually suspends; test uses Turbine to capture all three states (`false → true → false`).

- **`app/src/test/.../data/FakePlayerRepository.kt`** — added `clearQueueCallCount` and `clearQueue()` to support the `ClearQueue` test.
- **`app/.../ui/player/components/PlayerControls.kt`** — added content descriptions (`"Play"`/`"Pause"` toggled on playback state, `"Skip next"`, `"Skip previous"`) to the three icon buttons. These were `null` before, which also made them inaccessible to screen readers. Required to support the Compose UI tests and correct accessibility.

### Tier 3 — Room DAO tests

Both files live in `app/src/androidTest/java/com/jpishimwe/syncplayer/data/local/` and use `Room.inMemoryDatabaseBuilder` with `allowMainThreadQueries()`. Each `@Before` creates a fresh database; `@After` closes it.

- **`SongDaoTest.kt`** — 27 tests covering:
  - `getAllSongs`: empty state, title-ascending sort order
  - `getSongById`: found / not-found
  - `getSongsByIds`: subset filtering
  - `getAllAlbums`: grouping by `albumId`, aggregated `songCount`, name-ascending sort
  - `getAllArtists`: grouping by `artist`, `songCount` and `albumCount` aggregation
  - `getSongsByAlbum`: filter + `trackNumber ASC` sort
  - `getSongsByArtist`: filter correctness
  - `deleteAll`: removes all rows
  - `upsertSongs`: preserves `rating` and `playCount` on conflict; inserts new songs with defaults
  - `incrementPlayCount`: increments correctly on multiple calls
  - `setRating` / `getRating`: round-trip write and read; returns `null` for unknown song
  - `applySyncDelta`: overwrites `playCount` and `rating` directly
  - `getFavoriteSongs`: includes rating ≥ 4, excludes rating = 3
  - `getMostPlayedSongs`: excludes zero-playCount songs; descending order
  - `searchSongs`: title match, artist match, case-insensitive, no-match empty result
  - `searchAlbums`: album name match
  - `searchArtists`: artist name match with correct `songCount`

- **`PlaylistDaoTest.kt`** — 19 tests covering:
  - `insertPlaylist` and `getAllPlaylists`: insert and read back
  - `getAllPlaylists` sort: name-ascending
  - `getAllPlaylists` soft-delete filter: excludes `deletedAt > 0`
  - `getPlaylistById`: found / not-found
  - `updatePlaylist`: name change persists
  - `deletePlaylist`: hard-delete removes row
  - `softDeletePlaylist`: sets `deletedAt` and `lastModified` timestamp
  - `getAllPlaylistsList`: returns all rows including soft-deleted
  - `addSongToPlaylist` + `getSongsForPlaylist`: membership and position ordering
  - `removeSongFromPlaylist`: removes only target song
  - `clearPlaylistSongs`: empties playlist song membership
  - `replacePlaylistSongs`: correct order after clear+replace
  - `getSongCountForPlaylist`: correct count
  - `getAllPlaylistsWithCount`: correct per-playlist counts; excludes soft-deleted
  - `touchPlaylist`: updates `lastModified`
  - `getSongsForPlaylistList` (suspend): correct position order

- **`app/src/androidTest/.../ui/library/LibraryScreenTest.kt`** (updated) — fixed broken compilation caused by Phase 7's `LibraryScreenContent` signature expansion; added `metadataState`, `onQueryChanged`, `onClearSearchQuery`, `onSortOrderChanged` parameters to all 5 existing tests.

- **`app/build.gradle.kts`** — added `androidTestImplementation(libs.kotlinx.coroutines.test)` so `runTest` is available in instrumented tests.

### Tier 4 — Compose UI tests

All files live in `app/src/androidTest/java/com/jpishimwe/syncplayer/ui/`.

- **`player/NowPlayingScreenContentTest.kt`** — 7 tests:
  - Displays current song title and artist from `PlayerUiState`
  - Asserts `"Now Playing"` toolbar title is displayed when `currentSong == null` (renamed from `showsNothingPlayingWhenNoCurrentSong` which had no assertion)
  - Play button fires `PlayerEvent.PlayPause` when paused
  - Pause button fires `PlayerEvent.PlayPause` when playing
  - Skip-next button fires `PlayerEvent.SkipToNext`
  - Skip-previous button fires `PlayerEvent.SkipToPrevious`
  - Star favorite display when `rating == Rating.FAVORITE`
  - Back button invokes `onNavigateBack`

- **`playlists/PlaylistsScreenContentTest.kt`** — 4 tests:
  - Loading state shows `"Loading..."` text
  - Loaded state displays playlist names
  - Empty loaded state shows `"No playlists yet"`
  - Playlist click invokes `onPlaylistClick` with correct id

- **`settings/SettingsScreenContentTest.kt`** — 8 tests:
  - Signed-out shows `"Sign in"` button
  - Sign-in button click invokes `onSignIn`
  - Signed-in shows email and `"Sign out"` button
  - Sign-out button click invokes `onSignOut`
  - `SyncStatus.Error` shows `"Retry"` button
  - Retry button click invokes `onSyncNow`
  - `SyncStatus.Syncing` shows `"Syncing..."` text
  - Sync section absent when signed out

- **`library/AlbumDetailScreenContentTest.kt`** — 4 tests:
  - Album name appears in toolbar
  - Song titles are displayed
  - Song click passes correct index
  - Back button invokes `onNavigateBack`

- **`library/ArtistDetailScreenContentTest.kt`** — 4 tests (same pattern as album detail)

### Tier 5 — SyncOrchestrator integration tests

- **`app/src/test/.../data/sync/FakeSyncRepository.kt`** — implements `SyncRepository` with:
  - Call counters: `pushSongCallCount`, `pushPlaylistCallCount`, `pushHistoryCallCount`
  - Argument captures: `pushedSongs`, `pushedPlaylists`, `pushedHistoryEvents`
  - Configurable stub return values: `remoteSongs`, `remotePlaylists`, `remoteHistory`
  - Configurable failure injection: `throwOnPush`, `throwOnPull`

- **`app/src/test/.../data/sync/SyncOrchestratorTest.kt`** — 14 tests. Fake DAO implementations (`FakeSongDao`, `FakePlaylistDao`, `FakeListeningHistoryDao`) are defined inline in this file to avoid polluting the shared fake infrastructure with DAO-level concerns. `Context` and `SharedPreferences` are mocked with MockK (`relaxed = true`). Tests cover:
  - `syncIfSignedIn` guard: no-op when signed out, runs sync when signed in
  - Push phase: pushes modified songs (lastModified > lastSync); skips unmodified songs; pushes modified playlists; pushes listening history
  - Pull phase: applies `ConflictResolver` result via `applySyncDelta`; skips unknown fingerprints; creates new local playlist from remote; skips remotely-deleted playlists; applies remote playlist win via `updatePlaylist`; inserts remote history events
  - Error handling: sets `SyncStatus.Error` with correct message on exception
  - Status transitions: `Idle` → `Success` on clean sync (renamed from `Idle → Syncing → Success`: `Syncing` is set and immediately overwritten before any observer can see it with `UnconfinedTestDispatcher`; the test now only claims what it actually observes)

---

## Design decisions

- **Inline fake DAOs vs. shared fakes** — `FakeSongDao`, `FakePlaylistDao`, and `FakeListeningHistoryDao` are defined at the bottom of `SyncOrchestratorTest.kt` rather than in the shared `data/` test directory. This prevents these low-level DAO fakes from leaking into ViewModel tests, which should use higher-level repository fakes. The tradeoff is some duplication if DAO tests in `androidTest` need similar stubs, but those use a real in-memory database anyway.

- **MockK for Context instead of Robolectric** — `SyncOrchestrator` reads `SharedPreferences` via `context.getSharedPreferences()`. Rather than adding Robolectric (a large dependency that slows the JVM test suite), `Context` and `SharedPreferences` are mocked with MockK `relaxed = true`. The prefs mock returns `0L` for `getLong` and ignores writes. This keeps the test in `src/test` (fast JVM) rather than `src/androidTest` (requires emulator).

- **`androidTestImplementation(kotlinx.coroutines.test)`** — `runTest` is needed in DAO tests (`src/androidTest`) for suspend DAO functions. The library was already in `testImplementation` for JVM tests; adding it to `androidTestImplementation` too is the correct scope without duplicating the catalog entry.

- **DAO tests use `@RunWith(AndroidJUnit4::class)` + JUnit 4** — The instrumented test runner is JUnit 4 (the Android test runner). `@RunWith(AndroidJUnit4::class)` is required. The unit tests use JUnit 5 via `@RegisterExtension`. These are intentionally different; mixing them would break both test suites. `runTest` from `kotlinx-coroutines-test` works in both.

- **Compose UI tests use `createAndroidComposeRule<ComponentActivity>`** — Uses `ComponentActivity` (not a Hilt or app-specific activity) so no Hilt setup or app context is needed. All `ScreenContent` composables take their state as parameters (the testable composable pattern), so ViewModel wiring is bypassed entirely.

- **Search tests assert routing, not filtering** — `FakeSongRepository.searchSongs` returns the same `songsFlow` as `getAllSongs` (filtering is a DAO concern). The new search tests assert that the correct repository method was called (via `getAllSongsCallCount` / `searchSongsCallCount`), not that results are filtered. DAO-level filtering is tested in `SongDaoTest.searchSongs_*`.

- **`onAppResumed` tests control time via ordering** — `LibraryViewModel` tracks `lastScanTimestamp` as the system time after each successful scan. The "does not refresh" test calls `refreshLibrary()` first (sets timestamp to now) then immediately calls `onAppResumed()` — the 24-hour window has not elapsed, so no second scan fires. This avoids mocking `System.currentTimeMillis()`.

- **`SyncStatus.Error` test requires a pushable song** — The orchestrator's exception is only thrown when it actually calls into `SyncRepository`. With `songDao.songs = emptyList()` (the default), no push calls are made and no exception is thrown. The error test seeds `songDao.songs` with one song modified after `lastSync = 0`, ensuring the push code path is reached.

---

## Known gaps

- **`NowPlayingScreenContent` seek bar and star rating UI** — The seek bar interaction and multi-star rating tap are not Compose UI tested. SeekBar interaction requires `performTouchInput` with coordinate-based drag gestures, which are brittle and device-resolution-dependent. Star rating tap would require a tagged test node or content description, which the current `StarRating` composable does not expose. These are deferred until the composables expose stable `semantics` tags.

- **`LibraryScreenContent` search and sort UI** — The plan called for testing the search bar activation and sort dropdown. `DockedSearchBar` is a Material 3 experimental API whose internal node tree changes between versions; content description-based selection is unreliable. Deferred until the API stabilizes.

- **`PlayerRepositoryImpl`** — Still not tested. Requires a bound `MediaController` from a running `MediaSessionService`. Only feasible as an end-to-end instrumented test that starts `PlaybackService` — out of scope here.

- **`SyncOrchestrator.lastSyncTime` persistence** — The `SharedPreferences` write in the mock (`editor.putLong(...)`) is accepted but `getLong` always returns `0L`. Tests that verify `lastSyncTime` is stored and read back across calls cannot be written with this mock approach. Would require Robolectric or an instrumented test.
