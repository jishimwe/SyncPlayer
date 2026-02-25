# SyncPlayer — Project Plan

A polished local music player for Android that syncs metadata (play counts, playlists, favorites) across devices.

SyncPlayer plays audio from on-device storage and syncs listening data — not the audio files — so playlists, play counts, and preferences carry over seamlessly between a user's devices.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2.10 (bundled by AGP 9) |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM, single Activity |
| Local DB | Room 2.7.1 |
| Playback | Media3 1.9.2 (ExoPlayer) |
| DI | Hilt 2.59 |
| KSP | 2.3.5 |
| Images | Coil 3.1.0 |
| Navigation | Navigation Compose 2.9.0 |
| Auth | Firebase Auth (Google sign-in) |
| Sync | Cloud Firestore |
| Build | AGP 9.0.0, Gradle 9.1.0, Kotlin DSL, version catalog |
| Tests | JUnit 5 5.11.4, Turbine 1.2.1, Compose UI Test |

## Architecture

Single Activity, Jetpack Compose, MVVM pattern.

```
app/src/main/java/com/jpishimwe/syncplayer/
├── ui/             # Screens, composables, ViewModels
│   ├── theme/      # Color, Theme, Type
│   ├── components/ # Reusable UI components
│   ├── library/    # Library browsing (songs, albums, artists, detail screens)
│   ├── player/     # Now Playing screen, player controls, MiniPlayer
│   ├── playlists/  # Playlist management (list, detail, song picker)
│   └── navigation/ # NavGraph, Screen routes, bottom nav
├── data/           # Repositories, local DB
│   └── local/      # Room database, DAOs, entities
├── model/          # Data classes (Song, Album, Artist, PlaybackState, PlayerUiState)
├── service/        # PlaybackService, AudioFocusHandler
└── di/             # Hilt modules
```

- **UI layer**: Compose screens observe ViewModel state via `StateFlow`
- **ViewModel**: Holds UI state, calls into repositories
- **Repository**: Single source of truth, coordinates local DB and sync
- **Local DB**: Room for songs, playlists, play counts, metadata

## Navigation

Current routes:

| Route | Screen | Description |
|-------|--------|-------------|
| `library` | LibraryScreen | Songs/Albums/Artists tabs with MiniPlayer |
| `now_playing` | NowPlayingScreen | Full-screen player with controls |
| `album_detail/{albumId}/{albumName}` | AlbumDetailScreen | Songs in an album |
| `artist_detail/{artistName}` | ArtistDetailScreen | Songs by an artist |
| `playlists` | PlaylistsScreen | Playlist list with create/rename/delete |
| `playlist_detail/{playlistId}/{playlistName}` | PlaylistDetailScreen | Songs in a playlist with reorder/add/remove |

Bottom navigation bar switches between Library and Playlists tabs. A persistent MiniPlayer sits above the bottom nav on top-level screens. Navigation uses `saveState`/`restoreState` for tab state preservation.

## Build Phases

Phases are ordered from simple to complex. Each phase builds on the previous one.

### Phase 1: Library ✅

Scan the device for audio files and display them in a browsable list.

- ✅ Query `MediaStore` for audio files on device
- ✅ Display songs in a scrollable list (title, artist, duration)
- ✅ Browse by songs, albums, artists (tabbed with `PrimaryTabRow`)
- ✅ Room database to cache the scanned library
- ✅ Handle runtime permissions (`READ_MEDIA_AUDIO`) with three-state handling
- ✅ Automatic rescan on app resume (24-hour threshold)
- ✅ Stale data over error screen when DB has cached songs

**Plan doc**: [`docs/features/library-browsing/plan.md`](features/library-browsing/plan.md)
**Design doc**: [`docs/features/library-browsing/design.md`](features/library-browsing/design.md)

### Phase 2: Playback ✅

Play audio using Media3 with full media session support.

- ✅ Media3 `ExoPlayer` setup with `MediaSession`
- ✅ Now-playing screen (album art, track info, seek bar, controls)
- ✅ Play/pause, next, previous, seek
- ✅ Notification controls with `MediaSessionService`
- ✅ Lock screen controls
- ✅ Background playback
- ✅ Custom audio focus handling with smooth volume fades (500ms)
- ✅ Headphone disconnect pauses playback
- ✅ Queue management with drag-and-drop reordering (play next, add to end, remove, reorder)
- ✅ Shuffle and repeat modes (off / all / one)
- ✅ MiniPlayer on library screen
- ✅ Queue persistence via Room (restore on app restart)
- ✅ Navigation: Library ↔ NowPlaying

