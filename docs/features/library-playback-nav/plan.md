# Library → Playback Navigation - Plan

**Feature**: Connect library browsing UI (Songs, Albums, Artists) to the playback system
**Status**: In Progress
**Created**: 2026-02-14
**Updated**: 2026-02-15

## Context

Phase 2 (Playback) built the full playback engine — `PlaybackService`, `PlayerRepository`, `PlayerViewModel`, `NowPlayingScreen`, `MiniPlayer` — but left a critical gap: **nothing in the library UI triggers playback**. All three tabs (Songs, Albums, Artists) display data but tapping items does nothing. `SongListItem`, `AlbumGridItem`, and `ArtistListItem` have no `onClick` handlers.

Additionally, `PlayerEvent.PlaySongs` is missing the `startIndex` parameter (the Phase 2 plan had it, but the implementation dropped it), so even if we wired up clicks, we couldn't start playback at the correct song.

This plan connects the library browsing UI to the playback system so users can actually play music.

## Scope

**Included:**
- Tap song in Songs tab → queue all visible songs, start at tapped index, navigate to NowPlaying
- Tap album → navigate to new Album detail screen (song list sorted by track number) → tap song → play album songs from that index
- Tap artist → navigate to new Artist detail screen (song list sorted by album + track) → tap song → play artist songs from that index
- Fix `PlayerEvent.PlaySongs` to include `startIndex`
- Update `docs/PLAN.md` to reflect current project state

**Excluded:**
- Long-press context menus (add to queue, play next) — future enhancement
- Search or filtering within detail screens
- Album/artist header art or metadata beyond a song list
- New ViewModels for detail screens (unnecessary for simple read-only song lists)

## Approach

### Why this approach

The simplest path: add `onClick` callbacks through existing composables, create two new detail screens following the established `Screen`/`ScreenContent` testable pattern, and wire up navigation. No new ViewModels — `LibraryViewModel` already has access to `SongRepository` which exposes `getSongsByAlbum()` and `getSongsByArtist()`. The `PlayerViewModel` is already activity-scoped and shared across all screens.

### Alternative considered

**Separate AlbumDetailViewModel / ArtistDetailViewModel**: Would be more strictly MVVM, but these screens are simple read-only song lists with a single action (play). A ViewModel per screen would add 2 new files + 2 Hilt bindings + 2 test files for what amounts to a single Flow collection. Instead, we add thin wrapper methods to `LibraryViewModel` (`getSongsByAlbum`, `getSongsByArtist`) that delegate to `SongRepository`. This keeps composables ViewModel-dependent (not repository-dependent) while avoiding unnecessary boilerplate.

## Technical Architecture

### Navigation flow

```
LibraryScreen (Songs tab)
    └── SongListItem tap → PlayerEvent.PlaySongs(allSongs, index) → NowPlayingScreen

LibraryScreen (Albums tab)
    └── AlbumGridItem tap → AlbumDetailScreen
        └── SongListItem tap → PlayerEvent.PlaySongs(albumSongs, index) → NowPlayingScreen

LibraryScreen (Artists tab)
    └── ArtistListItem tap → ArtistDetailScreen
        └── SongListItem tap → PlayerEvent.PlaySongs(artistSongs, index) → NowPlayingScreen
```

### New routes

```kotlin
sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object NowPlaying : Screen("now_playing")
    data object AlbumDetail : Screen("album_detail/{albumId}/{albumName}")  // NEW
    data object ArtistDetail : Screen("artist_detail/{artistName}")         // NEW
}
```

### Callback threading

`LibraryScreen` currently receives `onNavigateToNowPlaying: () -> Unit`. It now needs two additional callbacks for detail screen navigation:

```kotlin
@Composable
fun LibraryScreen(
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbumDetail: (albumId: Long, albumName: String) -> Unit,  // NEW
    onNavigateToArtistDetail: (artistName: String) -> Unit,              // NEW
)
```

These flow down through `LibraryScreenContent` → tab composables → list items.

