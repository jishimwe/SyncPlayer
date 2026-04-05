---
type: plan
feature: history-tab-refactor
status: planned
tags:
  - type/plan
  - status/planned
  - feature/history-tab-refactor
---

# History Tab Refactor Plan

## Goal
Replace the binary show/hide collapse behaviour with a **preview-first** pattern: sections default to collapsed but show a meaningful subset of data, expanding to the full list on demand.

## Problems with current implementation
- All sections default to `expanded = true` → screen is overwhelming on first open
- Collapsed state = completely empty (no data shown at all)
- `albumSongs` / `artistSongs` filtering runs inside `stickyHeader` lambdas, recomputing on every recomposition
- No animation on section expand/collapse

---

## Tasks

- [ ] **Default all sections to collapsed** — change `mutableStateOf(true)` → `mutableStateOf(false)` for `songsExpanded`, `albumsExpanded`, `artistsExpanded`

- [ ] **Add preview constants** — define `SONGS_PREVIEW = 5`, `GRID_PREVIEW = 4` (= 2 rows) at file level

- [ ] **Songs section: preview mode**
  - Collapsed: emit first `SONGS_PREVIEW` song items
  - When `!expanded && recentSongs.size > SONGS_PREVIEW`: add a `"Show N more"` footer item that sets `songsExpanded = true`

- [ ] **Albums section: preview mode**
  - Collapsed: emit first `GRID_PREVIEW / 2` album rows (i.e. 2 rows)
  - Same `"Show N more"` footer pattern

- [ ] **Artists section: preview mode**
  - Same as albums

- [ ] **Add `count` param to `CollapsibleSectionHeader`**
  - Optional `Int?`, displayed as `"Title (N)"` in the header text when non-null
  - Lets users know section size without expanding

- [ ] **Hoist derived list computation**
  - Move `albumSongs` and `artistSongs` filtering above the `LazyColumn`, not inside sticky header lambdas

- [ ] **Add enter/exit animation**
  - Items appearing via `Modifier.animateItem()` on each `SongItem` / album row / artist row
  - Smooth enough without requiring `AnimatedVisibility` wrappers (which don't compose well inside `LazyColumn`)

- [ ] **Update `HistoryTabScreenPreview`** to include multiple songs/albums/artists so both collapsed and expanded states are testable in the preview

---

## Out of scope
- ViewModel changes (no new data needed)
- Changes to `AlbumGridItem` or `ArtistItem` components
- Horizontal LazyRow variant (grid preview is sufficient)