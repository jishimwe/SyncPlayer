# SyncPlayer UI Redesign Plan

## Context

Comprehensive UI refactor to match 12 Figma screens + a visual design spec document. The biggest structural change is replacing the bottom navigation bar with a horizontal scrollable top tab row. The visual language shifts to a Zune-inspired, content-forward aesthetic with frosted glass overlays, bold typography, and a strict accent color system.

**Source of truth**: `docs/features/ui-redesign/visual-design-spec.md`

### User Decisions
- **Most Played**: Merge into History tab as a section
- **Song overflow menu**: Standard set (Play Next, Add to Queue, Add to Playlist, Go to Artist, Go to Album)
- **Artist images**: Fetch from web (e.g., MusicBrainz/Last.fm API)

### Guesses / Open Questions (flagged)
- [GUESS] The spec says "repository layer — untouched" but History tab needs recently-played albums/artists data that doesn't currently exist in any DAO. Plan adds minimal DAO queries for this. If the intent is truly zero data-layer changes, History tab would only show songs.
- [GUESS] MiniPlayer -> Now Playing AnimatedContent expansion: the spec says "not navigation". This requires significant architectural change (removing the NowPlaying NavHost route and replacing with an overlay/AnimatedContent at the Scaffold level). Plan treats this as a Phase 6 change. If too risky, we can keep navigation for now and add the animation later.
- [GUESS] The spec mentions `SharedTransitionLayout` (Compose 1.7+). Need to verify our Compose version supports this. If not, shared element transitions get deferred.
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
| File | Action |
|------|--------|
| `ui/theme/Color.kt` | Add AccentColor (#FF1D58), BackgroundDark (#111113), FrostedGlassSurface |
| `ui/theme/Theme.kt` | Add LocalAccentColor CompositionLocal, override background, disable dynamic color |
| `ui/theme/Type.kt` | Add weight variants (ExtraBold, Bold, SemiBold) per spec |
| `ui/theme/GlassEffect.kt` | Create — frosted glass modifier |
| `gradle/libs.versions.toml` | Add palette-ktx |
| `app/build.gradle.kts` | Add palette-ktx dependency |

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
| File | Action |
|------|--------|
| `ui/navigation/NavGraph.kt` | Major rewrite |
| `ui/library/LibraryScreen.kt` | Decompose tabs out |
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

## Phase 2: New Reusable Components

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
| File | Action |
|------|--------|
| `ui/components/SortFilterBar.kt` | Create |
| `ui/components/AlphabeticalIndexSidebar.kt` | Create |
| `ui/components/CollapsibleSection.kt` | Create |
| `ui/components/SongItem.kt` | Create |
| `ui/components/SongOverflowMenu.kt` | Create |
| `ui/components/CircularArtistImage.kt` | Create |
| `ui/components/FrostedGlassPill.kt` | Create |

### Verify
- `assembleDebug` passes
- SongItem renders at 72dp with 56x56 album art
- Playing state shows accent color correctly
- Frosted glass pill renders with blur effect

---

## Phase 3: Tab Screens — Songs, Albums, Artists

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

### Files
| File | Action |
|------|--------|
| `ui/library/SongsTabContent.kt` | Create |
| `ui/library/AlbumsTabContent.kt` | Create |
| `ui/library/ArtistsTabContent.kt` | Create |
| `ui/library/ArtistGridItem.kt` | Create |
| `ui/library/AlbumGridItem.kt` | Update (corner radius, playing border, play overlay) |
| `model/Artist.kt` | Add `artUri` field |
| `data/local/SongDao.kt` | Update `getAllArtists()` query |
| `ui/library/LibraryScreen.kt` | Update to use new tab content composables |

### Verify
- `assembleDebug` passes
- Songs tab: sort bar + song list (72dp items with 56x56 art) + alpha sidebar
- Albums tab: 2-col grid with 4dp rounded art + alpha sidebar
- Artists tab: circular images with frosted glass name pills + alpha sidebar
- Currently playing song highlighted with accent color

---

## Phase 4: Tab Screens — History, Faves, Playlists

### History Tab (Figma Screen 6)

**`ui/library/HistoryTabContent.kt`** — New file (replaces RecentlyPlayedTab + MostPlayedTab)
- Three `CollapsibleSection`s: Songs, Albums, Artists
- Songs: `SongItem` with star rating display (Rated variant)
- Albums: 2-column grid preview (reuse `AlbumGridItem`)
- Artists: circular portrait grid preview
- Most Played data merged as sort/filter option within Songs section

**[GUESS] Data layer additions** — the spec says "repository untouched" but we need album/artist history data:

**`data/local/ListeningHistoryDao.kt`** — Add queries:
```sql
-- Recently played albums
SELECT s.albumId AS id, s.album AS name, s.artist,
       COUNT(DISTINCT s.id) AS songCount, s.albumArtUri AS albumArtUri
FROM listening_history h INNER JOIN songs s ON s.id = h.songId
GROUP BY s.albumId ORDER BY MAX(h.playedAt) DESC LIMIT 20

-- Recently played artists (with artUri heuristic)
SELECT s.artist AS name, COUNT(DISTINCT s.id) AS songCount,
       COUNT(DISTINCT s.albumId) AS albumCount,
       (SELECT albumArtUri FROM songs s2 WHERE s2.artist = s.artist LIMIT 1) AS artUri
FROM listening_history h INNER JOIN songs s ON s.id = h.songId
GROUP BY s.artist ORDER BY MAX(h.playedAt) DESC LIMIT 20
```

**`data/SongRepository.kt`** — Add:
- `getRecentlyPlayedAlbums(): Flow<List<Album>>`
- `getRecentlyPlayedArtists(): Flow<List<Artist>>`

**`ui/library/MetadataViewModel.kt`** — Update `MetadataUiState.Loaded`:
- Add `recentlyPlayedAlbums: List<Album>`
- Add `recentlyPlayedArtists: List<Artist>`

### Faves Tab (Figma Screen 12)

**`ui/library/FavesTabContent.kt`** — New file
- Sticky `SortFilterBar` at top (sort by: Name, Rating)
- `SongItem` with Rated variant (star + number between text and overflow)
- Shuffle + play all in sort bar

### Playlists Tab (Figma Screen 9)

**`ui/playlists/PlaylistsTabContent.kt`** — New file (adapts PlaylistsScreenContent)
- Remove standalone `Scaffold` wrapper and FAB
- Search/filter bar with create-playlist icon (replaces FAB)
- `PlaylistListItem` redesign per spec (72dp height):
  - **2x2 album art collage** (4 thumbnails at 28x28dp each) as leading image
  - Playlist name (Title Medium), "#Songs . duration" (Label Small)
  - Play button, overflow menu
- Create/rename/delete dialogs kept

**`model/Playlist.kt`** — Add `totalDuration: Long = 0`

**`data/local/PlaylistDao.kt`** — Update query for duration:
```sql
SELECT p.id, p.name, p.createdAt, COUNT(ps.songId) AS songCount,
       COALESCE(SUM(s.duration), 0) AS totalDuration
FROM playlists p LEFT JOIN playlist_songs ps ON p.id = ps.playlistId
LEFT JOIN songs s ON ps.songId = s.id
WHERE p.deletedAt = 0 GROUP BY p.id ORDER BY p.name ASC
```

**`ui/components/PlaylistCollage.kt`** — New file
- 2x2 grid of album art thumbnails (28x28dp each = 56x56dp total)
- Takes `List<String?>` of up to 4 album art URIs
- Falls back to music note icon if fewer than 4

### Files
| File | Action |
|------|--------|
| `ui/library/HistoryTabContent.kt` | Create |
| `ui/library/FavesTabContent.kt` | Create |
| `ui/playlists/PlaylistsTabContent.kt` | Create |
| `ui/components/PlaylistCollage.kt` | Create |
| `data/local/ListeningHistoryDao.kt` | Add album/artist queries |
| `data/local/PlaylistDao.kt` | Update query for duration |
| `data/SongRepository.kt` | Add new methods |
| `data/SongRepositoryImpl.kt` | Implement new methods |
| `ui/library/MetadataViewModel.kt` | Update state class |
| `model/Playlist.kt` | Add `totalDuration` field |

### Verify
- `assembleDebug` passes
- History tab: collapsible sections with song/album/artist content
- Faves tab: rated songs with star + number
- Playlists tab: 2x2 collage thumbnails, duration display

---

## Phase 5: Detail Screens

### Artist Detail (Figma Screens 3 & 4)

**`ui/library/ArtistDetailScreen.kt`** — Major rewrite

Per spec layout:
```
[Hero: full-width circular-cropped portrait ~240dp tall]
  [frosted glass strip top-left: back chevron | artist name | #albums . #songs]
  [frosted glass strip top-right: overflow menu]
[Segmented tabs: Songs | Albums — bottom edge of hero]
  [shuffle + play — right of tabs]
[Song list or Album grid]
[MiniPlayer]
```

- Hero image: circular-cropped artist portrait, ~240dp tall
- Frosted glass info overlay strips on hero (using `FrostedGlassPill` or custom)
- Sub-tabs: Songs | Albums
- Shuffle + play buttons beside tabs
- Songs sub-tab: `SongItem` list
- Albums sub-tab: 2-column `AlbumGridItem` grid
- Hero shrinks/parallax on scroll

**New DAO/Repository**: `getAlbumsByArtist(artist: String): Flow<List<Album>>`
```sql
SELECT albumId AS id, album AS name, artist, COUNT(*) AS songCount, albumArtUri
FROM songs WHERE artist = :artist GROUP BY albumId ORDER BY album ASC
```

### Album Detail (Figma — spec defines layout)

**`ui/library/AlbumDetailScreen.kt`** — Moderate rewrite

Per spec layout:
```
[Hero: full-width album art ~240dp tall]
  [frosted glass strip top-left: back chevron | album name | artist]
  [frosted glass strip bottom-right: shuffle + play]
[Song list — no alphabet index]
[MiniPlayer]
```

- Hero: full-width album art with frosted glass overlays
- Hero shrinks/parallax on scroll
- Switch to `SongItem` component for song list

### Playlist Detail (Figma Screen 8)

**`ui/playlists/PlaylistDetailScreen.kt`** — Moderate rewrite

Per spec layout:
```
[Header: playlist name | #songs . duration]
[Sub-header: shuffle + play]
[Deletable + reorderable song list]
[MiniPlayer]
```

- Playlist name header with song count + total duration
- Shuffle + play buttons
- Song list with `SongItem` (variant: Deletable for delete icon, Reorderable for drag)
- Move "add songs" to header action or overflow (remove FAB)

### Files
| File | Action |
|------|--------|
| `ui/library/ArtistDetailScreen.kt` | Major rewrite (hero + frosted glass + sub-tabs) |
| `ui/library/AlbumDetailScreen.kt` | Moderate rewrite (hero + frosted glass) |
| `ui/playlists/PlaylistDetailScreen.kt` | Moderate rewrite (header + SongItem) |
| `data/local/SongDao.kt` | Add `getAlbumsByArtist()` |
| `data/SongRepository.kt` | Add `getAlbumsByArtist()` |
| `data/SongRepositoryImpl.kt` | Implement |

### Verify
- `assembleDebug` passes
- Artist detail: hero with frosted glass strips, Songs/Albums sub-tabs
- Album detail: hero with frosted glass strips, song list
- Playlist detail: header + deletable/reorderable song list

---

## Phase 6: Now Playing & Queue

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
| File | Action |
|------|--------|
| `ui/player/NowPlayingScreenContent.kt` | Major rewrite |
| `ui/player/components/PlayerControls.kt` | Add repeat/shuffle, frosted pill, larger play |
| `ui/player/components/MiniPlayer.kt` | Major rewrite (pill, frosted glass, 3 controls) |
| `ui/player/components/QueueSheet.kt` | Redesign (frosted glass, SongItem Reorderable) |
| `ui/navigation/NavGraph.kt` | [GUESS] Remove NowPlaying route, add AnimatedContent |

### Verify
- `assembleDebug` passes
- MiniPlayer: frosted glass pill, floating, 3 controls
- Full player: large art, frosted glass controls pill, color-tinted background
- Queue: frosted glass sheet, accent highlighting, drag handles
- MiniPlayer -> Now Playing expansion animation (or navigation fallback)

---

## Phase 7: Artist Image Fetching, Transitions & Polish

### Artist Image Service

**`data/remote/ArtistImageService.kt`** — New file
- Fetch artist images from MusicBrainz or Last.fm API
- Cache results locally in Room
- `getArtistImageUrl(artistName: String): String?`

**`model/ArtistImage.kt`** — New Room entity
- `artist_images(artistName: String PK, imageUrl: String?, fetchedAt: Long)`

**`data/local/ArtistImageDao.kt`** — New DAO
- `getImage(artistName): Flow<ArtistImage?>`
- `insertImage(image: ArtistImage)`

**`data/local/SyncPlayerDatabase.kt`** — Add `ArtistImage` entity + DAO, increment version + migration

**`di/AppModule.kt`** — Bind new service

Integration: `ArtistsTabContent` and `ArtistDetailScreen` resolve images via service -> album art heuristic -> person icon fallback.

### Shared Element Transitions

[GUESS] Requires `SharedTransitionLayout` from Compose 1.7+. If supported:
- Album art: Albums grid item -> Album Detail hero
- Artist portrait: Artists grid item -> Artist Detail hero
- MiniPlayer album art -> Now Playing album art

If Compose version doesn't support this, defer to a future update.

### Scroll Behaviors
- Hero images on detail screens: parallax or clip on scroll
- Top App Bar: gains frosted glass background on scroll (using `nestedScroll` + `TopAppBarScrollBehavior`)
- Sort bar: sticks below tab row on scroll

### Cleanup
- Delete: `ui/library/ArtistListItem.kt` (replaced by ArtistGridItem)
- Delete: `ui/playlists/PlaylistsScreen.kt` (replaced by tab)
- Delete: `ui/playlists/PlaylistsScreenContent.kt` (merged into PlaylistsTabContent)
- Remove `BottomNavDestination` enum
- Remove unused `MostPlayedTab`, `RecentlyPlayedTab`, `FavoriteTab` composables from LibraryScreen
- Verify all files < 300 lines

### Test Updates
- `LibraryViewModelTest.kt` — Update for renamed tab enum
- `LibraryViewModelMetadataTest.kt` — Update for new MetadataUiState shape
- `NowPlayingScreenContentTest.kt` — Update for new layout
- `NowPlayingScreenTest.kt` — Update for new layout
- `PlayerViewModelTest.kt` — Mostly unaffected

### String Resources
- Tab labels, sort options, section headers, content descriptions

### Files
| File | Action |
|------|--------|
| `data/remote/ArtistImageService.kt` | Create |
| `model/ArtistImage.kt` | Create |
| `data/local/ArtistImageDao.kt` | Create |
| `data/local/SyncPlayerDatabase.kt` | Add entity + migration |
| `di/AppModule.kt` | Bind service |
| `ui/library/ArtistListItem.kt` | Delete |
| `ui/playlists/PlaylistsScreen.kt` | Delete |
| `ui/playlists/PlaylistsScreenContent.kt` | Delete |
| All test files | Update assertions |

### Verify
- `assembleDebug` passes
- `test` passes (all unit tests green)
- Artist images load from web with caching
- Shared element transitions work (or gracefully absent)
- Full manual walkthrough of all 12 Figma screens

---

## Summary: All New Files

| File | Phase |
|------|-------|
| `ui/theme/GlassEffect.kt` | 0 |
| `ui/components/SortFilterBar.kt` | 2 |
| `ui/components/AlphabeticalIndexSidebar.kt` | 2 |
| `ui/components/CollapsibleSection.kt` | 2 |
| `ui/components/SongItem.kt` | 2 |
| `ui/components/SongOverflowMenu.kt` | 2 |
| `ui/components/CircularArtistImage.kt` | 2 |
| `ui/components/FrostedGlassPill.kt` | 2 |
| `ui/library/SongsTabContent.kt` | 3 |
| `ui/library/AlbumsTabContent.kt` | 3 |
| `ui/library/ArtistsTabContent.kt` | 3 |
| `ui/library/ArtistGridItem.kt` | 3 |
| `ui/library/HistoryTabContent.kt` | 4 |
| `ui/library/FavesTabContent.kt` | 4 |
| `ui/playlists/PlaylistsTabContent.kt` | 4 |
| `ui/components/PlaylistCollage.kt` | 4 |
| `data/remote/ArtistImageService.kt` | 7 |
| `model/ArtistImage.kt` | 7 |
| `data/local/ArtistImageDao.kt` | 7 |

## Summary: All Modified Files

| File                                     | Phase | Severity                                                         |
|------------------------------------------|-------|------------------------------------------------------------------|
| `ui/theme/Color.kt`                      | 0     | Add AccentColor, BackgroundDark, FrostedGlassSurface             |
| `ui/theme/Theme.kt`                      | 0     | Add LocalAccentColor, override background, disable dynamic color |
| `ui/theme/Type.kt`                       | 0     | Add weight variants per spec                                     |
| `gradle/libs.versions.toml`              | 0     | Add palette-ktx                                                  |
| `app/build.gradle.kts`                   | 0     | Add palette-ktx dependency                                       |
| `ui/navigation/NavGraph.kt`              | 1, 6  | Major rewrite (tabs + AnimatedContent)                           |
| `ui/library/LibraryScreen.kt`            | 1, 3  | Major refactor                                                   |
| `ui/library/LibraryViewModel.kt`         | 1     | Update tab enum                                                  |
| `model/Artist.kt`                        | 3     | Add `artUri` field                                               |
| `data/local/SongDao.kt`                  | 3, 5  | Update + add queries                                             |
| `ui/library/AlbumGridItem.kt`            | 3     | Corner radius, playing border, play overlay                      |
| `data/local/ListeningHistoryDao.kt`      | 4     | Add album/artist queries                                         |
| `data/local/PlaylistDao.kt`              | 4     | Update query for duration                                        |
| `data/SongRepository.kt`                 | 4, 5  | Add methods                                                      |
| `data/SongRepositoryImpl.kt`             | 4, 5  | Implement methods                                                |
| `ui/library/MetadataViewModel.kt`        | 4     | Update state class                                               |
| `model/Playlist.kt`                      | 4     | Add `totalDuration` field                                        |
| `ui/library/ArtistDetailScreen.kt`       | 5     | Major rewrite (hero + frosted glass + sub-tabs)                  |
| `ui/library/AlbumDetailScreen.kt`        | 5     | Moderate rewrite (hero + frosted glass)                          |
| `ui/playlists/PlaylistDetailScreen.kt`   | 5     | Moderate rewrite                                                 |
| `ui/player/NowPlayingScreenContent.kt`   | 6     | Major rewrite                                                    |
| `ui/player/components/PlayerControls.kt` | 6     | Frosted pill, repeat/shuffle, larger play                        |
| `ui/player/components/MiniPlayer.kt`     | 6     | Major rewrite (pill, frosted glass)                              |
| `ui/player/components/QueueSheet.kt`     | 6     | Frosted glass, SongItem reorderable                              |
| `data/local/SyncPlayerDatabase.kt`       | 7     | Add ArtistImage entity + migration                               |
| `di/AppModule.kt`                        | 7     | Bind artist image service                                        |

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