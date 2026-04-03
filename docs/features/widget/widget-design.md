---
type: design
feature: widget
phase: 8
status: complete
tags:
  - type/design
  - status/complete
  - feature/widget
---

# Now Playing Widget - Design

## Overview

A Jetpack Glance home screen widget that displays the currently playing song and provides playback controls. Supports two responsive sizes (4×1 compact, 4×2 expanded) with a dark-themed design matching the app's MiniPlayer aesthetic.

## What was built

### New files
- `app/src/main/java/com/jpishimwe/syncplayer/ui/widget/NowPlayingWidget.kt`: `GlanceAppWidget` with responsive layout, custom dark theme, accent-colored title, album art, and control buttons
- `app/src/main/java/com/jpishimwe/syncplayer/ui/widget/NowPlayingWidgetReceiver.kt`: `GlanceAppWidgetReceiver` entry point
- `app/src/main/java/com/jpishimwe/syncplayer/ui/widget/WidgetActionCallbacks.kt`: `ActionCallback` implementations for play/pause, skip next, skip previous — each sends an intent to `PlaybackService`
- `app/src/main/java/com/jpishimwe/syncplayer/ui/widget/NowPlayingWidgetUpdater.kt`: Writes playback state to Glance `Preferences` and triggers `updateAll()`
- `app/src/main/java/com/jpishimwe/syncplayer/ui/widget/WidgetStateKeys.kt`: DataStore preference keys (title, artist, album, albumArtUri, isPlaying)
- `app/src/main/java/com/jpishimwe/syncplayer/ui/widget/NowPlayingWidgetPreview.kt`: Regular Compose `@Preview` functions mirroring the widget layout for design iteration
- `app/src/main/res/xml/now_playing_widget_info.xml`: Widget metadata (sizes, resize modes)
- `app/src/main/res/drawable/widget_background.xml`: Rounded dark rectangle
- `app/src/main/res/drawable/widget_play_btn_bg.xml`: Circular accent-tinted background for play/pause
- `app/src/main/res/drawable/widget_control_bg.xml`: Circular semi-transparent background for skip buttons
- `app/src/main/res/drawable/widget_album_art_clip.xml`: Rounded container for album art
- `app/src/main/res/drawable/ic_widget_play.xml`, `ic_widget_pause.xml`, `ic_widget_skip_next.xml`, `ic_widget_skip_previous.xml`: Control icon vectors

### Modified files
- `gradle/libs.versions.toml`: Added `glance = "1.1.1"`, `androidx-glance-appwidget`, `androidx-glance-material3`
- `app/build.gradle.kts`: Added Glance dependencies
- `app/src/main/AndroidManifest.xml`: Registered `NowPlayingWidgetReceiver`
- `app/src/main/java/com/jpishimwe/syncplayer/service/PlaybackService.kt`: Added `Player.Listener` for widget updates, `onStartCommand` for widget control actions
- `app/src/main/res/values/strings.xml`: Widget description and "Not playing" strings

## Design decisions

- **ActionCallback + startService over MediaController**: The initial approach of building a `MediaController` asynchronously in `ActionCallback.onAction()` failed silently in widget context. Switched to sending intents directly to `PlaybackService.onStartCommand`, which is synchronous and reliable — the same mechanism media notifications use under the hood.

- **Glance Preferences for state**: Standard Glance pattern. The updater writes song metadata into DataStore preferences before calling `updateAll()`. The widget reads them via `currentState<Preferences>()`. This avoids needing Hilt DI in the widget context.

- **Custom dark theme over GlanceTheme.colors**: The app uses a custom dark theme with accent color `#FF1D58`. Rather than relying on system Material You colors via `GlanceTheme`, the widget uses hardcoded colors matching the app's `myAccentColor` and `myBackgroundDark` for visual consistency with the MiniPlayer.

- **SizeMode.Responsive with two breakpoints**: One `GlanceAppWidget` class handles both 4×1 and 4×2 via `LocalSize.current`. Compact shows a horizontal row; expanded shows album art + info on top with controls below.

- **Compose @Preview for widget design**: Glance doesn't support `@Preview`. Created a parallel regular Compose file (`NowPlayingWidgetPreview.kt`) that mirrors the widget layout using Material 3 components, allowing visual iteration in Android Studio without deploying.

- **FLAG_ACTIVITY_SINGLE_TOP**: Widget tap uses `Intent.FLAG_ACTIVITY_SINGLE_TOP or FLAG_ACTIVITY_CLEAR_TOP` to bring the existing activity to front rather than creating a new instance (which would restart playback).

---

**Plan doc**: [[widget-plan]]
