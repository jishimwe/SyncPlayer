# SyncPlayer — Project Plan

A polished local music player for Android that syncs metadata (play counts, playlists, favorites) across devices.

SyncPlayer plays audio from on-device storage and syncs listening data — not the audio files — so playlists, play counts, and preferences carry over seamlessly between a user's devices.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM, single Activity |
| Local DB | Room |
| Playback | Media3 (ExoPlayer) |
| DI | Hilt |
| Auth | Firebase Auth (Google sign-in) |
| Sync | Cloud Firestore |
| Build | Gradle 9.1, Kotlin DSL, version catalog |

## Architecture

Single Activity, Jetpack Compose, MVVM pattern.

```
app/src/main/java/com/jpishimwe/syncplayer/
├── ui/           # Screens, composables, ViewModels
│   ├── theme/    # Color, Theme, Type
│   └── screens/  # Feature screens
├── data/         # Repositories, local DB, sync logic
│   ├── local/    # Room database, DAOs
│   └── sync/     # Metadata sync service
├── model/        # Data classes (Song, Playlist, Album, Artist)
└── di/           # Hilt modules
```

- **UI layer**: Compose screens observe ViewModel state via `StateFlow`
- **ViewModel**: Holds UI state, calls into repositories
- **Repository**: Single source of truth, coordinates local DB and sync
- **Local DB**: Room for songs, playlists, play counts, metadata

## Navigation

Bottom navigation bar with three top-level destinations:

- **Library** — Browse songs, albums, artists
- **Playlists** — User-created playlists
- **Settings** — App preferences, account, sync status

A persistent mini now-playing bar sits above the bottom nav (added in Phase 6).

## Build Phases

Phases are ordered from simple to complex. Each phase builds on the previous one.

### Phase 1: Library

Scan the device for audio files and display them in a browsable list.

- Query `MediaStore` for audio files on device
- Display songs in a scrollable list (title, artist, duration)
- Browse by songs, albums, artists (tabbed or segmented)
- Room database to cache the scanned library
- Handle runtime permissions (`READ_MEDIA_AUDIO`)

### Phase 2: Playback

Play audio using Media3 with full media session support.

- Media3 `ExoPlayer` setup with `MediaSession`
- Now-playing screen (album art, track info, seek bar, controls)
- Play/pause, next, previous, seek
- Notification controls with `MediaSessionService`
- Lock screen controls
- Background playback

### Phase 3: Playlists

Create, edit, and manage playlists stored locally.

- Room entities for playlists and playlist-song associations
- Create / rename / delete playlists
- Add / remove / reorder songs in a playlist
- Playlist detail screen
- Bottom navigation wired up

### Phase 4: Metadata Tracking

Track listening data locally in Room.

- Play count per song (increment on completion or threshold)
- Favorites (toggle per song)
- Listening history (timestamped log of played tracks)
- Surface stats in the UI (most played, recently played, favorites list)

### Phase 5: Sync

Sync metadata across devices using Firebase.

- Firebase Auth with Google sign-in
- Firestore data model for playlists, play counts, favorites, history
- Sync service: upload local changes, pull remote changes
- Conflict resolution (last-write-wins or merge strategy)
- Offline support (Firestore offline persistence)
- Sync status indicator in Settings

### Phase 6: Polish

UI refinements and quality-of-life features.

- Mini now-playing bar (persistent above bottom nav)
- Search across songs, albums, artists
- Sorting and filtering (by name, date added, play count)
- Settings screen (theme, playback preferences, account management)
- Shuffle and repeat modes
- Queue management

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
- Composables receive state as parameters — testable in isolation without ViewModels
- Suspend functions and Flows use `kotlinx-coroutines-test` for deterministic execution

### Tests per phase

**Phase 1: Library**
- Unit: `LibraryViewModel` state transitions (loading → loaded, empty state, error)
- Unit: Song mapping logic (MediaStore cursor → Song model)
- Room: Song DAO queries (insert, query by album/artist, search)
- UI: Song list renders correctly, tab switching works

**Phase 2: Playback**
- Unit: `PlaybackViewModel` state (play/pause toggling, track advancement, seek)
- Unit: Queue logic (next, previous, shuffle ordering)
- UI: Now-playing screen displays correct track info, controls respond

**Phase 3: Playlists**
- Unit: `PlaylistViewModel` CRUD operations reflected in state
- Room: Playlist DAO (create, rename, delete, add/remove/reorder songs)
- UI: Create playlist flow, add song to playlist

**Phase 4: Metadata Tracking**
- Unit: Play count increment logic (threshold-based counting)
- Room: Metadata DAO (play counts, favorites toggle, history insertion and queries)
- UI: Favorites toggle updates immediately

**Phase 5: Sync**
- Unit: Conflict resolution logic (last-write-wins, merge)
- Unit: Sync state machine (idle → syncing → success/error)
- Integration: `SyncProvider` implementations against fake/mock backends
- Integration: Merge scenarios (local-only changes, remote-only, conflicts)

**Phase 6: Polish**
- UI: Search returns correct results, sorting changes list order
- UI: Mini player bar appears when a track is playing

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
| 1 — Library | Room, Coil (album art), Hilt, Accompanist Permissions |
| 2 — Playback | Media3 (ExoPlayer, Session, UI) |
| 3 — Playlists | (Room — already added) |
| 4 — Metadata | (Room — already added) |
| 5 — Sync | Firebase Auth, Cloud Firestore, Google Play Services Auth |
| 6 — Polish | (no new major libraries expected) |
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
