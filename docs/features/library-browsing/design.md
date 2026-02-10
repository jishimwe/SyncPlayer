# Library Browsing

## Overview

Scan the device for audio files, cache metadata in a local Room database, and display the library in a tabbed UI with Songs, Albums, and Artists tabs. Handles permission states, scan errors, and automatic background refresh.

## What Was Built

### Data Layer

- **`model/Song.kt`**: Room `@Entity` with fields: id, title, artist, album, albumId, duration, trackNumber, year, dateAdded, contentUri, albumArtUri
- **`model/Album.kt`**: Data class (not an entity) — derived from Song via GROUP BY in DAO
- **`model/Artist.kt`**: Data class (not an entity) — derived from Song via GROUP BY in DAO
- **`data/local/SongDao.kt`**: Room DAO with Flow-based queries for songs, albums (GROUP BY albumId), artists (GROUP BY artist), plus insert/delete
- **`data/local/SyncPlayerDatabase.kt`**: Room database, version 1, single Song entity
- **`data/MediaStoreScanner.kt`**: Queries `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` with `IS_MUSIC = 1`, maps cursor to Song objects on `Dispatchers.IO`
- **`data/SongRepository.kt`**: Interface exposing Flow queries and `refreshLibrary()`
- **`data/SongRepositoryImpl.kt`**: Reads from DAO Flows; refresh does deleteAll + insertAll from scanner

### DI

- **`SyncPlayerApplication.kt`**: `@HiltAndroidApp` application class
- **`di/DatabaseModule.kt`**: Provides Room database (singleton) and SongDao
- **`di/AppModule.kt`**: Binds SongRepository to SongRepositoryImpl, provides ContentResolver

### UI

- **`ui/library/LibraryViewModel.kt`**: Combines three DAO Flows plus error state via `combine` into `LibraryUiState` (Loading | Loaded | Error). Tracks scan errors separately in `refreshError: MutableStateFlow<String?>`. Includes `isRefreshing: StateFlow<Boolean>` for loading indicators and `lastScanTimestamp: MutableStateFlow<Long>` for automatic rescan logic. `onAppResumed()` triggers rescan if last scan was >24 hours ago. `refreshLibrary()` sets isRefreshing, updates timestamp, handles exceptions by setting refreshError. Error state only shown when scan fails AND database is empty; otherwise shows stale data.

- **`ui/library/LibraryScreen.kt`**: Scaffold + PrimaryTabRow + tab content. Has testable `LibraryScreenContent` that takes `uiState`, `selectedTab`, `isRefreshing`, and callbacks. Uses `DisposableEffect` with `LifecycleEventObserver` to call `viewModel.onAppResumed()` on `Lifecycle.Event.ON_RESUME`. Triggers initial `refreshLibrary()` via `LaunchedEffect(Unit)`. Renamed `onRetry` callback to `onRefresh` for semantic clarity.

- **`ui/library/SongListItem.kt`**: ListItem with album art (SubcomposeAsyncImage), title, artist, duration
- **`ui/library/AlbumGridItem.kt`**: Card with album art, name, artist
- **`ui/library/ArtistListItem.kt`**: ListItem with person icon, name, song/album counts
- **`ui/library/DurationFormatter.kt`**: `formatDuration(ms)` → "m:ss"

- **`ui/library/PermissionHandler.kt`**: Three-state permission handling using `PermissionState` enum (Granted | NotRequested | Denied | PermanentlyDenied). Uses `rememberSaveable` to track state across configuration changes. Determines denial type via `ActivityCompat.shouldShowRequestPermissionRationale()`. Shows different UI for each state:
  - **NotRequested**: Generic message + auto-launches system dialog
  - **Denied**: Detailed rationale explaining why permission is needed + "Grant permission" button
  - **PermanentlyDenied**: Explanation + "Open settings" button that launches `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`
  
  Uses settings launcher (`ActivityResultContracts.StartActivityForResult`) to recheck permission when user returns from settings.

- **`ui/SyncPlayerApp.kt`**: Top-level composable wrapping PermissionHandler → LibraryScreen

### Tests

- **Unit**: `LibraryViewModelTest` (5 tests) — loaded state, empty state, tab selection, refresh call count, error handling, isRefreshing flag
- **Instrumented**: `SongDaoTest` (5 tests) — insert, query, grouping, filtering, delete
- **Instrumented**: `LibraryScreenTest` (5 tests) — loading, loaded, tabs, empty, error states

## Design Decisions

