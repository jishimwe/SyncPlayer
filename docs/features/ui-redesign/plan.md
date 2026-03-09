# SyncPlayer UI Redesign Plan

## Context

Comprehensive UI refactor to match 12 Figma screens + a visual design spec document. The biggest structural change is replacing the bottom navigation bar with a horizontal scrollable top tab row. The visual language shifts to a Zune-inspired, content-forward aesthetic with frosted glass overlays, bold typography, and a strict accent color system.

**Source of truth**: `docs/features/ui-redesign/visual-design-spec.md`

### User Decisions
- **Most Played**: Merge into History tab as a section
- **Song overflow menu**: Standard set (Play Next, Add to Queue, Add to Playlist, Go to Artist, Go to Album)
- **Artist images**: Fetch from web (e.g., MusicBrainz/Last.fm API)

### Implementation Discoveries (added during Phase 0-1)
- **Material 3 components are opinionated**: Components like `PrimaryScrollableTabRow`, `TopAppBar`, `Tab`, `Slider` enforce Material Design behavior and visuals. Fighting them with parameters is often more work than replacing them with primitives. Rule of thumb: if a Material component does <70% of what you need, ditch it and build from `Row`, `Box`, `Text`, `Modifier.clickable` etc.
- **`Tab` ripple cannot be suppressed externally**: `Tab`'s internal ripple ignores `indication = null` on its modifier. Must replace `Tab` with plain composables + `noRippleClickable` extension when ripple-free tabs are needed.
- **`TopAppBar` cannot have a gradient background**: Solid color only. Must replace with a custom `Box` + `Brush.verticalGradient`.
- **`PrimaryScrollableTabRow` / `SecondaryScrollableTabRow` enforce spacing and indicator behavior**: Cannot fully suppress the divider line and tab sizing behavior. Replace with custom `Row` + `horizontalScroll` for full control.
- **`HorizontalPager` required for swipe-to-change-tabs**: Native swipe gesture between tabs needs `HorizontalPager` (from `androidx.compose.foundation`). Tab row and pager share the same `PagerState`.
- **`ExtendedColorScheme` already exists in project**: The project has a generated `ExtendedColorScheme` with `accentColor: ColorFamily`. This was defined but never wired up via `CompositionLocal`. Added `LocalExtendedColorScheme` to fix this. Accent color accessed via `LocalExtendedColorScheme.current.accentColor.color`.
- **`LocalAccentColor` CompositionLocal not needed**: The plan originally suggested adding a standalone `LocalAccentColor`. Unnecessary — use `LocalExtendedColorScheme.current.accentColor.color` instead.
- **`dynamicColor` already `false`**: No change needed in `Theme.kt` for this.
- **Single `fontFamily` is cleaner**: Original code had `bodyFontFamily` and `displayFontFamily` both using Nunito Sans identically. Consolidated to one `fontFamily` with all weight variants (Normal, Medium, SemiBold, Bold, ExtraBold).
- **Font sizes**: Material baseline sizes for most slots are correct and don't need overriding. Only `headlineLarge` (32sp ExtraBold, app name) and `labelSmall` (12sp Normal, metadata) were explicitly set. `titleLarge` (22sp Bold) and `titleMedium` (16sp SemiBold) use Material defaults.
- **`backgroundDark` override**: Changed from `#141313` to `#111113` to match spec.
- **`frostedGlass` modifier**: Uses `Modifier.blur()` + `background(color.copy(alpha))`. Order matters — blur before background. Real behind-blur is not feasible in real-time; this approach is the practical alternative.
- **`LibraryViewModel` hoisted to Activity scope**: Search query lives in `LibraryViewModel`. Since the search bar is in `NavGraph`'s `topBar` slot and the ViewModel is used inside `HomeScreen`, it must be scoped to the Activity via `hiltViewModel(LocalActivity.current as ViewModelStoreOwner)` in both `NavGraph` and `HomeScreen` to share the same instance.
- **`rememberSaveable` for tab selection**: `selectedTab` and `searchActive` use `rememberSaveable` in `NavGraph` to survive rotation and process death.
- **`Screen.Library` kept temporarily**: Will be removed once `HomeScreen` and all tab content are fully working.
- **`Screen.Playlists` route kept temporarily**: Same reason — remove in Phase 7 cleanup.
- **MiniPlayer placement**: Moved from `Scaffold`'s `bottomBar` slot (which pushes content up) to a floating `Box` overlay inside the content area, positioned with `Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp, bottom = 8.dp)`.
- **`Scaffold.bottomBar` cannot float**: It's part of the layout system — it always anchors at the bottom and pushes content up. Floating elements must live inside the content `Box` with `Alignment.BottomCenter`.
- **`TopAppBar.actions` is already a `Row`**: Don't wrap actions in an extra `Row` or add `Spacer` — the slot handles spacing automatically. Just place `IconButton`s directly.
- **`Tab.selectedContentColor` / `unselectedContentColor` exist but ripple still can't be suppressed**: These params handle color correctly and are the right place for color logic (not on the `Text` directly). But even using them correctly, `Tab`'s internal ripple cannot be removed — replacing `Tab` with plain composables is still required.
- **Hyphens in version catalog become dots in code**: `palette-ktx` as a catalog key becomes `libs.palette.ktx` — ambiguous and error-prone. Use underscore-free or hyphen-free names. Final working entry: key `palette` in toml → accessed as `libs.palette` in `build.gradle.kts`. Version key must also match: `version.ref = "palette"` and `[versions] palette = "1.0.0"`.
- **`rememberSaveable` vs ViewModel rule of thumb**: `rememberSaveable` for pure UI state (which tab is selected, is dialog open) — survives rotation and process death via `Bundle`. ViewModel for state that triggers repository calls or holds `Flow`s. Selected tab index is `rememberSaveable`; search query that drives `LibraryViewModel` filtering is ViewModel.
- **Spacing tokens are not in Material by default**: `MaterialTheme` only exposes `colorScheme`, `typography`, `shapes`. To standardize spacing (e.g., `spacing.small = 8.dp`), create a custom `@Immutable data class Spacing(...)` and expose via `staticCompositionLocalOf`. **Deferred** — add to `ui/theme/Spacing.kt` when repeated dp values become painful, not upfront.
- **`@Preview` requires stateless composables**: The `NavGraph` itself cannot be previewed because it holds `NavController` and ViewModels. The `ScreenContent` pattern (every screen split into `Screen` with ViewModel and `ScreenContent` with only data params) is what enables `@Preview`. Apply this to every extracted composable: `TopAppBarContent`, `DockedSearchBarContent`, `HomeScreenContent`, all tab content screens. Each gets a `@Preview` with hardcoded sample data. Interactive Preview in Android Studio allows tapping/scrolling without deploying.

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

