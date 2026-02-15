# Library → Playback Navigation - Design

## Overview

Connected the library browsing UI to the playback system so users can start playing music by tapping songs, albums, or artists. Added album and artist detail screens with song lists, wired click handlers through the composable hierarchy, moved the MiniPlayer to `NavGraph` for global visibility, and fixed a missing `FOREGROUND_SERVICE` permission that prevented playback from starting.

## What was built

### Event layer

- `ui/player/PlayerEvent.kt`: Added `startIndex: Int = 0` to `PlaySongs` data class. Added `SeekToQueueItem(index: Int)` event for queue item selection.
- `ui/player/PlayerViewModel.kt`: Passes `event.startIndex` through to `playerRepository.playSongs()`. Routes `SeekToQueueItem` to `playerRepository.seekToQueueItem()`.

### Library screen updates

- `ui/library/SongListItem.kt`: Added `onClick: () -> Unit` parameter. Wraps `ListItem` with `Modifier.clickable`.
- `ui/library/AlbumGridItem.kt`: Added `onClick: () -> Unit = {}` parameter. Uses `Card`'s built-in `onClick` prop.
- `ui/library/ArtistListItem.kt`: Added `onArtistClick: () -> Unit = {}` parameter (note: not `onClick` — different naming convention than the other two).
- `ui/library/LibraryScreen.kt`: Added `onNavigateToAlbumDetail` callback to `LibraryScreen`. Added `onSongClick`, `onAlbumClick`, `onArtistClick` callbacks to `LibraryScreenContent`. Threaded callbacks through `SongsTab`, `AlbumsTab`, `ArtistsTab` to each list item. Removed `Scaffold` with `MiniPlayer` bottomBar (moved to `NavGraph`). Uses `itemsIndexed` instead of `items` in `SongsTab` for song index tracking.
- `ui/library/LibraryViewModel.kt`: Added `getSongsByAlbum(albumId: Long): Flow<List<Song>>` and `getSongsByArtist(artist: String): Flow<List<Song>>` wrappers delegating to `SongRepository`.

### New detail screens

- `ui/library/AlbumDetailScreen.kt`: Album detail screen showing songs sorted by track number. `AlbumDetailScreen` (ViewModel-connected) + `AlbumDetailScreenContent` (testable). Uses activity-scoped `LibraryViewModel` and `PlayerViewModel`. Collects `getSongsByAlbum()` flow via `collectAsStateWithLifecycle`. Song tap fires `PlaySongs(albumSongs, index)` then navigates to NowPlaying.
- `ui/library/ArtistDetailScreen.kt`: Artist detail screen showing songs sorted by album + track number. Same pattern as `AlbumDetailScreen`. Uses `getSongsByArtist()` flow.

### Navigation

- `ui/navigation/NavGraph.kt`: Added `Screen.AlbumDetail` (`album_detail/{albumId}/{albumName}`) and `Screen.ArtistDetail` (`artist_detail/{artistName}`) routes with `createRoute()` helper methods. Moved `MiniPlayer` from `LibraryScreen` to `NavGraph` by wrapping `NavHost` in a `Scaffold` with the MiniPlayer as `bottomBar`. This ensures the MiniPlayer is visible on all screens (Library, AlbumDetail, ArtistDetail) — not just Library.

### Repository and manifest

