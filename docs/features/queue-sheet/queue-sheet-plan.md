---
type: plan
feature: queue-sheet
status: planned
tags:
  - type/plan
  - status/planned
  - feature/queue-sheet
---

# QueueSheet — Analysis & Improvement Plan

## Context

`QueueSheet` is the modal bottom sheet that displays the current playback queue on the NowPlaying screen. It supports viewing, reordering (drag-and-drop), removing, and clearing queue items. This document analyzes the current implementation, identifies bugs and gaps, and proposes fixes and improvements.

## Current Architecture

```
NowPlayingScreenContent
  └── QueueSheet (ModalBottomSheet)
        └── LazyColumn + ReorderableItem (sh.calvin.reorderable:3.0.0)
              └── SongItem (variant = Reorderable)
```

> **Note (2026-03-21):** `QueueItemRow` was removed and replaced with the shared `SongItem` component using `SongItemVariant.Reorderable`. The `formatTime` parameter was removed from `QueueSheet`/`QueueSheetContent` as `SongItem` doesn't display duration in its trailing area for the Reorderable variant. `SongItem` gained an `isDragging: Boolean` parameter and its `reorderableScope` type was changed from `ReorderableListItemScope` to `ReorderableCollectionItemScope`.

**Data flow:** `PlayerRepositoryImpl` → `PlayerUiState.queue` (StateFlow) → `PlayerViewModel` → `NowPlayingScreenContent` → `QueueSheet`

**Files involved:**
| File | Role |
|------|------|
| `ui/components/QueueSheet.kt` | UI component (~215 lines) |
| `model/QueueItem.kt` | Data class wrapping Song + index + id |
| `data/local/QueueDao.kt` | Room DAO for queue persistence |
| `data/PlayerRepositoryImpl.kt` | Queue operations + MediaController sync |
| `ui/player/PlayerEvent.kt` | Sealed events for queue actions |
| `ui/player/PlayerUiState.kt` | State holding queue list + currentQueueIndex |
| `ui/player/NowPlayingScreenContent.kt` | Host composable that shows/hides QueueSheet |

## Bugs

### Bug 1: `onRemove` callback is accepted but never used in the UI

`QueueSheet` accepts `onRemove: (String) -> Unit` as a parameter (line 54), and `NowPlayingScreenContent` wires it to `PlayerEvent.RemoveFromQueue`. However, `QueueItemRow` has **no remove button or swipe-to-dismiss** — the callback is dead code inside `QueueSheet`. Users have no way to remove individual songs from the queue.

**Severity:** High — core feature is silently missing.

### Bug 2: `onSongClick` triggers `PlayPause` unconditionally

In `NowPlayingScreenContent` (line 68-69):
```kotlin
onSongClick = { index ->
    onEvent(PlayerEvent.SeekToQueueItem(index))
    onEvent(PlayerEvent.PlayPause)  // ← toggles, doesn't guarantee play
}
```
If the player is already playing, `PlayPause` will **pause** it. The intent is to jump to a song and start playing it, but `PlayPause` is a toggle. If playback is active when the user taps a different queue item, it will seek then immediately pause.

**Severity:** Medium — intermittent bad UX depending on playback state.

### Bug 3: Reorder key mismatch potential

The `ReorderableItem` uses `key = item.song.id` (a `Long`), and the reorder callback extracts the key via `from.key.toString()`. This converts a `Long` to a `String` (e.g., `"12345"`). In `PlayerRepositoryImpl.reorderQueue`, the `queueItemId` is matched against `QueueEntity.id` which is set to `song.id.toString()` — so this works **only because** `QueueItem.id` defaults to `song.id.toString()` after the earlier fix (per PLAN.md line 137). However, the `QueueItem.id` field defaults to `UUID.randomUUID().toString()` in the model class, meaning if anyone constructs a `QueueItem` without overriding `id`, the reorder key won't match the DAO key.

**Severity:** Low — currently works because `syncQueueState()` never sets `QueueItem.id`, but fragile if the constructor default is ever used.

### Bug 4: No auto-scroll to the currently playing item

When the queue sheet opens, the `LazyColumn` starts at position 0 regardless of `currentIndex`. If the user is 50 songs into a playlist, they have to manually scroll to find the current track.

**Severity:** Medium — poor UX for large queues.

## Gaps

### Gap 1: Hardcoded strings

"Queue", "Clear queue", "Collapse queue", "Playing", "Reorder" are all hardcoded in the composable instead of referencing `strings.xml`. Violates the project quality checklist.

### Gap 2: No empty state

If `queue` is empty (e.g., after `ClearQueue`), the sheet shows a header with an empty list and no feedback to the user.

### Gap 3: No queue count or total duration

The header shows "Queue" but not how many songs are queued or total remaining time — information users commonly expect.

### Gap 4: No "Up Next" / "Previously Played" separation

All items are in a flat list. Most music players visually separate "now playing", "up next", and "history" sections within the queue view.

### Gap 5: Missing testable composable pattern

Per project convention, every `FooScreen()` should have a `FooScreenContent()` for testing. `QueueSheet` wraps `ModalBottomSheet` directly, making it hard to test the content in isolation.

### Gap 6: QueueSheet is only accessible from NowPlayingScreen

The MiniPlayer (visible on all home screens) has no queue access. Users must navigate to the full NowPlaying screen first.

## Where and When QueueSheet Should Be Used

### Current usage
- **Only** from `NowPlayingScreenContent` via the queue icon button (top-right corner).
- Triggered by local state: `var showQueue by remember { mutableStateOf(false) }`.

