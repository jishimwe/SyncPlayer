# Post-Phase 2 Bug Fixes — Plan

## Context

After implementing the library browsing, playback engine, and library→playback navigation features, manual testing revealed 4 categories of bugs:

1. **Shuffle, repeat, and next/previous buttons don't visually update** — the media player state changes but the UI never reflects it
2. **Seek bar is frozen** — position stays at 0:00, duration shows 0:00, and seeking restarts the song
3. **Artist click doesn't work** — tapping an artist row does nothing (3 separate issues in the chain)
4. **Queue sheet never opens** — the queue button has an empty onClick handler

All bugs trace to missing wiring — the underlying implementations exist but aren't connected.

## Root cause analysis

### Bug 1: Shuffle / Repeat / Next buttons

**What works:** `PlayerViewModel.onEvent()` correctly routes `ToggleShuffle`, `ToggleRepeat`, `SkipToNext`, `SkipToPrevious` to `PlayerRepositoryImpl`. The repository correctly calls `mediaController` methods. The buttons in `NowPlayingScreenContent` correctly fire the events.

**What's broken:**
- `PlayerRepositoryImpl.playerListener` doesn't implement `onShuffleModeEnabledChanged()` or `onRepeatModeChanged()`. So `_playbackState.isShuffleEnabled` stays `false` and `_playbackState.repeatMode` stays `OFF` forever — the UI never updates.
- `onMediaItemTransition` updates `currentSong` but not `currentQueueIndex`, so skip next/prev updates the song but the queue highlight doesn't follow.
- **Bonus bug:** `ShuffleButton` icons are swapped — `isEnabled=true` shows `Icons.Default.Shuffle` (off icon), `isEnabled=false` shows `Icons.Default.ShuffleOn` (on icon).

### Bug 2: Seek bar frozen

**What's broken:**
- `PlayerRepositoryImpl.startPositionUdates()` (typo in name) exists but is **never called** — not in `initialize()`, not in `playerListener`, nowhere. So `PlayerUiState.currentPosition` stays `0L`.
- The position update loop also has a structural problem: it only runs while `isPlaying == true`, but it needs to restart whenever playback resumes. It should be triggered from the `onIsPlayingChanged` listener callback.
- `PlayerUiState.duration` is never populated — no listener callback sets it. The `SeekBar` works around this by using `uiState.currentSong?.duration`, but position is still stuck at 0.

### Bug 3: Artist click doesn't work (3 failures in chain)

1. **`ArtistListItem.kt` line 20:** `onArtistClick` parameter is accepted but never wired to `Modifier.clickable` or `ListItem`'s modifier. Tapping the row doesn't trigger anything.
2. **`LibraryScreen.kt` lines 88-90:** `onArtistClick` navigates via `onNavigateToAlbumDetail(artistName.hashCode().toLong(), artistName)` — wrong destination. Should navigate to `ArtistDetail`, not `AlbumDetail`.
3. **`NavGraph.kt`:** `Screen.ArtistDetail` route is declared (line 37) but has no `composable(Screen.ArtistDetail.route)` entry in the `NavHost`. Even if navigation was attempted, it would crash.

### Bug 4: Queue sheet never opens

- `NowPlayingScreenContent.kt` line 56: `QueueButton(onClick = {})` — empty lambda.
- `NowPlayingScreenContent` doesn't have queue sheet state or render `QueueSheet`.
- `PlayerUiState.queue` is always `emptyList()` — when songs are loaded via `playSongs()`, the queue list in `PlayerUiState` is never populated with `QueueItem` objects.
- `PlayerRepositoryImpl.seekToQueueItem()` is `TODO()` — would crash if called from the queue sheet.

## Implementation plan

### Layer 1: Fix `playerListener` in `PlayerRepositoryImpl`

**File:** `data/PlayerRepositoryImpl.kt`

Add missing listener callbacks to `playerListener`:

```kotlin
override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
    _playbackState.update {
        it.copy(isShuffleEnabled = shuffleModeEnabled)
    }
}

override fun onRepeatModeChanged(repeatMode: Int) {
    _playbackState.update {
        it.copy(
            repeatMode = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            },
        )
    }
}
```