- `data/PlayerRepository.kt`: Added `seekToQueueItem(index: Int)` to the interface.
- `data/PlayerRepositoryImpl.kt`: Implemented `seekToQueueItem` using `mediaController?.seekToDefaultPosition(index)`.
- `AndroidManifest.xml`: Added missing `android.permission.FOREGROUND_SERVICE` base permission (was only declaring `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, which requires the base).

### Tests

- `test/data/FakePlayerRepository.kt`: Added `lastSeekToQueueItemIndex` tracking for `seekToQueueItem`. Records calls instead of `TODO()`.
- `androidTest/ui/library/LibraryScreenTest.kt`: Updated all 5 existing tests to pass new `onSongClick`, `onAlbumClick`, `onArtistClick` callbacks. Replaced `{} as (Type) -> Unit` casts with proper lambdas (`{ _, _ -> }`).

## Design decisions

- **MiniPlayer in NavGraph, not per-screen**: The plan placed MiniPlayer in `LibraryScreen`'s `Scaffold.bottomBar`. During implementation, this meant the MiniPlayer disappeared when navigating to AlbumDetail or ArtistDetail. Fixed by moving the MiniPlayer into `NavGraph.kt`, wrapping the entire `NavHost` in a single `Scaffold`. This is the correct approach — the MiniPlayer is a global concern, not a per-screen concern. The `onClick` uses `navController.navigate(Screen.NowPlaying.route)` (not `navigateUp()`), since the intent is to open the NowPlaying screen, not go back.

- **No dedicated ViewModels for detail screens**: `AlbumDetailScreen` and `ArtistDetailScreen` reuse the activity-scoped `LibraryViewModel` for `getSongsByAlbum`/`getSongsByArtist` data, and the activity-scoped `PlayerViewModel` for playback commands. This avoids creating 2 new ViewModels + 2 Hilt bindings + 2 test fakes for what amounts to a single `Flow` collection and one event dispatch each.

- **Artist navigation via album detail route (workaround)**: The `ArtistDetail` route was declared in `Screen` and `ArtistDetailScreen.kt` existed, but `NavGraph.kt` did not have a `composable(Screen.ArtistDetail.route)` entry. Instead, `LibraryScreen` wired artist clicks through `onNavigateToAlbumDetail` using `artistName.hashCode().toLong()` as a fake album ID. **Fixed in bugfixes-phase2**: `NavGraph.kt` now has the `ArtistDetail` composable route, `LibraryScreen` has a dedicated `onNavigateToArtistDetail` callback, and `ArtistListItem` has `Modifier.clickable` wired.

- **`onArtistClick` naming inconsistency**: `SongListItem` and `AlbumGridItem` use `onClick: () -> Unit`. `ArtistListItem` uses `onArtistClick: () -> Unit` with a default value. This naming inconsistency remains but the click is now functional — `Modifier.clickable(onClick = onArtistClick)` was added in bugfixes-phase2.

- **`FOREGROUND_SERVICE` permission fix**: The Phase 2 manifest had `FOREGROUND_SERVICE_MEDIA_PLAYBACK` but was missing the base `FOREGROUND_SERVICE` permission. This was always latent — it only surfaced when song taps actually triggered playback for the first time. Both permissions are now declared.

- **`SeekToQueueItem` added beyond plan scope**: The plan didn't include `seekToQueueItem`, but it was added to `PlayerEvent`, `PlayerRepository`, and `PlayerRepositoryImpl` during implementation to support queue item selection from the QueueSheet.

- **`AlbumGridItem.onClick` has default value**: `onClick: () -> Unit = {}` with a default empty lambda, unlike `SongListItem.onClick` which is required. This means album clicks silently do nothing if the caller forgets to pass `onClick`.

## Known gaps

- ~~**Artist navigation not wired in NavGraph**~~: Fixed in bugfixes-phase2. `NavGraph.kt` now has `composable(Screen.ArtistDetail.route)` with proper `navArgument("artistName")`.
- ~~**`ArtistListItem` click not attached to modifier**~~: Fixed in bugfixes-phase2. `Modifier.clickable(onClick = onArtistClick)` added.
- ~~**Shuffle, repeat, and next buttons not working**~~: Fixed in bugfixes-phase2. Added `onShuffleModeEnabledChanged` and `onRepeatModeChanged` listener callbacks to `PlayerRepositoryImpl`. Fixed swapped shuffle icons.
- ~~**Seek bar not working**~~: Fixed in bugfixes-phase2. Position polling now triggered from `onIsPlayingChanged`, duration set in both polling loop and `onPlaybackStateChanged(STATE_READY)`.
- ~~**Queue sheet not showing**~~: Fixed in bugfixes-phase2. `QueueButton` wired to show `QueueSheet`, queue state populated via `syncQueueState()` helper.
- ~~**`PlayerViewModelTest` not updated**~~: Fixed. Added `startIndex` passthrough test and `SeekToQueueItem` routing test.
- ~~**`FakePlayerRepository.playSongs` doesn't record `startIndex`**~~: Fixed. Added `lastPlayedStartIndex` tracking.
- **No new tests for detail screens**: `AlbumDetailScreenTest` and `ArtistDetailScreenTest` were planned but not created. Only existing `LibraryScreenTest` was updated with new callback parameters.
- **Duplicate intent-filter in manifest**: `PlaybackService` still declares the `androidx.media3.session.MediaSessionService` intent-filter twice, and `foregroundServiceType` has a redundant `mediaPlayback|mediaPlayback`.