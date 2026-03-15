# SyncPlayer UI Redesign Plan

## Context

Comprehensive UI refactor to match 12 Figma screens + a visual design spec document. The biggest structural change is replacing the bottom navigation bar with a horizontal scrollable top tab row. The visual language shifts to a Zune-inspired, content-forward aesthetic with frosted glass overlays, bold typography, and a strict accent color system.

**Source of truth**: `docs/features/ui-redesign/visual-design-spec.md`

### User Decisions
- **Most Played**: Merge into History tab as a section
- **Song overflow menu**: Standard set (Play Next, Add to Queue, Add to Playlist, Go to Artist, Go to Album)
- **Artist images**: Fetch from web via Deezer API (no API key required, returns high-res 1000x1000 images)

### Guesses / Open Questions (flagged)
- [GUESS] The spec says "repository layer — untouched" but History tab needs recently-played albums/artists data that doesn't currently exist in any DAO. Plan adds minimal DAO queries for this. If the intent is truly zero data-layer changes, History tab would only show songs.
- [GUESS] MiniPlayer -> Now Playing AnimatedContent expansion: the spec says "not navigation". This requires significant architectural change (removing the NowPlaying NavHost route and replacing with an overlay/AnimatedContent at the Scaffold level). Plan treats this as a Phase 6 change. If too risky, we can keep navigation for now and add the animation later.
- [RESOLVED] `SharedTransitionLayout` is supported (Compose BOM 2026.01.01). Implemented in Phase 7 for album art and artist portrait transitions between grid and detail screens.
- [GUESS] Frosted glass pre-API-31 fallback: not needed — Min SDK is 34.
- [RESOLVED] Palette-ktx dependency: added to version catalog as `palette = "1.0.0"`, library entry `palette = { group = "androidx.palette", name = "palette-ktx", version.ref = "palette" }`, accessed as `libs.palette` in build.gradle.kts.
- [GUESS] `Slider` for seek bar in Now Playing: Material `Slider` may need heavy customization or full replacement. Needs testing before committing.

---

## Custom Component Strategy

**Rule**: If a Material component does <70% of what's needed, replace with primitives.

| Component needed       | Material equivalent         | Verdict                                                       |
|------------------------|-----------------------------|---------------------------------------------------------------|
| Top bar with gradient  | `TopAppBar`                 | ❌ Replace with `Box` + `Brush.verticalGradient`               |
| Scrollable tab row     | `PrimaryScrollableTabRow`   | ❌ Replace with `Row` + `horizontalScroll` + `HorizontalPager` |
| Tab item (no ripple)   | `Tab`                       | ❌ Replace with `Text` + `noRippleClickable`                   |
| Artist detail sub-tabs | `SecondaryScrollableTabRow` | ❌ Same issue as main tabs — replace with custom `Row`         |
| Song list item         | `ListItem`                  | ❌ Too opinionated — fully custom at 72dp                      |
| Frosted glass pill     | none                        | ❌ Fully custom                                                |
| MiniPlayer pill        | none                        | ❌ Fully custom                                                |
| Artist grid item       | none                        | ❌ Fully custom (circle + overlapping pill)                    |
| Playlist collage       | none                        | ❌ Fully custom 2x2 grid                                       |
| Alphabet sidebar       | none                        | ❌ Fully custom with `pointerInput`                            |
| Seek bar               | `Slider`                    | ⚠️ Test first — may need custom                               |
| Collapsible section    | none                        | ❌ Custom with `AnimatedVisibility`                            |
| Sort/filter bar        | none                        | ❌ Custom `Row`                                                |
| Album grid item        | `Card`                      | ⚠️ Partial — needs corner radius + border + overlay           |
| Overflow dropdown      | `DropdownMenu`              | ✅ Fine as-is                                                  |
| Bottom sheet (Queue)   | `ModalBottomSheet`          | ✅ Fine as container, contents custom                          |
| Collapsible animation  | none                        | ✅ `AnimatedVisibility` works                                  |
| Hero parallax          | none                        | ❌ Custom `nestedScroll` + `graphicsLayer`                     |

**New reusable utility — add to `ui/theme/` or `ui/components/`:**
```kotlin
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    )
}
```

---

## Phase 0: Theme & Design Tokens

**Goal**: Update the theme system to match the visual design spec before any component work. Everything else builds on these tokens.

### Color Changes

**`ui/theme/Color.kt`**
- Add new accent color: `val AccentColor = Color(0xFFFF1D58)` (hot pink, replacing `#B9003A`)
- Add background override: `val BackgroundDark = Color(0xFF111113)` (near-black)
- Add frosted glass surface: `val FrostedGlassSurface = Color(0xFF1C1C1E)` (at 60-70% opacity)
- Keep existing Material color scheme tokens — only add/override the accent + background

**`ui/theme/Theme.kt`**
- Expose `AccentColor` via a `CompositionLocal` (e.g., `LocalAccentColor`) so all components can access it without passing it around
- Override `backgroundDark` to `#111113` in the dark color scheme
- Disable dynamic color by default (the design spec has a specific color language)
  - Current: `dynamicColor: Boolean = true` — change to `false`

**`ui/theme/Type.kt`**
- Add specific weight variants per the spec:
  - Display: 32sp, 800 ExtraBold (app name "SyncPlayer")
  - Title Large: 22sp, 700 Bold (active tab label, screen headings)
  - Title Medium: 16sp, 600 SemiBold (song title, playlist name, album title)
  - Body Medium: 14sp, 400 Regular (artist name, album name subtitle)
  - Label Small: 12sp, 400 Regular (metadata: counts, durations, timestamps)
- Current: uses only `FontFamily(Font(googleFont))` without weight variants
- Need to add multiple `Font()` entries with explicit `FontWeight` for Bold, SemiBold, ExtraBold
- Note: Google Fonts downloadable fonts may not support all weights. May need to bundle Nunito Sans font files in `res/font/` instead.

### New File: `ui/theme/GlassEffect.kt`
- Frosted glass `Modifier` extension for API 31+ (Min SDK 34, so no fallback needed):
```kotlin
fun Modifier.frostedGlass(alpha: Float = 0.65f, blurRadius: Dp = 10.dp): Modifier
```
- Combines: `BlurEffect` render effect + semi-transparent surface fill
- Used by: MiniPlayer, info overlays, Queue sheet, controls pill

### New Dependency: `androidx.palette:palette-ktx`
- Add to version catalog
- Used for album art color extraction on Now Playing screen

