---
type: plan
feature: now-playing-sheet
status: implemented
tags:
  - type/plan
  - status/implemented
  - feature/now-playing-sheet
---

# Now Playing Sheet — Plan

## Context

The current expand/collapse for the Now Playing player is a discrete boolean: `isNowPlayingExpanded`
drives an `AnimatedVisibility` that slides the screen in from the bottom or out downward.
The gesture detector lives only on the album art `Box` — a small sub-region of the UI. Three
problems fall out of this:

1. **Swipe-down dismiss is architecturally broken.** `onNavigateBack()` fires *immediately* on
   drag-end, so the `NowPlayingExit` screen animation and the art's `SwipeExitFling` run in
   parallel with no coordination. The screen is already leaving before the art animation matters.

2. **There is no song-change transition.** Swiping left/right triggers the event and flies only
   the art off-screen while the title, artist, and seek bar snap to the new values instantly.

3. **The gesture area is too small.** Users naturally swipe down anywhere on the player to
   dismiss — not just on the album art.

Option B fixes all three at the root: replace the boolean + `AnimatedVisibility` with a single
continuous `progress: Float` (0 = collapsed, 1 = expanded) that drives both the panel position
and all visual transitions. The result is a physically connected swipe-to-dismiss (like a bottom
sheet) and a foundation for a coherent horizontal song-transition.

## Scope

**Included:**

- `PlayerSheetState` — new Compose state holder managing `Animatable<Float>` progress, drag
  tracking, velocity-based anchor snapping, and all derived layout/visual values.
- Sheet scaffold in `NavGraph` — replace `AnimatedVisibility` + `MiniPlayer` overlay with a
  single permanently-composed draggable panel that translates between the mini and expanded states.
- Nested scroll integration — swipe-to-dismiss only activates when the NowPlaying content scroll
  position is at the top, so the gesture does not fight the vertical scroll.
- Song-change transition — `AnimatedContent` keyed on `currentSong?.id` wrapping the song-specific
  content block (art + title + track info + rating) with a horizontal slide + fade so the whole
  content unit transitions, not just the art.
- Remove the now-redundant art-only gesture dispatcher. Horizontal swipe-to-skip moves to the
  full NowPlaying content area. Vertical sheet drag replaces swipe-down-to-dismiss.
- `MotionTokens` additions for sheet spring specs and song-transition specs.

**Excluded:**

- No MiniPlayer visual redesign. The mini-player row rendered inside the sheet at progress=0
  will match the existing MiniPlayer exactly in layout and styling.
- No queue swipe-up gesture replacement in this pass. The queue icon button remains; swipe-up
  as a gesture for the queue is a separate feature.
- No shared element transition between MiniPlayer art and expanded art (deferred).
- No waveform or lyrics panel (separate features).

## Approach

### Why this over Option A

Option A (fix art-only gesture coordination) patches the symptoms: delay `onNavigateBack()`,
add an `AnimatedContent` around the whole column. But the gesture area remains small, and the
two composable hierarchies (MiniPlayer + NowPlayingScreen) remain separate with no physical
connection. You'd end up with a dismiss gesture that *looks* continuous but is still two discrete
animations stitched together.

Option B builds the right primitive once. A continuous `progress: Float` is the correct
abstraction for a draggable bottom sheet: it naturally handles mid-gesture interruptions (finger
changes direction), velocity-based fling-to-anchor, and interpolating between collapsed and
expanded visuals. The song-transition is then a second, orthogonal `AnimatedContent` that works
correctly because the parent container is no longer entering/exiting.

### Core mechanic

```
progress = 0f  →  panel translateY = screenHeight - miniPeekPx  (only mini strip visible)
progress = 1f  →  panel translateY = 0f                         (full screen)
```

The panel is always in the composition. `draggable` (vertical, with `NestedScrollConnection`) on
the expanded content drives progress continuously. `VelocityTracker` + `splineBasedDecay` decides
which anchor to fling to on release.

Derived visual values, all computed from `progress`:

| Value               | At progress=0       | At progress=1     | Interpolation       |
|---------------------|---------------------|-------------------|---------------------|
| `translateY`        | `screenH - peek`    | `0f`              | linear              |
| `cornerRadius`      | `16.dp`             | `0.dp`            | `lerp`              |
| `miniContentAlpha`  | `1f`                | `0f`              | `coerceIn(0, 1)`    |
| `playerContentAlpha`| `0f`                | `1f`              | linear              |
| `blurScrimAlpha`    | `0f`                | `0.7f`            | linear              |
| `statusBarPadding`  | `0.dp`              | `statusBarHeight` | `lerp`              |

