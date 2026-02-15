# Phase 2: Playback - Design

## Overview

A complete audio playback system built on Media3 ExoPlayer with background playback via `MediaSessionService`, notification and lock screen controls via `MediaSession`, queue management with drag-and-drop reordering, and custom audio focus handling with smooth volume fades. The system follows MVVM with a `PlayerRepository` interface bridging the UI layer to the `PlaybackService` through a `MediaController`.

## What was built

### Service layer

- `service/PlaybackService.kt`: `MediaSessionService` that initializes ExoPlayer with manual audio focus (disabled built-in handling), creates a `MediaSession`, and registers `AudioFocusHandler` and `BecomingNoisyReceiver`. `BecomingNoisyReceiver` is a private inner class that pauses playback on headphone disconnect.
- `service/AudioFocusHandler.kt`: Handles audio focus lifecycle with coroutine-based volume fades (500ms fade in/out, 20 steps). Supports delayed focus grants (`AUDIOFOCUS_REQUEST_DELAYED`), transient loss with resume, permanent loss with abandon, and ducking to 10% volume.

### Model layer

- `model/PlaybackState.kt`: `PlaybackState` enum (IDLE, BUFFERING, PLAYING, PAUSED, ENDED, ERROR) and `RepeatMode` enum (OFF, ONE, ALL).
- `model/QueueItem.kt`: Wraps a `Song` with a UUID for stable identity during reordering.
- `model/PlayerUiState.kt`: Single state class holding current song, playback state, position, duration, queue, shuffle/repeat, and error.
- `model/Song.kt`: Extended with `toMediaItem()` and `MediaItem.toSong()` conversion functions.

### Data layer

- `data/PlayerRepository.kt`: Interface defining all playback and queue operations.
- `data/PlayerRepositoryImpl.kt`: Implementation connecting to `PlaybackService` via `MediaController`. Bridges `Player.Listener` callbacks to a `MutableStateFlow<PlayerUiState>`. Handles queue persistence through `QueueDao` and queue restore on initialization.
- `data/local/QueueDao.kt`: Room DAO with `QueueEntity` for persisting queue order across app restarts.
- `data/local/SyncPlayerDatabase.kt`: Updated to include `QueueEntity` and expose `QueueDao`.

### DI layer

- `di/AppModule.kt`: Binds `PlayerRepository` to `PlayerRepositoryImpl`.
- `di/DatabaseModule.kt`: Provides `QueueDao` singleton.

### ViewModel layer

- `ui/player/PlayerViewModel.kt`: Exposes repository's `StateFlow` via `stateIn(WhileSubscribed(5000))`. Routes `PlayerEvent` sealed interface events to repository. Provides `formatTime()` utility.
- `ui/player/PlayerEvent.kt`: Sealed interface with events: `PlayPause`, `SkipToNext`, `SkipToPrevious`, `SeekTo`, `ToggleShuffle`, `ToggleRepeat`, `PlaySongs`, `AddToQueue`, `PlayNext`, `RemoveFromQueue`, `ReorderQueue`.

### UI layer

- `ui/player/NowPlayingScreen.kt`: Composable wrapper collecting ViewModel state, delegating to `NowPlayingScreenContent`.
- `ui/player/NowPlayingScreenContent.kt`: Full-screen player with album artwork, track info, seek bar, shuffle/repeat toggles, and playback controls.
- `ui/player/components/PlayerControls.kt`: Previous / Play-Pause / Next button row.
- `ui/player/components/SeekBar.kt`: Slider with temporary drag position and formatted time labels.
- `ui/player/components/MiniPlayer.kt`: Compact bottom bar with album art, song info, play/pause, and skip next.
- `ui/player/components/QueueSheet.kt`: `ModalBottomSheet` with `LazyColumn` and `reorderable` library for drag-and-drop queue management.

### Navigation

- `ui/navigation/NavGraph.kt`: `NavHost` with Library (start) and NowPlaying routes. Sealed `Screen` class for route constants.
- `ui/SyncPlayerApp.kt`: Root composable now hosts `NavGraph` instead of hardcoded `LibraryScreen`.
- `ui/library/LibraryScreen.kt`: Integrates `MiniPlayer` in `Scaffold.bottomBar` and `PermissionHandler` (moved from `SyncPlayerApp`).

### Tests