**Plan doc**: [`docs/features/playback/plan.md`](features/playback/plan.md)
**Design doc**: [`docs/features/playback/design.md`](features/playback/design.md)

### Phase 3: Library → Playback Navigation ✅

Connect the library browsing UI to the playback system.

- ✅ Tap song in Songs tab → queue all songs, start at tapped index, navigate to NowPlaying
- ✅ Tap album → Album detail screen (song list by track number) → tap song to play
- ✅ Tap artist → Artist detail screen (song list by album + track) → tap song to play
- ✅ Fix `PlayerEvent.PlaySongs` to include `startIndex`
- ✅ Add `AlbumDetail` and `ArtistDetail` navigation routes

**Plan doc**: [`docs/features/library-playback-nav/plan.md`](features/library-playback-nav/plan.md)
**Design doc**: [`docs/features/library-playback-nav/design.md`](features/library-playback-nav/design.md)

### Phase 3.1: Post-Phase 2 Bug Fixes ✅

Fixed bugs discovered during manual testing after Phases 2 and 3.

- ✅ Shuffle/repeat buttons: added `onShuffleModeEnabledChanged` and `onRepeatModeChanged` listener callbacks
- ✅ Seek bar frozen: wired position polling from `onIsPlayingChanged`, fixed duration tracking
- ✅ Artist click not working: wired `Modifier.clickable`, added `onNavigateToArtistDetail` callback, registered `ArtistDetail` route in NavGraph
- ✅ Queue sheet not opening: wired `QueueSheet` with show/hide state, populated queue in `PlayerUiState` via `syncQueueState()` helper
- ✅ Queue reorder/delete/play-from-queue: fixed `QueueItem.id` from random UUID to stable song ID, fixed key mismatches in `QueueSheet`
- ✅ ShuffleButton icons swapped, RepeatOne icon missing
- ✅ MiniPlayer hidden on NowPlaying screen
- ✅ Dead `positionUpdateFlow` removed from PlayerViewModel
- ✅ Tests updated: `startIndex` passthrough, `SeekToQueueItem` routing, shuffle/repeat/skip button events

**Plan doc**: [`docs/features/bugfixes-phase2/plan.md`](features/bugfixes-phase2/plan.md)
**Design doc**: [`docs/features/bugfixes-phase2/design.md`](features/bugfixes-phase2/design.md)

### Phase 4: Playlists ✅

Create, edit, and manage playlists stored locally.

- ✅ Room entities for playlists (`PlaylistEntity`) and playlist-song associations (`PlaylistSongCrossRef`)
- ✅ Create / rename / delete playlists (with dialogs)
- ✅ Add / remove / reorder songs in a playlist (drag-to-reorder, song picker with diff-based sync)
- ✅ Playlist detail screen (tap song to play playlist from that index)
- ✅ Bottom navigation bar (Library | Playlists) with state preservation
- ✅ MiniPlayer stacked below bottom nav on top-level screens

**Plan doc**: [`docs/features/playlists/plan.md`](features/playlists/plan.md)
**Design doc**: [`docs/features/playlists/design.md`](features/playlists/design.md)

### Phase 5: Metadata Tracking ✅

Track listening data locally in Room.

- ✅ Play count per song — increments once per play when position passes `min(150s, 70% of duration)`
- ✅ 5-star rating system (`Rating` enum: NONE, POOR, FAIR, GOOD, GREAT, FAVORITE) replacing `isFavorite: Boolean`
- ✅ Favorite button on NowPlaying — shortcut to `Rating.FAVORITE`; tap again clears to `Rating.NONE`
- ✅ Star rating bar on NowPlaying — tap a star to set rating; tap active star to clear back to `Rating.NONE`
- ✅ Listening history log (`ListeningHistoryEntity` with timestamp, append-only)
- ✅ `lastPlayed` timestamp on Song entity
- ✅ Three new Library tabs: Faves (4+ stars), Top Plays, Recent (deduplicated by `GROUP BY songs.id`)
- ✅ `currentSongRating` StateFlow on `PlayerViewModel` — reacts to song changes via `flatMapLatest`

**Plan doc**: [`docs/features/metadata-tracking/plan.md`](features/metadata-tracking/plan.md)
**Design doc**: [`docs/features/metadata-tracking/design.md`](features/metadata-tracking/design.md)

### Phase 6: Sync 🔄 In Progress

Sync metadata across devices using Firebase.

