# `ui/library` Package Analysis

**Date**: 2026-03-20
**Scope**: All files in `app/src/main/java/com/jpishimwe/syncplayer/ui/library/`

---

## Bugs

### ~~B1 — `formatDuration` is broken for songs > 1 hour~~ FIXED
**File**: `DurationFormatter.kt`

Fixed `minutes` to use `(totalSeconds % 3600) / 60` (remainder minutes). Removed nonsensical AM/PM branching (`hours > 12` subtracted 12).

---

### ~~B2 — Sort order callbacks are miswired across tabs~~ FIXED
**Note**: `LibraryScreen.kt` was dead code — `HomeScreen` with `ui/home/tabs/` handles tab routing. Sort wiring in the tab screens is tracked in `docs/features/tabs/plan.md` (A1, Task 2.1–2.3).

---

### ~~B3 — `AlbumsTab` local sort state initialized to wrong value~~ FIXED
**Note**: Same as B2 — resolved by deleting the dead `LibraryScreen.kt`. Tab-level sort init is tracked in the tabs plan.

---

### ~~B4 — `MetadataViewModel._favoriteSortOrder` is unused~~ FIXED
**File**: `MetadataViewModel.kt`

Wired `_favoriteSortOrder` into the `combine` flow so favorites are actually sorted when the user changes sort order.

---

### ~~B5 — Double refresh on first launch~~ FIXED
**File**: `LibraryViewModel.kt`

Initialized `lastScanTimestamp` to `System.currentTimeMillis()` so `onAppResumed()` doesn't trigger a redundant second refresh on first launch. Updated corresponding test.

---

### ~~B6 — `AlbumGridItem.isPlaying` parameter is accepted but never used~~ FIXED
**File**: `ui/library/AlbumGridItem.kt` — deleted (see B7).

---

### ~~B7 — Duplicate `AlbumGridItem` composable with incompatible APIs~~ FIXED
Deleted `ui/library/AlbumGridItem.kt`. The canonical version lives in `ui/components/AlbumItem.kt`.

---

### ~~B8 — `fetchMissingArtistImages` silently dies on first failure~~ FIXED
**File**: `LibraryViewModel.kt`

Wrapped `getArtistImageUrl` in `try/catch` so one network failure doesn't abort remaining artists.

---

### ~~B9 — `PermissionHandler` redundant context variable~~ FIXED
**File**: `PermissionHandler.kt`

Removed redundant `LocalContext.current` call, reused existing `context` variable with explicit `Activity` cast.

---

## Gaps (Missing Functionality)

### ~~G1 — `PLAYLISTS` and `HISTORY` tabs render blank~~ RESOLVED
Dead code — `LibraryScreen.kt` deleted. `HomeScreen` routes to `PlaylistsTabScreen` and `HistoryTabScreen` which have their own implementations.

---

### ~~G2 — `AddToPlaylist` menu action is a no-op~~ FIXED
**Files**: `AlbumDetailScreen.kt`, `ArtistDetailScreen.kt`, `NavGraph.kt`

Added `onAddToPlaylist` callback through both detail screens. Wired `PlaylistPickerSheet` at the NavGraph level (same pattern as HomeScreen). Per-song and bulk "Add all to playlist" both work.

---

### ~~G3 — Overflow menus are TODO stubs~~ FIXED
**Files**: `AlbumDetailTopBar.kt`, `ArtistDetailTopBar.kt`

Replaced TODO stubs with working `DropdownMenu` containing "Add all to playlist" action. Wired through to the `PlaylistPickerSheet` via `onAddAllToPlaylist` callback.

---

### ~~G4 — No `Error` state in `MetadataUiState`~~ FIXED
**File**: `MetadataViewModel.kt`

Added `MetadataUiState.Error(message)` variant, `.catch` on the `combine` flow, and error UI in `HomeScreen`.

---

### ~~G5 — Dead code: `MostPlayedTab` and `RecentlyPlayedTab`~~ RESOLVED
Deleted with `LibraryScreen.kt`. `HistoryTabScreen` in `ui/home/tabs/` handles recently played display.

---

### ~~G6 — No pull-to-refresh UI~~ FIXED
**File**: `HomeScreen.kt`

Wrapped `HorizontalPager` in Material 3 `PullToRefreshBox`. Wired `isRefreshing` from `LibraryViewModel` and `onRefresh` to `refreshLibrary()`.

---

### ~~G7 — Artist list item doesn't show artist art~~ RESOLVED
`ArtistListItem.kt` was dead code — deleted. The canonical `ArtistItem` in `ui/components/` already shows `imageUri` (artist art).

---

## Improvements

### ~~I1 — Extract duplicated sort dropdown into a reusable composable~~ RESOLVED
**File**: `ui/components/SortFilterBar.kt`

Already extracted. All tab screens use the shared `SortFilterBar` component — no duplication exists.

---

### ~~I2 — `LibraryScreen.kt` exceeds 300-line guideline (445 lines)~~ RESOLVED
`LibraryScreen.kt` was dead code — `HomeScreen` with `ui/home/tabs/` replaced it entirely. Deleted the file and its test (`LibraryScreenTest.kt`).

---

### ~~I3 — `AlbumDetailTopBar` and `ArtistDetailTopBar` are near-duplicates~~ FIXED
**Files**: `DetailTopBar.kt` (new), `AlbumDetailTopBar.kt`, `ArtistDetailTopBar.kt`

Extracted shared `DetailTopBar(title, subtitle, onBack, onAddAllToPlaylist, ...)` component. Both top bars are now thin wrappers that format their subtitle string and delegate.

---

### ~~I4 — Preview data is duplicated~~ FIXED
**File**: `PreviewData.kt` (new), `AlbumDetailPreviews.kt`, `ArtistDetailPreviews.kt`

Extracted shared song, album, and artist preview constants into `PreviewData.kt`. Both preview files now reference the shared data.

---

### ~~I5 — `getArtistByName` is inefficient~~ FIXED
**Files**: `SongDao.kt`, `SongRepository.kt`, `SongRepositoryImpl.kt`, `LibraryViewModel.kt`

Added a direct `getArtistByName(name)` DAO query with `WHERE albumArtist = :name` instead of fetching all artists and filtering client-side.

---

### ~~I6 — `fetchMissingArtistImages` should use structured concurrency~~ FIXED
**File**: `LibraryViewModel.kt`

Stored the coroutine `Job` in `fetchArtistImagesJob`. Calling `fetchMissingArtistImages()` again cancels any in-flight fetch before starting a new one.

---

### ~~I7 — `DetailHeroImage` gradient uses hardcoded `Color.Black`~~ FIXED
**File**: `DetailHeroImage.kt`

Replaced `Color.Black.copy(alpha = 0.7f)` with `MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)` so the gradient adapts to light/dark theme.

---

## Summary

| Category     | Total | Fixed | Remaining |
|--------------|-------|-------|-----------|
| Bugs         | 9     | 9     | 0         |
| Gaps         | 7     | 7     | 0         |
| Improvements | 7     | 7     | 0         |

All items resolved.