Update `onMediaItemTransition` to also set `currentQueueIndex`:

```kotlin
override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
    _playbackState.update {
        it.copy(
            currentSong = mediaItem?.toSong(),
            currentQueueIndex = mediaController?.currentMediaItemIndex ?: -1,
        )
    }
}
```

**Build & verify:** `assembleDebug`

---

### Layer 2: Fix position polling

**File:** `data/PlayerRepositoryImpl.kt`

**Problem:** `startPositionUdates()` exists but is never called, and only runs while `isPlaying`.

**Fix:** Rename (fix typo), add a `Job` field to track the coroutine, and trigger it from `onIsPlayingChanged`:

```kotlin
private var positionUpdateJob: Job? = null

private fun startPositionUpdates() {
    positionUpdateJob?.cancel()
    positionUpdateJob = repositoryScope.launch {
        while (isActive) {
            mediaController?.let { controller ->
                _playbackState.update {
                    it.copy(
                        currentPosition = controller.currentPosition,
                        duration = controller.duration.coerceAtLeast(0L),
                    )
                }
            }
            delay(500)
        }
    }
}

private fun stopPositionUpdates() {
    positionUpdateJob?.cancel()
    positionUpdateJob = null
}
```

Update `onIsPlayingChanged` in `playerListener`:

```kotlin
override fun onIsPlayingChanged(isPlaying: Boolean) {
    _playbackState.update {
        it.copy(
            playbackState = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED,
        )
    }
    if (isPlaying) {
        startPositionUpdates()
    } else {
        stopPositionUpdates()
    }
}
```

Also update `seekTo()` to immediately reflect the new position in the UI (prevents the slider from snapping back before the next poll):

```kotlin
override fun seekTo(positionMs: Long) {
    mediaController?.seekTo(positionMs)
    _playbackState.update { it.copy(currentPosition = positionMs) }
}
```

**Clean up:** Remove unused `positionUpdateFlow` from `PlayerViewModel.kt` (lines 34-40) and its unused imports (`currentCoroutineContext`, `delay`, `flow`, `isActive`).

**Build & verify:** `assembleDebug`

---

### Layer 3: Fix artist click (3 fixes)

#### 3a. Wire click to ArtistListItem

**File:** `ui/library/ArtistListItem.kt`

Add `Modifier.clickable` to the `ListItem`:

```kotlin
ListItem(
    modifier = modifier.clickable(onClick = onArtistClick),
    // ... rest unchanged
)
```

Add import: `import androidx.compose.foundation.clickable`

#### 3b. Fix LibraryScreen artist navigation

**File:** `ui/library/LibraryScreen.kt`

`LibraryScreen` needs an `onNavigateToArtistDetail` callback instead of routing through `onNavigateToAlbumDetail`.

Change `LibraryScreen` signature to add `onNavigateToArtistDetail`:

```kotlin
@Composable
fun LibraryScreen(
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbumDetail: (albumId: Long, albumName: String) -> Unit,
    onNavigateToArtistDetail: (artistName: String) -> Unit,  // ADD
    // ...
)
```

Fix the `onArtistClick` wiring:

```kotlin
// Change from:
onArtistClick = { artistName ->
    onNavigateToAlbumDetail(artistName.hashCode().toLong(), artistName)
},
// To:
onArtistClick = { artistName ->
    onNavigateToArtistDetail(artistName)
},
```

#### 3c. Wire ArtistDetail route in NavGraph

**File:** `ui/navigation/NavGraph.kt`

Add `onNavigateToArtistDetail` to `LibraryScreen` call, and add the `composable` entry for `ArtistDetail`:

```kotlin
composable(Screen.Library.route) {
    LibraryScreen(
        onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
        onNavigateToAlbumDetail = { id, name ->
            navController.navigate(Screen.AlbumDetail.createRoute(id, name))
        },
        onNavigateToArtistDetail = { name ->
            navController.navigate(Screen.ArtistDetail.createRoute(name))
        },
        modifier = modifier,
    )
}

// Add after AlbumDetail composable:
composable(
    Screen.ArtistDetail.route,
    arguments = listOf(
        navArgument("artistName") { type = NavType.StringType },
    ),
) { backStackEntry ->
    ArtistDetailScreen(
        artistName = backStackEntry.arguments?.getString("artistName") ?: "",
        onNavigateBack = { navController.navigateUp() },
        onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
    )
}
```

