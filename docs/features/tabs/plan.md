# Tabs Package Improvements - Plan

**Status: Complete** — All 5 phases implemented and verified. See `design.md` for final summary.

## Context

The `ui/home/tabs` package contains 6 tab screens (Songs, Albums, Artists, Favorites, History, Playlists) hosted by `HomeScreen` via `HorizontalPager`. A thorough analysis revealed 4 critical bugs, 6 missing features, 6 architecture inconsistencies, and 4 polish/testing gaps. This plan addresses all of them in priority order.

## Scope

**Included:**
- Fix all critical bugs (type mismatches, wrong conditionals, broken handlers, scroll offsets)
- Align state management patterns across all tabs
- Wire missing feature handlers (sort options, overflow menus, playback state, playlist art)
- Implement playlist picker dialog for AddToPlaylist flow
- Add testable composable wrappers and previews
- Extract hardcoded strings to resources
- Improve accessibility labels

**Excluded:**
- Playback service implementation (separate feature)
- Media scanning (separate feature)
- Sync functionality (separate feature)
- New UI designs or layout changes beyond fixing existing issues

## Analysis: Issues Found

### Critical Bugs

| ID | File                                         | Line(s)    | Description                                                                                                                                 |
|----|----------------------------------------------|------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| C1 | `FavoriteTabScreen.kt`                       | 26, 47, 62 | `selectedSort` is `String` but `SortFilterBar` expects `SortOrder` enum. Type mismatch causes silent failure or crash.                      |
| C2 | `HistoryTabScreen.kt`                        | 118        | "Recently played albums" section checks `recentSongs.isNotEmpty()` instead of `recentAlbums.isNotEmpty()`. Albums section may never render. |
| C3 | `PlaylistsTabScreen.kt`                      | 89         | `onRename` passes empty string `""` as new name, creating blank playlist names. Needs a rename dialog.                                      |
| C4 | `AlbumsTabScreen.kt` / `ArtistsTabScreen.kt` | 108 / 118  | Scroll offset `+1` is wrong — sort bar is floating and doesn't consume scroll space. Skips first item.                                      |

### Missing Features

| ID | File                    | Line(s)   | Description                                                                               |
|----|-------------------------|-----------|-------------------------------------------------------------------------------------------|
| F1 | `SongsTabScreen.kt`     | 139       | `AddToPlaylist` action disabled — TODO says "re-add when playlist picker is implemented". |
| F2 | `AlbumsTabScreen.kt`    | 117       | Only 2 of 6 `SortOrder` options wired (BY_ALBUM, BY_ARTIST).                              |
| F3 | `ArtistsTabScreen.kt`   | —         | No sort options wired; hardcoded to BY_ARTIST only.                                       |
| F4 | `PlaylistsTabScreen.kt` | 55-64, 94 | Empty state UI commented out. Art URIs always `emptyList()`.                              |
| F5 | `FavoriteTabScreen.kt`  | 74        | `isPlaying = false` hardcoded — no current-song indicator.                                |
| F6 | `HistoryTabScreen.kt`   | 124-125   | Shuffle/play-all handlers empty `{}` for albums and artists.                              |

### Architecture & Consistency

| ID | Description                                                                                                                                  |
|----|----------------------------------------------------------------------------------------------------------------------------------------------|
| A1 | **Inconsistent state hoisting** — SongsTab receives `sortOrder` from parent; Albums/Artists/Favorites manage it locally in composable state. |
| A2 | **Inconsistent playback state** — Most tabs don't reflect playing/paused status. Only SongsTab partially wires it.                           |
| A3 | **Local sorting** — FavoriteTabScreen and HistoryTabScreen sort client-side instead of delegating to ViewModel.                              |
| A4 | **No error state** in `PlaylistViewModel` / `PlaylistUiState`.                                                                               |
| A5 | **Empty lambda handlers** — `onMenuClick = {}` in Albums/Artists tabs (no overflow menu functionality).                                      |
| A6 | **Dead code** — ArtistsTabScreen has empty full-width grid item (lines 88-89).                                                               |

### Polish & Testing

| ID | Description                                                                                                                         |
|----|-------------------------------------------------------------------------------------------------------------------------------------|
| P1 | No testable content composables (only SongsTabScreen has `SongsTabScreenContent`).                                                  |
| P2 | No `@Preview` composables in Albums, Artists, Favorites, History tabs.                                                              |
| P3 | Hardcoded strings not using `strings.xml` resources ("No songs found", "Recently played", sort labels, etc.).                       |
| P4 | No accessibility labels on sidebar letters, grid items, play/pause buttons. Touch targets inconsistent (some 32dp vs 48dp minimum). |