- `test/data/FakePlayerRepository.kt`: Test double recording all method calls for assertion.
- `test/ui/library/PlayerViewModelTest.kt`: Unit tests for event routing (play/pause toggle, skip, seek, shuffle, repeat, playSongs) and `formatTime`.
- `test/ui/library/NowPlayingScreenTest.kt`: Compose UI tests for song display and control button events.
- `androidTest/ui/library/LibraryScreenTest.kt`: Instrumented tests for library screen with MiniPlayer integration.

### Configuration

- `gradle/libs.versions.toml`: Media3 1.9.2, `kotlinx-coroutines-guava` 1.10.2, `reorderable` 3.0.0.
- `AndroidManifest.xml`: `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK` permissions. `PlaybackService` declared with `mediaPlayback` foreground service type.

## Design decisions

- **Interface-based PlayerRepository**: The plan had `PlayerRepository` as a concrete class. Implementation extracted an interface (`PlayerRepository`) with a separate `PlayerRepositoryImpl` to enable testability via `FakePlayerRepository` without mocking frameworks.

- **Manual audio focus over ExoPlayer built-in**: Disabled ExoPlayer's audio focus handling (`setAudioAttributes(..., false)`) to implement custom coroutine-based volume fades. This adds ~150 lines but provides smooth transitions instead of abrupt volume changes. The `FadeConfig` data class exists in `AudioFocusHandler.kt` for future configurability but is not yet wired up.

- **Ducking at 10% instead of 20%**: The plan specified ducking to 20% volume; implementation uses 10% (`player.volume = 0.1f`) and applies it instantly rather than fading. This is simpler and matches the behavior of most Android music players for brief interruptions.

- **Simplified MediaSession callback**: The plan included a custom `MediaSession.Callback` with `onConnect` and `onCustomCommand`. The implementation uses the default `MediaSession.Builder(this, player).build()` without a custom callback, since queue management is handled entirely through `MediaController` commands rather than custom session commands. The commented-out callback code remains as a reference for future custom command needs.

- **BecomingNoisyReceiver as private inner class**: The plan had this as a separate file. The implementation keeps it as a private class within `PlaybackService.kt` since it's tightly coupled to the service and only ~10 lines. Note: the receiver is instantiated but not dynamically registered/unregistered with `IntentFilter` — Media3's `MediaSessionService` handles this automatically.

- **Activity-scoped ViewModel via `ViewModelStoreOwner`**: Both `LibraryScreen` and `NowPlayingScreen` use `hiltViewModel(LocalActivity.current as ViewModelStoreOwner)` to share the same `PlayerViewModel` instance, ensuring playback state is consistent across navigation.

- **Position updates in repository, not ViewModel**: The plan placed `startPositionUpdates()` in the repository with a `repositoryScope`. The implementation has this method but it is not called from `initialize()` — position updates rely on `Player.Listener` callbacks flowing through the `StateFlow`. A `positionUpdateFlow` property exists in the ViewModel as a polling fallback but is currently unused.

- **Media3 1.9.2 instead of 1.5.0**: The plan targeted Media3 1.5.0; the actual implementation uses 1.9.2 (latest stable at time of implementation) for better AGP 9 compatibility.

- **Reorderable 3.0.0 instead of 2.4.0**: Upgraded from the planned 2.4.0 to 3.0.0 for the latest API and Compose compatibility.

## Known gaps

- **Position polling not active**: `startPositionUpdates()` in `PlayerRepositoryImpl` is defined but never called. The seek bar position currently only updates on `Player.Listener` callbacks (media transitions, state changes), not on a 1-second tick. This means the seek bar may not update smoothly during playback.
- **PlaybackNotificationManager**: A stub file exists (`service/PlaybackNotificationManager.kt`) but is empty. Media3's `MediaSessionService` handles basic notification automatically, so this is only needed for custom notification layouts.
- **Repository unit tests**: `PlayerRepositoryTest.kt` exists but is empty. `MediaController` requires a running `PlaybackService`, making pure unit tests impractical. Queue reorder helpers (`moveUp`/`moveDown`) are testable but not yet covered.
- **Manual testing incomplete**: Audio focus fade behavior with phone calls, Bluetooth controls, and end-to-end background playback have not been verified on device (see plan Phase 2.4 checklist).
- **No `clearQueue` in interface**: `PlayerRepositoryImpl` has a private `clearQueue()` method but it's not exposed on the `PlayerRepository` interface.