Add import: `import com.jpishimwe.syncplayer.ui.library.ArtistDetailScreen`

**Build & verify:** `assembleDebug`

---

### Layer 4: Wire queue sheet

#### 4a. Populate queue in PlayerUiState

**File:** `data/PlayerRepositoryImpl.kt`

After `playSongs()` sets up the media items, populate the queue in UI state:

```kotlin
override suspend fun playSongs(songs: List<Song>, startIndex: Int) {
    mediaController?.clearMediaItems()
    mediaController?.addMediaItems(songs.map { it.toMediaItem() })
    mediaController?.prepare()
    mediaController?.seekToDefaultPosition(startIndex)
    mediaController?.play()

    // Update UI queue state
    _playbackState.update {
        it.copy(
            queue = songs.map { song -> QueueItem(song) },
            currentQueueIndex = startIndex,
        )
    }

    // Persist to DB
    var index = startIndex
    queueDao.clearQueue()
    queueDao.insertList(songs.map { QueueEntity(it.id.toString(), it.id, index++) })
}
```

Similarly update `addToQueue`, `playNext`, `removeFromQueue`, `reorderQueue` to keep `_playbackState.queue` in sync. The simplest approach: add a helper that rebuilds the queue from the mediaController:

```kotlin
private fun syncQueueState() {
    val controller = mediaController ?: return
    val items = (0 until controller.mediaItemCount).map { i ->
        QueueItem(controller.getMediaItemAt(i).toSong())
    }
    _playbackState.update {
        it.copy(
            queue = items,
            currentQueueIndex = controller.currentMediaItemIndex,
        )
    }
}
```

Call `syncQueueState()` at the end of `addToQueue`, `playNext`, `removeFromQueue`, `reorderQueue`, and in `onMediaItemTransition`.

#### 4b. Implement `seekToQueueItem`

**File:** `data/PlayerRepositoryImpl.kt`

Replace the `TODO()`:

```kotlin
override fun seekToQueueItem(index: Int) {
    mediaController?.seekToDefaultPosition(index)
}
```

#### 4c. Show QueueSheet from NowPlayingScreenContent

**File:** `ui/player/NowPlayingScreenContent.kt`

Add queue sheet state and wire the button:

```kotlin
@Composable
fun NowPlayingScreenContent(
    uiState: PlayerUiState,
    onEvent: (PlayerEvent) -> Unit,
    onNavigateBack: () -> Unit,
    formatTime: (Long) -> String,
) {
    var showQueue by remember { mutableStateOf(false) }

    if (showQueue) {
        QueueSheet(
            queue = uiState.queue,
            currentIndex = uiState.currentQueueIndex,
            onDismiss = { showQueue = false },
            onRemove = { onEvent(PlayerEvent.RemoveFromQueue(it)) },
            onReorder = { id, pos -> onEvent(PlayerEvent.ReorderQueue(id, pos)) },
            formatTime = formatTime,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = { BackButon(onClick = onNavigateBack) },
                actions = { QueueButton(onClick = { showQueue = true }) },
            )
        },
    ) { // ... rest unchanged }
}
```

Add imports: `QueueSheet`, `mutableStateOf`, `remember`, `setValue`, `getValue`.

**Build & verify:** `assembleDebug`

---

### Layer 5: Fix ShuffleButton icon swap

**File:** `ui/player/NowPlayingScreenContent.kt`

Swap the icons (lines 138-141):

```kotlin
// Change from:
if (isEnabled) {
    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle on")
} else {
    Icon(Icons.Default.ShuffleOn, contentDescription = "Shuffle off")
}

// To:
if (isEnabled) {
    Icon(Icons.Default.ShuffleOn, contentDescription = "Shuffle on")
} else {
    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle off")
}
```

**Build & verify:** `assembleDebug`

