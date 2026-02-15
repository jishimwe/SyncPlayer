# Post-Phase 2 Bug Fixes — Design

## Overview

Fixed 4 categories of bugs discovered after implementing library browsing, playback, and library→playback navigation: (1) shuffle/repeat/skip buttons not updating the UI, (2) seek bar frozen at 0:00, (3) artist click doing nothing, and (4) queue sheet never opening. A follow-up phase fixed 3 additional queue bugs (reorder, delete, play-from-queue) that surfaced after the queue sheet was wired. All bugs traced to missing wiring between existing implementations and the UI state layer.

## What was built

### Phase 1 — Core bug fixes

- `data/PlayerRepositoryImpl.kt`: Added `onShuffleModeEnabledChanged` and `onRepeatModeChanged` listener callbacks to `playerListener` so shuffle/repeat state propagates to `PlayerUiState`. Updated `onMediaItemTransition` to also set `currentQueueIndex`. Fixed position polling: renamed `startPositionUdates` → `startPositionUpdates`, added `positionUpdateJob: Job?` tracking, triggered start/stop from `onIsPlayingChanged`. Polls every 500ms and updates both `currentPosition` and `duration`. Added `duration` update in `onPlaybackStateChanged` (for the pre-play case when `STATE_READY` fires). Added optimistic position update in `seekTo()` to prevent slider snap-back. Added `syncQueueState()` helper that rebuilds `PlayerUiState.queue` from `mediaController`'s media items — called from `onMediaItemTransition`, `addToQueue`, `playNext`, `removeFromQueue`, `reorderQueue`. Populated queue state in `playSongs()` inline. Implemented `seekToQueueItem()` (was `TODO()`).

- `model/PlaybackState.kt`: Added `READY` enum value to match Media3's `STATE_READY`, used for pre-play duration setting.

- `model/QueueItem.kt`: Added `index: Int` field to track position in the queue (used by `QueueSheet` for click-to-play).

- `ui/player/PlayerViewModel.kt`: Removed dead `positionUpdateFlow` (was defined but never collected) and its unused imports.

- `ui/player/NowPlayingScreenContent.kt`: Fixed `ShuffleButton` — swapped `Icons.Default.Shuffle` and `Icons.Default.ShuffleOn` (were inverted). Added `RepeatOne` icon for repeat-one mode. Wired `QueueSheet`: added `showQueue` state, renders `QueueSheet` when true, changed `QueueButton(onClick = {})` to `QueueButton(onClick = { showQueue = true })`. Changed `SeekBar` to use `uiState.duration` instead of `uiState.currentSong?.duration` (duration now comes from the position polling loop).

- `ui/library/ArtistListItem.kt`: Added `Modifier.clickable(onClick = onArtistClick)` to `ListItem`'s modifier (was accepting the callback but not wiring it).

- `ui/library/LibraryScreen.kt`: Added `onNavigateToArtistDetail: (artistName: String) -> Unit` callback. Changed artist click routing from `onNavigateToAlbumDetail(artistName.hashCode().toLong(), artistName)` to `onNavigateToArtistDetail(artistName)`.

- `ui/navigation/NavGraph.kt`: Added `onNavigateToArtistDetail` callback to `LibraryScreen` call site. Added `composable(Screen.ArtistDetail.route)` entry with `navArgument("artistName")` → renders `ArtistDetailScreen`. Added MiniPlayer visibility logic: `isOnNowPlayingScreen` check hides MiniPlayer when on NowPlaying screen.

- `ui/player/components/QueueSheet.kt`: Added `onSongClick: (index: Int) -> Unit` parameter. Wired song click via `Modifier.clickable` on `QueueItemRow`. Changed `QueueItemRow` signature to accept `onSongClick` and `onRemove` with proper types.

### Phase 2 — Queue bug fixes

- `model/QueueItem.kt`: Removed random UUID default for `id`. `QueueItem` now requires `index: Int` and uses `song.id.toString()` implicitly through callers (the `id` field was removed entirely — callers use `song.id` directly for keying).

