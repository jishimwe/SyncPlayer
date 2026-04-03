---
type: plan
feature: songs-tab-refactor
status: planned
tags:
  - type/plan
  - status/planned
  - feature/songs-tab-refactor
---

# SongsTabScreen Refactor — Plan

## Context

`SongsTabScreen.kt` has a critical sorting bug and several code quality issues accumulated during rapid feature development in Phases 1–7. Sorting is broken end-to-end: three independent sort states exist across `SongsTabScreen`, `SortFilterBar`, and `LibraryViewModel`, none of which are connected. The screen also lacks the testable composable pattern required by the style guide and has stale/dead code.

## Scope

**Included:**
- Fix broken sort wiring (P0 — functional bug)
- Clean up `SortFilterBar` to be a controlled component
- Clean up `LibraryViewModel` dead/duplicate sort code
- Add testable composable pattern (`SongsTabScreenContent`)
- Adapt alphabetical sidebar to sort order
- Optimize letter-index lookup performance
- Remove dead `else -> {}` branch
- Handle `AddToPlaylist` action (hide or stub with TODO)
- Add `animateItem()` to LazyColumn items

**Excluded:**
- Moving shared components from `ui/player/components/` to `ui/components/` (large cross-cutting refactor, separate plan)
- Implementing `AddToPlaylist` (Phase 3 scope, tracked elsewhere)
- Adding accessibility content descriptions (separate pass)
- Adding unit tests (separate Layer 5 — will be covered after implementation)

## Approach

### The Sort Bug (3 disconnected states)

The sorting system is broken across 3 layers:

1. **`SortFilterBar`** (line 88): has its own `var selectedSortOrder by remember { mutableStateOf(SortOrder.BY_TITLE) }` and displays `selectedSortOrder.label` on line 97, **completely ignoring the `sortLabel` parameter** it receives.

2. **`SongsTabScreen`** (line 53): has `var selectedSort by remember { mutableStateOf(SortOrder.BY_TITLE) }` that updates when `SortFilterBar.onSortClick` fires, but **never applies it to the song list** and **never propagates it to the ViewModel**.

3. **`LibraryViewModel`**: has `_sortOrder` StateFlow with `onSortOrder(order)` to update it, and sorts songs in `songsFlow` (lines 107-116). But **nobody calls `onSortOrder()`** from `SongsTabScreen` or `HomeScreen`. Additionally, songs are sorted a **second time** in the `uiState` combine block (lines 165-182) with inconsistent cases (only handles 3 of 6 sort orders).

**Fix approach**: Single source of truth in `LibraryViewModel`.
- Make `SortFilterBar` a fully controlled component (receives `selectedSort`, no internal state)
- Remove local sort state from `SongsTabScreen`
- Pass `sortOrder` and `onSortOrderChanged` from `HomeScreen` → `SongsTabScreen`, backed by `LibraryViewModel`
- Remove duplicate sorting in `uiState` combine block (keep only the one in `songsFlow`)
- Remove dead `songsFlowOld`

### Alphabetical Sidebar

Currently always indexes by song title first letter. After fixing sort:
- For title/artist/album sorts → index by the active sort field's first letter
- For numeric sorts (duration, date, play count) → hide the sidebar (letters are meaningless)
- Pre-compute a `Map<Char, Int>` for O(1) scroll-to instead of `indexOfFirst` on every tap

### Testable Composable Pattern

Extract stateless `SongsTabScreenContent(songs, sortOrder, letters, ...)` per style guide. `SongsTabScreen` becomes a thin wrapper that receives ViewModel-backed state from `HomeScreen`.

## Tasks

### Task 1: Fix `SortFilterBar` — make it a controlled component

**Files:** `ui/player/components/SortFilterBar.kt`

**Changes:**
- Remove internal `selectedSortOrder` state (line 88)
- Add `selectedSort: SortOrder` parameter
- Display `selectedSort.label` instead of internal state's label (line 97)
- Remove unused `sortLabel: String` and `sortOptions: List<String>` parameters
- Update preview to pass `selectedSort`

**Why:** `SortFilterBar` should be stateless — caller owns the sort state. Currently `sortLabel` is accepted but ignored, and internal state makes the component uncontrollable.

### Task 2: Fix `LibraryViewModel` — remove duplicate/dead sort code

**Files:** `ui/library/LibraryViewModel.kt`

**Changes:**
- Delete `songsFlowOld` (lines 87-94) — dead code, replaced by `songsFlow`
- Delete `albumsFlowOld` (lines 120-127) — dead code, replaced by `albumsFlow`
- Remove duplicate sorting in `uiState` combine block (lines 164-182) — `songsFlow` already sorts, so the combine should just pass `songs` through directly
- Verify all 6 `SortOrder` cases are handled in `songsFlow` (they are: lines 108-116)

**Why:** Songs are sorted twice with inconsistent logic. The `songsFlow` sort handles all 6 cases correctly; the `uiState` combine sort only handles 3 and re-sorts data that's already sorted.

### Task 3: Wire sort state through `HomeScreen` → `SongsTabScreen`

**Files:** `ui/home/HomeScreen.kt`, `ui/home/tabs/SongsTabScreen.kt`