## Phase 0: Theme & Design Tokens ✅ DONE

**Goal**: Update the theme system to match the visual design spec before any component work.

### What was actually done (differs from original plan)

**`ui/theme/Color.kt`**
- Updated `backgroundDark` to `Color(0xFF111113)`
- Kept existing `ExtendedColorScheme` with `accentColor: ColorFamily` — already had the right structure
- Added `myAccentColor`, `myBackgroundDark`, `myFrostedGlassSurface` as reference values (kept but not primary)

**`ui/theme/Theme.kt`**
- Added `LocalExtendedColorScheme = staticCompositionLocalOf { extendedLight }` at file level
- Added `CompositionLocalProvider(LocalExtendedColorScheme provides extendedColorScheme)` wrapping `MaterialTheme`
- `extendedColorScheme` selected based on `darkTheme` and `dynamicColor` flags, consistent with `colorScheme` selection
- `dynamicColor` was already `false` — no change needed

**`ui/theme/Type.kt`**
- Consolidated `bodyFontFamily` + `displayFontFamily` into single `fontFamily` with 5 weight variants: Normal, Medium, SemiBold, Bold, ExtraBold
- Updated `AppTypography` with spec-mapped slots:
  - `headlineLarge`: ExtraBold (32sp baseline = app name "SyncPlayer")
  - `titleLarge`: Bold, 22sp (active tab, screen headings)
  - `titleMedium`: SemiBold, 16sp baseline (song titles)
  - `bodyMedium`: Regular, 14sp baseline (subtitles)
  - `labelSmall`: Normal, 12sp (metadata)

**`ui/theme/GlassEffect.kt`** — New file
```kotlin
fun Modifier.frostedGlass(
    color: Color = primaryContainerDark,
    alpha: Float = 0.65f,
    blurRadius: Dp = 10.dp,
): Modifier = this.blur(blurRadius).background(color.copy(alpha = alpha))
```

**`gradle/libs.versions.toml` + `app/build.gradle.kts`**
- Added `palette = "1.0.0"` to versions
- Added `palette = { group = "androidx.palette", name = "palette-ktx", version.ref = "palette" }` to libraries
- Added `implementation(libs.palette)` to dependencies