---

## Approach

Work in 5 phases, each independently buildable and testable. Each phase builds on the previous but can be paused after any phase.

**Why this ordering:**
1. Bug fixes first — broken code blocks accurate testing of everything else
2. State alignment second — consistent architecture makes feature wiring cleaner
3. Feature wiring third — depends on clean state management
4. Playlist picker fourth — cross-cutting feature that touches multiple tabs
5. Polish last — testability and accessibility improvements on stable code

---

## Tasks

### Phase 1: Critical Bug Fixes

**Goal:** Fix the 4 bugs that break existing functionality.

#### Task 1.1 — Fix FavoriteTabScreen sort type mismatch (C1)
- **File:** `FavoriteTabScreen.kt`
- **What:** Replace `faveSortOptions: List<String>` with `List<SortOrder>` using `SortOrder.BY_TITLE`, `SortOrder.BY_ARTIST`, `SortOrder.BY_PLAY_COUNT` (closest to "Rating")
- **What:** Change `selectedSort` from `mutableStateOf(faveSortOptions.first())` (String) to `mutableStateOf(SortOrder.BY_TITLE)` (SortOrder)
- **What:** Fix `onSortClick` lambda to assign `SortOrder` directly instead of `it.label`
- **Why:** Type mismatch causes sort to silently fail or crash at runtime

#### Task 1.2 — Fix HistoryTabScreen albums conditional (C2)
- **File:** `HistoryTabScreen.kt`
- **What:** Change line 118 from `if (recentSongs.isNotEmpty())` to `if (recentAlbums.isNotEmpty())`
- **Why:** Albums section never renders when there are recent albums but no recent songs

#### Task 1.3 — Fix PlaylistsTabScreen rename with empty string (C3)
- **File:** `PlaylistsTabScreen.kt`
- **What:** Add a `RenamePlaylistDialog` composable that shows a text field pre-filled with the current name. On confirm, dispatch `PlaylistEvent.RenamePlaylist(id, newName)`. Wire it via a `showRenameDialog` state variable.
- **Why:** Current code creates blank playlist names

#### Task 1.4 — Fix scroll offset in Albums/Artists tabs (C4)
- **Files:** `AlbumsTabScreen.kt`, `ArtistsTabScreen.kt`
- **What:** Remove the `+1` from `gridState.scrollToItem(targetIndex + 1)` → `gridState.scrollToItem(targetIndex)`
- **Why:** Sort bar is floating (not a grid item), so +1 skips the actual first item

#### Verification — Phase 1
- `assembleDebug` succeeds
- `test` passes
- Manual: Open Favorites tab → sort options work without crash. Open History → albums section appears. Rename playlist → dialog appears. Tap sidebar letter in Albums → scrolls to correct item.

---

### Phase 2: State Architecture Alignment

**Goal:** Make all tabs follow the same state hoisting pattern as SongsTabScreen.

#### Task 2.1 — Hoist sort order for AlbumsTabScreen (A1)
- **Files:** `AlbumsTabScreen.kt`, `LibraryViewModel.kt`, `HomeScreen.kt`
- **What:** Add `albumSortOrder: StateFlow<SortOrder>` to `LibraryViewModel`. Remove local `selectedSort` from `AlbumsTabScreen`. Pass `sortOrder` and `onSortOrderChanged` from `HomeScreen` through to `AlbumsTabScreen`.
- **Why:** Consistent with SongsTabScreen pattern; sort order survives recomposition and is testable via ViewModel

#### Task 2.2 — Hoist sort order for ArtistsTabScreen (A1)
- **Files:** `ArtistsTabScreen.kt`, `LibraryViewModel.kt`, `HomeScreen.kt`
- **What:** Same pattern as Task 2.1. Add `artistSortOrder: StateFlow<SortOrder>` to `LibraryViewModel`.
- **Why:** Same reasons as 2.1

#### Task 2.3 — Hoist sort order for FavoriteTabScreen (A1, A3)
- **Files:** `FavoriteTabScreen.kt`, `MetadataViewModel.kt`, `HomeScreen.kt`
- **What:** Add `favoriteSortOrder: StateFlow<SortOrder>` to `MetadataViewModel`. Move sorting logic from composable into ViewModel (expose sorted favorites as `StateFlow`). Remove local sort state.
- **Why:** Sorting in composable is untestable and inconsistent with other tabs

#### Task 2.4 — Wire unified playback state (A2)
- **Files:** All 6 tab screens, `HomeScreen.kt`
- **What:** Ensure `HomeScreen` passes both `currentSongId` and `isPlaying` (from `PlayerViewModel`) to every tab. Replace all `isPlaying = false` hardcoded values with the actual state.
- **Why:** Users can't see which song is playing in Favorites, History, or Playlists tabs