- `ui/player/components/QueueSheet.kt`: Changed `LazyColumn` key from `item.id` to `item.song.id` for consistency with `ReorderableItem` key. Fixed `from.key as String` → `from.key.toString()` to prevent `ClassCastException` (key is `Long`, not `String`).

- `data/PlayerRepositoryImpl.kt`: Fixed queue position initialization in `playSongs()` — changed `var index = startIndex` to `var index = 0` so DB positions start at 0 regardless of which song was tapped.

## Design decisions

- **Position polling at 500ms in repository, not ViewModel**: The plan considered two locations for position updates. The polling loop was kept in `PlayerRepositoryImpl` (not `PlayerViewModel`) because the repository owns `_playbackState` and has direct access to `mediaController.currentPosition`. This avoids exposing `mediaController` to the ViewModel layer. 500ms interval balances smooth slider movement against battery usage.

- **Dual duration update (polling + `onPlaybackStateChanged`)**: Duration is set in two places: the polling loop (while playing) and the `onPlaybackStateChanged(STATE_READY)` callback (before play starts). This wasn't in the original plan but was needed because `SeekBar` needs the duration before playback begins (e.g., when the user opens NowPlaying before pressing play). This was discovered during implementation.

- **`READY` added to `PlaybackState` enum**: The original enum didn't have a `READY` value, so `onPlaybackStateChanged(STATE_READY)` fell through to the `else` branch and was silently ignored. Adding `READY` ensures the state transition is explicit and the duration callback fires correctly.

- **`syncQueueState()` rebuilds from mediaController**: Rather than tracking queue mutations individually (which would require keeping `PlayerUiState.queue` in sync across `addToQueue`, `playNext`, `removeFromQueue`, `reorderQueue`), a single `syncQueueState()` helper rebuilds the entire queue list from `mediaController.getMediaItemAt(i)`. This is simpler and always consistent with the actual player state. Called after every queue mutation and on media item transitions.

- **`QueueItem.index` field added**: The original `QueueItem` only had `song` and `id`. An `index` field was added so `QueueSheet` can fire `SeekToQueueItem(item.index)` when the user taps a queue item. The index represents the item's position in the media controller's playlist.

- **Queue ID changed from random UUID to song-based**: `QueueItem.id` was `UUID.randomUUID().toString()`, which generated a new value on every instantiation. This meant IDs couldn't match against `MediaItem.mediaId` (which uses `song.id.toString()`) or `QueueEntity.id` in the DB. This single root cause broke reorder, delete, and play-from-queue. Fixed by using `song.id` as the key throughout.

- **MiniPlayer hidden on NowPlaying screen**: Not in the original plan. The MiniPlayer overlapped with the full NowPlaying screen, which was visually redundant. Added `isOnNowPlayingScreen` check in `NavGraph` to conditionally hide it.

- **`QueueSheet.onSongClick` fires `SeekToQueueItem` + `PlayPause`**: When tapping a queue item, the implementation fires both `SeekToQueueItem(index)` (to jump to that position in the playlist) and `PlayPause` (to start playback if paused). This is the simplest approach — `seekToDefaultPosition` doesn't auto-play.

## Known gaps

- **Tests not updated for Phase 2 changes**: `PlaybackState.READY` was added but no test verifies the `onPlaybackStateChanged` → `READY` path. `QueueItem` gained an `index` field but `FakePlayerRepository` doesn't verify queue state. `LibraryScreenTest` passes but doesn't cover artist click navigation.
- **Queue not restored on app restart**: `restoreQueue()` rebuilds media items from DB but doesn't update `PlayerUiState.queue`, so the queue sheet would be empty after a cold start until the user plays something.
- **`playSongs` queue position uses `PlayPause` workaround**: Tapping a queue item fires `PlayPause` after `SeekToQueueItem`, which toggles playback. If the player is already playing, this pauses it instead of continuing. Should use `play()` unconditionally.