### Existing infrastructure reused

| Component | Where | How |
|-----------|-------|-----|
| `SongRepository.getSongsByAlbum(albumId)` | `data/SongRepository.kt:19` | Already returns `Flow<List<Song>>` sorted by track number |
| `SongRepository.getSongsByArtist(artist)` | `data/SongRepository.kt:21` | Already returns `Flow<List<Song>>` sorted by album + track |
| `SongListItem` | `ui/library/SongListItem.kt` | Reused in detail screens (just needs `onClick` added) |
| `PlayerEvent.PlaySongs` | `ui/player/PlayerEvent.kt:19` | Needs `startIndex` added, then used everywhere |
| `PlayerViewModel` (activity-scoped) | `ui/player/PlayerViewModel.kt` | Shared via `hiltViewModel(LocalActivity.current as ViewModelStoreOwner)` |
| `BackButton` composable | `ui/player/NowPlayingScreenContent.kt:193` | Reused in detail screen top bars (note: currently named `BackButon` — typo) |

## Implementation Layers

### Layer 1: Fix `PlayerEvent.PlaySongs` startIndex

**Files:**
- `ui/player/PlayerEvent.kt`
- `ui/player/PlayerViewModel.kt`

**Changes:**

`PlayerEvent.kt` — add `startIndex` field:
```kotlin
data class PlaySongs(
    val songs: List<Song>,
    val startIndex: Int = 0,  // ADD
) : PlayerEvent
```

`PlayerViewModel.kt` line 77 — pass `startIndex` through:
```kotlin
// Current:
playerRepository.playSongs(event.songs)
// Changed to:
playerRepository.playSongs(event.songs, event.startIndex)
```

**Build & verify:** `assembleDebug`

---

### Layer 2: Wire song tap in Songs tab

**Files:**
- `ui/library/SongListItem.kt`
- `ui/library/LibraryScreen.kt`

**SongListItem.kt** — add `onClick` parameter:
```kotlin
@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,          // ADD
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),  // ADD clickable modifier
        // ... rest unchanged
    )
}
```

**LibraryScreen.kt** — thread callbacks:

1. `LibraryScreenContent` gets new parameter:
   ```kotlin
   onSongClick: (index: Int, songs: List<Song>) -> Unit
   ```

2. `SongsTab` signature changes:
   ```kotlin
   @Composable
   private fun SongsTab(
       state: LibraryUiState.Loaded,
       onSongClick: (index: Int, songs: List<Song>) -> Unit,
   )
   ```
   Uses `itemsIndexed` instead of `items` to get the index:
   ```kotlin
   itemsIndexed(state.songs, key = { _, song -> song.id }) { index, song ->
       SongListItem(
           song = song,
           onClick = { onSongClick(index, state.songs) },
       )
   }
   ```

3. `LibraryScreen` wires it:
   ```kotlin
   onSongClick = { index, songs ->
       playerViewModel.onEvent(PlayerEvent.PlaySongs(songs, index))
       onNavigateToNowPlaying()
   }
   ```

   > **💡 Learning note — callback threading pattern:**
   > The lambda is defined at `LibraryScreen` (the level that has `playerViewModel` and `onNavigateToNowPlaying`), not at `SongListItem`. Each level just passes the typed callback down:
   > - `LibraryScreen` defines `(Int, List<Song>) -> Unit` with `{ index, songs -> ... }`
   > - `LibraryScreenContent` and `SongsTab` accept and forward it unchanged
   > - `SongListItem`'s `onClick: () -> Unit` calls `onSongClick(index, state.songs)`, capturing those values from the enclosing `itemsIndexed` scope
   >
   > **Don't cast the lambda** (`as (Int, List<Song>) -> Unit`) — Kotlin infers the type from the parameter declaration. The explicit cast is unnecessary and will cause issues if parameter order doesn't match exactly.

**Build & verify:** `assembleDebug`

---

### Layer 3: Album detail screen + navigation

