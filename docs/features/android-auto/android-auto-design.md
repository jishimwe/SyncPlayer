---
type: design
feature: android-auto
phase: 8
status: complete
tags:
  - type/design
  - status/complete
  - feature/android-auto
---

# Android Auto Integration - Design

## Overview

SyncPlayer now supports Android Auto by exposing a browsable media tree through Media3's `MediaLibraryService`. Drivers can browse Songs, Albums, Artists, Playlists, Favorites, and the current Queue from the car head unit, with search support. No new dependencies were added — the existing `media3-session:1.9.2` includes everything needed.

## What was built

- `app/src/main/res/xml/automotive_app_desc.xml`: Declares SyncPlayer as an Android Auto media app
- `app/src/main/AndroidManifest.xml`: Added `<meta-data>` for car discovery + updated service intent-filter from `MediaSessionService` to `MediaLibraryService`
- `app/src/main/java/.../service/MediaBrowseTree.kt`: Hilt-injectable class that builds the browse tree from repository data. 6 root categories, 100-item cap per category, suspend methods using `Flow.first()`, Queue reads directly from ExoPlayer
- `app/src/main/java/.../service/PlaybackService.kt`: Converted from `MediaSessionService` to `MediaLibraryService` with `@AndroidEntryPoint`, `MediaLibrarySession`, and full callback implementation (browse, search)
- `app/src/test/java/.../service/MediaBrowseTreeTest.kt`: 12 unit tests covering all tree nodes, drill-downs, edge cases

## Design decisions

- **`MediaLibraryService` over `MediaSessionService`**: Required for Android Auto browse support. It's a superset — all existing functionality (notifications, lock screen, widget) continues unchanged since `MediaLibrarySession` extends `MediaSession`
- **`@AndroidEntryPoint` for DI**: Standard Hilt approach on the service. Alternative was manual `EntryPointAccessors` but that adds boilerplate for no benefit
- **Queue reads from ExoPlayer directly**: The `PlayerRepository` lives on the client side (connected via `MediaController`). Inside the service, the `ExoPlayer` instance is the source of truth for queue state, so the browse tree takes a `Player?` parameter
- **Coroutine→ListenableFuture bridge**: `MediaLibrarySession.Callback` returns `ListenableFuture`. Used `CoroutineScope.future {}` from `kotlinx-coroutines-guava` (already a dependency) to bridge suspend repository calls
- **100-item cap**: Android Auto recommends keeping lists short for driver safety and performance. Songs capped to 100 most recent, albums/artists to 100 alphabetically
- **Search cached in memory**: `onSearch()` runs the query and caches results, `onGetSearchResult()` returns the cache. Simple and sufficient since search is user-driven and sequential

## Known gaps

- Manual testing on real Android Auto (car or DHU) not yet performed
- Album art uses `content://` URIs — should work on Android Auto but not verified on all head units. May need a `ContentProvider` if some units can't resolve content URIs
- `onGetItem()` only searches root-level children, not deep tree items — sufficient for Android Auto's typical usage but could be expanded

---

**Plan doc**: [[android-auto-plan]]