- Firebase Auth with Google Sign-In (Android Credential Manager)
- Firestore data model for playlists, play counts, ratings, listening history
- Song fingerprinting (`title + artist + album + duration` → SHA-256) for cross-device song matching
- Room migration 4→5: `lastModified` on Song + PlaylistEntity; `remoteId` on PlaylistEntity
- `SyncOrchestrator`: push local changes, pull remote changes, per-entity conflict resolution
- Conflict resolution: `playCount` = max-wins; `rating` = last-write-wins; playlists = LWW; history = union merge
- Offline support via Firestore's built-in offline persistence
- Settings screen with sign-in/out, sync status, manual sync trigger
- Settings tab added to bottom navigation (Library | Playlists | Settings)
- Sync triggered on every app foreground (`MainActivity.onResume`)

**Plan doc**: [`docs/features/sync/plan.md`](features/sync/plan.md)

### Phase 7: Polish

UI refinements and quality-of-life features.

- Search across songs, albums, artists
- Sorting and filtering (by name, date added, play count)
- Settings screen (theme, playback preferences, account management)
- Bottom navigation bar (Library, Playlists, Settings)

## External Integrations

SyncPlayer doesn't exist in isolation — it syncs metadata with other music players the user already uses. The integration layer is designed to be pluggable so new players can be added over time.

### MusicBee (Desktop)