**Files:**
- `ui/library/AlbumDetailScreen.kt` — **NEW**
- `ui/library/AlbumGridItem.kt` — add `onClick`
- `ui/library/LibraryScreen.kt` — add `onAlbumClick` callback threading
- `ui/library/LibraryViewModel.kt` — add `getSongsByAlbum()` wrapper
- `ui/navigation/NavGraph.kt` — add `AlbumDetail` route + `Screen.AlbumDetail`

**AlbumDetailScreen.kt** (new file, ~80 lines):
```kotlin
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    albumName: String,
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    playerViewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val songs by viewModel.getSongsByAlbum(albumId)
        .collectAsStateWithLifecycle(emptyList())

    AlbumDetailScreenContent(
        albumName = albumName,
        songs = songs,
        onSongClick = { index ->
            playerViewModel.onEvent(PlayerEvent.PlaySongs(songs, index))
            onNavigateToNowPlaying()
        },
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun AlbumDetailScreenContent(
    albumName: String,
    songs: List<Song>,
    onSongClick: (index: Int) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(albumName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                SongListItem(song = song, onClick = { onSongClick(index) })
            }
        }
    }
}
```

**LibraryViewModel.kt** — add wrapper (1 line):
```kotlin
fun getSongsByAlbum(albumId: Long): Flow<List<Song>> = songRepository.getSongsByAlbum(albumId)
```

**AlbumGridItem.kt** — add `onClick` using `Card`'s built-in `onClick`:
```kotlin
@Composable
fun AlbumGridItem(
    album: Album,
    onClick: () -> Unit,          // ADD
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        onClick = onClick,          // Card supports onClick directly
    ) { /* unchanged */ }
}
```

**LibraryScreen.kt** — add `onAlbumClick` callback:
- `LibraryScreen` signature: add `onNavigateToAlbumDetail: (albumId: Long, albumName: String) -> Unit`
- `LibraryScreenContent`: add `onAlbumClick: (Album) -> Unit`
- `AlbumsTab`: accept `onAlbumClick`, pass to each `AlbumGridItem`
- `LibraryScreen` wires: `onAlbumClick = { album -> onNavigateToAlbumDetail(album.id, album.name) }`

**NavGraph.kt** — add route:
```kotlin
data object AlbumDetail : Screen("album_detail/{albumId}/{albumName}") {
    fun createRoute(albumId: Long, albumName: String) =
        "album_detail/$albumId/${Uri.encode(albumName)}"
}

// In NavHost:
composable(
    Screen.AlbumDetail.route,
    arguments = listOf(
        navArgument("albumId") { type = NavType.LongType },
        navArgument("albumName") { type = NavType.StringType },
    ),
) { backStackEntry ->
    AlbumDetailScreen(
        albumId = backStackEntry.arguments?.getLong("albumId") ?: 0L,
        albumName = backStackEntry.arguments?.getString("albumName") ?: "",
        onNavigateBack = { navController.navigateUp() },
        onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
    )
}
```

Wire in Library composable:
```kotlin
composable(Screen.Library.route) {
    LibraryScreen(
        onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
        onNavigateToAlbumDetail = { id, name ->
            navController.navigate(Screen.AlbumDetail.createRoute(id, name))
        },
        // ... artist callback added in Layer 4
    )
}
```

**Build & verify:** `assembleDebug`

---

### Layer 4: Artist detail screen + navigation

**Files:**
- `ui/library/ArtistDetailScreen.kt` — **NEW**
- `ui/library/ArtistListItem.kt` — add `onClick`
- `ui/library/LibraryScreen.kt` — add `onArtistClick` callback threading
- `ui/library/LibraryViewModel.kt` — add `getSongsByArtist()` wrapper
- `ui/navigation/NavGraph.kt` — add `ArtistDetail` route

**Same pattern as Layer 3.** Key differences:
- Route uses `artistName` (String) instead of `albumId` (Long)
- Songs sorted by album + track number (already handled by `SongDao.getSongsByArtist`)
- `ArtistListItem` uses `Modifier.clickable` (not `Card.onClick`)