#### Task 2.5 — Remove dead code (A6)
- **File:** `ArtistsTabScreen.kt`
- **What:** Remove the empty `item(span = { GridItemSpan(maxLineSpan) }) {}` block at lines 88-89
- **Why:** Dead code with no purpose

#### Task 2.6 — Uncomment and fix PlaylistsTabScreen empty state (F4 partial)
- **File:** `PlaylistsTabScreen.kt`
- **What:** Uncomment the empty state block (lines 55-64). Fix layout so `PlaylistsActionBar` is always visible and "No playlists yet" message is centered below it.
- **Why:** Users see a blank screen when they have no playlists

#### Verification — Phase 2
- `assembleDebug` succeeds
- `test` passes
- Manual: Change sort in Albums tab → survives tab switch. Playing song shows indicator in all tabs. Empty playlists tab shows message.

---

### Phase 3: Missing Feature Wiring

**Goal:** Connect stub handlers to real functionality.

#### Task 3.1 — Wire remaining sort options for AlbumsTabScreen (F2)
- **Files:** `AlbumsTabScreen.kt`, `LibraryViewModel.kt`
- **What:** Expand `sortOptions` to include `BY_DATE_ADDED` and `BY_DURATION` in addition to `BY_ALBUM` and `BY_ARTIST`. Add sorting logic in ViewModel for each.
- **Why:** Users can only sort albums by 2 criteria; 4 are relevant for albums

#### Task 3.2 — Wire sort options for ArtistsTabScreen (F3)
- **Files:** `ArtistsTabScreen.kt`, `LibraryViewModel.kt`
- **What:** Add `sortOptions = listOf(SortOrder.BY_ARTIST, SortOrder.BY_PLAY_COUNT)` and wire the sort logic.
- **Why:** Artists tab has no sorting capability

#### Task 3.3 — Implement Album/Artist overflow menus (A5)
- **Files:** `AlbumsTabScreen.kt`, `ArtistsTabScreen.kt`
- **What:** Create `AlbumMenuAction` and `ArtistMenuAction` sealed interfaces (Play, PlayNext, AddToQueue). Wire `onMenuClick` to show a dropdown with these actions. Handle actions by collecting album/artist songs and delegating to `PlayerViewModel`.
- **Why:** Overflow menu buttons exist but do nothing

#### Task 3.4 — Wire HistoryTabScreen shuffle/play-all (F6)
- **Files:** `HistoryTabScreen.kt`
- **What:** Wire `onShuffle` and `onPlayAll` lambdas for album and artist sections. Collect songs for the selected album/artist and pass to `PlayerViewModel.play(songs, startIndex)`.
- **Why:** Shuffle and play-all buttons in history are non-functional

#### Task 3.5 — Wire playlist art URIs (F4 complete)
- **Files:** `PlaylistsTabScreen.kt`, `PlaylistRepository.kt` (or `PlaylistDao.kt`)
- **What:** Add a query to fetch the first 4 album art URIs for each playlist's songs. Expose as part of playlist data. Pass to `PlaylistItem`'s `artUris` parameter.
- **Why:** All playlists show generic music-note fallback instead of collage art

#### Task 3.6 — Wire FavoriteTabScreen playback indicator (F5)
- **File:** `FavoriteTabScreen.kt`
- **What:** Replace `isPlaying = false` with actual check: `isPlaying = song.id == currentSongId && isCurrentlyPlaying`
- **Why:** Already addressed by Task 2.4 but verify it's working after sort hoisting changes

#### Verification — Phase 3
- `assembleDebug` succeeds
- `test` passes
- Manual: Sort albums by date → correct order. Long-press album → menu appears with Play/PlayNext/AddToQueue. History shuffle button plays shuffled songs. Playlists show album art collage.

---

### Phase 4: Playlist Picker & AddToPlaylist

**Goal:** Implement the cross-cutting AddToPlaylist feature.

#### Task 4.1 — Create PlaylistPickerDialog
- **New file:** `ui/components/PlaylistPickerDialog.kt`
- **What:** Modal bottom sheet listing all playlists with artwork. Includes "Create new playlist" option at top. On selection, returns the playlist ID. On "Create new", shows inline text field to name and create, then auto-selects it.
- **Why:** Required by AddToPlaylist flow; reusable across all tabs