### Nested scroll for dismiss

`NowPlayingScreenContent` uses `Modifier.verticalScroll`. When fully expanded, a downward swipe
should scroll the content first; only when the content's scroll offset is at 0 should the swipe
begin dismissing the sheet. This is handled by a `NestedScrollConnection` attached above the
scroll:

```
onPreScroll: if scrolling UP → pass through (let content scroll)
onPreScroll: if scrolling DOWN and scroll offset == 0 → consume delta for sheet drag
onPostScroll: available remainder → pass to sheet drag if not consumed
```

### Horizontal skip gesture

Horizontal swipes are unambiguously lateral — they cannot conflict with the sheet's vertical
drag. Move `detectDragGestures` from the art `Box` to a `pointerInput` on the full `Column`
content area (but only active when `progress == 1f` to avoid spurious triggers while animating
open). Keep the 3D tilt feedback on the art as-is, but now it reflects the full-content drag
rather than just the art's local offset.

The song-change `AnimatedContent` wraps the content region (art + title + rating) and uses
`slideInHorizontally + fadeIn` / `slideOutHorizontally + fadeOut` keyed on `currentSong?.id`.
The seek bar and controls live *outside* the `AnimatedContent` so they update independently.

### State holder design

`PlayerSheetState` is a plain Kotlin class (not a `ViewModel`) created with `remember` in
`NavGraph`. It holds:

- `progress: Animatable<Float>` — the single source of truth
- `velocityTracker: VelocityTracker` — populated on each drag event
- `scope: CoroutineScope` — for launching animations

Public API:
```kotlin
fun onDragDelta(deltaY: Float, screenHeightPx: Float)  // called from pointerInput
suspend fun onDragEnd(velocityY: Float, screenHeightPx: Float)  // snap or fling
suspend fun expand()
suspend fun collapse()
val isExpanded: Boolean get() = progress.value > 0.5f
```

`onDragEnd` uses `splineBasedDecay` to project the fling endpoint, then snaps to nearest
anchor (0f or 1f) using the appropriate spring spec.

## Tasks

### Task 1 — MotionTokens additions (no new dependencies)

Add to `ui/theme/Motion.kt`:
- `SheetExpandSpring` — spring, `dampingRatio = 0.8f`, `stiffness = 380f` (responsive, slight
  overshoot suppressed)
- `SheetCollapseSpring` — spring, `dampingRatio = Spring.DampingRatioNoBouncy`, `stiffness = 450f`
  (snappy, no bounce going down)
- `SongSlideEnter` / `SongSlideExit` — `slideInHorizontally + fadeIn` / `slideOutHorizontally +
  fadeOut` at `DurationMedium2` with `EasingEmphasizedDecelerate`

Build gate: `assembleDebug`.

### Task 2 — `PlayerSheetState` (new file)

Create `ui/player/PlayerSheetState.kt`:
- `rememberPlayerSheetState(screenHeightPx: Float, miniPeekPx: Float): PlayerSheetState`
- All drag logic, velocity tracking, anchor snapping
- Derived property `translateY: Float` (the only value `NavGraph` needs for positioning)
- Derived properties `progress: Float`, `cornerRadius: Dp`, `miniAlpha: Float`, `playerAlpha: Float`
  consumed inside the sheet composable

Build gate: `assembleDebug`.

### Task 3 — Sheet scaffold in NavGraph

Replace in `NavGraph.kt`:
- Remove `isNowPlayingExpanded: Boolean`, `expandNowPlaying`, `collapseNowPlaying`
- Remove `AnimatedVisibility` for both MiniPlayer and NowPlayingScreen
- Remove the `isOnTopLevelScreen` guard on mini-player visibility — the sheet always peeks
  when `currentSong != null`, regardless of route
- NavHost bottom padding becomes a constant `miniPeekDp` when `currentSong != null` (not
  only on the home route)