**Changes in `HomeScreen`:**
- Collect `libraryViewModel.sortOrder` as state
- Pass `sortOrder` and `onSortOrderChanged = libraryViewModel::onSortOrder` to `HomeScreenContent`
- `HomeScreenContent` forwards both to `SongsTabScreen`

**Changes in `SongsTabScreen`:**
- Add `sortOrder: SortOrder` parameter
- Add `onSortOrderChanged: (SortOrder) -> Unit` parameter
- Remove local `selectedSort` state (line 53)
- Remove `songSortOptions` top-level val (line 30) — no longer needed
- Pass `sortOrder` to `SortFilterBar.selectedSort`
- Pass `onSortOrderChanged` to `SortFilterBar.onSortClick`

**Why:** Sort state must live in the ViewModel to survive configuration changes and actually affect the data. The screen should be a pass-through.

### Task 4: Adapt alphabetical sidebar to sort order

**Files:** `ui/home/tabs/SongsTabScreen.kt`

**Changes:**
- Compute `letters` based on active sort order:
  - `BY_TITLE` → `song.title.first().uppercaseChar()`
  - `BY_ARTIST` → `song.artist.first().uppercaseChar()`
  - `BY_ALBUM` → `song.album.first().uppercaseChar()`
  - `BY_DURATION`, `BY_DATE_ADDED`, `BY_PLAY_COUNT` → hide sidebar entirely
- Same logic for `onLetterSelected` scroll target
- Pre-compute `letterIndexMap: Map<Char, Int>` (first occurrence index for each letter) instead of `indexOfFirst` per tap → O(1) lookup
- Remember both `letters` and `letterIndexMap` keyed on `(songs, sortOrder)`

**Why:** The sidebar should reflect what the list is sorted by. Showing A-Z by title when sorted by artist is confusing. Numeric sorts have no meaningful letter index.

### Task 5: Add testable composable pattern

**Files:** `ui/home/tabs/SongsTabScreen.kt`

**Changes:**
- Rename current `SongsTabScreen` logic to `SongsTabScreenContent` (stateless, receives all data)
- Create new `SongsTabScreen` as thin wrapper that just delegates to `SongsTabScreenContent`
- `SongsTabScreenContent` signature:
  ```kotlin
  @Composable
  fun SongsTabScreenContent(
      songs: List<Song>,
      sortOrder: SortOrder,
      currentSongId: Long?,
      onSongClick: (songs: List<Song>, index: Int) -> Unit,
      onSortOrderChanged: (SortOrder) -> Unit,
      onNavigateToArtist: (String) -> Unit,
      onNavigateToAlbum: (Long, String) -> Unit,
      onPlayNext: (Song) -> Unit,
      onAddToQueue: (Song) -> Unit,
      onPlayNow: (Song) -> Unit,
      modifier: Modifier = Modifier,
  )
  ```

**Why:** Style guide requires `FooScreen` + `FooScreenContent` split for testability and Compose Previews.

### Task 6: Clean up menu actions and dead code

**Files:** `ui/home/tabs/SongsTabScreen.kt`

**Changes:**
- Remove `else -> {}` catch-all (line 104) — all sealed members are handled; removing it enables exhaustiveness checking for future additions
- Remove `SongMenuAction.AddToPlaylist` from the `menuActions` list — don't show a no-op option to users. Add a `// TODO: re-add AddToPlaylist when playlist picker is implemented` comment where the action list is built

**Why:** `else -> {}` silently swallows future menu actions. Showing a non-functional menu item is bad UX.

### Task 7: Add item animations

**Files:** `ui/home/tabs/SongsTabScreen.kt`

**Changes:**
- Add `Modifier.animateItem()` to each `SongItem` inside `itemsIndexed`
- The `key` lambda already provides stable keys (`song.id`), so animations will work correctly on sort changes

**Why:** When the user changes sort order, items currently jump instantly. `animateItem()` gives smooth reordering transitions with no extra effort since keys are already set.

### Task 8: Build and verify

- Run `assembleDebug` after each task
- Run `test` after all tasks complete
- Manual verification:
  - Open Songs tab → change sort order → list re-sorts correctly
  - Sort by Artist → sidebar shows artist initials
  - Sort by Duration → sidebar hidden
  - Tap a sidebar letter → list scrolls to correct position
  - Rotate device → sort order preserved
  - Menu shows no AddToPlaylist option
  - Sort change animates smoothly

## Open Questions

1. **Should other tab screens (Albums, Artists, Favorites, History) also use the ViewModel's sort order?** Currently only `SongsTabScreen` has a sort bar. If we want consistent sort across tabs, the ViewModel already supports it, but the other tabs don't consume it. Recommendation: defer to a separate plan.

2. **Should `SortFilterBar` be moved to `ui/components/`?** It's in `ui/player/components/` but used by library tabs, not the player. This is part of the larger component relocation mentioned in Excluded scope. Recommendation: defer.

## Verification

- `assembleDebug` succeeds after each task
- `test` passes all tests
- Manual: sort order changes in the Songs tab correctly re-sort the list, sidebar adapts, animations play, sort survives rotation