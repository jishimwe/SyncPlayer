---
type: plan
feature: animation-improvements
status: implemented
tags:
  - type/plan
  - status/implemented
  - feature/animation-improvements
---

# Animation Improvements Plan

## Context

Following the animation system overhaul (centralized [[Motion]] tokens in `ui/theme/Motion.kt`, extracted `rememberContentFadeIn()` helper), a detailed audit identified actionable issues ranging from a timing desync to missing motion polish. This plan tracks the fixes in priority order.

## Audit Corrections

An earlier draft of this plan contained several factual errors that were caught during review. They are documented here so the same mistakes are not repeated:

1. **Default spring IS NOT bouncy.** Both `animateFloatAsState` and `animateColorAsState` default to `spring(dampingRatio = Spring.DampingRatioNoBouncy)` — damping ratio of 1.0 (critically damped, no overshoot). The earlier draft incorrectly claimed these defaults were `DampingRatioMediumBouncy` (0.5). The chevron rotation (L1-old) and queue swipe color (M4-old) do NOT bounce. These issues have been removed.
2. **`CollapsibleSectionHeader` has no content lambda.** The commented-out `AnimatedVisibility` in `CollapsibleSection.kt` is dead code from before the component was refactored into a header-only component. Expand/collapse in `HistoryTabScreen` is handled by the `LazyColumn` data layer (preview count vs full list), and items already use `Modifier.animateItem()`. The earlier draft's proposal to add a content lambda and wrap it in `AnimatedVisibility` would conflict with the `LazyColumn` architecture. Call sites are `HistoryTabScreen.kt` (3 usages), NOT `AlbumDetailScreen`/`ArtistDetailScreen`.
3. **Tab font-size animation is intentional layout, not "thrash."** Animating `fontSize.sp` does cause text re-layout, but that IS the intended behavior — the tab must physically change size so that tab widths and spacing remain correct. A `graphicsLayer { scaleX; scaleY }` fix would break tab spacing because `graphicsLayer` doesn't affect layout bounds. Severity has been downgraded.

## Issue Registry

### Medium

| # | Issue | File | Location |
|---|-------|------|----------|
| M1 | Now Playing fade (300ms) finishes 100ms before slide (400ms) — visible desync on exit | `Motion.kt` | `NowPlayingEnter/Exit` tokens |
| M2 | Album art swipe exit uses `tween(180)` — ignores gesture velocity at drag end | `NowPlayingScreenContent.kt` | lines 241–254 |
| M3 | MiniPlayer entrance is pure fade — no positional motion from bottom edge | `Motion.kt` | `MiniPlayerEnter` token |

### Low / Polish

| # | Issue | File | Location |
|---|-------|------|----------|
| L1 | `BackgroundColorCrossFade` uses `FastOutSlowInEasing` — spatial easing on ambient color | `Motion.kt` | `BackgroundColorCrossFade` token |
| L2 | Artist shared element missing `boundsTransform` — inconsistent vs. album transition | `ArtistDetailScreen.kt` | lines 156–159 |
| L3 | Tab label color snaps instantly while font size animates — desynchronized | `TopBarComponents.kt` | lines 200–206 |
| L4 | `NavHost` uses default transitions — no enter/exit configured for screen navigation | `NavGraph.kt` | line 96 |

### Not Issues (false positives from earlier draft)

| # | Original claim | Why it's wrong |
|---|----------------|---------------|
| ~~C1~~ | Tab font-size causes layout thrash | Layout change is intentional; `graphicsLayer` scale fix would break tab spacing |
| ~~C2~~ | `AnimatedVisibility` content wrapper commented out | Dead code; expand/collapse handled by LazyColumn data + `.animateItem()` |
| ~~M4~~ | Queue swipe color uses bouncy spring | `animateColorAsState` defaults to `DampingRatioNoBouncy` (1.0) — no bounce |
| ~~L1-old~~ | Chevron rotation uses bouncy spring | `animateFloatAsState` defaults to `DampingRatioNoBouncy` (1.0) — no bounce |

---

## Approach

Fix medium issues first (visible UX defects), then low/polish items. No architecture changes needed.

---

## Tasks

### M1 — Fix Now Playing enter/exit timing desync

**Problem:** `NowPlayingEnter` combines `slideInVertically(400ms)` + `fadeIn(300ms)`. On enter, the fade completes 100ms before the slide — the screen is fully opaque while still moving. On exit, `slideOutVertically(400ms)` + `fadeOut(200ms)`: the screen becomes invisible at 200ms but the slide continues to 400ms, meaning an alpha-0 element occupies space for 200ms extra.