#### Task 4.2 — Add PlaylistEvent.AddSongs
- **File:** `PlaylistViewModel.kt`
- **What:** Add `AddSongs(playlistId: Long, songIds: List<Long>)` to `PlaylistEvent` sealed interface. Handle in ViewModel by calling `PlaylistRepository.addSongsToPlaylist()`.
- **Why:** Backend for the AddToPlaylist action

#### Task 4.3 — Wire AddToPlaylist in SongsTabScreen (F1)
- **File:** `SongsTabScreen.kt`
- **What:** Uncomment `SongMenuAction.AddToPlaylist` from `menuActions`. Add state for showing `PlaylistPickerDialog`. On selection, dispatch `PlaylistEvent.AddSongs`.
- **Why:** This TODO has been waiting for the playlist picker

#### Task 4.4 — Wire AddToPlaylist in Album/Artist overflow menus
- **Files:** `AlbumsTabScreen.kt`, `ArtistsTabScreen.kt`
- **What:** Add `AddToPlaylist` to `AlbumMenuAction` and `ArtistMenuAction`. Show `PlaylistPickerDialog`, then add all album/artist songs to selected playlist.
- **Why:** Consistent AddToPlaylist availability across all content types

#### Verification — Phase 4
- `assembleDebug` succeeds
- `test` passes
- Manual: Long-press song → "Add to playlist" → picker shows → select playlist → song added. Same flow works from album and artist overflow menus.

---

### Phase 5: Testing & Polish

**Goal:** Testability, previews, string resources, accessibility.

#### Task 5.1 — Extract testable content composables (P1)
- **Files:** `AlbumsTabScreen.kt`, `ArtistsTabScreen.kt`, `FavoriteTabScreen.kt`, `HistoryTabScreen.kt`, `PlaylistsTabScreen.kt`
- **What:** For each tab, extract a `*TabScreenContent(state, callbacks)` composable that receives data and callbacks as parameters (no ViewModel). The outer `*TabScreen` composable collects ViewModel state and delegates.
- **Why:** Enables UI testing without Hilt/ViewModel setup

#### Task 5.2 — Add @Preview composables (P2)
- **Files:** All 6 tab screens
- **What:** Add `@Preview` annotated composables showing both empty state and populated state (with sample data).
- **Why:** Enables visual verification in Android Studio without running the app

#### Task 5.3 — Extract hardcoded strings to resources (P3)
- **Files:** All 6 tab screens + `app/src/main/res/values/strings.xml`
- **What:** Extract all user-visible strings: "No songs found", "No playlists yet", "Recently played", "Shuffle", "Play All", sort option labels, section headers, etc. Use `stringResource(R.string.*)` in composables.
- **Why:** Required for localization and consistent with project guidelines (no hardcoded strings)

#### Task 5.4 — Add accessibility labels (P4)
- **Files:** All 6 tab screens, relevant components
- **What:** Add `contentDescription` to: sidebar letters ("Jump to letter A"), play/pause buttons ("Play song / Pause song"), grid items ("Album: name by artist"), sort buttons. Ensure all interactive elements have minimum 48dp touch targets.
- **Why:** Screen reader support and Material Design touch target compliance

#### Task 5.5 — Add ViewModel unit tests for new logic
- **New files:** Tests for sort order persistence, playlist art query, AddSongs event
- **What:** Test `LibraryViewModel` album/artist sort order changes. Test `MetadataViewModel` favorite sort order. Test `PlaylistViewModel.AddSongs` event. Use Turbine for StateFlow assertions.
- **Why:** New ViewModel logic from Phases 2-4 needs test coverage

#### Verification — Phase 5
- `assembleDebug` succeeds
- `test` passes (including new tests)
- Manual: Previews render in Android Studio. TalkBack reads all interactive elements. No hardcoded strings in tab screens.

---

## Dependencies

No new library dependencies required. All work uses existing stack (Compose, Material 3, Hilt, Room, Coil, Navigation).

## Decisions (resolved)

1. **Sort order persistence** — ViewModel only (session-scoped). Future improvement: persist to DataStore.
2. **Playlist picker style** — Modal bottom sheet.
3. **Album/Artist menu actions** — Defer "Play Next" and "Add to Queue" until queue management is implemented. Only include Play and AddToPlaylist for now.
4. **Accessibility scope** — Fix both tab screens AND shared components (`SongItem`, `AlbumGridItem`, etc.).

## Verification

After all phases:
- `assembleDebug` succeeds after each task
- `test` passes all tests (existing + new)
- Manual verification per phase (see individual phase verification sections)
- All 6 tabs follow consistent state hoisting pattern
- No hardcoded strings remain in tab screens
- All interactive elements have accessibility labels
- Testable content composables exist for all tabs