**LibraryViewModel.kt** — add wrapper:
```kotlin
fun getSongsByArtist(artistName: String): Flow<List<Song>> = songRepository.getSongsByArtist(artistName)
```

**Build & verify:** `assembleDebug`

---

### Layer 5: Update tests

**Files:**
- `test/data/FakePlayerRepository.kt` — add `lastPlayedStartIndex: Int?` tracking
- `test/ui/library/PlayerViewModelTest.kt` — update `PlaySongs` test to verify `startIndex`
- `androidTest/ui/library/LibraryScreenTest.kt` — update `LibraryScreenContent` calls to include new callback params; add test for song click

**FakePlayerRepository** — track `startIndex`:
```kotlin
var lastPlayedStartIndex: Int? = null

override suspend fun playSongs(songs: List<Song>, startIndex: Int) {
    lastPlayedSongs = songs
    lastPlayedStartIndex = startIndex
}
```

**PlayerViewModelTest** — add test:
```kotlin
@Test
fun `PlaySongs passes startIndex to repository`() = runTest {
    viewModel.onEvent(PlayerEvent.PlaySongs(listOf(testSong), startIndex = 2))
    advanceUntilIdle()
    assertEquals(2, repository.lastPlayedStartIndex)
}
```

**LibraryScreenTest** — update existing tests to provide new required callbacks (`onSongClick`, `onAlbumClick`, `onArtistClick`), add song click test.

**Build & verify:** `test`

---

### Layer 6: Update `docs/PLAN.md`

Update the global project plan to reflect actual state:
- **Tech stack**: Kotlin 2.2.10, AGP 9.0.0, Media3 1.9.2
- **Architecture**: Update package structure (`ui/<feature>/` not `ui/screens/`)
- **Phase 1 (Library)**: ✅ Complete — note what was built
- **Phase 2 (Playback)**: ✅ Complete — note that shuffle, repeat, queue management, MiniPlayer were built here (moved up from Phase 6)
- **Library → Playback Navigation**: ✅ Complete — this work
- **Phase 3 (Playlists)**: Next up
- **Navigation**: Updated to reflect actual routes (Library, NowPlaying, AlbumDetail, ArtistDetail)

---

## Implementation Checklist

- [x] Add `startIndex` to `PlayerEvent.PlaySongs`
- [x] Pass `startIndex` through `PlayerViewModel` to repository
- [x] Add `onClick` to `SongListItem`
- [x] Wire song tap in Songs tab → play + navigate
- [x] Add `getSongsByAlbum()` to `LibraryViewModel`
- [x] Add `getSongsByArtist()` to `LibraryViewModel`
- [x] Create `AlbumDetailScreen` + `AlbumDetailScreenContent`
- [x] Add `onClick` to `AlbumGridItem`
- [x] Wire album tap → navigate to AlbumDetail
- [x] Add `AlbumDetail` route to `NavGraph`
- [x] Create `ArtistDetailScreen` + `ArtistDetailScreenContent`
- [x] Add `onClick` to `ArtistListItem`
- [x] Wire artist tap → navigate to ArtistDetail
- [x] Add `ArtistDetail` route to `NavGraph`
- [x] Add `onNavigateToAlbumDetail` + `onNavigateToArtistDetail` callbacks to `LibraryScreen`
- [ ] Update `FakePlayerRepository` with `startIndex` tracking
- [ ] Update `PlayerViewModelTest` with `startIndex` test
- [ ] Update `LibraryScreenTest` with new callback params + song click test
- [ ] Update `docs/PLAN.md` to reflect project state

## Discovered During Implementation

### 1. Missing `FOREGROUND_SERVICE` permission (crash on first playback)

**Symptom:** App crashed immediately on first song tap with:
```
SecurityException: requires android.permission.FOREGROUND_SERVICE
The service must be declared with a foregroundServiceType that includes mediaPlayback
```

