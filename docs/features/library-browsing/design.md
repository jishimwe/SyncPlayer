# Library Browsing

## Overview

Scan the device for audio files, cache metadata in a local Room database, and display the library in a tabbed UI with Songs, Albums, and Artists tabs.

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

- **`ui/library/LibraryViewModel.kt`**: Combines three DAO Flows via `combine` into `LibraryUiState` (Loading | Loaded | Error). Exposes tab selection and `refreshLibrary()`
- **`ui/library/LibraryScreen.kt`**: Scaffold + PrimaryTabRow + tab content. Has a testable `LibraryScreenContent` variant that takes state directly. Triggers `refreshLibrary()` via `LaunchedEffect(Unit)` on first composition.
- **`ui/library/SongListItem.kt`**: ListItem with album art (SubcomposeAsyncImage), title, artist, duration
- **`ui/library/AlbumGridItem.kt`**: Card with album art, name, artist
- **`ui/library/ArtistListItem.kt`**: ListItem with person icon, name, song/album counts
- **`ui/library/DurationFormatter.kt`**: `formatDuration(ms)` → "m:ss"
- **`ui/library/PermissionHandler.kt`**: Checks/requests `READ_MEDIA_AUDIO` using Activity Result API
- **`ui/SyncPlayerApp.kt`**: Top-level composable wrapping PermissionHandler → LibraryScreen

### Tests

- **Unit**: `LibraryViewModelTest` (5 tests) — loaded state, empty state, tab selection, refresh, error handling
- **Instrumented**: `SongDaoTest` (5 tests) — insert, query, grouping, filtering, delete
- **Instrumented**: `LibraryScreenTest` (5 tests) — loading, loaded, tabs, empty, error states

## Design Decisions

1. **Single Song entity**: Albums and artists are derived from Song via SQL GROUP BY. Avoids data duplication and sync complexity.
2. **Full rescan on refresh**: Simple deleteAll + insertAll approach. Adequate for local libraries (typically < 10K songs). Can optimize with diffing later if needed.
3. **ContentUri as String**: Room doesn't support Uri natively. Store as string, convert when needed.
4. **Hilt for DI**: Standard Android DI solution, integrates well with ViewModel and Compose.
5. **Testable screen composable**: `LibraryScreenContent` takes state and callbacks directly, so UI tests don't need Hilt or a ViewModel.
6. **Activity Result API for permissions**: Used instead of Accompanist to avoid an extra dependency, since minSdk 34 guarantees the API is available.

## Known Gaps

These were in the original requirements but are not yet implemented:

1. **No user-triggered rescan**: `refreshLibrary()` only runs automatically on screen load via `LaunchedEffect(Unit)`. There is no pull-to-refresh gesture or refresh button in the loaded state. Only the error state has a "Retry" button.
2. **Error state is unreachable**: `LibraryViewModel.refreshLibrary()` catches all exceptions silently. The `LibraryUiState.Error` variant exists in the sealed interface and renders correctly, but is never emitted. A failed scan leaves the database unchanged.
3. **No permission rationale**: `PermissionHandler` doesn't distinguish between first-time request and "should show rationale". It shows the same generic message regardless.
4. **No permanent-denial handling**: If the user permanently denies the permission, the UI just shows the "Grant permission" button forever. There's no prompt to open app settings.