**Fix:** Add dedicated fade specs that match the slide duration:

```kotlin
// Motion.kt — add:
val NowPlayingFadeIn: FiniteAnimationSpec<Float> =
    tween(DurationMedium4, easing = EasingStandard)          // 400ms, matches slide

val NowPlayingFadeOut: FiniteAnimationSpec<Float> =
    tween(DurationMedium4, easing = EasingStandardAccelerate) // 400ms, matches slide

// Update the transition combinations:
val NowPlayingEnter: EnterTransition =
    slideInVertically(animationSpec = OverlaySlide) { it } +
        fadeIn(animationSpec = NowPlayingFadeIn)

val NowPlayingExit: ExitTransition =
    slideOutVertically(animationSpec = OverlaySlide) { it } +
        fadeOut(animationSpec = NowPlayingFadeOut)
```

**Build checkpoint:** `assembleDebug`

---

### M2 — Replace swipe exit tween with spring

**Problem:** `SwipeExitFling = tween(180)` ignores the user's drag velocity. A hard flick and a barely-triggered swipe exit at the same 180ms rate. Using `tween` for a gesture-driven animation is also interruption-unfriendly: if the user taps during the exit tween, the animation doesn't respond naturally.

**Fix (simple — no velocity tracking):** Replace the tween with a spring. A spring naturally handles interruption and its stiffness provides a consistent "feel" regardless of velocity. The trade-off is we lose the tight 180ms duration guarantee — the spring settles when physics says it settles.

```kotlin
// Motion.kt — change:
val SwipeExitFling: AnimationSpec<Offset> =
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
```

`StiffnessHigh` (10000f) gives a fast exit (~100-200ms depending on distance), and `DampingRatioNoBouncy` prevents the art from oscillating at the exit position.

If velocity handoff is desired later, it requires replacing `detectDragGestures` with manual `awaitPointerEventScope` + `VelocityTracker`, which is a larger refactor better done as a separate task.

**Build checkpoint:** `assembleDebug` + manual test of swipe

---

### M3 — Add positional motion to MiniPlayer entrance

**Problem:** MiniPlayer fades in/out only. A bottom-anchored bar should arrive from the bottom edge, not materialize from nothing.

**Fix:** Update `MiniPlayerEnter`/`MiniPlayerExit` in `Motion.kt`:

```kotlin
val MiniPlayerEnter: EnterTransition =
    slideInVertically(
        animationSpec = tween(DurationMedium2, easing = EasingEmphasizedDecelerate),
    ) { it / 2 } + fadeIn(animationSpec = FadeInSpec)

val MiniPlayerExit: ExitTransition =
    slideOutVertically(
        animationSpec = tween(DurationShort4, easing = EasingStandardAccelerate),
    ) { it / 3 } + fadeOut(animationSpec = FadeOutSpec)
```

`{ it / 2 }` = slides from 50% below final position. Subtle but clearly spatial.

**Build checkpoint:** `assembleDebug`

---

### L1 — Fix `BackgroundColorCrossFade` easing

**Problem:** `tween(600)` defaults to `FastOutSlowInEasing`. This easing was designed for spatial motion (decelerate toward destination). For an ambient background color crossfade there is no spatial destination — `LinearEasing` is more perceptually neutral.

**Fix:**
```kotlin
// Motion.kt — change:
val BackgroundColorCrossFade: AnimationSpec<Color> =
    tween(DurationLong4, easing = LinearEasing)
```

---

### L2 — Add `boundsTransform` to ArtistDetailScreen shared element

**Problem:** `AlbumDetailScreen` specifies `boundsTransform = MotionTokens.SharedElementBoundsSpec` (300ms tween) but `ArtistDetailScreen` does not, falling through to the Compose default (a spring). Album and artist transitions have different motion characters.

**Fix:**
```kotlin
// ArtistDetailScreen.kt — add boundsTransform:
mod.sharedElement(
    rememberSharedContentState(key = "artist_art_$artistName"),
    animatedVisibilityScope = animatedVisibilityScope,
    boundsTransform = { _, _ -> MotionTokens.SharedElementBoundsSpec },
)
```

---

### L3 — Animate tab label color

**Problem:** Tab label color flips instantly between `accentColor` and `onSurface` while font size animates over 200ms. The two visual properties are desynchronized.