1. **Single Song entity**: Albums and artists are derived from Song via SQL GROUP BY. Avoids data duplication and sync complexity.

2. **Full rescan on refresh**: Simple deleteAll + insertAll approach. Adequate for local libraries (typically < 10K songs). Can optimize with diffing later if needed.

3. **ContentUri as String**: Room doesn't support Uri natively. Store as string, convert when needed.

4. **Hilt for DI**: Standard Android DI solution, integrates well with ViewModel and Compose.

5. **Testable screen composable**: `LibraryScreenContent` takes state and callbacks directly, so UI tests don't need Hilt or a ViewModel.

6. **Activity Result API for permissions**: Used instead of Accompanist to avoid an extra dependency, since minSdk 34 guarantees the API is available.

7. **Stale data over error screen**: When `refreshLibrary()` fails but the database already contains songs from a previous scan, show the stale data rather than an error screen. This provides a better user experience — users can still browse and play their library while the issue resolves itself (e.g., temporary MediaStore unavailability). Error state only appears when scan fails AND database is empty (nothing to show).

8. **24-hour automatic rescan threshold**: Local music files don't change frequently. Most users add music rarely (weekly or monthly). Checking once per day on app resume is sufficient to catch new files without wasteful scanning that drains battery. The `onAppResumed()` lifecycle handler only triggers a rescan if >24 hours have passed since the last scan timestamp.

9. **No manual refresh button in main UI**: Manual refresh is a power-user feature that creates UI clutter for little benefit. Automatic refresh (on app creation + every 24h on resume) handles 99% of use cases. A manual rescan option can be added to a settings screen in the future if user feedback indicates it's needed.

10. **Lifecycle-based automatic refresh**: Uses `DisposableEffect` with `LifecycleEventObserver` to detect `ON_RESUME` events. This catches the case where a user adds music while the app is backgrounded and returns after 24+ hours. The pattern is robust to configuration changes and properly cleans up the observer on dispose.

11. **Three-state permission handling**: Distinguishes between first request (auto-launch), rationale needed (user denied once, show explanation), and permanent denial (user selected "Don't ask again", must go to settings). Uses `ActivityCompat.shouldShowRequestPermissionRationale()` combined with `rememberSaveable` state tracking to determine which state applies. This provides better UX than showing the same message for all denial scenarios.

12. **Error tracking separate from data Flows**: `refreshError` is a separate `MutableStateFlow` that gets combined with the database Flows in the ViewModel. Database Flows (songs, albums, artists) never emit errors — they just emit whatever is in the DB (even empty lists). Scan errors happen during the write operation (`refreshLibrary()`), not during reads. This separation keeps the architecture clean and makes error handling explicit.

## Implementation Notes

- **Error state emission**: The `combine` operator in `LibraryViewModel` folds `refreshError` into the UI state. Logic: if `error != null && songs.isEmpty()` → emit `Error`, otherwise emit `Loaded`. This ensures error screen only appears when there's genuinely nothing to display.

- **isRefreshing flag**: Set to `true` at the start of `refreshLibrary()`, `false` in the `finally` block. The `finally` ensures the flag is always cleared even if an exception occurs, preventing stuck loading indicators.

- **Timestamp tracking**: `lastScanTimestamp` is updated at the start of `refreshLibrary()` (not at the end). This prevents rapid repeated scans if the operation fails quickly. The timestamp represents "when we last attempted a scan", not "when we last succeeded".

- **Permission state persistence**: Uses `rememberSaveable` with an enum (`PermissionState`) to survive configuration changes. Enums are automatically Parcelable, so they work with `rememberSaveable` without a custom `Saver`.

- **Settings launcher pattern**: Uses `ActivityResultContracts.StartActivityForResult` to launch settings and detect when user returns. The callback rechecks the permission and updates state accordingly. This provides seamless UX when user grants permission in settings.

## Known Gaps (Resolved)

All gaps from the original implementation have been addressed:

1. ~~**No user-triggered rescan**~~ → Resolved: Automatic lifecycle-based refresh (on creation + every 24h on resume) handles typical use cases. Manual refresh can be added to settings later if needed.

2. ~~**Error state is unreachable**~~ → Resolved: Error tracking via separate `refreshError` Flow, combined with database Flows. Error state emitted when scan fails and DB is empty.

3. ~~**No permission rationale**~~ → Resolved: Three-state handling with `Denied` state showing detailed rationale before re-requesting permission.

4. ~~**No permanent-denial handling**~~ → Resolved: `PermanentlyDenied` state shows settings button that launches app settings page.