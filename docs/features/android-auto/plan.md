# Android Auto Integration - Plan

## Context

SyncPlayer already has a `PlaybackService` extending Media3's `MediaSessionService`, which gives us lock-screen controls and notification playback. Android Auto requires a **browsable media tree** so drivers can navigate the library (songs, albums, artists, playlists) from the car head unit. The core change is upgrading from `MediaSessionService` → `MediaLibraryService` and implementing the browse tree callback.

## Scope

**Included:**
- Convert `PlaybackService` from `MediaSessionService` to `MediaLibraryService`
- Implement `MediaLibrarySession.Callback` with a content browse tree
- Browse tree categories: Songs (recent/all), Albums, Artists, Playlists, Favorites
- Playable items with album art metadata for display on car screen
- Search support via `onSearch()` callback
- `automotive_app_desc.xml` manifest resource
- Manifest updates for Android Auto discovery
- Unit tests for the browse tree builder

**Excluded:**
- Android Automotive OS (embedded car OS) — separate concern, different manifest
- Custom UI templates (Android Auto provides its own media UI)
- Voice assistant deep integration beyond standard media search
- Streaming/casting to car — this is local playback only

## Approach

### Why MediaLibraryService?

Android Auto connects to media apps through the `MediaLibraryService` API. It calls `onGetLibraryRoot()` to get the root, then `onGetChildren()` to browse categories and items. This is the **only supported way** to provide browsable content to Android Auto with Media3.

Since `MediaLibraryService` extends `MediaSessionService`, all existing playback functionality (notifications, lock screen, widget updates) continues to work unchanged. The `MediaSession` becomes a `MediaLibrarySession` (which extends `MediaSession`), so `MediaController` clients are unaffected.

### Browse Tree Design

```
[ROOT]
├── [SONGS]         → Recent 100 songs (sorted by date added)
├── [ALBUMS]        → All albums → tap album → album songs
├── [ARTISTS]       → All artists → tap artist → artist songs
├── [PLAYLISTS]     → All playlists → tap playlist → playlist songs
├── [FAVORITES]     → Songs with rating == FAVORITE
└── [QUEUE]         → Current playback queue
```

Each category node has `FLAG_BROWSABLE`. Leaf song items have `FLAG_PLAYABLE`. Albums/Artists/Playlists have both flags where applicable (browsable to drill in, playable to shuffle-all).

### Impact on Existing Code

| Component | Change |
|-----------|--------|
| `PlaybackService` | Extends `MediaLibraryService` instead of `MediaSessionService` |
| `MediaSession` | Becomes `MediaLibrarySession` (superset, backwards compatible) |
| `PlayerRepositoryImpl` | `SessionToken` construction unchanged — `MediaController` works with both service types |
| `AndroidManifest.xml` | Service intent-filter updated + `<meta-data>` for automotive |
| Widget integration | Unchanged — still listens to player events |

## Tasks

### Layer 1: Manifest & Resources ✅

1. ✅ **Create `res/xml/automotive_app_desc.xml`** — declares this as a media app for Android Auto
2. ✅ **Update `AndroidManifest.xml`**:
   - Added `<meta-data android:name="com.google.android.gms.car.application" android:resource="@xml/automotive_app_desc"/>` to `<application>`
   - Updated `PlaybackService` intent-filter to `MediaLibraryService` + legacy `MediaBrowserService` action
3. ✅ **Build verify**: `assembleDebug` passed

### Layer 2: Browse Tree Builder ✅

4. ✅ **Created `service/MediaBrowseTree.kt`** — `@Inject`-able class with 7 node IDs, `suspend getChildren(parentId, player)`, 100-item cap, conversion helpers for Song/Album/Artist/Playlist → MediaItem
5. ✅ **Build verify**: `assembleDebug` passed

### Layer 3: Convert PlaybackService ✅

7. ✅ **Converted `PlaybackService`**: `MediaSessionService` → `MediaLibraryService`, `MediaSession` → `MediaLibrarySession`, implemented full `MediaLibrarySession.Callback` (onGetLibraryRoot, onGetChildren, onGetItem, onSearch, onGetSearchResult), cleaned up commented-out TODO
8. ✅ **Added `@AndroidEntryPoint`** + `@Inject MediaBrowseTree`
9. ✅ **Build verify**: `assembleDebug` passed

### Layer 4: Verify PlayerRepositoryImpl ✅

10. ✅ **Verified** — `SessionToken` + `MediaController.Builder` works identically with `MediaLibraryService`. No code changes needed.
11. ✅ **Build verify**: passed (part of Layer 3 build)

### Layer 5: Tests ✅

12. ✅ **Unit test `MediaBrowseTreeTest`** — 12 tests, all passing:
    - Root children = 6 categories, all browsable/non-playable
    - Songs sorted by dateAdded descending, all playable
    - Albums browsable with MEDIA_TYPE_ALBUM
    - Album drill-down returns playable songs
    - Artists browsable with MEDIA_TYPE_ARTIST
    - Artist drill-down returns playable songs
    - Playlists browsable with MEDIA_TYPE_PLAYLIST
    - Playlist drill-down returns playable songs
    - Favorites returns only rating==5 songs
    - Queue returns empty when player is null
    - Search returns matching songs
    - Unknown parent returns empty list

13. ✅ **Run tests**: `gradlew.bat test` — all pass

### Layer 6: Manual Verification (pending)

14. **Test on real Android Auto** — pending car/head unit connection. DHU setup is complex on modern Android Auto versions; recommend testing on actual car or via Media Controller Test app (github.com/googlesamples/android-media-controller).

## Dependencies

**No new dependencies required.** `MediaLibraryService` and `MediaLibrarySession` are part of `androidx.media3:media3-session` which is already at version 1.9.2 in the project.

## Open Questions

1. **Browse tree item limits** — **Decided: cap at 100 items per category.** Songs show the 100 most recently added. Albums/Artists show the first 100 alphabetically. Playlist songs capped at 100.

2. **Album art in car display** — Current `Song.albumArtUri` uses `content://` URIs. Android Auto should handle these, but we may need to verify. If not, we'd need to provide a `ContentProvider` to serve artwork.

3. **Hilt on Service** — **Decided: use `@AndroidEntryPoint`.** This is the standard Hilt approach and the simplest. The alternative is manual DI — grab the `SingletonComponent` from `(application as SyncPlayerApplication)` and call `entryPointAccessors` to get repository instances. This avoids annotating the service but adds boilerplate and couples to the Application class. `@AndroidEntryPoint` is the right call here.

4. **Queue visibility** — **Decided: yes.** The current queue will appear as a browsable node in the car browse tree.

## Verification

- `assembleDebug` succeeds after each layer
- `test` passes all tests
- Manual: Install on device → open Android Auto DHU → verify:
  - App appears in Android Auto media app list
  - Browse tree shows Songs, Albums, Artists, Playlists, Favorites
  - Tapping a song starts playback with correct metadata on car screen
  - Skip/pause/play controls work
  - Search returns relevant results
  - Notification and lock screen controls still work (regression check)
  - Home screen widget still works (regression check)