**Fix:** `AndroidManifest.xml` already had `FOREGROUND_SERVICE_MEDIA_PLAYBACK` but was missing the required base permission:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```
`FOREGROUND_SERVICE_MEDIA_PLAYBACK` requires the base `FOREGROUND_SERVICE` permission to also be declared. This crash was always latent — it only surfaced once song taps actually triggered playback for the first time.

---

### 2. MiniPlayer disappearing on detail screens

**Symptom:** MiniPlayer visible on LibraryScreen but disappeared on AlbumDetail/ArtistDetail screens.

**Root cause:** MiniPlayer was inside the `Scaffold` of `LibraryScreen` only, so it unmounted on navigation.

**Fix:** Moved MiniPlayer to `NavGraph.kt`, wrapping the entire `NavHost` in a single `Scaffold`:
```kotlin
@Composable
fun NavGraph(navController: NavHostController, ...) {
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (playerState.currentSong != null) {
                MiniPlayer(
                    uiState = playerState,
                    onEvent = playerViewModel::onEvent,
                    onClick = { navController.navigate(Screen.NowPlaying.route) },  // NOT navigateUp()
                )
            }
        },
    ) { padding ->
        NavHost(..., modifier = modifier.padding(padding)) { ... }
    }
}
```

> **💡 Key detail:** `onClick` must use `navController.navigate(Screen.NowPlaying.route)`, not `navController.navigateUp()`. `navigateUp()` goes *back* in the stack — it would close the current screen, not open NowPlaying.

---

### 3. Remaining known issues (to debug)

- [ ] Shuffle, repeat, and next buttons not working
- [ ] Seek bar not working (no time displayed, not updating, click restarts song)
- [ ] Artist tap not navigating
- [ ] Queue not showing up

## Files Summary

| File | Status | Change |
|------|--------|--------|
| `ui/player/PlayerEvent.kt` | Modify | Add `startIndex: Int = 0` to `PlaySongs` |
| `ui/player/PlayerViewModel.kt` | Modify | Pass `event.startIndex` to `playSongs()` |
| `ui/library/SongListItem.kt` | Modify | Add `onClick: () -> Unit` param + `clickable` modifier |
| `ui/library/AlbumGridItem.kt` | Modify | Add `onClick: () -> Unit` param to `Card` |
| `ui/library/ArtistListItem.kt` | Modify | Add `onClick: () -> Unit` param + `clickable` modifier |
| `ui/library/LibraryScreen.kt` | Modify | Add 3 navigation callbacks, thread click handlers through tabs |
| `ui/library/LibraryViewModel.kt` | Modify | Add `getSongsByAlbum()` + `getSongsByArtist()` wrappers |
| `ui/library/AlbumDetailScreen.kt` | **NEW** | Album detail screen + testable content variant |
| `ui/library/ArtistDetailScreen.kt` | **NEW** | Artist detail screen + testable content variant |
| `ui/navigation/NavGraph.kt` | Modify | Add `AlbumDetail` + `ArtistDetail` routes, update `Screen` sealed class |
| `test/data/FakePlayerRepository.kt` | Modify | Track `lastPlayedStartIndex` |
| `test/ui/library/PlayerViewModelTest.kt` | Modify | Test `startIndex` passthrough |
| `androidTest/ui/library/LibraryScreenTest.kt` | Modify | Add new callbacks to existing tests, add song click test |
| `docs/PLAN.md` | Modify | Update to reflect completed phases |

## Dependencies

No new dependencies. All libraries needed are already in `gradle/libs.versions.toml`.

## Verification

- `assembleDebug` succeeds after each layer
- `test` passes all tests
- Manual: tap song in Songs tab → playback starts at that song, NowPlaying screen shows
- Manual: tap album → album detail shows songs sorted by track number → tap song → playback starts at that song
- Manual: tap artist → artist detail shows songs sorted by album + track → tap song → playback starts at that song
- Manual: MiniPlayer appears on library screen after starting playback from any entry point
- Manual: back navigation from detail screens returns to library
- Manual: MiniPlayer on detail screens navigates to NowPlaying when tapped