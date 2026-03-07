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
| Tests | JUnit 5 5.11.4, Turbine 1.2.1, MockK 1.14.9, Compose UI Test |

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
│   ├── settings/   # Sign-in, sync status, manual trigger
│   └── navigation/ # NavGraph, Screen routes, bottom nav
├── data/           # Repositories, local DB
│   ├── local/      # Room database, DAOs, entities
│   └── sync/       # Firebase Auth, Firestore, conflict resolution, orchestrator
├── model/          # Data classes (Song, Album, Artist, PlaybackState, PlayerUiState)
├── service/        # PlaybackService, AudioFocusHandler
└── di/             # Hilt modules
```

- **UI layer**: Compose screens observe ViewModel state via `StateFlow`
- **ViewModel**: Holds UI state, calls into repositories
- **Repository**: Single source of truth, coordinates local DB and sync
- **Local DB**: Room for songs, playlists, play counts, metadata
- **Sync layer**: `SyncOrchestrator` pushes/pulls via `SyncRepository` (Firestore); conflict resolution in `ConflictResolver`

## Navigation

Current routes:

| Route | Screen | Description |
|-------|--------|-------------|
| `library` | LibraryScreen | Songs/Albums/Artists/Faves/Top Plays/Recent tabs with MiniPlayer |
| `now_playing` | NowPlayingScreen | Full-screen player with controls and queue sheet |
| `album_detail/{albumId}/{albumName}` | AlbumDetailScreen | Songs in an album |
| `artist_detail/{artistName}` | ArtistDetailScreen | Songs by an artist |
| `playlists` | PlaylistsScreen | Playlist list with create/rename/delete |
| `playlist_detail/{playlistId}/{playlistName}` | PlaylistDetailScreen | Songs in a playlist with reorder/add/remove |

Bottom navigation bar switches between Library, Playlists, and Settings tabs. A persistent MiniPlayer sits above the bottom nav on top-level screens. Navigation uses `saveState`/`restoreState` for tab state preservation.

## Build Phases

Phases are ordered from simple to complex. Each phase builds on the previous one.

---

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

---

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

---

### Phase 3: Library → Playback Navigation ✅

Connect the library browsing UI to the playback system.

- ✅ Tap song in Songs tab → queue all songs, start at tapped index, navigate to NowPlaying
- ✅ Tap album → Album detail screen (song list by track number) → tap song to play
- ✅ Tap artist → Artist detail screen (song list by album + track) → tap song to play
- ✅ Fix `PlayerEvent.PlaySongs` to include `startIndex`
- ✅ Add `AlbumDetail` and `ArtistDetail` navigation routes

**Plan doc**: [`docs/features/library-playback-nav/plan.md`](features/library-playback-nav/plan.md)
**Design doc**: [`docs/features/library-playback-nav/design.md`](features/library-playback-nav/design.md)

---

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

---

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

---

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

---

### Phase 6: Sync ✅

Sync metadata across devices using Firebase.

- ✅ Firebase Auth with Google Sign-In (Android Credential Manager API)
- ✅ Firestore data model for playlists, play counts, ratings, listening history
- ✅ Song fingerprinting (`title + artist + album + duration` → SHA-256) for cross-device song matching
- ✅ Room migration 4→5: `lastModified` on Song + PlaylistEntity; `remoteId` on PlaylistEntity
- ✅ `SyncOrchestrator`: push local changes since last sync, pull remote changes, per-entity conflict resolution
- ✅ Conflict resolution: `playCount` = max-wins; `rating` = last-write-wins by `lastModified`; playlists = LWW; history = append-only union merge
- ✅ Offline support via Firestore's built-in offline persistence
- ✅ Settings screen with sign-in/out, sync status, manual sync trigger, last sync time
- ✅ Settings tab added to bottom navigation (Library | Playlists | Settings)
- ✅ Sync triggered on every app foreground (`MainActivity.onResume`)

**Plan doc**: [`docs/features/sync/plan.md`](features/sync/plan.md)
**Design doc**: [`docs/features/sync/design.md`](features/sync/design.md)

---

### Phase 7: Polish ✅

Four sub-goals, each shipped independently. See [`docs/features/phase7/design.md`](features/phase7/design.md) for full design notes.

**Plan doc**: [`docs/features/phase7/plan.md`](features/phase7/plan.md)
**Design doc**: [`docs/features/phase7/design.md`](features/phase7/design.md)

#### 7.1 — Sync gap closure ✅

- ✅ `ConflictResolver.mergeHistoryEvent` — replaced `map { !! }` with `mapNotNull`; unknown fingerprints silently dropped
- ✅ Sign-in error snackbar — separate `snackbarMessage: StateFlow<String?>` on `SettingsViewModel`; `SignInError` and `ClearSnackbar` events; `SnackbarHost` in `SettingsScreenContent`
- ✅ Sync retry button — "Retry" `TextButton` rendered in the sync-status card when `SyncStatus.Error`; fires existing `SyncNow` event
- ✅ Playlist soft-delete sync — Room migration 5→6 adds `deletedAt INTEGER NOT NULL DEFAULT 0`; `deletePlaylist()` writes `deletedAt = now()`; orchestrator push/pull respect it; UI queries filter `WHERE deletedAt = 0`

#### 7.2 — Bug fixes ✅

- ✅ Bug 1 (songCount always 0): `getAllPlaylistsWithCount()` DAO query joins `playlist_songs` and `COUNT`s; `PlaylistRepositoryImpl` delegates to it
- ✅ Bug 2 (raw timestamp): confirmed pre-fixed — `PlaylistListItem` already used `DateTimeFormatter`
- ✅ Bug 3 (Loading shows error icon): replaced `Icons.Default.Error` with `CircularProgressIndicator` in `PlaylistsScreenContent`
- ✅ Bug 4 (no trim): confirmed pre-fixed — dialogs already called `.trim()`; hardened `PlaylistViewModel` to also trim and reject blanks
- ✅ Bug 5 (empty copy): "No playlists found" → "No playlists yet"
- ✅ Bug 6 (rating downgrade): confirmed pre-fixed — `StarRating` already used `if (rating == star) NONE else star`
- ✅ Bug 7 (`NowPlayingScreenTest` package): moved from `ui.library` to `ui.player`
- ✅ Bug 8 (missing playlist event tests): 4 tests added; `FakePlaylistRepository` extended with `lastRemovedSongId`, `lastReorderedIds`, `reorderCallCount`

#### 7.3 — Library UX ✅

- ✅ Full-text search — `searchSongs` / `searchAlbums` / `searchArtists` DAO queries with `LIKE` predicates; `LibraryViewModel` switches via `flatMapLatest` on `_searchQuery`; `DockedSearchBar` in `LibraryScreen`
- ✅ Sort — `SortOrder` enum (`BY_TITLE`, `BY_ARTIST`, `BY_ALBUM`, `BY_DURATION`, `BY_DATE_ADDED`, `BY_PLAY_COUNT`); combined `flatMapLatest` on `_searchQuery + _sortOrder`; in-memory sort applied in `map`; `FilterChip` + `DropdownMenu` in Songs and Albums tabs
- ✅ Play count badge — `SongListItem` shows `"${playCount}×"` in muted color when `playCount > 0`

#### 7.4 — Architecture refactor ✅

- ✅ `clearQueue()` — exposed on `PlayerRepository` interface; `PlayerEvent.ClearQueue` added; "Clear" button in `QueueSheet`
- ✅ `LibraryViewModel` split — `MetadataViewModel` owns favorites/mostPlayed/recentlyPlayed and `MetadataUiState`; `LibraryUiState.Loaded` now carries only `songs`, `albums`, `artists`; `LibraryScreen` collects both ViewModels

#### 7.5 — Playback polish (deferred) ⏭️

Explicitly out of scope for Phase 7. Carries to Phase 8 backlog:
- Custom notification layout (`PlaybackNotificationManager` is still a stub)
- Slide/drag gesture for star rating
- Listening history detail screen

#### Known gaps from Phase 7

- **Dead code**: `songsFlowOld` and `albumsFlowOld` in `LibraryViewModel` — unused private flows left from an intermediate refactor; no behavioral impact, cleanup deferred
- **Sort for Artists tab**: not implemented — sort dropdown shown only on Songs and Albums tabs
- **Search test fidelity**: `FakeSongRepository.searchSongs` returns `songsFlow` unfiltered; search filtering is a DAO concern and requires an in-memory Room database to test at that level

---

### Phase 8: External Integrations (Planned)

**Prerequisite:** Phase 7 complete and stable. ✅ Requires designing a `SyncProvider` plugin interface before any implementation starts.

Before implementing integrations, the following Phase 7 deferred items and backlog should be reviewed:

**Backlog (prioritized for pre-Phase 8 cleanup):**

| Item | Priority | Effort | Source |
|------|----------|--------|--------|
| Listening history detail screen | Low | Medium | `improvements/plan.md` #2 |
| Slide/drag gesture for star rating | Low | Medium | `improvements/plan.md` #3 |
| Consolidate favorite/star rating | Low | Small | `improvements/plan.md` #4 — defer to dogfooding |
| Audio focus edge case testing (manual) | Medium | Medium | `improvements/plan.md` #7 |
| Custom notification layout | Low | Medium | `improvements/plan.md` #9 |
| Dead code cleanup in `LibraryViewModel` | Low | Trivial | Phase 7 design doc |
| Sort for Artists tab | Low | Small | Phase 7 design doc |
| Seek bar and star rating Compose UI tests | Low | Small | Testing design doc — deferred pending `semantics` tags |
| `LibraryScreenContent` search/sort UI tests | Low | Small | Testing design doc — deferred pending Material 3 API stability |

**Recommended order before starting integrations:**
1. Audio focus manual verification (#7) — find real bugs before complexity grows
2. Listening history screen (#2) — visible gap users will notice
3. Notification layout (#9) — polish users see every session
4. Rating gesture (#3) — quality-of-life, after dogfooding decides #4

#### MusicBee (Desktop)

Sync metadata with [MusicBee](https://getmusicbee.com/), a popular desktop music player for Windows.

- Import/export play counts, ratings, playlists, and favorites
- MusicBee stores its library in an XML database — read and write to this format
- Sync via shared Firestore backend or direct file-based sync (local network, shared cloud folder)
- Match songs between SyncPlayer and MusicBee by fingerprint or metadata (title + artist + album + duration)
- Handle mismatches gracefully (songs on one device but not the other)

#### YouTube Music (Online)

Sync metadata with YouTube Music so listening activity stays consistent across platforms.

- Pull liked songs, playlists, and play history from YouTube Music
- Push SyncPlayer favorites and playlists to YouTube Music
- Use YouTube Music APIs or unofficial libraries for data access
- Match songs between local library and YouTube Music catalog
- Periodic or on-demand sync with conflict resolution

#### Plugin Architecture

The integration layer should follow a provider pattern so additional players can be added without modifying core sync logic.

- Define a common `SyncProvider` interface (connect, pull metadata, push metadata, match songs)
- Each integration (MusicBee, YouTube Music, future players) implements this interface
- Users enable/disable providers in Settings
- Potential future providers: Spotify, Last.fm, Jellyfin, Navidrome, Plex, foobar2000, Winamp

---

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
- Unit: `LibraryViewModel` — favorites, mostPlayed, recentlyPlayed flow through to `LibraryUiState.Loaded` (now covered by `MetadataViewModelTest` after Phase 7 split)
- UI: `NowPlayingScreenContent` favorite button — emits `SetRating(FAVORITE)` when unrated; emits `SetRating(NONE)` when already favorited
- Fake: `FakeSongRepository` updated with stubs for all metadata methods

**Phase 6: Sync** ✅
- Unit: `SongFingerprintTest` — 11 tests (determinism, 16-char hex, normalisation, duration bucketing, uniqueness)
- Unit: `ConflictResolverTest` — 17 tests across `ResolveSongMetadata`, `RemotePlaylistWins`, `MergeHistoryEvent` (all resolution rules and edge cases)
- Unit: `SettingsViewModelTest` — 10 tests (all `SettingsEvent` types, all `SyncStatus` transitions, `lastSyncTime` derivation)
- Fake: `FakeAuthRepository` implements `AuthRepository` interface with observable call counts

**Phase 7: Polish** ✅
- Unit: `ConflictResolverTest` — 1 new test: `unknown fingerprint in remote is silently dropped`
- Unit: `SettingsViewModelTest` — 2 new tests: `SignInError event sets snackbarMessage`, `ClearSnackbar clears snackbarMessage`
- Unit: `PlaylistViewModelTest` — 4 new tests: `AddSongsToPlaylist`, `RemoveSongFromPlaylist`, `RemoveSongsFromPlaylist`, `ReorderSongs`; `FakePlaylistRepository` extended with `lastRemovedSongId`, `lastReorderedIds`, `reorderCallCount`
- Unit: `PlayerViewModelTest` — 1 new test: `ClearQueue event calls playerRepository.clearQueue`; `FakePlayerRepository` extended with `clearQueueCallCount`
- Unit: `LibraryViewModelTest` — 4 new tests: search-active state, search-cleared state, sort by play count descending, sort by date added descending
- Unit: `MetadataViewModelTest` (new, replaces `LibraryViewModelMetadataTest`) — 3 tests covering favorites, mostPlayed, recentlyPlayed flows in `MetadataViewModel`

**Testing pass — Post-Phase 7** ✅ (see [`docs/features/testing/design.md`](features/testing/design.md))

A dedicated audit and implementation pass closed all coverage gaps identified after Phase 7. Work was delivered in five tiers:

- **Tier 1 (infrastructure)**: Deleted empty `PlayerRepositoryTest.kt`; relocated `PlayerViewModelTest.kt` to correct `ui.player` package; sharpened blank-name test; restored commented-out error assertion via Turbine.
- **Tier 2 (unit test gaps)**: Added `getAllSongsCallCount`/`searchSongsCallCount` to `FakeSongRepository`; 4 new `PlayerViewModel` event tests (`AddToQueue`, `PlayNext`, `RemoveFromQueue`, `ReorderQueue`) + 3 `currentSongRating` tests; 3 search-routing tests and 6 sort-order tests in `LibraryViewModelTest`; 2 `onAppResumed` tests.
- **Tier 3 (Room DAO)**: `SongDaoTest` — 27 tests (all queries, search `LIKE` predicates, `upsertSongs` conflict behaviour, `getFavoriteSongs`, `getMostPlayedSongs`, `applySyncDelta`). `PlaylistDaoTest` — 19 tests (soft-delete filter, `getAllPlaylistsWithCount` JOIN, position ordering, clear/replace). Fixed `LibraryScreenTest` broken by Phase 7 signature changes.
- **Tier 4 (Compose UI)**: `NowPlayingScreenContentTest` expanded to 7 tests (play/pause, skip, back navigation, rating display); new `PlaylistsScreenContentTest` (4), `SettingsScreenContentTest` (8), `AlbumDetailScreenContentTest` (4), `ArtistDetailScreenContentTest` (4).
- **Tier 5 (integration)**: `FakeSyncRepository` created; `SyncOrchestratorTest` — 14 tests covering the signed-out guard, push delta detection, pull conflict application, remote playlist creation/skip-if-deleted, history merge, error status, and status transitions.

**Final test counts**: 110 JVM unit tests (`./gradlew test`); 78 instrumented tests (`./gradlew connectedAndroidTest`); 188 total. Instrumented tests compile cleanly and assertions have been audited against production source; execution requires a connected device or emulator.

### Test libraries

| Library | Purpose | Scope |
|---------|---------|-------|
| JUnit 5 | Test runner and assertions | `testImplementation` |
| Turbine | Testing Kotlin Flows (`StateFlow` assertions) | `testImplementation` |
| kotlinx-coroutines-test | Deterministic coroutine execution | `testImplementation` + `androidTestImplementation` |
| MockK 1.14.9 | Mocking concrete classes with Android dependencies (e.g. `SyncOrchestrator` + `Context`) | `testImplementation` |
| Compose UI Test (junit4) | `createAndroidComposeRule` for screen-level UI assertions | `androidTestImplementation` |
| Room Testing | `Room.inMemoryDatabaseBuilder` for DAO tests | `androidTestImplementation` |
| AndroidX Test (JUnit4 runner) | `@RunWith(AndroidJUnit4::class)` for instrumented tests | `androidTestImplementation` |

---

## CI/CD

GitHub Actions pipeline for automated builds, tests, and releases. **Not yet implemented** — all verification is manual with local Gradle commands.

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

---

## Key Libraries by Phase

| Phase | Libraries |
|-------|-----------|
| 1 — Library | Room, Coil (album art), Hilt, Activity Result API (permissions) |
| 2 — Playback | Media3 (ExoPlayer, Session, UI), kotlinx-coroutines-guava, reorderable |
| 3 — Navigation | (no new libraries) |
| 4 — Playlists | (Room — already added) |
| 5 — Metadata | (Room — already added) |
| 6 — Sync | Firebase Auth, Cloud Firestore, Google Play Services Auth, MockK (tests) |
| 7 — Polish | (no new libraries) |
| 8 — External Integrations | XML parsing (kotlinx.serialization or built-in), Retrofit (YouTube Music API) |
| Testing (all phases) | JUnit 5, Turbine, kotlinx-coroutines-test, Compose UI Test, Hilt Testing |

---

## Future Improvements

Features to consider after the core phases and external integrations are complete, ordered by priority.

1. **Android Auto support** — Expose the media library and playback controls to Android Auto. `MediaSession` and `MediaBrowserService` (already used for notification controls) get most of the way there; the remaining work is declaring Auto support in the manifest and ensuring the browse tree is well-structured.
2. **Gapless playback / crossfade** — Seamless transitions between tracks. Media3 supports gapless natively; crossfade requires mixing two player instances or using audio effects.
3. **Sleep timer** — Stop playback after a user-defined duration or at end of current track/album. Simple countdown timer that pauses the player.
4. **Equalizer** — Built-in audio equalizer and presets. Media3 supports Android's `AudioEffect` APIs (EQ, bass boost, virtualizer).
5. **Home screen widget** — Glance (Jetpack) widget showing current track with play/pause, next, previous controls.
6. **Lyrics display** — Show lyrics on the now-playing screen. Read embedded lyrics from audio file tags first, with optional fetch from an external lyrics API.
7. **Last.fm scrobbling** — Track listening history on Last.fm. Fits naturally into the plugin architecture as another `SyncProvider` implementation.