### Verify
- ✅ `assembleDebug` passes
- ✅ Background is near-black (#111113) in dark theme
- ✅ Accent color accessible via `LocalExtendedColorScheme.current.accentColor.color`
- ✅ "SyncPlayer" title renders in headlineLarge (32sp ExtraBold)

---

## Phase 1: Navigation Restructure ✅ IN PROGRESS

**Goal**: Replace bottom NavigationBar with horizontal scrollable top tab row.

### What Changes

**`ui/navigation/NavGraph.kt`** — Major rewrite
- Removed `BottomNavDestination` enum and `NavigationBar`
- Added custom tab row (see below) with 6 tabs: History, Faves, Songs, Albums, Artists, Playlists
- **Tab styling**: No ripple, no underline, no divider. Active = accent color + `titleLarge`. Inactive = `onSurface` at reduced opacity + `titleMedium`. Uses `noRippleClickable`.
- Top bar: Custom `Box` with `Brush.verticalGradient` (background color → transparent, top to bottom). Contains "SyncPlayer" in `headlineLarge` + search icon + settings gear.
- Settings: gear icon navigates to `Screen.Settings`
- `searchActive` and `selectedTab` use `rememberSaveable`
- `LibraryViewModel` hoisted to Activity scope for search bar access
- `PlayerViewModel` hoisted to Activity scope (unchanged from before)
- `Screen.Home` is start destination
- MiniPlayer floats in content `Box` at `Alignment.BottomCenter` with `padding(horizontal = 8.dp, bottom = 8.dp)`
- Tab row synced with `HorizontalPager` via shared `PagerState` (see below)

**Custom tab row implementation** (replaces `PrimaryScrollableTabRow`):
```
Row(horizontalScroll) {
    forEach tab:
        Text + noRippleClickable + padding
}
```

**`HorizontalPager` for swipe gesture** (replaces NavHost tab content):
- `PagerState` shared between tab row and pager
- Swiping pager updates `selectedTab`; tapping tab updates pager position
- `LaunchedEffect(selectedTab)` animates pager to correct page on tab click
- `LaunchedEffect(pagerState.currentPage)` updates `selectedTab` on swipe

**`ui/navigation/NavGraph.kt`** — extracted composables (each gets a `@Preview`):
- `TopAppBarContent` — custom gradient box with title + actions + tab row. Params: `selectedTab`, `onTabSelected`, `onSearchClick`, `onSettingsClick`. No `NavController` in params — uses callbacks. Previewable with hardcoded `selectedTab = LibraryTab.SONGS`.
- `DockedSearchBarContent` — search bar shown when `searchActive`. Params: `query`, `active`, `onQueryChanged`, `onActiveChanged`, `onClearSearchQuery`. Previewable with hardcoded `active = true`.

**`Screen` sealed class** — Updated
- Added `Screen.Home`
- Kept `Screen.Library` temporarily (remove in Phase 7 cleanup)
- Kept `Screen.Playlists` temporarily (remove in Phase 7 cleanup)
- Kept: `Screen.NowPlaying`, `Screen.AlbumDetail`, `Screen.ArtistDetail`, `Screen.PlaylistDetail`, `Screen.Settings`

**`ui/home/HomeScreen.kt`** — New file (replaces `LibraryScreen` as top-level)
- `HomeScreen`: obtains `LibraryViewModel`, `MetadataViewModel`, `PlayerViewModel` (all Activity-scoped). Handles permissions, lifecycle, `LaunchedEffect` for refresh.
- `HomeScreenContent`: pure composable, receives states + callbacks. `when(selectedTab)` delegates to tab content composables.
- Navigation callbacks passed down from `NavGraph` through `HomeScreen` to `HomeScreenContent` to tab screens.

**`LibraryTab` enum** — Moved from `LibraryViewModel` to `NavGraph.kt`:
```kotlin
enum class LibraryTab(val label: String) {
    HISTORY("History"), FAVORITES("Faves"), SONGS("Songs"),
    ALBUMS("Albums"), ARTISTS("Artists"), PLAYLISTS("Playlists")
}
```

### Remaining Phase 1 work
- [ ] Wire `HorizontalPager` — currently tab clicks work but swipe gesture not yet implemented
- [ ] `TopAppBar` → custom gradient `Box` (currently still using `TopAppBar`)
- [ ] Inactive tab opacity — currently `onSurface` solid, should be `onSurface.copy(alpha = 0.4f)`
- [ ] Remove `Screen.Library` composable block from NavHost (after HomeScreen fully working)
- [ ] Pass navigation callbacks from `HomeScreenContent` down to all tab screens

### Files
| File                             | Action                      |
|----------------------------------|-----------------------------|
| `ui/navigation/NavGraph.kt`      | Major rewrite — in progress |
| `ui/home/HomeScreen.kt`          | New file — in progress      |
| `ui/library/LibraryViewModel.kt` | `LibraryTab` enum moved out |

### Verify
- `assembleDebug` passes
- All 6 tabs render
- Active tab: accent color + larger text, no underline, no ripple, no divider
- Swipe gesture changes tabs
- Settings accessible via gear icon
- MiniPlayer floats correctly
- Detail screens navigate correctly
- Search bar toggles correctly and filters results

---

## Phase 2: New Reusable Components

**Goal**: Build shared UI components used across multiple screens. All components are fully custom — no Material equivalents fit the spec.

### New Files

**`ui/theme/Modifiers.kt`** — New file (or add to existing utility file)
- `fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier` — reusable ripple-free clickable
- Used everywhere tabs, song items, and other components need click without ripple

**`ui/components/SortFilterBar.kt`**
- Custom `Row`: filter icon + sort label (dropdown), `Spacer(weight)`, shuffle `IconButton`, play `IconButton`
- **Sticky** on scroll (parent handles `stickyHeader` in LazyColumn or `nestedScroll`)
- Params: `sortLabel: String`, `onSortClick`, `onShuffle`, `onPlayAll`, `modifier`
- Used on: Faves, Albums, Artists, History sections

**`ui/components/AlphabeticalIndexSidebar.kt`**
- Vertical `Column` of A-Z letters on right edge — fully custom, no Material equivalent
- Touch/drag scrolls adjacent list to matching letter
- Uses `pointerInput` for drag detection
- Params: `letters: List<Char>`, `onLetterSelected: (Char) -> Unit`, `modifier`
- Used on: Songs, Albums, Artists tabs

**`ui/components/CollapsibleSection.kt`**
- Header row: section name + shuffle + play + collapse/expand chevron
- Expandable content area using `AnimatedVisibility`
- Params: `title`, `onShuffle`, `onPlayAll`, `isExpanded`, `onToggle`, `content: @Composable () -> Unit`
- Used on: History tab

**`ui/components/SongItem.kt`** — Fully custom, 72dp height
- `Row`: 56x56dp album art (4dp corner radius, `AsyncImage`) | text column | optional trailing
- Title (`titleMedium`, 16sp SemiBold), subtitle "Artist · Album" (`labelSmall`, 12sp)
- Optional trailing: star rating display (non-interactive), overflow menu (48x48 touch target)
- **States** per spec:
  - Default: white text
  - Playing: accent color on title, subtitle, icons; accent border (1.5dp) on album art via `border` modifier
  - Selected: accent border on entire item (1dp)
  - Reorderable: replaces overflow with drag handle (`unfold_more` icon)
  - Deletable: replaces overflow with trash icon
  - Rated: star display between text and overflow
- Params: `song`, `onClick`, `isPlaying`, `isSelected`, `showRating`, `variant: SongItemVariant` (Default, Reorderable, Deletable), `onMenuAction: (SongMenuAction) -> Unit`
- `SongMenuAction` sealed interface: PlayNext, AddToQueue, AddToPlaylist, GoToArtist, GoToAlbum
- Uses `noRippleClickable` — no ripple on item tap

**`ui/components/SongOverflowMenu.kt`**
- `DropdownMenu` (Material — fine as-is) with 5 items
- Params: `expanded`, `onDismiss`, `onAction: (SongMenuAction) -> Unit`

**`ui/components/CircularArtistImage.kt`**
- `SubcomposeAsyncImage` clipped to `CircleShape` via `Modifier.clip(CircleShape)`
- Person icon fallback in `error` slot
- Params: `imageUri: String?`, `artistName`, `modifier`

**`ui/components/FrostedGlassPill.kt`** — Fully custom
- `Box` with `Modifier.clip(RoundedCornerShape(50))` (fully rounded) + `frostedGlass` modifier
- Params: `modifier`, `content: @Composable RowScope.() -> Unit`
- Used on: Artist grid item name pill, MiniPlayer, Now Playing controls

### Files
| File                                        | Action                                 |
|---------------------------------------------|----------------------------------------|
| `ui/theme/Modifiers.kt`                     | Create — `noRippleClickable` extension |
| `ui/components/SortFilterBar.kt`            | Create                                 |
| `ui/components/AlphabeticalIndexSidebar.kt` | Create                                 |
| `ui/components/CollapsibleSection.kt`       | Create                                 |
| `ui/components/SongItem.kt`                 | Create                                 |
| `ui/components/SongOverflowMenu.kt`         | Create                                 |
| `ui/components/CircularArtistImage.kt`      | Create                                 |
| `ui/components/FrostedGlassPill.kt`         | Create                                 |

### Verify
- `assembleDebug` passes
- SongItem renders at 72dp with 56x56 album art, all state variants correct
- Playing state shows accent color + border correctly
- Frosted glass pill renders with blur + semi-transparent fill
- No ripple on `SongItem` or tab taps

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
- `AlphabeticalIndexSidebar` on right edge

**`ui/library/AlbumGridItem.kt`** — Update existing
- Album art: full-width square, 4dp corner radius via `Modifier.clip(RoundedCornerShape(4.dp))`
- Title: `bodyMedium` Bold (14sp)
- Artist: `labelSmall` secondary text (12sp)
- Optional accent border for playing state via `Modifier.border()`
- Subtle play button overlay: `Box` with `Icon` at `Alignment.Center`, low alpha until pressed

### Artists Tab (Figma Screen 5)

**`ui/library/ArtistsTabContent.kt`** — New file
- Sticky `SortFilterBar` at top (sort by: Name)
- `LazyVerticalGrid(GridCells.Fixed(2))` with `ArtistGridItem`
- `AlphabeticalIndexSidebar` on right edge

**`ui/library/ArtistGridItem.kt`** — New file, fully custom
- `Box`: `CircularArtistImage` full width
- `FrostedGlassPill` overlapping bottom of circle by ~12dp (negative `offset` or `padding` trick)
- Pill content: play icon | artist name (truncated) | overflow 3-dots
- Params: `artist: Artist`, `onClick`, `modifier`

### Data Layer Changes

**`model/Artist.kt`** — Add `artUri: String? = null`

**`data/local/SongDao.kt`** — Update `getAllArtists()`:
```sql
SELECT artist AS name, COUNT(*) AS songCount,
       COUNT(DISTINCT albumId) AS albumCount,
       (SELECT albumArtUri FROM songs s2 WHERE s2.artist = songs.artist LIMIT 1) AS artUri
FROM songs GROUP BY artist ORDER BY artist ASC
```

### Files
| File                              | Action                                               |
|-----------------------------------|------------------------------------------------------|
| `ui/library/SongsTabContent.kt`   | Create                                               |
| `ui/library/AlbumsTabContent.kt`  | Create                                               |
| `ui/library/ArtistsTabContent.kt` | Create                                               |
| `ui/library/ArtistGridItem.kt`    | Create                                               |
| `ui/library/AlbumGridItem.kt`     | Update (corner radius, playing border, play overlay) |
| `model/Artist.kt`                 | Add `artUri` field                                   |
| `data/local/SongDao.kt`           | Update `getAllArtists()` query                       |
| `ui/home/HomeScreen.kt`           | Wire navigation callbacks to tab screens             |

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
- `PlaylistListItem` redesign per spec (72dp height) — fully custom:
  - `PlaylistCollage` as leading image
  - Playlist name (`titleMedium`), "#Songs · duration" (`labelSmall`)
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

**`ui/components/PlaylistCollage.kt`** — New file, fully custom
- `Box` with 2x2 `LazyVerticalGrid` or manual `Row`+`Column` of 28x28dp `AsyncImage`s
- Up to 4 album art URIs; falls back to music note icon for empty slots
- Total size: 56x56dp

### Files
| File                                  | Action                    |
|---------------------------------------|---------------------------|
| `ui/library/HistoryTabContent.kt`     | Create                    |
| `ui/library/FavesTabContent.kt`       | Create                    |
| `ui/playlists/PlaylistsTabContent.kt` | Create                    |
| `ui/components/PlaylistCollage.kt`    | Create                    |
| `data/local/ListeningHistoryDao.kt`   | Add album/artist queries  |
| `data/local/PlaylistDao.kt`           | Update query for duration |
| `data/SongRepository.kt`              | Add new methods           |
| `data/SongRepositoryImpl.kt`          | Implement new methods     |
| `ui/library/MetadataViewModel.kt`     | Update state class        |
| `model/Playlist.kt`                   | Add `totalDuration` field |

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
  [frosted glass strip top-left: back chevron | artist name | #albums · #songs]
  [frosted glass strip top-right: overflow menu]
[Custom tab row: Songs | Albums — bottom edge of hero]  ← NOT SecondaryScrollableTabRow
  [shuffle + play — right of tabs]
[Song list or Album grid]
[MiniPlayer]
```

- Hero image: `CircularArtistImage` stretched to full width, ~240dp tall
- Frosted glass info overlay strips: custom `Box` + `frostedGlass` modifier + `RoundedCornerShape`
- **Sub-tabs: custom `Row` with `noRippleClickable`** — same pattern as main tab row. `SecondaryScrollableTabRow` has the same ripple/indicator problems as `PrimaryScrollableTabRow`.
- `HorizontalPager` for Songs/Albums sub-tab swipe
- Shuffle + play buttons beside tabs
- Hero shrinks/parallax on scroll: `nestedScroll` + `graphicsLayer { translationY = ... }`

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

- Hero: `AsyncImage` full width, `~240dp` height, `ContentScale.Crop`
- Frosted glass overlays: custom `Box` + `frostedGlass` + positioned with `Alignment`
- Hero parallax on scroll: same `nestedScroll` + `graphicsLayer` pattern as artist detail
- Switch to `SongItem` component for song list

### Playlist Detail (Figma Screen 8)

**`ui/playlists/PlaylistDetailScreen.kt`** — Moderate rewrite

Per spec layout:
```
[Header: playlist name | #songs · duration]
[Sub-header: shuffle + play]
[Deletable + reorderable song list]
[MiniPlayer]
```

- Playlist name header with song count + total duration
- Shuffle + play buttons
- Song list with `SongItem` (variant: Deletable / Reorderable)
- Move "add songs" to header action or overflow — remove FAB

### Files
| File                                   | Action                                                 |
|----------------------------------------|--------------------------------------------------------|
| `ui/library/ArtistDetailScreen.kt`     | Major rewrite (hero + frosted glass + custom sub-tabs) |
| `ui/library/AlbumDetailScreen.kt`      | Moderate rewrite (hero + frosted glass + parallax)     |
| `ui/playlists/PlaylistDetailScreen.kt` | Moderate rewrite (header + SongItem)                   |
| `data/local/SongDao.kt`                | Add `getAlbumsByArtist()`                              |
| `data/SongRepository.kt`               | Add `getAlbumsByArtist()`                              |
| `data/SongRepositoryImpl.kt`           | Implement                                              |

### Verify
- `assembleDebug` passes
- Artist detail: hero with frosted glass strips, custom Songs/Albums sub-tabs, swipe gesture
- Album detail: hero with frosted glass strips, parallax on scroll, song list
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
1. Remove `TopAppBar` → simple `Row` with chevron + queue icon
2. Album art: nearly full-width hero, ~1:1 aspect ratio, `ContentScale.Crop`
3. SeekBar immediately below art — test `Slider` first, go custom if needed
4. Star rating + heart on same row below seek bar — fully custom
5. Track info below stars: title (`titleMedium`), album (`bodyMedium`), artist (`bodyMedium`)
6. **Controls in `FrostedGlassPill`** at bottom: repeat | prev | play(large, accent bg circle) | next | shuffle
7. Background: subtle tint from album art dominant color via `palette-ktx` (`DarkVibrantSwatch` preferred, `DarkMutedSwatch` fallback), animated with `animateColorAsState`

**`ui/player/components/PlayerControls.kt`** — Expand
- Add repeat + shuffle buttons flanking prev/play/next
- Play button: larger, accent-tinted circular background via `Modifier.background(accentColor, CircleShape)`
- Wrap entire row in `FrostedGlassPill`

### MiniPlayer -> Now Playing Transition

**[GUESS] Major architectural change**: The spec says MiniPlayer -> NowPlaying is NOT navigation, it's an `AnimatedContent` expansion. This requires:
- Remove `Screen.NowPlaying` route from NavHost
- Instead, manage a `isNowPlayingExpanded: Boolean` state at the Scaffold level in NavGraph
- When expanded: `AnimatedContent` transitions MiniPlayer pill into full Now Playing screen
- Shared element: album art grows from 56dp thumbnail to full-width hero via `SharedTransitionLayout`
- This is the riskiest change. If too complex, keep as navigation and defer animation.

### MiniPlayer (Figma bottom bars)

**`ui/player/components/MiniPlayer.kt`** — Major rewrite, fully custom
- `Box` with `Modifier.clip(RoundedCornerShape(36.dp))` + `frostedGlass` modifier
- Height: ~72dp
- Content `Row`: 56x56 album art | title / "Artist · Album" (fill weight) | skip prev | play/pause (accent bg) | skip next
- Play/pause: accent-tinted `Box` background with `CircleShape` or `RoundedCornerShape`
- `noRippleClickable` on the whole pill (except controls which keep their own click)
- Floating position managed by NavGraph (see Phase 1)

### Queue (Figma Screen 10)

**`ui/player/components/QueueSheet.kt`** — Moderate rewrite
- `ModalBottomSheet` as container (fine as-is), content custom
- Sheet background: `frostedGlass` modifier on content container
- Header `Row`: "Queue" text + trash `IconButton` (clear) + collapse chevron
- Currently playing item: accent border + accent-colored text
- Each item: `SongItem` variant Reorderable (drag handle replaces overflow)

### Files
| File                                     | Action                                               |
|------------------------------------------|------------------------------------------------------|
| `ui/player/NowPlayingScreenContent.kt`   | Major rewrite                                        |
| `ui/player/components/PlayerControls.kt` | Add repeat/shuffle, frosted pill, larger play        |
| `ui/player/components/MiniPlayer.kt`     | Major rewrite (pill, frosted glass, 3 controls)      |
| `ui/player/components/QueueSheet.kt`     | Redesign (frosted glass, SongItem Reorderable)       |
| `ui/navigation/NavGraph.kt`              | [GUESS] Remove NowPlaying route, add AnimatedContent |

### Verify
- `assembleDebug` passes
- MiniPlayer: frosted glass pill, floating, 3 controls, no ripple
- Full player: large art, frosted glass controls pill, color-tinted background
- Queue: frosted glass sheet, accent highlighting, drag handles
- MiniPlayer → Now Playing expansion animation (or navigation fallback)

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

Integration: `ArtistsTabContent` and `ArtistDetailScreen` resolve images via service → album art heuristic → person icon fallback.

### Shared Element Transitions

[GUESS] Requires `SharedTransitionLayout` from Compose 1.7+. If supported:
- Album art: Albums grid item → Album Detail hero
- Artist portrait: Artists grid item → Artist Detail hero
- MiniPlayer album art → Now Playing album art

If Compose version doesn't support this, defer.

### Scroll Behaviors
- Hero images on detail screens: parallax via `nestedScroll` + `graphicsLayer`
- Top bar: gradient already handles the "fades on scroll" look — no additional scroll behavior needed
- Sort bar: sticks below tab row via `stickyHeader` in `LazyColumn`

### Cleanup
- Delete: `ui/library/ArtistListItem.kt` (replaced by ArtistGridItem)
- Delete: `ui/playlists/PlaylistsScreen.kt` (replaced by tab)
- Delete: `ui/playlists/PlaylistsScreenContent.kt` (merged into PlaylistsTabContent)
- Remove `Screen.Library` and its composable block from NavHost
- Remove `Screen.Playlists` and its composable block from NavHost
- Remove `BottomNavDestination` enum (already done in Phase 1)
- Verify all files < 300 lines

### Test Updates
- `LibraryViewModelTest.kt` — Update for renamed tab enum (`LibraryTab` moved to NavGraph.kt)
- `LibraryViewModelMetadataTest.kt` — Update for new MetadataUiState shape
- `NowPlayingScreenContentTest.kt` — Update for new layout
- `NowPlayingScreenTest.kt` — Update for new layout
- `PlayerViewModelTest.kt` — Mostly unaffected

### String Resources
- Tab labels, sort options, section headers, content descriptions — no hardcoded strings

### Files
| File                                     | Action                 |
|------------------------------------------|------------------------|
| `data/remote/ArtistImageService.kt`      | Create                 |
| `model/ArtistImage.kt`                   | Create                 |
| `data/local/ArtistImageDao.kt`           | Create                 |
| `data/local/SyncPlayerDatabase.kt`       | Add entity + migration |
| `di/AppModule.kt`                        | Bind service           |
| `ui/library/ArtistListItem.kt`           | Delete                 |
| `ui/playlists/PlaylistsScreen.kt`        | Delete                 |
| `ui/playlists/PlaylistsScreenContent.kt` | Delete                 |
| All test files                           | Update assertions      |

### Verify
- `assembleDebug` passes
- `test` passes (all unit tests green)
- Artist images load from web with caching
- Shared element transitions work (or gracefully absent)
- Full manual walkthrough of all 12 Figma screens

---

## Summary: All New Files

| File                                        | Phase                                             |
|---------------------------------------------|---------------------------------------------------|
| `ui/theme/GlassEffect.kt`                   | 0                                                 |
| `ui/theme/Spacing.kt`                       | deferred — add when dp repetition becomes painful |
| `ui/theme/Modifiers.kt`                     | 2                                                 |
| `ui/home/HomeScreen.kt`                     | 1                                                 |
| `ui/components/SortFilterBar.kt`            | 2                                                 |
| `ui/components/AlphabeticalIndexSidebar.kt` | 2                                                 |
| `ui/components/CollapsibleSection.kt`       | 2                                                 |
| `ui/components/SongItem.kt`                 | 2                                                 |
| `ui/components/SongOverflowMenu.kt`         | 2                                                 |
| `ui/components/CircularArtistImage.kt`      | 2                                                 |
| `ui/components/FrostedGlassPill.kt`         | 2                                                 |
| `ui/library/SongsTabContent.kt`             | 3                                                 |
| `ui/library/AlbumsTabContent.kt`            | 3                                                 |
| `ui/library/ArtistsTabContent.kt`           | 3                                                 |
| `ui/library/ArtistGridItem.kt`              | 3                                                 |
| `ui/library/HistoryTabContent.kt`           | 4                                                 |
| `ui/library/FavesTabContent.kt`             | 4                                                 |
| `ui/playlists/PlaylistsTabContent.kt`       | 4                                                 |
| `ui/components/PlaylistCollage.kt`          | 4                                                 |
| `data/remote/ArtistImageService.kt`         | 7                                                 |
| `model/ArtistImage.kt`                      | 7                                                 |
| `data/local/ArtistImageDao.kt`              | 7                                                 |

## Summary: All Modified Files

| File                                     | Phase | Severity                                                          |
|------------------------------------------|-------|-------------------------------------------------------------------|
| `ui/theme/Color.kt`                      | 0     | backgroundDark override, reference colors added                   |
| `ui/theme/Theme.kt`                      | 0     | LocalExtendedColorScheme wired up                                 |
| `ui/theme/Type.kt`                       | 0     | Single fontFamily, all weight variants, spec slots mapped         |
| `gradle/libs.versions.toml`              | 0     | palette-ktx added                                                 |
| `app/build.gradle.kts`                   | 0     | palette-ktx dependency added                                      |
| `ui/navigation/NavGraph.kt`              | 1, 6  | Major rewrite (custom tabs + HorizontalPager + AnimatedContent)   |
| `ui/library/LibraryViewModel.kt`         | 1     | LibraryTab enum removed (moved to NavGraph.kt)                    |
| `model/Artist.kt`                        | 3     | Add `artUri` field                                                |
| `data/local/SongDao.kt`                  | 3, 5  | Update + add queries                                              |
| `ui/library/AlbumGridItem.kt`            | 3     | Corner radius, playing border, play overlay                       |
| `data/local/ListeningHistoryDao.kt`      | 4     | Add album/artist queries                                          |
| `data/local/PlaylistDao.kt`              | 4     | Update query for duration                                         |
| `data/SongRepository.kt`                 | 4, 5  | Add methods                                                       |
| `data/SongRepositoryImpl.kt`             | 4, 5  | Implement methods                                                 |
| `ui/library/MetadataViewModel.kt`        | 4     | Update state class                                                |
| `model/Playlist.kt`                      | 4     | Add `totalDuration` field                                         |
| `ui/library/ArtistDetailScreen.kt`       | 5     | Major rewrite (hero + frosted glass + custom sub-tabs + parallax) |
| `ui/library/AlbumDetailScreen.kt`        | 5     | Moderate rewrite (hero + frosted glass + parallax)                |
| `ui/playlists/PlaylistDetailScreen.kt`   | 5     | Moderate rewrite (header + SongItem)                              |
| `ui/player/NowPlayingScreenContent.kt`   | 6     | Major rewrite                                                     |
| `ui/player/components/PlayerControls.kt` | 6     | Frosted pill, repeat/shuffle, larger play                         |
| `ui/player/components/MiniPlayer.kt`     | 6     | Major rewrite (pill, frosted glass)                               |
| `ui/player/components/QueueSheet.kt`     | 6     | Frosted glass, SongItem reorderable                               |
| `data/local/SyncPlayerDatabase.kt`       | 7     | Add ArtistImage entity + migration                                |
| `di/AppModule.kt`                        | 7     | Bind artist image service                                         |

## Summary: Deleted Files

| File                                     | Phase | Reason                          |
|------------------------------------------|-------|---------------------------------|
| `ui/library/ArtistListItem.kt`           | 7     | Replaced by ArtistGridItem      |
| `ui/playlists/PlaylistsScreen.kt`        | 7     | Playlists is now a tab          |
| `ui/playlists/PlaylistsScreenContent.kt` | 7     | Merged into PlaylistsTabContent |

## Key Spec Details to Reference During Implementation

These are easy to forget — reference during coding:
- **Accent max**: 1-2 accented elements per screen, state only, never decorative
- **Accent access**: `LocalExtendedColorScheme.current.accentColor.color`
- **Song item height**: 72dp, album art 56x56dp, corner radius 4dp
- **Album art in grid**: 4dp corner radius, no border default
- **Artist pill**: frosted glass, overlaps circle bottom, fully rounded ends
- **Playlist thumbnail**: 2x2 collage of 28x28dp album arts
- **MiniPlayer**: pill (36dp corner radius), floating (8dp horizontal + 8dp bottom margin), frosted glass
- **Tab row**: custom `Row` + `horizontalScroll` + `HorizontalPager`. NO `PrimaryScrollableTabRow`. NO ripple. NO divider. NO underline indicator.
- **Tab active**: accent color + `titleLarge` (22sp Bold)
- **Tab inactive**: `onSurface.copy(alpha = 0.4f)` + `titleMedium` (16sp SemiBold)
- **Top bar**: custom `Box` + `Brush.verticalGradient` (background → transparent). NO `TopAppBar`.
- **Controls pill**: frosted glass on Now Playing screen
- **Background**: #111113 (near-black) = `MaterialTheme.colorScheme.background`
- **Min SDK 34**: No need for pre-API-31 blur fallbacks
- **No ripple anywhere**: use `noRippleClickable` extension on all interactive elements that shouldn't show ripple
- **`noRippleClickable`**: cannot suppress ripple on Material `Tab` — must replace with plain composable