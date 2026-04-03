---
type: plan
feature: widget
phase: 8
status: complete
tags:
  - type/plan
  - status/complete
  - feature/widget
---

# Now Playing Widget - Plan

**Status: ✅ Complete**

## Context

Users want quick visibility into what's currently playing without opening the app. An Android home screen widget provides at-a-glance "now playing" info — song title, artist, and album art — in a compact, always-visible form.

## Scope

**Included:**
- 4×1 compact widget (song title + artist, small album art, playback controls)
- 4×2 expanded widget (larger album art + song title + artist + playback controls)
- Playback controls: play/pause toggle, skip next, skip previous
- Real-time updates when the current song or playback state changes
- Tap-to-open: tapping the song info area opens the app
- Jetpack Glance (Compose-based widget API) with custom dark theme
- Compose @Preview for widget visual iteration

**Excluded:**
- Widget configuration activity
- Multiple widget styles/themes
- Lock screen widget
- Seekbar / progress display
- Shuffle / repeat controls

## Approach

**Jetpack Glance** over traditional RemoteViews because:
- Compose-like declarative API fits the existing codebase style
- Less boilerplate than XML-based RemoteViews
- `glance-material3` provides ready-made M3 color mappings

**State updates via Player.Listener in PlaybackService**: Direct listener on ExoPlayer triggers `NowPlayingWidgetUpdater.update()` which writes Glance `Preferences` and calls `updateAll()`.

**Playback controls via ActionCallback + startService**: Each button uses `actionRunCallback` which sends an `Intent` with a custom action to `PlaybackService.onStartCommand`. Simpler and more reliable than async `MediaController` connections from widget context.

**Two size presets, one `GlanceAppWidget`**: A single widget class with responsive layout via `SizeMode.Responsive` and `LocalSize`.

## Tasks

### Layer 1: Dependencies ✅
1. ✅ Added `glance = "1.1.1"` to version catalog
2. ✅ Added `androidx-glance-appwidget` and `androidx-glance-material3` libraries
3. ✅ Added dependencies to `app/build.gradle.kts`

### Layer 2: Widget infrastructure ✅
4. ✅ Created `res/xml/now_playing_widget_info.xml`
5. ✅ Created `NowPlayingWidgetReceiver`
6. ✅ Created `NowPlayingWidget`
7. ✅ Registered receiver in `AndroidManifest.xml`

### Layer 3: Widget UI ✅
8. ✅ Implemented responsive layout (compact 4×1, expanded 4×2)
9. ✅ Custom dark theme with accent color (#FF1D58), styled control buttons
10. ✅ Album art with rounded container, placeholder fallback
11. ✅ Click on song info opens app (FLAG_ACTIVITY_SINGLE_TOP)

### Layer 4: Playback controls & state updates ✅
12. ✅ ActionCallback implementations → startService with action intents
13. ✅ PlaybackService.onStartCommand handles PLAY_PAUSE, SKIP_NEXT, SKIP_PREVIOUS
14. ✅ Player.Listener in PlaybackService triggers widget updates
15. ✅ NowPlayingWidgetUpdater writes Glance Preferences + updateAll()

### Layer 5: Tests & preview ✅
16. ✅ Unit tests for WidgetStateKeys
17. ✅ Compose @Preview file for visual iteration (6 previews: compact/expanded × playing/paused/idle)

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.glance:glance-appwidget` | 1.1.1 | Compose-based widget framework |
| `androidx.glance:glance-material3` | 1.1.1 | M3 color/theme integration for Glance |

## Decisions

1. **Album art fallback**: Placeholder music note icon tinted with semi-transparent white
2. **Widget preview image**: Deferred — using Compose @Preview for dev-time iteration instead
3. **Button mechanism**: `ActionCallback` + `startService` (not `actionSendBroadcast`, not async `MediaController`)
4. **Visual style**: Custom dark theme matching MiniPlayer — accent title, circular control buttons with backgrounds, `album · artist` subtitle format
5. **Subtitle format**: `album · artist` matching MiniPlayer's `buildSubtitle` pattern

## Verification

- ✅ `assembleDebug` succeeds
- ✅ `test` passes all tests
- Manual: Add widget → shows "Not playing" → play song → widget updates → play/pause button works → skip works → tap song info opens app → works in both 4×1 and 4×2 sizes

---

**Design doc**: [[widget-design]]