---

### Layer 6: Fix manifest duplicates

**File:** `app/src/main/AndroidManifest.xml`

Remove the duplicate intent-filter and fix the redundant `foregroundServiceType`:

```xml
<!-- Change from: -->
android:foregroundServiceType="mediaPlayback|mediaPlayback"
<!-- To: -->
android:foregroundServiceType="mediaPlayback"

<!-- Remove duplicate intent-filter block (keep only one): -->
<intent-filter>
    <action android:name="androidx.media3.session.MediaSessionService" />
    <action android:name="android.media.browse.MediaBrowserService" />
</intent-filter>
```

**Build & verify:** `assembleDebug`

---

### Layer 7: Update tests

#### 7a. FakePlayerRepository

**File:** `test/data/FakePlayerRepository.kt`

No changes needed — already tracks all calls. `seekToQueueItem` already works (line 86-88).

#### 7b. Update LibraryScreenTest

**File:** `androidTest/ui/library/LibraryScreenTest.kt`

If `LibraryScreenContent` signature changed (it didn't — `onArtistClick` was already there), update test calls. No changes needed here since the `LibraryScreenContent` signature stays the same.

#### 7c. PlayerViewModelTest (optional)

The dead `positionUpdateFlow` is removed from `PlayerViewModel`. No test currently covers it, so no test changes needed for that removal.

**Verify:** `gradlew.bat test`

---

## Files modified (summary)

| File | Change |
|------|--------|
| `data/PlayerRepositoryImpl.kt` | Add listener callbacks for shuffle/repeat/queue index; fix position polling; populate queue state; implement `seekToQueueItem`; add `syncQueueState` helper |
| `ui/player/PlayerViewModel.kt` | Remove dead `positionUpdateFlow` and unused imports |
| `ui/player/NowPlayingScreenContent.kt` | Fix ShuffleButton icons; wire QueueSheet with show/hide state; change queue button onClick |
| `ui/library/ArtistListItem.kt` | Add `Modifier.clickable` to ListItem |
| `ui/library/LibraryScreen.kt` | Add `onNavigateToArtistDetail` callback; fix artist click routing |
| `ui/navigation/NavGraph.kt` | Add `onNavigateToArtistDetail` to LibraryScreen; add `ArtistDetail` composable route |
| `AndroidManifest.xml` | Remove duplicate intent-filter; fix redundant `foregroundServiceType` |

## Verification

- `gradlew.bat assembleDebug` after each layer
- `gradlew.bat test` after all layers
- Manual: tap shuffle → icon toggles between Shuffle/ShuffleOn
- Manual: tap repeat → cycles OFF → ALL → ONE → OFF with correct icons
- Manual: skip next/prev → song changes, seek bar resets to new position
- Manual: seek bar moves during playback, shows correct time
- Manual: drag seek bar → playback jumps to new position
- Manual: tap artist → artist detail screen shows songs → tap song → playback starts
- Manual: tap queue button → QueueSheet opens showing current queue
- Manual: reorder/remove items in queue → queue updates

## Implementation checklist

- [x] Layer 1: Add shuffle/repeat/queue index listener callbacks to `playerListener`
- [x] Layer 2: Fix position polling (rename, trigger from listener, update seekTo)
  - [x] Call `startPositionUpdates()` from `onIsPlayingChanged`
  - [x] Populate `duration` in polling loop
  - [x] Populate `duration` in `onPlaybackStateChanged` (pre-play case — not in original plan)
  - [x] Fix `SeekBar` call site to use `uiState.duration` instead of `uiState.currentSong?.duration`
  - [x] Optimistic position update in `seekTo`
- [x] Layer 3a: Wire `Modifier.clickable` to `ArtistListItem`
- [x] Layer 3b: Add `onNavigateToArtistDetail` to `LibraryScreen`
- [x] Layer 3c: Add `ArtistDetail` composable route to `NavGraph`
- [x] Layer 4a: Populate queue in `PlayerUiState` + `syncQueueState` helper
  - [x] Fix `syncQueueState` to use `mapNotNull` correctly (original plan had `?: null` bug)
- [x] Layer 4b: Implement `seekToQueueItem`
- [x] Layer 4c: Wire `QueueSheet` in `NowPlayingScreenContent`
- [x] Layer 5: Fix `ShuffleButton` icon swap
- [x] Layer 6: Fix manifest duplicates
- [x] Bonus: Hide MiniPlayer when on NowPlayingScreen (not in original plan)
- [ ] Layer 7: Run tests, verify no regressions

---

## Phase 3 — Queue Bug Fixes

### Context

After wiring the queue sheet, manual testing revealed 3 remaining queue bugs:

1. **Reorder not working** — dragging queue items has no effect
2. **Delete not working** — removing a queue item has no effect
3. **Can't play a song from the queue** — tapping a queue item doesn't start playback

All three bugs shared a single root cause: `QueueItem.id` was a randomly generated UUID, making it impossible to match queue items against `MediaItem.mediaId` (which is `song.id.toString()`) or `QueueEntity.id` in the DB.

### Root cause analysis

**Root cause (all 3 bugs):** `QueueItem.id` was defined as `UUID.randomUUID().toString()` — a new random value generated every time a `QueueItem` is instantiated. This made it impossible to correlate a `QueueItem` with its corresponding `MediaItem` in the media controller (which uses `song.id.toString()` as `mediaId`) or with its `QueueEntity` in the DB.

**Secondary bug (reorder):** `itemsIndexed` used `item.id` as the key, but `ReorderableItem` used `item.song.id.toString()` as its key. The reorderable library reads keys from the `LazyColumn`, so `from.key` in the `rememberReorderableLazyListState` callback was `item.id`, not `item.song.id.toString()` — causing a key mismatch that silently dropped reorder events.

**Additional bug (reorder):** `ReorderableItem` key was cast to `String` via `from.key as String`, but the `LazyColumn` key was `item.song.id` (a `Long`) — causing a runtime `ClassCastException`.

**Additional bug (playSongs):** Queue positions in the DB were initialized starting at `startIndex` instead of `0`, causing an off-by-one mismatch when `removeFromQueue` used the DB position as a media controller index.

### Layer 8: Fix queue item ID and key consistency

**Files:** `model/QueueItem.kt`, `ui/player/components/QueueSheet.kt`

**Fix 1 — Use stable ID in `QueueItem`:**

```kotlin
// Change from:
data class QueueItem(
    val song: Song,
    val id: String = UUID.randomUUID().toString(),
)

// To:
data class QueueItem(
    val song: Song,
    val id: String = song.id.toString(),
)
```

**Fix 2 — Make `LazyColumn` and `ReorderableItem` keys consistent in `QueueSheet`:**

```kotlin
itemsIndexed(
    items = queue,
    key = { _, item -> item.id },  // was item.id (UUID), now song.id.toString()
) { index, item ->
    ReorderableItem(reorderableLazyListState, key = item.id) {  // match LazyColumn key
        ...
    }
}
```

**Fix 3 — Fix `Long` → `String` cast in reorder callback:**

```kotlin
// Change from:
onReorder(from.key as String, to.index)

// To:
onReorder(from.key.toString(), to.index)
```

**Fix 4 — Fix queue position initialization in `playSongs`:**

```kotlin
// Change from:
var index = startIndex

// To:
var index = 0
```

### Layer 9 & 10: Delete and play from queue

Both were fixed by the same root cause fix (Layer 8). Once `QueueItem.id == song.id.toString()`, the lookups in `removeFromQueue` and `seekToQueueItem` correctly match against `MediaItem.mediaId` and `QueueEntity.id`.

## Phase 3 checklist

- [x] Layer 8: Fix `QueueItem.id` to use `song.id.toString()` instead of random UUID
- [x] Fix `LazyColumn` and `ReorderableItem` key mismatch in `QueueSheet`
- [x] Fix `Long` → `String` cast crash in reorder callback
- [x] Fix queue position off-by-one in `playSongs`
- [x] Layer 9: Delete working (fixed by Layer 8)
- [x] Layer 10: Play from queue working (fixed by Layer 8)
- [ ] Run tests, verify no regressions