### Files
| File                        | Action                                                                            |
|-----------------------------|-----------------------------------------------------------------------------------|
| `ui/theme/Color.kt`         | Add AccentColor (#FF1D58), BackgroundDark (#111113), FrostedGlassSurface          |
| `ui/theme/Theme.kt`         | Add LocalAccentColor CompositionLocal, override background, disable dynamic color |
| `ui/theme/Type.kt`          | Add weight variants (ExtraBold, Bold, SemiBold) per spec                          |
| `ui/theme/GlassEffect.kt`   | Create — frosted glass modifier                                                   |
| `gradle/libs.versions.toml` | Add palette-ktx                                                                   |
| `app/build.gradle.kts`      | Add palette-ktx dependency                                                        |

### Verify
- `assembleDebug` passes
- Background is near-black (#111113) in dark theme
- Accent color renders as #FF1D58
- "SyncPlayer" title renders in 32sp ExtraBold

---

## Phase 1: Navigation Restructure ✅

**Goal**: Replace bottom NavigationBar with horizontal scrollable top tab row.

### What Changes

**`ui/navigation/NavGraph.kt`** — Major rewrite
- Remove `BottomNavDestination` enum and `NavigationBar`
- Add `ScrollableTabRow` with 6 tabs: History, Faves, Songs, Albums, Artists, Playlists
- **Tab styling per spec**: No underline indicator. Active tab = accent color + Title Large (22sp Bold). Inactive tab = white at reduced opacity + Title Medium (16sp SemiBold). Tabs overflow right.
- Top bar: "SyncPlayer" in Display style (32sp ExtraBold) left-aligned + search icon right
- Settings: gear icon in top bar actions (navigates to Settings screen)
- Top app bar background: transparent, gains frosted glass blur on scroll
- MiniPlayer stays bottom-anchored as a floating pill (see Phase 2 for MiniPlayer redesign)
- Tab content rendered inline (not via NavHost routes for tabs)
- NavHost still used for detail screens: AlbumDetail, ArtistDetail, PlaylistDetail, NowPlaying, Settings

New structure:
```
Scaffold(
    topBar = {
        Column {
            TopAppBar(
                title = "SyncPlayer" (Display, 32sp, ExtraBold),
                actions = [search icon, settings gear]
            )
            ScrollableTabRow(6 tabs, no indicator)
        }
    }
) { padding ->
    Box {
        // Tab content OR NavHost for detail screens
        // MiniPlayer floating at bottom with 8dp horizontal margin
    }
}
```

**`ui/library/LibraryScreen.kt`** — Decompose
- Remove internal `PrimaryTabRow` and `Scaffold`
- Extract tab content into separate files (Phases 3-4)
- `LibraryScreen` becomes a thin wrapper delegating to per-tab content
- Permission handling + lifecycle stays here

**`ui/library/LibraryViewModel.kt`** — Update tab enum
- Rename `LibraryTab` entries: `HISTORY`, `FAVES`, `SONGS`, `ALBUMS`, `ARTISTS`, `PLAYLISTS`
- Remove `MOST_PLAYED` and `RECENTLY_PLAYED` (merged into History)

**`Screen` sealed class** — Update
- Remove `Screen.Library` (becomes default tab host)
- Remove `Screen.Playlists` (becomes a tab)
- Keep: `Screen.NowPlaying`, `Screen.AlbumDetail`, `Screen.ArtistDetail`, `Screen.PlaylistDetail`, `Screen.Settings`
- Add `Screen.Home` for the tab host

### Files
| File                             | Action                       |
|----------------------------------|------------------------------|
| `ui/navigation/NavGraph.kt`      | Major rewrite                |
| `ui/library/LibraryScreen.kt`    | Decompose tabs out           |
| `ui/library/LibraryViewModel.kt` | Update enum, remove old tabs |

### Implementation Notes
- `Scaffold` abandoned — replaced with plain `Box` to avoid `topBar` slot constraining child widths
- `CustomTabRow` uses leading + trailing `Spacer(halfViewportDp)` so all tabs can scroll to center (Opus solution)
- Scroll target: `tabX + tabWidth/2 - viewportWidth/2` clamped to `scrollState.maxValue`
- `positionInParent()` gives layout-space positions which are correct for `animateScrollTo`
- `HorizontalPager` + `snapshotFlow { pagerState.targetPage }` syncs pager → tab; `LaunchedEffect(selectedTab)` syncs tab → pager
- Overlay height measured at runtime via `onGloballyPositioned` on the overlay `Column`

### Future Improvements
- **DockedSearchBar revamp**: current search UI uses Material3 `DockedSearchBar` which is visually inconsistent with the custom design. Replace with a custom search bar matching the spec — animated expand from the search icon, full-width input with frosted glass background, dismiss on back/outside tap.

### Verify ✅
- `assembleDebug` passes
- All 6 tabs render (even if content is placeholder)
- Active tab: accent color + larger text, no underline
- Settings accessible via gear icon
- MiniPlayer still works
- Detail screens still navigate correctly

---

## Phase 2: New Reusable Components ✅

**Goal**: Build shared UI components used across multiple screens.

### New Files

**`ui/components/SortFilterBar.kt`**
- `Row`: filter icon + sort label (dropdown), `Spacer(weight)`, shuffle `IconButton`, play `IconButton`
- **Sticky** on scroll (parent handles `stickyHeader` in LazyColumn or `nestedScroll`)
- Params: `sortLabel: String`, `onSortClick`, `onShuffle`, `onPlayAll`, `modifier`
- Used on: Faves, Albums, Artists, History sections

**`ui/components/AlphabeticalIndexSidebar.kt`**
- Vertical `Column` of A-Z letters on right edge
- Touch/drag scrolls adjacent list to matching letter
- Uses `pointerInput` for drag detection
- Params: `letters: List<Char>`, `onLetterSelected: (Char) -> Unit`, `modifier`
- Used on: Songs, Albums, Artists tabs

**`ui/components/CollapsibleSection.kt`**
- Header row: section name + shuffle + play + collapse/expand chevron
- Expandable content area with animation
- Params: `title`, `onShuffle`, `onPlayAll`, `isExpanded`, `onToggle`, `content: @Composable () -> Unit`
- Used on: History tab

**`ui/components/SongItem.kt`** — New unified song item (72dp height)
- **Album art thumbnail** (56x56dp, 4dp corner radius) — NOT music note icon
- Title (Title Medium, 16sp SemiBold), subtitle "Artist . Album" (Label Small, 12sp)
- Optional trailing: star rating display (non-interactive), overflow menu (48x48 touch target)
- **States** per spec:
  - Default: white text
  - Playing: accent color on title, subtitle, icons; accent border (1.5dp) on album art
  - Selected: accent border on entire item (1dp)
  - Reorderable: replaces overflow with drag handle (`unfold_more` icon)
  - Deletable: replaces overflow with trash icon
  - Rated: star display between text and overflow
- Params: `song`, `onClick`, `isPlaying`, `isSelected`, `showRating`, `variant: SongItemVariant` (Default, Reorderable, Deletable), `onMenuAction: (SongMenuAction) -> Unit`
- `SongMenuAction` sealed interface: PlayNext, AddToQueue, AddToPlaylist, GoToArtist, GoToAlbum

**`ui/components/SongOverflowMenu.kt`**
- Dropdown with 5 items: Play Next, Add to Queue, Add to Playlist, Go to Artist, Go to Album
- Params: `expanded`, `onDismiss`, `onAction: (SongMenuAction) -> Unit`

**`ui/components/CircularArtistImage.kt`**
- Circular `SubcomposeAsyncImage` clipped to `CircleShape`
- Person icon fallback
- Params: `imageUri: String?`, `artistName`, `modifier`

**`ui/components/FrostedGlassPill.kt`**
- Reusable frosted glass pill container (fully rounded ends)
- Uses the `frostedGlass` modifier from Phase 0
- Params: `modifier`, `content: @Composable RowScope.() -> Unit`
- Used on: Artist grid item name pill, MiniPlayer, Now Playing controls

### Files
| File                                        | Action |
|---------------------------------------------|--------|
| `ui/components/SortFilterBar.kt`            | Create |
| `ui/components/AlphabeticalIndexSidebar.kt` | Create |
| `ui/components/CollapsibleSection.kt`       | Create |
| `ui/components/SongItem.kt`                 | Create |
| `ui/components/SongOverflowMenu.kt`         | Create |
| `ui/components/CircularArtistImage.kt`      | Create |
| `ui/components/FrostedGlassPill.kt`         | Create |

### Verify ✅
- `assembleDebug` passes
- SongItem renders at 72dp with 56x56 album art
- Playing state shows accent color correctly
- Frosted glass pill renders with blur effect

### Implementation Notes (deviations from plan)
- `ArtistItem.kt` built in `ui/components/` — supersedes the planned `ArtistGridItem.kt` in `ui/library/`. Do NOT create `ArtistGridItem.kt`.
- `FrostedGlassPill` uses a two-layer `Box` (clip → `matchParentSize` blur layer + unblurred content `Row` on top). `BlurEffect` blurs the layer output, not just the background — single-layer approach blurs content too.
- `SortFilterBar` made fully generic: accepts `sortLabel: String` + `sortOptions: List<String>` instead of a `SortOrder` enum, so it has no dependency on `ui.library`. Callers own the enum/state.
- `CollapsibleSectionHeader` is header-only (no content slot) to avoid nested lazy layout issues. Callers gate item visibility as siblings in the parent lazy layout using `if (isExpanded) { items(...) }`.
- `frostedGlassRendered` modifier does not render in Android Studio preview — requires device/emulator.

---

## Phase 3: Tab Screens — Songs, Albums, Artists ✅

**Goal**: Rebuild the three core library tabs to match Figma + spec.

### Songs Tab (Figma Screen 11)

**`ui/library/SongsTabContent.kt`** — New file
- Sticky `SortFilterBar` at top (sort by: Title, Artist, Album, Duration, Date Added)
- `LazyColumn` with `SongItem` (variant: Default)
- Currently playing song: accent text + accent border on album art (needs `currentSongId` from PlayerViewModel)
- `AlphabeticalIndexSidebar` on right edge
- Layout: `Box { LazyColumn(stickyHeader for SortFilterBar) { items }; AlphabeticalIndexSidebar }`

### Albums Tab (Figma Screen 2)

**`ui/library/AlbumsTabContent.kt`** — New file
- Sticky `SortFilterBar` at top (sort by: Album, Artist)
- `LazyVerticalGrid(GridCells.Fixed(2))` with `AlbumGridItem`
- Album art: 4dp corner radius, no border default, accent border when playing
- Play/pause button overlaid on art (subtle, discoverable — per spec)
- `AlphabeticalIndexSidebar` on right edge

**`ui/library/AlbumGridItem.kt`** — Update existing
- Album art: full-width square, 4dp corner radius
- Title: 14sp Bold
- Artist: 12sp secondary text
- Add optional accent border for playing state
- Add subtle play button overlay on art

### Artists Tab (Figma Screen 5)

**`ui/library/ArtistsTabContent.kt`** — New file
- Sticky `SortFilterBar` at top (sort by: Name)
- `LazyVerticalGrid(GridCells.Fixed(2))` with `ArtistGridItem`
- `AlphabeticalIndexSidebar` on right edge

**`ui/library/ArtistGridItem.kt`** — New file
- Full-width circular portrait (using `CircularArtistImage`)
- **Frosted glass pill** overlapping bottom of circle: play icon | artist name | overflow (3 dots)
- Pill has fully rounded ends, slight overlap with circle above
- Params: `artist: Artist`, `onClick`, `modifier`

### Data Layer Changes

**`model/Artist.kt`** — Add `artUri: String? = null`

**`data/local/SongDao.kt`** — Update `getAllArtists()` to include artUri heuristic:
```sql
SELECT artist AS name, COUNT(*) AS songCount,
       COUNT(DISTINCT albumId) AS albumCount,
       (SELECT albumArtUri FROM songs s2 WHERE s2.artist = songs.artist LIMIT 1) AS artUri
FROM songs GROUP BY artist ORDER BY artist ASC
```

### Files ✅
| File                                | Action                                                                  |
|-------------------------------------|-------------------------------------------------------------------------|
| `ui/home/tabs/SongsTabScreen.kt`    | Created (was `ui/library/SongsTabContent.kt`)                           |
| `ui/home/tabs/AlbumsTabScreen.kt`   | Created (was `ui/library/AlbumsTabContent.kt`)                          |
| `ui/home/tabs/ArtistsTabScreen.kt`  | Created (was `ui/library/ArtistsTabContent.kt`)                         |
| `ui/library/ArtistGridItem.kt`      | NOT created — `ArtistItem.kt` in `ui/components/` used directly         |
| `ui/player/components/AlbumItem.kt` | Created (replaces `ui/library/AlbumGridItem.kt`)                        |
| `model/Artist.kt`                   | Updated — `artUri: String? = null` added                                |
| `data/local/SongDao.kt`             | Updated — `getAllArtists()` + `searchArtists()` include artUri subquery |
| `ui/home/HomeScreen.kt`             | Updated — wires `playerUiState` to tab screens                          |

### Verify ✅
- `assembleDebug` passes
- Songs tab: sort bar + song list + alpha sidebar
- Albums tab: 2-col grid with playing state + alpha sidebar
- Artists tab: circular images with frosted glass name pills + alpha sidebar
- Currently playing song/album/artist highlighted with accent color

### Implementation Notes (deviations from plan)
- Tab screen files live in `ui/home/tabs/` not `ui/library/` — they are wired through `HomeScreen` via `HorizontalPager`, not `LibraryScreen`.
- `ArtistGridItem.kt` was never created. `ArtistItem.kt` (Phase 2, `ui/components/`) handles both grid display and playback state and is used directly in `ArtistsTabScreen`.
- `AlbumGridItem.kt` replaced by `AlbumItem.kt` in `ui/player/components/`. Uses a plain clipped `Box` instead of `Card` — `Card` was fighting with the custom border overlay. `AlbumPlaybackState` (Default/Playing/Paused) replaces the `isPlaying: Boolean` flag. Active border is rendered as the last child with `matchParentSize()` so it draws on top of content. `onPlayClick` and `onMenuClick` added as separate params — play icon bottom-left, menu icon top-right, both in semi-transparent dark circles.
- `SortFilterBar` in grid tabs (`AlbumsTabScreen`, `ArtistsTabScreen`) cannot use `stickyHeader` — `LazyVerticalGrid` doesn't support it. Instead the bar is overlaid on top of the full-height grid: `onSizeChanged` measures the bar's pixel height, converts to dp, and passes it as `contentPadding(top = barHeightDp + 8.dp)` so items start visible but scroll behind the frosted bar.
- `SongDao` artUri subquery improved beyond the plan: `AND s2.albumArtUri IS NOT NULL ORDER BY s2.dateAdded DESC LIMIT 1` — skips null-art rows entirely and picks the most recently added album art. Artist image priority chain: portrait URI (Phase 7 fetch) → most recent non-null album art (DAO) → Person icon placeholder (Coil error slot).

---

## Phase 4: Tab Screens — History, Faves, Playlists ✅

### History Tab (Figma Screen 6)

**`ui/home/tabs/HistoryTabScreen.kt`** — New file (replaces RecentlyPlayedTab + MostPlayedTab)
- Three `CollapsibleSectionHeader`s: Recently Played (songs), Recently Played Albums, Recently Played Artists
- Songs: `SongItem` with `showRating = true`
- Albums and artists: `.chunked(2)` rows inside `LazyColumn` items — avoids nested lazy layout (`LazyVerticalGrid` cannot be nested inside `LazyColumn`)
- Section expansion state preserved across tab switches via `rememberSaveable`

**`data/local/ListeningHistoryDao.kt`** — Added queries:
```sql
-- Recently played albums (correlated subquery for albumArtUri — avoids undefined
-- GROUP BY column selection for non-aggregated fields)
SELECT s.albumId AS id, s.album AS name, s.artist,
       COUNT(DISTINCT s.id) AS songCount,
       (SELECT s2.albumArtUri FROM songs s2
        WHERE s2.albumId = s.albumId AND s2.albumArtUri IS NOT NULL LIMIT 1) AS albumArtUri
FROM listening_history h INNER JOIN songs s ON s.id = h.songId
GROUP BY s.albumId ORDER BY MAX(h.playedAt) DESC LIMIT 20

-- Recently played artists (same artUri heuristic as SongDao.getAllArtists)
SELECT s.artist AS name, COUNT(DISTINCT s.id) AS songCount,
       COUNT(DISTINCT s.albumId) AS albumCount,
       (SELECT s2.albumArtUri FROM songs s2
        WHERE s2.artist = s.artist AND s2.albumArtUri IS NOT NULL
        ORDER BY s2.dateAdded DESC LIMIT 1) AS artUri
FROM listening_history h INNER JOIN songs s ON s.id = h.songId
GROUP BY s.artist ORDER BY MAX(h.playedAt) DESC LIMIT 20
```

**`data/SongRepository.kt`** — Added:
- `getRecentlyPlayedAlbums(): Flow<List<Album>>`
- `getRecentlyPlayedArtists(): Flow<List<Artist>>`

**`ui/library/MetadataViewModel.kt`** — Updated `MetadataUiState.Loaded`:
- Added `recentlyPlayedAlbums: List<Album>`
- Added `recentlyPlayedArtists: List<Artist>`
- `combine` expanded from 3 flows to 5 (uses the 5-arg overload from kotlinx-coroutines)

### Faves Tab (Figma Screen 12)

**`ui/home/tabs/FavoriteTabScreen.kt`** — New file
- Sticky `SortFilterBar` at top (sort by: Title, Artist, Rating)
- `SongItem` with `showRating = true`
- Sorting done locally via `remember(songs, selectedSort)` — no ViewModel round-trip for a purely presentational operation

### Playlists Tab (Figma Screen 9)

**`ui/home/tabs/PlaylistsTabScreen.kt`** — New file (adapts PlaylistsScreenContent)
- Sticky `PlaylistsActionBar` with create-playlist icon (replaces FAB)
- `PlaylistItem` rows with collage thumbnail, play/pause control, overflow menu
- `currentPlaylistId` + `isPlaying` drive per-row `PlaylistPlaybackState` — only the active playlist row gets an accent border

**`ui/components/PlaylistItem.kt`** — New file
- 72dp height row: `PlaylistCollage` | name + subtitle | play/pause | overflow `⋮`
- `PlaylistPlaybackState` enum: Default, Playing, Paused
- Accent border on **full row** (not just collage thumbnail) — matches Figma; collage is too visually busy for a thumbnail-only border to read clearly
- Playing → pause icon; Default and Paused → play icon (tint difference conveys paused vs inactive)
- Three previews (one per state)

**`ui/components/PlaylistsActionBar.kt`** — New file
- Same frosted glass pill + gradient accent border as `SortFilterBar`
- Single `PlaylistAdd` icon on trailing edge; left side empty (reserved for future search)

**`ui/components/PlaylistCollage.kt`** — New file
- 2×2 grid of album art thumbnails (28×28dp each = 56×56dp total)
- Takes `List<String?>` of up to 4 album art URIs
- Falls back to centred music note icon when list is empty
- Individual empty cells render as transparent — no icon clutter at 28dp

**`model/Playlist.kt`** — Added `totalDuration: Long = 0` (defaulted so all existing callsites compile unchanged)

**`data/local/PlaylistDao.kt`** — Updated `getAllPlaylistsWithCount()` query:
```sql
SELECT p.id, p.name, p.createdAt, COUNT(ps.songId) AS songCount,
       COALESCE(SUM(s.duration), 0) AS totalDuration
FROM playlists p LEFT JOIN playlist_songs ps ON p.id = ps.playlistId
LEFT JOIN songs s ON ps.songId = s.id
WHERE p.deletedAt = 0 GROUP BY p.id ORDER BY p.name ASC
```

### Files ✅
| File                                  | Action                                              |
|---------------------------------------|-----------------------------------------------------|
| `ui/home/tabs/HistoryTabScreen.kt`    | Created (was `ui/library/HistoryTabContent.kt`)     |
| `ui/home/tabs/FavoriteTabScreen.kt`   | Created (was `ui/library/FavesTabContent.kt`)       |
| `ui/home/tabs/PlaylistsTabScreen.kt`  | Created (was `ui/playlists/PlaylistsTabContent.kt`) |
| `ui/components/PlaylistItem.kt`       | Created — was not planned separately                |
| `ui/components/PlaylistsActionBar.kt` | Created — was not planned separately                |
| `ui/components/PlaylistCollage.kt`    | Created                                             |
| `data/local/ListeningHistoryDao.kt`   | Updated — album/artist queries added                |
| `data/local/PlaylistDao.kt`           | Updated — duration JOIN added                       |
| `data/SongRepository.kt`              | Updated — two new method signatures                 |
| `data/SongRepositoryImpl.kt`          | Updated — two new overrides                         |
| `ui/library/MetadataViewModel.kt`     | Updated — 5-flow combine, two new state fields      |
| `model/Playlist.kt`                   | Updated — `totalDuration: Long = 0` added           |

### Verify ✅
- `assembleDebug` passes
- History tab: collapsible sections with song/album/artist content
- Faves tab: rated songs with sort bar
- Playlists tab: collage thumbnails, duration display, create-playlist action bar

### Implementation Notes (deviations from plan)
- Tab screen files live in `ui/home/tabs/` not `ui/library/` or `ui/playlists/` — consistent with Phase 3 pattern
- `PlaylistItem` and `PlaylistPlaybackState` extracted into `ui/components/PlaylistItem.kt` rather than being inlined in `PlaylistsTabScreen` — cleaner separation and enables reuse in `PlaylistDetailScreen` (Phase 5)
- `PlaylistsActionBar` extracted into `ui/components/PlaylistsActionBar.kt` — same pattern as `SortFilterBar`
- `FavoriteTabScreen` sorts locally rather than routing through `MetadataViewModel` — avoids a ViewModel round-trip for a purely presentational operation
- `HistoryTabScreen` uses `.chunked(2)` rows in `LazyColumn` for albums and artists rather than nested `LazyVerticalGrid` — nested lazy layouts are not supported in Compose
- `PlaylistItem` accent border wraps the full row rather than just the collage thumbnail — diverges from the `SongItem` thumbnail-only border pattern but matches the Figma and is more legible given the collage's visual complexity
- `artUris = emptyList()` passed to `PlaylistItem` for now — per-playlist album art URIs require a dedicated DAO query that is deferred; `PlaylistCollage` handles the empty case with a music note fallback

---

## Phase 5: Detail Screens ✅

### Artist Detail (Figma Screens 3 & 4)

**`ui/library/ArtistDetailScreen.kt`** — Major rewrite

Layered `Box` layout with 3 z-layers:
```
Box(fillMaxSize, statusBarsPadding) {
    Layer 0 — BlurredBackground (screenshot of previous screen, GPU-blurred + dark scrim)
    Layer 1 — Fixed portrait (360dp, full-width, ContentScale.Crop) + bottom gradient fade
    Layer 2 — LazyColumn(contentPadding top = portrait - 24dp overlap) {
                  Glass panel lip (rounded top corners, frostedGlassRendered)
                  stickyHeader: Songs | Albums tab bar (accent gradient border, frosted glass)
                  Song list or Album grid (background-wrapped items)
              }
    Layer 3 — Pinned Row: back pill | artist name + metadata pill | overflow pill
}
```

- Portrait: full-width edge-to-edge, 360dp, `ContentScale.Crop` (not 240dp circle)
- List scrolls over portrait with rounded-top glass panel edge (16dp corner radius)
- Bottom gradient fade on portrait (72dp, transparent → 70% black)
- Sticky tab bar styled like `SortFilterBar`: `frostedGlassRendered()` + accent gradient border + `RoundedCornerShape(8.dp)`
- Pinned `FrostedGlassPill` buttons always visible at top: down-chevron back, centered name `Column` with `weight(1f)`, overflow menu
- Songs sub-tab: `SongItem` list with `background(colorScheme.background)` wrapper
- Albums sub-tab: 2-column `AlbumGridItem` grid via `.chunked(2)` rows

### Album Detail (Figma — spec defines layout)

**`ui/library/AlbumDetailScreen.kt`** — Major rewrite (same layered pattern)

```
Box(fillMaxSize, statusBarsPadding) {
    Layer 0 — BlurredBackground
    Layer 1 — Fixed album art (300dp, full-width) + bottom gradient fade
    Layer 2 — LazyColumn(contentPadding top = art - 24dp overlap) {
                  Glass panel lip
                  stickyHeader: song count label + shuffle + play (accent border bar)
                  Song list (background-wrapped items)
              }
    Layer 3 — Pinned Row: back pill | album name + artist pill | overflow pill
}
```

- Same structural pattern as ArtistDetailScreen, no sub-tabs (single song list)
- Sticky bar shows song count label instead of tab toggles

### Playlist Detail (Figma Screen 8)

**`ui/playlists/PlaylistDetailScreen.kt`** — Major rewrite (homogenized with Album/Artist)

Per spec layout:
```
Box(fillMaxSize, statusBarsPadding) {
    Layer 0 — BlurredBackground
    Column(padding top = 56dp for pinned header clearance) {
        Action bar: song count + duration label | shuffle | play (accent border, frosted glass)
        LazyColumn(reorderable) {
            PlaylistSongItem items (background-wrapped, draggable)
        }
    }
    Pinned Row: back pill | playlist name + metadata pill | add-songs pill
}
```

- No hero image (playlists don't have one) — content starts below pinned header
- Action bar outside `LazyColumn` (not `stickyHeader`) because reorderable library needs consistent item keys
- Add-songs button replaces overflow in the pinned row (right pill)
- `PlaylistSongItem` + reorderable logic unchanged from pre-redesign

### Blurred Background System

**`ui/components/BlurredBackground.kt`** — New file

- `ScreenshotHolder` singleton: holds a nullable `Bitmap`, `capture(view)` uses `View.drawToBitmap()`, `clear()` recycles
- `BlurredBackground` composable: reads from `ScreenshotHolder`, draws bitmap with `RenderEffect.createBlurEffect(25f, 25f)` via `graphicsLayer`, overlays dark scrim (55% black). Falls back to plain dark background if no screenshot.
- Capture trigger: `ScreenshotHolder.capture(view)` called immediately before `navController.navigate()` in every navigation callback that leads to a detail screen
- Perf: one bitmap capture (~1-2 frames) + GPU-accelerated blur (API 31+, minSdk 34). Static after initial render — zero per-frame cost.

### Files
| File                                   | Action                                                    |
|----------------------------------------|-----------------------------------------------------------|
| `ui/library/ArtistDetailScreen.kt`     | Major rewrite (layered Box, fixed portrait, glass panel)  |
| `ui/library/AlbumDetailScreen.kt`      | Major rewrite (same layered pattern as Artist)            |
| `ui/playlists/PlaylistDetailScreen.kt` | Major rewrite (homogenized header + action bar)           |
| `ui/components/BlurredBackground.kt`   | Created (ScreenshotHolder + BlurredBackground composable) |
| `data/local/SongDao.kt`                | Add `getAlbumsByArtist()`                                 |
| `data/SongRepository.kt`               | Add `getAlbumsByArtist()`                                 |
| `data/SongRepositoryImpl.kt`           | Implement                                                 |
| Navigation trigger points              | Add `ScreenshotHolder.capture(view)` before `navigate()`  |

### Verify ✅
- `assembleDebug` passes
- Artist detail: full-width portrait, glass panel slides over on scroll, sticky tab bar, pinned pills
- Album detail: full-width album art, glass panel, sticky action bar, pinned pills
- Playlist detail: pinned header pills, accent-border action bar, reorderable song list
- Blurred background visible behind all detail screens (previous screen blurred + scrimmed)
- Portrait not muted (no `frostedGlassRendered` overlay on portrait layer)

### Implementation Notes (deviations from plan)
- Portrait is full-width rectangle (not 240dp circle) — better visual impact and matches Zune inspiration
- Scroll behavior is list-overlaps-portrait with rounded-top glass panel edge, not parallax/shrink — simpler implementation, cleaner visual
- Back button uses `KeyboardArrowDown` (down chevron) in `FrostedGlassPill`, not `ArrowBackIosNew` in a black circle — consistent across all three detail screens
- All three detail screens share identical Layer 3 pattern: `Row(SpaceBetween) { FrostedGlassPill × 3 }` with back, center info, and trailing action
- `BlurredBackground` uses `ScreenshotHolder` singleton (not ViewModel) — bitmaps are large and should not survive config changes
- Playlist action bar is outside `LazyColumn` (not `stickyHeader`) to avoid conflicts with `rememberReorderableLazyListState` — reorderable items need a clean `LazyColumn` with consistent keys

---

## Phase 6: Now Playing & Queue ✅

### Full-Screen Player (Figma Screen 7)

**`ui/player/NowPlayingScreenContent.kt`** — Major rewrite

Per spec layout:
```
[Back chevron top-left] [Queue icon top-right]
[Full-screen album art hero]
[Seek bar with timestamps]
[Star rating (5 stars) + heart favorite]
[Track title bold / album / artist]
[Frosted glass pill: repeat | prev | play/pause(large) | next | shuffle]
```

Key changes:
1. Remove `TopAppBar` -> simple `Row` with chevron + queue icon
2. Album art: nearly full-width hero, ~1:1 aspect ratio
3. SeekBar immediately below art
4. Star rating + heart on same row below seek bar
5. Track info below stars: title (Title Medium), album (Body Medium), artist (Body Medium)
6. **Controls in frosted glass pill** at bottom: repeat | prev | play(large, accent background) | next | shuffle
7. Background: subtle tint from album art dominant color (palette-ktx, `DarkVibrantSwatch` preferred, `DarkMutedSwatch` fallback, animated with `animateColorAsState`)

**`ui/player/components/PlayerControls.kt`** — Expand
- Add repeat + shuffle buttons flanking prev/play/next
- Play button: larger, accent-tinted circular background
- Wrap entire row in `FrostedGlassPill`

### MiniPlayer -> Now Playing Transition

**[GUESS] Major architectural change**: The spec says MiniPlayer -> NowPlaying is NOT navigation, it's an `AnimatedContent` expansion. This requires:
- Remove `Screen.NowPlaying` route from NavHost
- Instead, manage a `isNowPlayingExpanded: Boolean` state at the Scaffold level
- When expanded: `AnimatedContent` transitions MiniPlayer pill into full Now Playing screen
- Shared element: album art grows from 56dp thumbnail to full-width hero
- This is the riskiest change. If too complex, keep as navigation and defer animation.

### MiniPlayer (Figma bottom bars)

**`ui/player/components/MiniPlayer.kt`** — Major rewrite
- **Pill shape**: height ~72dp, corner radius 36dp (fully rounded)
- **Frosted glass background** (using `frostedGlass` modifier)
- **Floating**: 8dp horizontal margin from screen edges
- Content: 56x56 album art | title / "Artist . Album" | skip prev | play/pause (accent bg) | skip next
- Play/pause button: accent-tinted square/rounded background in active state
- Tapping anywhere (except controls) expands to Now Playing

### Queue (Figma Screen 10)

**`ui/player/components/QueueSheet.kt`** — Moderate rewrite
- **Frosted glass background** for the bottom sheet
- Header: "Queue" + trash icon (clear) + collapse chevron
- Currently playing item: accent border + accent-colored album art border
- Each item: `SongItem` variant Reorderable (album art, title, subtitle, drag handle replacing overflow)
- First item accent-highlighted per spec

### Files
| File                                            | Action                                                    |
|-------------------------------------------------|-----------------------------------------------------------|
| `ui/player/NowPlayingScreenContent.kt`          | Major rewrite                                             |
| `ui/player/components/PlayerControls.kt`        | Layered glass pill, diamond play, bordered prev/next      |
| `ui/player/components/MiniPlayer.kt`            | Major rewrite (pill, frosted glass, 3 controls)           |
| `ui/player/components/QueueSheet.kt`            | Redesign (frosted glass, accent playing item, drag-only)  |
| `ui/player/components/SeekBar.kt`               | Custom track + thumb (accent progress, pill thumb)        |
| `ui/player/components/BlurredBackground.kt`     | PixelCopy capture + hardware RenderEffect blur            |
| `ui/navigation/NavGraph.kt`                     | Remove NowPlaying route, add AnimatedVisibility overlay   |

### Verify ✅
- `assembleDebug` passes
- MiniPlayer: frosted glass pill, floating, 3 controls, accent states
- Full player: large art, glass controls pill, palette-tinted background, blurred background
- Queue: frosted glass sheet, accent highlighting, drag handles
- MiniPlayer -> Now Playing AnimatedVisibility slide-up transition
- System back collapses Now Playing (BackHandler)

### Implementation Notes (deviations from plan)
- **AnimatedContent → AnimatedVisibility**: Plan proposed `AnimatedContent` to morph MiniPlayer into NowPlaying. In practice, `AnimatedContent` couldn't properly size-transition between a small pill and a full-screen overlay. Replaced with two separate `AnimatedVisibility` blocks — MiniPlayer fades in/out, NowPlaying slides up/down. Simpler and more reliable.
- **Shared element deferred**: MiniPlayer → NowPlaying shared element not implemented — NowPlaying uses AnimatedVisibility overlay (not navigation), so `SharedTransitionLayout` doesn't apply. Album/artist grid → detail transitions implemented in Phase 7.
- **PlayerControls diamond play button**: Plan said "accent-tinted circular background". Figma showed a more complex design: two imbricated rounded squares rotated 45° (ghost layer + shadow layer), with bordered prev/next and borderless repeat/shuffle. Implemented per Figma, not plan text.
- **SeekBar fully custom**: Plan flagged `Slider` as "test first — may need custom". Material `Slider` couldn't achieve the thick rounded track + narrow pill thumb from the Figma. Replaced entirely with custom `Box`-based track + thumb using `pointerInput` for tap and drag.
- **ScreenshotHolder.capture switched to PixelCopy**: `View.drawToBitmap()` uses software rendering which fails on views containing hardware bitmaps (Coil album art). Switched to `PixelCopy.request()` with a background `HandlerThread` to avoid main-thread deadlock. Also fixes blurred backgrounds on detail screens (Phase 5).
- **BlurredBackground blur baked into bitmap**: Compose's `Modifier.blur()` and `graphicsLayer { BlurEffect }` failed to blur `Image(bitmap = asImageBitmap())`. Replaced with Android `HardwareRenderer` + `RenderEffect.createBlurEffect()` that pre-blurs the bitmap before Compose renders it. Zero per-frame cost.
- **`Screen.NowPlaying` removed**: No longer a navigation route. Now Playing is an overlay managed by `isNowPlayingExpanded: Boolean` state in `NavGraph`. All `onNavigateToNowPlaying` callbacks in HomeScreen and detail screens now set this flag instead of navigating.
- **Per-item delete removed from QueueSheet**: Spec says drag handle replaces overflow — individual delete removed. Header trash icon handles bulk clear.

---

## Phase 7: Artist Image Fetching, Transitions & Polish ✅

**Status**: Implemented. `assembleDebug` and `test` pass.

### Artist Image Service ✅

**`data/remote/ArtistImageService.kt`** — New file
- Fetches artist images from **Deezer API** (`https://api.deezer.com/search/artist?q={name}&limit=1`)
- Uses `java.net.HttpURLConnection` + `org.json.JSONObject` (zero new deps — both built into Android)
- Parses `data[0].picture_xl` for high-res 1000x1000 images
- Runs on `Dispatchers.IO`, 10s connect/read timeouts

**`data/ArtistImageRepository.kt`** — New interface
- `suspend fun getArtistImageUrl(artistName: String): String?`

**`data/ArtistImageRepositoryImpl.kt`** — New implementation
- Cache-first: checks Room, returns immediately if cached and < 7 days old
- Failed lookups (null imageUrl) only cached for 1 day (retries sooner)
- Blank artist names skipped
- `@Singleton`, `@Inject constructor(dao, service)`

**`model/ArtistImage.kt`** — New Room entity
- `artist_images(artistName: String PK, imageUrl: String?, fetchedAt: Long)`

**`data/local/ArtistImageDao.kt`** — New DAO
- `getByArtistName(name): ArtistImage?` (suspend)
- `insertOrReplace(entity)` (suspend, REPLACE strategy)
- `getAll(): Flow<List<ArtistImage>>`

**`data/local/SyncPlayerDatabase.kt`** — Added `ArtistImage` entity, bumped version 7→8, added `MIGRATION_7_8` (CREATE TABLE), added `artistImageDao()` abstract method

**`di/DatabaseModule.kt`** — Added `MIGRATION_7_8` to builder, added `provideArtistImageDao()` provider

**`di/AppModule.kt`** — Added `@Binds abstract fun bindArtistImageRepository(impl): ArtistImageRepository`

**`gradle/libs.versions.toml`** — Added `coil-network-okhttp` library entry (group `io.coil-kt.coil3`, uses existing `coil` version ref)

**`app/build.gradle.kts`** — Added `implementation(libs.coil.network.okhttp)` for Coil 3 network fetching

**`app/src/main/AndroidManifest.xml`** — Added `<uses-permission android:name="android.permission.INTERNET" />`

**Integration via SQL JOIN** — `data/local/SongDao.kt`: Updated `getAllArtists()` and `searchArtists()` queries with LEFT JOIN on `artist_images` table:
```sql
COALESCE(ai.imageUrl, (SELECT albumArtUri FROM songs s2 WHERE ...)) AS artUri
```
Priority chain: Deezer URL → most recent album art → placeholder icon.

**Background fetch** — `ui/library/LibraryViewModel.kt`: Injects `ArtistImageRepository`. `init` block calls `fetchMissingArtistImages()` which waits for non-empty artist list, then calls `getArtistImageUrl()` for each artist (throttled to 1 req/sec for Deezer rate limits).

### Shared Element Transitions ✅

Compose BOM 2026.01.01 supports `SharedTransitionLayout`. Implemented for:
- **Album art**: `AlbumGridItem` → `AlbumDetailScreen` hero (key: `"album_art_${album.id}"`)
- **Artist portrait**: `ArtistItem` → `ArtistDetailScreen` hero (key: `"artist_art_$artistName"`)
- MiniPlayer → Now Playing: **not implemented** (Now Playing uses AnimatedVisibility overlay, not navigation — shared element not applicable)

**Modified files:**
- `ui/navigation/NavGraph.kt` — Wrapped `NavHost` in `SharedTransitionLayout`, passed scopes to screens
- `ui/home/HomeScreen.kt` — Threaded `SharedTransitionScope` + `AnimatedVisibilityScope` to tab screens
- `ui/home/tabs/AlbumsTabScreen.kt` — Passes scopes to `AlbumGridItem`
- `ui/home/tabs/ArtistsTabScreen.kt` — Passes scopes to `ArtistItem`
- `ui/player/components/AlbumItem.kt` — Applies `sharedElement` modifier to album art image
- `ui/player/components/ArtistItem.kt` — Applies `sharedElement` modifier to `CircularArtistImage`
- `ui/library/AlbumDetailScreen.kt` — Matching `sharedElement` on hero album art
- `ui/library/ArtistDetailScreen.kt` — Matching `sharedElement` on hero portrait

### Scroll Behaviors ✅

- **Parallax**: Hero images on `AlbumDetailScreen` and `ArtistDetailScreen` use `graphicsLayer { translationY = firstVisibleItemScrollOffset * 0.5f }` derived from `LazyListState`
- Top App Bar frosted glass on scroll: not implemented (deferred)
- Sort bar sticky: not implemented (deferred)

### Cleanup

**Deferred** — file deletion and dead code cleanup to be done separately per user request.

### Test Updates ✅

- `LibraryViewModelTest.kt` — Updated setup to provide fake `ArtistImageRepository`, added `albumArtist` to `testSong` helper, added `artUri = null` to `testArtist` helper
- `ArtistImageRepositoryTest.kt` — **New test file** with `FakeArtistImageDao` and `FakeArtistImageService`. Tests: cache hit, cache miss (service called), stale cache refresh, null result caching
- `FakeSongRepository.kt` — Added missing interface methods: `getAlbumsByArtist`, `getRecentlyPlayedAlbums`, `getRecentlyPlayedArtists`
- Multiple test files fixed for pre-existing `albumArtist` compilation errors: `ConflictResolverTest`, `SyncOrchestratorTest`, `LibraryViewModelMetadataTest`, `NowPlayingScreenTest`, `PlayerViewModelTest`

### Files
| File                                            | Action                             |
|-------------------------------------------------|------------------------------------|
| `data/remote/ArtistImageService.kt`             | Created                            |
| `data/ArtistImageRepository.kt`                 | Created (interface)                |
| `data/ArtistImageRepositoryImpl.kt`             | Created                            |
| `model/ArtistImage.kt`                          | Created                            |
| `data/local/ArtistImageDao.kt`                  | Created                            |
| `data/local/SyncPlayerDatabase.kt`              | Added entity + migration 7→8       |
| `di/DatabaseModule.kt`                          | Added DAO provider + migration     |
| `di/AppModule.kt`                               | Added repository binding           |
| `data/local/SongDao.kt`                         | Updated artist queries (LEFT JOIN) |
| `ui/library/LibraryViewModel.kt`                | Added image fetch logic            |
| `ui/navigation/NavGraph.kt`                     | SharedTransitionLayout wrapper     |
| `ui/home/HomeScreen.kt`                         | Thread transition scopes           |
| `ui/home/tabs/AlbumsTabScreen.kt`               | Pass transition scopes             |
| `ui/home/tabs/ArtistsTabScreen.kt`              | Pass transition scopes             |
| `ui/player/components/AlbumItem.kt`             | sharedElement modifier             |
| `ui/player/components/ArtistItem.kt`            | sharedElement modifier             |
| `ui/library/AlbumDetailScreen.kt`               | sharedElement + parallax           |
| `ui/library/ArtistDetailScreen.kt`              | sharedElement + parallax           |
| `gradle/libs.versions.toml`                     | Added coil-network-okhttp          |
| `app/build.gradle.kts`                          | Added coil-network-okhttp dep      |
| `app/src/main/AndroidManifest.xml`              | Added INTERNET permission          |
| `app/src/test/.../ArtistImageRepositoryTest.kt` | Created                            |
| `app/src/test/.../LibraryViewModelTest.kt`      | Updated                            |
| `app/src/test/.../FakeSongRepository.kt`        | Updated                            |

### Verify
- ✅ `assembleDebug` passes
- ✅ `test` passes (all unit tests green)
- ✅ Artist images fetch from Deezer with Room caching (requires device internet)
- ✅ Album art shared element transition: grid → detail
- ✅ Artist portrait shared element transition: grid → detail
- ✅ Parallax scroll on detail screen hero images

---

## Summary: All New Files

| File                                         | Phase |
|----------------------------------------------|-------|
| `ui/theme/GlassEffect.kt`                    | 0     |
| `ui/components/SortFilterBar.kt`             | 2     |
| `ui/components/AlphabeticalIndexSidebar.kt`  | 2     |
| `ui/components/CollapsibleSection.kt`        | 2     |
| `ui/components/SongItem.kt`                  | 2     |
| `ui/components/SongOverflowMenu.kt`          | 2     |
| `ui/components/CircularArtistImage.kt`       | 2     |
| `ui/components/FrostedGlassPill.kt`          | 2     |
| `ui/library/SongsTabContent.kt`              | 3     |
| `ui/library/AlbumsTabContent.kt`             | 3     |
| `ui/library/ArtistsTabContent.kt`            | 3     |
| `ui/library/ArtistGridItem.kt`               | 3     |
| `ui/home/tabs/HistoryTabScreen.kt`           | 4     |
| `ui/home/tabs/FavoriteTabScreen.kt`          | 4     |
| `ui/home/tabs/PlaylistsTabScreen.kt`         | 4     |
| `ui/components/PlaylistCollage.kt`           | 4     |
| `ui/components/PlaylistItem.kt`              | 4     |
| `ui/components/PlaylistsActionBar.kt`        | 4     |
| `ui/components/BlurredBackground.kt`         | 5     |
| `data/remote/ArtistImageService.kt`          | 7     |
| `data/ArtistImageRepository.kt`              | 7     |
| `data/ArtistImageRepositoryImpl.kt`          | 7     |
| `model/ArtistImage.kt`                       | 7     |
| `data/local/ArtistImageDao.kt`               | 7     |
| `test/.../ArtistImageRepositoryTest.kt`      | 7     |

## Summary: All Modified Files

| File                                        | Phase | Severity                                                               |
|---------------------------------------------|-------|------------------------------------------------------------------------|
| `ui/theme/Color.kt`                         | 0     | Add AccentColor, BackgroundDark, FrostedGlassSurface                   |
| `ui/theme/Theme.kt`                         | 0     | Add LocalAccentColor, override background, disable dynamic color       |
| `ui/theme/Type.kt`                          | 0     | Add weight variants per spec                                           |
| `gradle/libs.versions.toml`                 | 0     | Add palette-ktx                                                        |
| `app/build.gradle.kts`                      | 0     | Add palette-ktx dependency                                             |
| `ui/library/LibraryScreen.kt`               | 1, 3  | Major refactor                                                         |
| `ui/library/LibraryViewModel.kt`            | 1     | Update tab enum                                                        |
| `model/Artist.kt`                           | 3     | Add `artUri` field                                                     |
| `data/local/SongDao.kt`                     | 3, 5  | Update + add queries                                                   |
| `ui/library/AlbumGridItem.kt`               | 3     | Corner radius, playing border, play overlay                            |
| `data/local/ListeningHistoryDao.kt`         | 4     | Add album/artist queries                                               |
| `data/local/PlaylistDao.kt`                 | 4     | Update query for duration                                              |
| `data/SongRepository.kt`                    | 4, 5  | Add methods                                                            |
| `data/SongRepositoryImpl.kt`                | 4, 5  | Implement methods                                                      |
| `ui/library/MetadataViewModel.kt`           | 4     | Update state class                                                     |
| `model/Playlist.kt`                         | 4     | Add `totalDuration` field                                              |
| `ui/library/ArtistDetailScreen.kt`          | 5     | Major rewrite (layered Box, fixed portrait, glass panel, pinned pills) |
| `ui/library/AlbumDetailScreen.kt`           | 5     | Major rewrite (same layered pattern as Artist)                         |
| `ui/playlists/PlaylistDetailScreen.kt`      | 5     | Major rewrite (homogenized header + action bar)                        |
| `ui/player/NowPlayingScreenContent.kt`      | 6     | Major rewrite (palette bg, blurred bg, spec layout)                    |
| `ui/player/components/PlayerControls.kt`    | 6     | Layered glass pill, diamond play, bordered prev/next                   |
| `ui/player/components/MiniPlayer.kt`        | 6     | Major rewrite (pill, frosted glass, 3 accent states)                   |
| `ui/player/components/QueueSheet.kt`        | 6     | Frosted glass, accent playing item, drag-handle-only                   |
| `ui/player/components/SeekBar.kt`           | 6     | Custom track + pill thumb (replaced Material Slider)                   |
| `ui/player/components/BlurredBackground.kt` | 5, 6  | PixelCopy capture (fixes hardware bitmap), HardwareRenderer blur       |
| `ui/navigation/NavGraph.kt`                 | 1, 6  | Remove NowPlaying route, AnimatedVisibility overlay, BackHandler       |
| `data/local/SyncPlayerDatabase.kt`          | 7     | Add ArtistImage entity + migration 7→8                                 |
| `di/DatabaseModule.kt`                      | 7     | Add ArtistImageDao provider + migration                                |
| `di/AppModule.kt`                           | 7     | Bind ArtistImageRepository                                             |
| `data/local/SongDao.kt`                     | 7     | LEFT JOIN artist_images for artUri                                     |
| `ui/library/LibraryViewModel.kt`            | 7     | Inject ArtistImageRepository, background fetch                         |
| `ui/navigation/NavGraph.kt`                 | 7     | SharedTransitionLayout wrapper                                         |
| `ui/home/HomeScreen.kt`                     | 7     | Thread transition scopes                                               |
| `ui/home/tabs/AlbumsTabScreen.kt`           | 7     | Pass transition scopes                                                 |
| `ui/home/tabs/ArtistsTabScreen.kt`          | 7     | Pass transition scopes                                                 |
| `ui/player/components/AlbumItem.kt`         | 7     | sharedElement modifier on album art                                    |
| `ui/player/components/ArtistItem.kt`        | 7     | sharedElement modifier on artist portrait                              |
| `ui/library/AlbumDetailScreen.kt`           | 7     | sharedElement + parallax scroll                                        |
| `ui/library/ArtistDetailScreen.kt`          | 7     | sharedElement + parallax scroll                                        |
| `gradle/libs.versions.toml`                 | 7     | Add coil-network-okhttp                                                |
| `app/build.gradle.kts`                      | 7     | Add coil-network-okhttp dependency                                     |
| `app/src/main/AndroidManifest.xml`          | 7     | Add INTERNET permission                                                |

## Summary: Deleted Files

| File                                     | Phase | Reason                          |
|------------------------------------------|-------|---------------------------------|
| `ui/library/ArtistListItem.kt`           | 7     | Replaced by ArtistGridItem      |
| `ui/playlists/PlaylistsScreen.kt`        | 7     | Playlists is now a tab          |
| `ui/playlists/PlaylistsScreenContent.kt` | 7     | Merged into PlaylistsTabContent |

## Key Spec Details to Reference During Implementation

These are easy to forget — reference during coding:
- **Accent max**: 1-2 accented elements per screen, state only, never decorative
- **Song item height**: 72dp, album art 56x56dp, corner radius 4dp
- **Album art in grid**: 4dp corner radius, no border default
- **Artist pill**: frosted glass, overlaps circle, fully rounded ends
- **Playlist thumbnail**: 2x2 collage of 28x28dp album arts
- **MiniPlayer**: pill (36dp radius), floating (8dp margin), frosted glass
- **Tab indicator**: NO underline — size + color differentiate active/inactive
- **Controls pill**: frosted glass on Now Playing screen
- **Background**: #111113 (near-black)
- **Min SDK 34**: No need for pre-API-31 blur fallbacks