### Recommended usage points

| Location | Trigger | Priority |
|----------|---------|----------|
| **NowPlayingScreen** (current) | Queue icon button in top bar | Already done |
| **MiniPlayer long-press** | Long-press on the MiniPlayer | Medium — quick access without full-screen navigation |
| **Context menus** (song/album/playlist) | "Add to queue" confirmation → "View queue" action | Low — secondary entry point |

The MiniPlayer is the strongest candidate because it's always visible during playback on home screens.

## Scope

**Included (this plan):**
- Fix Bug 1: Add swipe-to-dismiss for individual item removal
- Fix Bug 2: Ensure `onSongClick` always starts playback (not toggles)
- Fix Bug 4: Auto-scroll to current item on open
- Fix Gap 1: Extract strings to `strings.xml`
- Fix Gap 2: Add empty state
- Fix Gap 3: Show queue count in header
- Fix Gap 5: Extract `QueueSheetContent` for testability

**Excluded (future work):**
- Bug 3: Model-level fix for `QueueItem.id` default (separate refactor)
- Gap 4: Up Next / History sections (significant UX redesign)
- Gap 6: MiniPlayer queue access (separate feature)

## Approach

### Why swipe-to-dismiss over a delete icon button
The row already has album art, text, duration, playing indicator, and drag handle — adding another icon button would be cramped. Swipe-to-dismiss is the standard Android pattern for queue removal and keeps the row clean. We'll use Material 3's `SwipeToDismissBox`.

### Why not split into "Up Next" sections now
The current queue model is a flat `List<QueueItem>` with a single `currentIndex`. Splitting into sections requires rethinking the data model and how `PlayerRepositoryImpl.syncQueueState()` builds the list. This is a larger effort better handled as its own feature.

## Tasks

### Task 1: Extract `QueueSheetContent` composable
- Move the `Column` content out of `ModalBottomSheet` into a standalone `QueueSheetContent` composable
- `QueueSheet` becomes a thin wrapper: `ModalBottomSheet { QueueSheetContent(...) }`
- This enables preview and test without the modal scaffold
- **Why:** Follows project testable composable pattern

### Task 2: Add swipe-to-dismiss for individual items ✅ Done
- Wrap each `QueueItemRow` in `SwipeToDismissBox` (Material 3)
- On dismiss, call `onRemove(item.song.id.toString())`
- Show a red background with trash icon during swipe
- **Why:** Fixes Bug 1 — `onRemove` is wired but has no UI trigger
- **Polish (2026-03-21):** Added `animateColorAsState` red background with delete icon, 80dp `positionalThreshold` to prevent accidental dismissals, and `HapticFeedbackType.LongPress` on confirmed removal

### Task 3: Fix `onSongClick` behavior
- If tapped index == `currentIndex`: call `onDismiss` (dismiss the sheet)
- If tapped index != `currentIndex`: send `SeekToQueueItem` only — verify that `seekToQueueItem` calls `controller.play()` after seeking so we don't need the `PlayPause` toggle
- Remove the unconditional `PlayPause` event that currently causes the toggle bug
- **Why:** Fixes Bug 2 + implements Decision 3

### Task 4: Auto-scroll to current item on open
- Add `LaunchedEffect(Unit)` that scrolls `lazyListState` to `currentIndex` when the sheet opens
- Use `animateScrollToItem` for a smooth experience
- **Why:** Fixes Bug 4 — users shouldn't have to hunt for the current track

### Task 5: Change `clearQueue` to preserve current song
- In `PlayerRepositoryImpl.clearQueue()`: instead of `controller.clearMediaItems()`, remove only items after `currentMediaItemIndex`
- In `QueueDao`: delete only queue entities with `position > currentPosition`
- Call `syncQueueState()` after to update the UI
- **Why:** Implements Decision 2 — "Clear queue" should not stop the current song

### Task 6: Add empty state
- When `queue.isEmpty()`, show a centered message ("Your queue is empty") instead of the LazyColumn
- **Why:** Fixes Gap 2

### Task 7: Add queue count to header
- Change header from "Queue" to "Queue (N songs)"
- **Why:** Fixes Gap 3

### Task 8: Extract hardcoded strings to `strings.xml`
- Move "Queue", "Clear queue", "Collapse queue", "Playing", "Reorder", empty state text to `strings.xml`
- **Why:** Fixes Gap 1, required by project quality checklist

### Task 9: Build verification
- Run `assembleDebug` after implementation
- Run `test` to verify no regressions

## Dependencies

No new dependencies required. `SwipeToDismissBox` is part of Material 3 (already in the project). `sh.calvin.reorderable:3.0.0` is already present.

## Decisions (resolved)

1. **Swipe-to-dismiss direction:** End-to-start only.
2. **Clear queue behavior:** Keep current song playing, only clear upcoming items. Requires changing `clearQueue()` to preserve the currently-playing item in both MediaController and Room.
3. **Tap currently-playing item:** Dismiss the sheet (call `onDismiss`).

## Verification

- `assembleDebug` succeeds after each task
- `test` passes all tests
- Manual:
  - Open NowPlaying → tap queue icon → sheet opens scrolled to current song
  - Swipe a non-playing item left → it is removed from the queue
  - Tap a different song → it starts playing (not pauses)
  - Tap currently-playing item → sheet dismisses
  - Clear queue → current song keeps playing, upcoming items removed
  - Drag to reorder → queue updates correctly
