---
type: design
feature: tabs
phase: 7
status: complete
tags:
  - type/design
  - status/complete
  - feature/tabs
---

# Tabs Package Improvements - Design

## Overview

Comprehensive improvements to the `ui/home/tabs` package: fixed 4 critical bugs, aligned state management across all 6 tabs, wired missing features (sort options, overflow menus, playback indicators, playlist art URIs), implemented a cross-cutting AddToPlaylist flow with a modal playlist picker, and added testing infrastructure (testable composables, previews, ViewModel unit tests) and accessibility improvements.

## What was built

### Phase 1: Bug Fixes
- `tabs/FavoriteTabScreen.kt`: Fixed sort type mismatch (String → SortOrder enum)
- `tabs/HistoryTabScreen.kt`: Fixed albums section conditional (`recentAlbums.isNotEmpty()`)
- `tabs/PlaylistsTabScreen.kt`: Added `RenamePlaylistDialog` composable (was writing empty string)
- `tabs/AlbumsTabScreen.kt`, `tabs/ArtistsTabScreen.kt`: Removed incorrect `+1` scroll offsets, dead code

### Phase 2: State Architecture Alignment
- `library/LibraryViewModel.kt`: Added `albumSortOrder`/`artistSortOrder` MutableStateFlows with `onAlbumSortOrder()`/`onArtistSortOrder()` setters; rewired `albumsFlow` and `artistsFlow` to use independent sort orders via `combine` + `flatMapLatest`
- `library/MetadataViewModel.kt`: Added `favoriteSortOrder` StateFlow with `onFavoriteSortOrder()` setter
- `tabs/AlbumsTabScreen.kt`, `tabs/ArtistsTabScreen.kt`, `tabs/FavoriteTabScreen.kt`: Removed local sort state; receive `sortOrder`/`onSortOrderChanged` from parent
- `tabs/FavoriteTabScreen.kt`, `tabs/HistoryTabScreen.kt`: Added `currentSongId`/`isPlaying` params for playback indicators
- `home/HomeScreen.kt`: Wired `albumSortOrder`, `artistSortOrder`, `favoriteSortOrder`, `isCurrentlyPlaying` to all tabs

### Phase 3: Data Wiring
- `data/local/PlaylistDao.kt`: Added `getArtUrisForPlaylist()` query (JOIN songs + playlist_songs, DISTINCT, LIMIT 4)
- `data/PlaylistRepository.kt`, `data/PlaylistRepositoryImpl.kt`: Added `getArtUrisForPlaylist()` method
- `playlists/PlaylistViewModel.kt`: Changed `uiState` to `flatMapLatest` + `combine` to fetch art URIs per playlist reactively
- `playlists/PlaylistUiState.kt`: Added `playlistArtUris: Map<Long, List<String>>` to `Loaded`
- `tabs/HistoryTabScreen.kt`: Wired shuffle/play-all by filtering `recentSongs` for album/artist sections
- `tabs/AlbumsTabScreen.kt`, `tabs/ArtistsTabScreen.kt`: Wired `onMenuClick` to navigate to detail screens

### Phase 4: AddToPlaylist
- `components/PlaylistPickerSheet.kt`: **New file** — modal bottom sheet listing playlists with "Create new playlist" inline text field
- `tabs/SongsTabScreen.kt`, `tabs/FavoriteTabScreen.kt`, `tabs/HistoryTabScreen.kt`: Added `SongMenuAction.AddToPlaylist` to song menus with `onAddToPlaylist` callback
- `home/HomeScreen.kt`: Added `pendingSongIds` state, `PlaylistViewModel` dependency, and `PlaylistPickerSheet` rendering

### Phase 5: Testing & Polish
- `res/values/strings.xml`: Extracted 26 string resources (13 user-facing + 13 accessibility)
- `components/AlphabeticalIndexSidebar.kt`: Added `contentDescription` to each letter via `Modifier.semantics`
- `components/AlbumItem.kt`: Added contextual `contentDescription` to play button, menu button, album art; increased touch targets from 32dp to 48dp
- `components/ArtistItem.kt`: Added contextual `contentDescription` to play/pause and menu icons
- `components/SongListItem.kt`: Added "Now playing" semantics, grouped rating description, contextual remove/reorder descriptions
- `tabs/PlaylistsTabScreen.kt`: Extracted `PlaylistsTabScreenContent` for testable composable pattern
- All 6 tabs: Added `@Preview` composables with sample data
- `test/.../LibraryViewModelTest.kt`: +6 tests for album/artist sort order logic
- `test/.../MetadataViewModelTest.kt`: +2 tests for favorite sort order
- `test/.../PlaylistViewModelTest.kt`: +3 tests for art URI fetching logic
- `test/.../FakePlaylistRepository.kt`: Enhanced with configurable `artUrisMap`

## Design decisions

- **Sort order in ViewModels (session-scoped)**: Sort orders are stored in MutableStateFlows within ViewModels, not persisted to disk. Simpler implementation; DataStore persistence is a future improvement.
- **Playlist picker at HomeScreen level**: `PlaylistPickerSheet` is rendered in `HomeScreen` with `pendingSongIds` state, avoiding the need to thread playlist data through every tab. Any tab sets `pendingSongIds` → sheet appears → user picks → songs added.
- **Album/Artist menus deferred**: "Play Next" and "Add to Queue" for albums/artists are deferred until queue management is fully implemented. Only Play and navigate-to-detail are wired.
- **48dp touch targets via wrapper Box**: For AlbumGridItem play/menu buttons, a 48dp clickable Box wraps the 32dp visual circle, preserving the visual design while meeting accessibility minimums.
- **Accessibility via string resources**: All `contentDescription` values use `stringResource()` for localization, with `%1$s` format args for contextual labels (e.g., "Play Abbey Road").

## Known gaps

- Sort orders not persisted across app restarts (session-scoped only)
- "Play Next" / "Add to Queue" not available for albums and artists (requires queue wiring)
- ArtistItem play/pause and menu icons are 20dp/18dp visual size — touch target is the pill container, but individual icon clickable areas are below 48dp within the pill
- No UI/instrumented tests (only ViewModel unit tests added)

---

**Plan doc**: [[tabs-plan]]