**Fix:** Add `animateColorAsState`:

```kotlin
// TopBarComponents.kt — add alongside existing animateFloatAsState:
val labelColor by animateColorAsState(
    targetValue = if (selectedTab == tab) {
        LocalExtendedColorScheme.current.accentColor.color
    } else {
        MaterialTheme.colorScheme.onSurface
    },
    animationSpec = MotionTokens.TabSizeTransition,
    label = "tabColor",
)
// Then use: color = labelColor
```

---

### L4 — Configure NavHost transitions

**Problem:** `NavHost` at `NavGraph.kt:96` uses all default transitions. Screen-to-screen navigation (Home → AlbumDetail, Home → ArtistDetail) has no configured enter/exit/pop transition, relying on the Compose Navigation default (crossfade or none depending on version).

**Fix:** Add enter/exit transitions to `NavHost`:

```kotlin
NavHost(
    navController = navController,
    startDestination = Screen.Home.route,
    enterTransition = { fadeIn(tween(MotionTokens.DurationMedium2)) },
    exitTransition = { fadeOut(tween(MotionTokens.DurationShort4)) },
    popEnterTransition = { fadeIn(tween(MotionTokens.DurationMedium2)) },
    popExitTransition = { fadeOut(tween(MotionTokens.DurationShort4)) },
    modifier = modifier.padding(top = if (isOnTopLevelScreen) overlayHeightDp else 0.dp),
)
```

Note: Shared element transitions will compose ON TOP of these transitions; they are independent.

---

## Animations confirmed correct (no changes needed)

| Animation | File | Notes |
|-----------|------|-------|
| `SnapBackSpring` on drag cancel | `NowPlayingScreenContent.kt` | Spring is correct for gesture interruption |
| Album art 3D `graphicsLayer` transforms | `NowPlayingScreenContent.kt` | All properties driven from single `Animatable`, efficient single RenderNode |
| `rememberContentFadeIn()` | `AnimatedModifiers.kt` | Correct one-shot `Animatable` + `LaunchedEffect(Unit)` |
| `sharedElement` in `AlbumItem`, `AlbumDetailScreen` | Both | Consistent `boundsTransform` via `MotionTokens.SharedElementBoundsSpec` |
| `animateScrollToItem` on queue open | `QueueSheet.kt` | Fires once on composition, appropriate for initial scroll |
| `animateScrollToPage` on tab tap | `HomeScreen.kt` | Default pager spring animation, reasonable |
| `scrollToItem` for alphabet fast-scroll | `ArtistsTabScreen.kt`, `AlbumsTabScreen.kt` | Instant jump is correct for fast-scroll (no animation needed) |
| Parallax `graphicsLayer { translationY }` | `DetailHeroImage.kt` | State-driven render-thread transform, efficient |
| `SharedTransitionLayout` scope | `NavGraph.kt` | Correctly wraps full navigation graph |
| `renderInSharedTransitionScopeOverlay` | `DetailTopBar.kt` | Correct z-ordering during shared element transitions |
| Chevron rotation `animateFloatAsState` | `CollapsibleSection.kt` | Default spring (NoBouncy, StiffnessMedium) — smooth, no overshoot |
| Swipe-dismiss `animateColorAsState` | `QueueSheet.kt` | Default spring (NoBouncy, StiffnessMediumLow) — no bounce |
| Tab font-size `animateFloatAsState` | `TopBarComponents.kt` | Layout change is intentional for correct tab spacing |
| Tab auto-center `scrollState.animateScrollTo` | `TopBarComponents.kt` | Uses default spec, smooth centering |
| `.animateItem()` on LazyColumn items | `HistoryTabScreen.kt` | 6 usages, correct API for item placement animation |

---

## Scope

**Included:**
- Fixes to the 7 issues above (M1–M3, L1–L4)
- New tokens added to `Motion.kt` where needed

**Excluded:**
- New animations that don't exist yet
- `CollapsibleSection.kt` commented-out code cleanup (unrelated to animation)
- Velocity-tracking refactor for album art gesture (separate task if desired)
- Instrumented/UI tests for animations

## Verification

- `assembleDebug` succeeds after each task
- `test` passes throughout
- Manual: open Now Playing (slide + fade should be in sync), dismiss Now Playing (no invisible ghost), swipe album art fast vs slow, observe MiniPlayer appear/disappear, switch tabs, navigate to album/artist detail