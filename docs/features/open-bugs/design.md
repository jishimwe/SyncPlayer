# `ui/library` Package Cleanup — Design Doc

**Date**: 2026-03-21
**Status**: Complete
**Analysis**: [`ui-library-analysis.md`](ui-library-analysis.md)

---

## Summary

Full audit and cleanup of the `ui/library` package. Found and resolved 9 bugs, 7 feature gaps, and 7 improvements. All items verified with `assembleDebug` and unit tests (125 tests passing).

---

## Changes by Category

### Bugs Fixed (9/9)

| ID | Fix | Files |
|----|-----|-------|
| B1 | `formatDuration` — fixed hours/minutes math, removed AM/PM branching | `DurationFormatter.kt` |
| B2 | Dead code — `LibraryScreen.kt` deleted (HomeScreen handles tabs) | `LibraryScreen.kt` (deleted) |
| B3 | Same as B2 — sort init resolved by deleting dead code | `LibraryScreen.kt` (deleted) |
| B4 | Wired `_favoriteSortOrder` into `combine` flow | `MetadataViewModel.kt` |
| B5 | Init `lastScanTimestamp` to `currentTimeMillis()` to prevent double refresh | `LibraryViewModel.kt` |
| B6 | Deleted duplicate `AlbumGridItem.kt` with unused `isPlaying` param | `AlbumGridItem.kt` (deleted) |
| B7 | Same as B6 — canonical version in `ui/components/AlbumItem.kt` | `AlbumGridItem.kt` (deleted) |
| B8 | Wrapped `getArtistImageUrl` in try/catch per artist | `LibraryViewModel.kt` |
| B9 | Removed redundant `LocalContext.current`, reused `context` | `PermissionHandler.kt` |

### Gaps Closed (7/7)

| ID | Fix | Files |
|----|-----|-------|
| G1 | Dead code — LibraryScreen deleted; HomeScreen routes to tab screens | `LibraryScreen.kt` (deleted) |
| G2 | Wired `onAddToPlaylist` through detail screens + PlaylistPickerSheet at NavGraph | `AlbumDetailScreen.kt`, `ArtistDetailScreen.kt`, `NavGraph.kt` |
| G3 | Replaced overflow menu TODO stubs with working DropdownMenu | `AlbumDetailTopBar.kt`, `ArtistDetailTopBar.kt` |
| G4 | Added `MetadataUiState.Error(message)` + `.catch` on combine flow + error UI | `MetadataViewModel.kt`, `HomeScreen.kt` |
| G5 | Dead code — deleted with LibraryScreen | `LibraryScreen.kt` (deleted) |
| G6 | Wrapped HorizontalPager in Material 3 `PullToRefreshBox` | `HomeScreen.kt` |
| G7 | Dead code — `ArtistListItem.kt` deleted; canonical `ArtistItem` shows art | `ArtistListItem.kt` (deleted) |

### Improvements Completed (7/7)

| ID | Fix | Files |
|----|-----|-------|
| I1 | Already resolved — `SortFilterBar` was already the shared component | (no changes) |
| I2 | Dead code — LibraryScreen.kt + test deleted | `LibraryScreen.kt`, `LibraryScreenTest.kt` (deleted) |
| I3 | Extracted shared `DetailTopBar` component; both top bars are thin wrappers | `DetailTopBar.kt` (new), `AlbumDetailTopBar.kt`, `ArtistDetailTopBar.kt` |
| I4 | Shared `PreviewData.kt` for preview constants | `PreviewData.kt` (new), `AlbumDetailPreviews.kt`, `ArtistDetailPreviews.kt` |
| I5 | Direct DAO query `getArtistByName` instead of fetch-all + filter | `SongDao.kt`, `SongRepository.kt`, `SongRepositoryImpl.kt`, `LibraryViewModel.kt` |
| I6 | Store `Job` for cancellation support in `fetchMissingArtistImages` | `LibraryViewModel.kt` |
| I7 | `MaterialTheme.colorScheme.surface` instead of hardcoded `Color.Black` | `DetailHeroImage.kt` |

---

## Dead Code Removed

| File | Reason |
|------|--------|
| `ui/library/LibraryScreen.kt` | Replaced by `HomeScreen` + `ui/home/tabs/` |
| `ui/library/AlbumGridItem.kt` | Duplicate of `ui/components/AlbumItem.kt` |
| `ui/library/ArtistListItem.kt` | Replaced by `ui/components/ArtistItem.kt` |
| `test/.../LibraryScreenTest.kt` | Test for deleted LibraryScreen |

## New Files

| File | Purpose |
|------|---------|
| `ui/library/DetailTopBar.kt` | Shared top bar for album/artist detail screens |
| `ui/library/PreviewData.kt` | Shared preview Song/Album/Artist constants |
| `ui/components/PlaylistPickerSheet.kt` | ModalBottomSheet for playlist selection (used by HomeScreen + NavGraph) |

---

## Patterns Used

- **PlaylistPickerSheet wiring**: State-hoisted `pendingSongIds: List<Long>?` at the host level (HomeScreen, NavGraph). Setting it non-null shows the sheet; selecting a playlist fires `PlaylistEvent.AddSongsToPlaylist` and clears the state.
- **Thin wrapper delegation**: `AlbumDetailTopBar` and `ArtistDetailTopBar` format their subtitle string and delegate entirely to `DetailTopBar`.
- **DAO-level filtering**: `getArtistByName` moved from `getAllArtists().map { find }` to a direct `WHERE albumArtist = :name` query.