- Screenshot capture:
  - Tap path: capture synchronously in the `onClick` before calling `sheetState.expand()`
    (same as today's `expandNowPlaying`)
  - Drag path: capture in `onDragStart` inside `detectDragGestures`, but only when
    `sheetState.progress.value == 0f` (first expand, not a re-drag while open)
- Add `PlayerSheetState` via `rememberPlayerSheetState` using `BoxWithConstraints` to get
  `screenHeightPx` and derive `miniPeekPx` from `MiniPlayerPeek` constant

The sheet `Box` layout:
```
Box(Modifier.fillMaxSize()) {
    // ─── behind: the NavHost content ───
    NavHost(..., modifier = Modifier.padding(bottom = if (isCollapsed) miniPeekDp else 0.dp))

    // ─── sheet panel ─── (always composed)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = sheetState.translateY }
            .clip(RoundedCornerShape(sheetState.cornerRadius))
            .pointerInput(...) { /* vertical drag → sheetState.onDragDelta / onDragEnd */ }
    ) {
        // Layer 0: blurred background (alpha driven by sheetState.playerAlpha)
        BlurredBackground(...)

        // Layer 1: palette tint

        // Layer 2: NowPlayingScreenContent (alpha = sheetState.playerAlpha)
        NowPlayingScreen(...)

        // Layer 3: MiniPlayer row at bottom of panel (alpha = sheetState.miniAlpha)
        // (reuse MiniPlayer composable, aligned to Alignment.BottomCenter)
        MiniPlayer(..., modifier = Modifier.align(Alignment.BottomCenter))
    }
}
```

Key constraint: the MiniPlayer and NowPlayingScreenContent must be *siblings* inside the sheet
`Box`, not nested. MiniPlayer is always at the visual bottom of the panel; NowPlayingScreenContent
fills the panel from top. At progress=0 the panel sits low enough that only the MiniPlayer strip
is on-screen.

`BackHandler(enabled = sheetState.isExpanded)` replaces the old `BackHandler`.

Build gate: `assembleDebug`. Manual check: tap MiniPlayer → sheet slides up; back → slides down.
Sheet bottom always visible (mini-player peek) on all routes.

### Task 4 — Status bar appearance animation

In `NavGraph.kt`, add a `LaunchedEffect(sheetState.isExpanded)` that calls
`WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !sheetState.isExpanded`.

- `isExpanded` is `progress.value > 0.5f` — a derived `Boolean` so the effect fires at
  the crossover point, not on every frame
- Requires getting the `Window` from `LocalActivity.current.window` and the `View` from
  `LocalView.current` — both already available in NavGraph
- The effect runs on both directions (expand past 50% → dark icons off; collapse past 50%
  → restore to `!darkTheme`)
- Restore to the theme's default on collapse: read `darkTheme` from `isSystemInDarkTheme()`
  at the call site

No new dependency — `WindowInsetsControllerCompat` is in `androidx.core:core` which is already
on the classpath via AndroidX.

Build gate: `assembleDebug`. Manual check: drag sheet up slowly, icons flip to light at 50%;
drag back down, icons flip back.

### Task 5 — Nested scroll connection in NowPlayingScreenContent

Add a `NestedScrollConnection` at the top of the `Column` (outermost content container) in
`NowPlayingScreenContent.kt`. The connection:
- tracks whether the inner `ScrollState.value == 0` (scroll position at top)
- on downward pre-scroll when at top: consumes the delta and calls `onSheetDrag(deltaY)`
  (a new lambda parameter added to the composable signature)
- all other cases: pass through to the inner scroll

This means `NowPlayingScreenContent` gains one new parameter:
```kotlin
onSheetDrag: (deltaY: Float) -> Unit,    // called when swipe-down should dismiss
onSheetDragEnd: (velocityY: Float) -> Unit,
```

`NowPlayingScreen` (the thin ViewModel wrapper) passes these through to the content composable.
`NavGraph` wires them to `sheetState.onDragDelta(...)` and `sheetState.onDragEnd(...)`.

Build gate: `assembleDebug`. Manual check: swipe down on scrolled content → scrolls normally;
swipe down from top → sheet collapses.

### Task 6 — Horizontal skip gesture (full-content area)

Remove the `pointerInput` from the art `Box` entirely. Add `detectHorizontalDragGestures` to a
new `pointerInput` modifier on the `Column` in `NowPlayingScreenContent` (the content container),
active only when `isFullyExpanded: Boolean` parameter is `true` (passed from NavGraph via
sheetState).

The album art 3D tilt effect is rebuilt as a local `artTiltOffset: Animatable<Offset>` driven by
the horizontal drag (same math as before). The song-change `AnimatedContent` replaces the art
`Box` and the `TrackInfo`/rating row — both change together on skip.

The art `Box` retains its `clip` and `graphicsLayer` for the 3D effect, but loses the
`pointerInput`. The tilt is now driven by `artTiltOffset`, which is updated from the full-content
horizontal drag handler.

`NowPlayingScreenContent` gains one more parameter:
```kotlin
isFullyExpanded: Boolean,
```

Build gate: `assembleDebug`. Manual check: swipe left on full player → next track, art + title
transition together; swipe right → previous track.

### Task 7 — Song-change AnimatedContent

Wrap this block in `NowPlayingScreenContent` inside `AnimatedContent(targetState = uiState.currentSong?.id)`:
- Album art `Box` (with tilt graphicsLayer)
- `TrackInfo(song = ...)`
- Star rating + favorite row

Everything *outside* the `AnimatedContent` (seek bar, controls, spacers, back/queue bar at top)
stays static on skip.

Use `MotionTokens.SongSlideEnter` / `SongSlideExit`, direction determined by whether the new
song index is higher (→ slide left) or lower (← slide right) than the previous. Requires tracking
`previousSongId` in the composable with `LaunchedEffect` or `derivedStateOf`.

Build gate: `assembleDebug`. Manual check: skip forward → art + title slide left, new song
slides in from right.

### Task 8 — Tests

Unit tests for `PlayerSheetState`:
- `expand()` animates progress to 1f
- `collapse()` animates progress to 0f
- `onDragDelta` clamps progress to [0, 1]
- `onDragEnd` with velocity above threshold snaps to expected anchor
- `onDragEnd` with velocity below threshold snaps to nearest anchor by position

`NowPlayingScreenContent` content test (preview-based, checking composable renders at both
`isFullyExpanded = true` and `false` without crash).

## Dependencies

No new dependencies. All animation primitives used (`Animatable`, `splineBasedDecay`,
`VelocityTracker`, `NestedScrollConnection`, `AnimatedContent`) are already in Compose.

## Design Decisions (resolved)

1. **Screenshot timing** — Capture on drag-start (`onDragStart` in `detectDragGestures`),
   accepting the possible 1-frame flicker. `ScreenshotHolder.capture(view)` is called only when
   `progress == 0f` at drag-start (i.e., first expand gesture, not mid-fling). Tap-to-expand
   still captures synchronously before animating (same as today).

2. **Peek on non-Home screens** — The sheet always peeks. Remove the `isOnTopLevelScreen` guard
   on the mini-player visibility. Any screen with a song loaded shows the mini strip. The
   NavHost bottom padding becomes a constant `miniPeekDp` whenever `currentSong != null`, not
   only on the home route.

3. **Status bar appearance** — Animate the status bar icons/color as progress increases.
   Use `WindowInsetsControllerCompat` to interpolate: at progress=0 the status bar matches the
   host screen (light or dark per system theme); at progress=1 it is forced dark-content
   (Now Playing has a dark blurred background regardless of theme). The crossover threshold is
   `progress > 0.5f` — flip appearance once to avoid per-frame `WindowInsetsController` calls.
   A `LaunchedEffect` on `(progress > 0.5f)` handles the flip.

## Verification

- `assembleDebug` succeeds after each task.
- `test` passes after Task 7.
- Manual — Expand: tap MiniPlayer → sheet slides up with corner radius morphing, blurred
  background fades in, mini content fades out, player content fades in.
- Manual — Drag dismiss: drag down from top of player content → sheet follows finger, release
  above 50% → snaps back; release below 50% or fling down → collapses to mini.
- Manual — Scroll then dismiss: scroll down in player content, then swipe down → content scrolls
  first; from scroll-top, swipe down → sheet collapses.
- Manual — Skip forward: swipe left anywhere on expanded player → art + title + rating slide left
  together, new song slides in from right, seek bar + controls update instantly.
- Manual — Skip back: swipe right → reverse direction.
- Manual — Back button: system back while expanded → sheet collapses, does NOT pop navigation.
- Manual — No ghost: after collapse, expanded player content is not visible.
