# SyncPlayer

A local audio player for Android that syncs listening metadata — play counts, ratings, playlists, and history — across devices via the cloud.

## Features

| Phase | Feature | Status |
|-------|---------|--------|
| 1 | Library browsing (songs, albums, artists) | ✅ Done |
| 2 | Background playback, queue, audio focus | ✅ Done |
| 3 | Album and artist detail screens | ✅ Done |
| 4 | Playlist management | ✅ Done |
| 5 | Play counts, star ratings, listening history | ✅ Done |
| 6 | Cross-device sync via Firebase | 🔜 Planned |

## Tech Stack

- **Language:** Kotlin 2.2.10
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM, single Activity, Hilt DI
- **Playback:** Media3 ExoPlayer with `MediaSessionService`
- **Database:** Room
- **Min SDK:** 34 | **Target SDK:** 36

## Building

```bash
# Debug APK
.\gradlew.bat assembleDebug

# Unit tests
.\gradlew.bat test

# Instrumented tests (device/emulator required)
.\gradlew.bat connectedAndroidTest
```

## Documentation

- [`CLAUDE.md`](CLAUDE.md) — project conventions and AI instructions
- [`docs/architecture.md`](docs/architecture.md) — MVVM pattern and project structure
- [`docs/style-guide.md`](docs/style-guide.md) — coding conventions
- [`docs/dependencies.md`](docs/dependencies.md) — tech stack and compatibility notes
- [`docs/features/`](docs/features/) — per-feature plans and design docs