Sync metadata with [MusicBee](https://getmusicbee.com/), a popular desktop music player for Windows.

- Import/export play counts, ratings, playlists, and favorites
- MusicBee stores its library in an XML database — read and write to this format
- Sync via a shared backend (Firestore) or direct file-based sync (e.g., local network, shared cloud folder)
- Match songs between SyncPlayer and MusicBee by fingerprint or metadata (title + artist + album + duration)
- Handle mismatches gracefully (songs on one device but not the other)

### YouTube Music (Online)

Sync metadata with YouTube Music so listening activity stays consistent across platforms.

- Pull liked songs, playlists, and play history from YouTube Music
- Push SyncPlayer favorites and playlists to YouTube Music
- Use YouTube Music's available APIs or unofficial libraries for data access
- Match songs between local library and YouTube Music catalog
- Periodic or on-demand sync with conflict resolution

### Plugin Architecture (Future)

The integration layer should follow a provider pattern so additional players can be added without modifying core sync logic.

- Define a common `SyncProvider` interface (connect, pull metadata, push metadata, match songs)
- Each integration (MusicBee, YouTube Music, future players) implements this interface
- Users enable/disable providers in Settings
- Potential future providers: Spotify, Last.fm, Jellyfin, Navidrome, Plex, local desktop players (foobar2000, Winamp, etc.)

## Testing Strategy

Testing is not a phase — it ships with every phase. Code should be structured for testability from day one: inject dependencies, use interfaces for repositories, keep business logic out of composables.

### Test types

| Type | Runs on | Speed | What to test |
|------|---------|-------|-------------|
| **Unit tests** | JVM (local) | Fast | ViewModels, Repositories, mappers, business logic |
| **Room tests** | Emulator/device | Fast | DAOs with in-memory database |
| **Compose UI tests** | Emulator/device | Medium | Critical user flows, screen states |
| **Integration tests** | Emulator/device | Slow | Sync logic, provider implementations |

### Principles

- Test behavior, not implementation — assert on outcomes, not internal calls
- Use fakes over mocks where possible (e.g., `FakeSongRepository` implements `SongRepository`)
- Hilt test modules swap real dependencies for fakes in tests
- Don't chase coverage numbers — test logic that can break, skip trivial code
- UI tests cover happy paths and key interactions, not every screen permutation

### Testability patterns

- Every Repository has an interface — ViewModels depend on the interface, tests provide a fake
- ViewModels expose `StateFlow` — tests collect and assert on state transitions
- Composables receive state as parameters — testable in isolation without ViewModels (`ScreenContent` pattern)
- Suspend functions and Flows use `kotlinx-coroutines-test` for deterministic execution

### Tests per phase

**Phase 1: Library** ✅
- Unit: `LibraryViewModel` state transitions (loading → loaded, empty state, error)
- Room: Song DAO queries (insert, query by album/artist)
- UI: Song list renders correctly, tab switching works

**Phase 2: Playback** ✅
- Unit: `PlayerViewModel` event routing (play/pause toggle, skip, seek, shuffle, repeat, playSongs)
- Unit: `formatTime` utility
- UI: NowPlayingScreenContent displays correct track info, controls respond

**Phase 3: Library → Playback Navigation** ✅
- Unit: `PlayerViewModel` passes `startIndex` to repository
- Unit: `SeekToQueueItem` routes to repository
- UI: Song click triggers playback + navigation callback
- UI: Album/Artist detail screens display songs and handle clicks

**Phase 3.1: Bug Fixes** ✅
- Unit: Shuffle/repeat/skip button events fire correct `PlayerEvent`
- UI: NowPlayingScreenContent shuffle, repeat, skip previous buttons

**Phase 4: Playlists** ✅
- Unit: `PlaylistViewModel` state transitions, CRUD events, blank-name guards
- Fake: `FakePlaylistRepository` with call counters and argument recording

**Phase 5: Metadata Tracking** ✅
- Unit: `PlayerViewModel` SetRating event — routes to `SongRepository.setRating` with correct song ID and rating; no-ops when no song is playing
- Unit: `LibraryViewModel` — favorites, mostPlayed, recentlyPlayed flow through to `LibraryUiState.Loaded`
- UI: `NowPlayingScreenContent` favorite button — emits `SetRating(FAVORITE)` when unrated; emits `SetRating(NONE)` when already favorited
- Fake: `FakeSongRepository` updated with stubs for all metadata methods

**Phase 6: Sync**
- Unit: Conflict resolution logic (last-write-wins, merge)
- Unit: Sync state machine (idle → syncing → success/error)
- Integration: `SyncProvider` implementations against fake/mock backends
- Integration: Merge scenarios (local-only changes, remote-only, conflicts)

**Phase 7: Polish**
- UI: Search returns correct results, sorting changes list order

### Test libraries

| Library | Purpose |
|---------|---------|
| JUnit 5 | Test runner and assertions |
| Turbine | Testing Kotlin Flows (`StateFlow` assertions) |
| kotlinx-coroutines-test | Deterministic coroutine execution in tests |
| Compose UI Test | `composeTestRule` for UI assertions |
| Hilt Testing | `@HiltAndroidTest`, test modules for DI |
| Room Testing | In-memory database for DAO tests |

## CI/CD

GitHub Actions pipeline for automated builds, tests, and releases.

### On every push / PR

- Build debug APK (`gradlew assembleDebug`)
- Run unit tests (`gradlew test`)
- Lint checks (`gradlew lint`)
- Cache Gradle dependencies and build outputs to speed up runs

### On merge to main

- Everything above, plus:
- Run instrumented tests on an Android emulator (`gradlew connectedAndroidTest`)
- Build release AAB/APK (signed with release keystore from GitHub Secrets)
- Upload APK as build artifact for manual testing

### Release (tag-triggered)

- Build signed release AAB
- Publish to Google Play internal track via Fastlane or Google Play Developer API
- Optionally distribute via Firebase App Distribution for beta testers

### Secrets management

- Keystore file and passwords stored in GitHub Actions encrypted secrets
- Firebase config (`google-services.json`) stored as a secret, written at build time
- Never commit signing keys or service account credentials to the repo

## Key Libraries by Phase

| Phase | Libraries |
|-------|-----------|
| 1 — Library | Room, Coil (album art), Hilt, Activity Result API (permissions) |
| 2 — Playback | Media3 (ExoPlayer, Session, UI), kotlinx-coroutines-guava, reorderable |
| 3 — Navigation | (no new libraries) |
| 4 — Playlists | (Room — already added) |
| 5 — Metadata | (Room — already added) |
| 6 — Sync | Firebase Auth, Cloud Firestore, Google Play Services Auth |
| 7 — Polish | (no new major libraries expected) |
| External — MusicBee | XML parsing (kotlinx.serialization or built-in), file I/O |
| External — YouTube Music | YouTube Music API / unofficial client libraries, Retrofit |
| Testing (all phases) | JUnit 5, Turbine, kotlinx-coroutines-test, Compose UI Test, Hilt Testing |

## Future Improvements

Features to consider after the core phases and external integrations are complete, ordered by priority.

1. **Android Auto support** — Expose the media library and playback controls to Android Auto. `MediaSession` and `MediaBrowserService` (already used for notification controls) get most of the way there; the remaining work is declaring Auto support in the manifest and ensuring the browse tree is well-structured.
2. **Gapless playback / crossfade** — Seamless transitions between tracks. Media3 supports gapless natively; crossfade requires mixing two player instances or using audio effects.
3. **Sleep timer** — Stop playback after a user-defined duration or at end of current track/album. Simple countdown timer that pauses the player.
4. **Equalizer** — Built-in audio equalizer and presets. Media3 supports Android's `AudioEffect` APIs (EQ, bass boost, virtualizer).
5. **Home screen widget** — Glance (Jetpack) widget showing current track with play/pause, next, previous controls.
6. **Lyrics display** — Show lyrics on the now-playing screen. Read embedded lyrics from audio file tags first, with optional fetch from an external lyrics API.
7. **Last.fm scrobbling** — Track listening history on Last.fm. Fits naturally into the plugin architecture as another `SyncProvider` implementation.