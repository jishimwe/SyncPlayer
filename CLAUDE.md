# SyncPlayer

Local audio player for Android that syncs metadata (play counts, playlists, favorites) across devices.

## Vision

SyncPlayer is a polished local music player where users' listening data stays consistent across their devices. The app plays audio from on-device storage and syncs metadata — not the audio files themselves — so playlists, play counts, and preferences carry over seamlessly.

### Core Features (Planned)

- Local audio playback (on-device music library)
- Library browsing (songs, albums, artists, playlists)
- Playlist creation and management
- Play count and listening history tracking
- Cross-device metadata sync (playlists, play counts, favorites)
- Now-playing controls with notification and lock screen support

## Project Info

- **Package**: `com.jpishimwe.syncplayer`
- **Min SDK**: 34 | **Target SDK**: 36
- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose + Material 3
- **Build**: Gradle 9.1.0, Kotlin DSL, version catalog (`gradle/libs.versions.toml`)

## Build Commands

```bash
gradlew.bat assembleDebug        # Build debug APK
gradlew.bat test                 # Unit tests
gradlew.bat connectedAndroidTest # Instrumented tests
```

## Architecture — MVVM

Single Activity, Jetpack Compose, MVVM pattern.

```
app/src/main/java/com/jpishimwe/syncplayer/
├── ui/           # Screens, composables, ViewModels
│   ├── theme/    # Color, Theme, Type
│   └── screens/  # Feature screens (library, player, playlists, etc.)
├── data/         # Repositories, local DB, sync logic
│   ├── local/    # Room database, DAOs
│   └── sync/     # Metadata sync service
├── model/        # Data classes (Song, Playlist, Album, Artist)
└── di/           # Dependency injection setup
```

- **UI layer**: Compose screens observe ViewModel state via `StateFlow`
- **ViewModel**: Holds UI state, calls into repositories
- **Repository**: Single source of truth, coordinates local DB and sync
- **Local DB**: Room for songs, playlists, play counts, metadata

## Coding Guidelines

- **Compose-first UI** — no XML layouts, no Fragments
- **State hoisting** — composables receive state and events as parameters; ViewModels own the state
- **Unidirectional data flow** — State flows down, events flow up
- **Naming**:
  - Composables: `PascalCase` (e.g., `NowPlayingScreen`, `SongListItem`)
  - ViewModels: `FeatureNameViewModel` (e.g., `LibraryViewModel`)
  - Repositories: `FeatureNameRepository` (e.g., `PlaylistRepository`)
  - State classes: `FeatureNameUiState` (e.g., `LibraryUiState`)
- **Package by feature** over package by layer where possible
- **Prefer `StateFlow`** over `LiveData` for reactive state
- **Coroutines** for async work — no callbacks, no RxJava
- Keep composables small and focused; extract reusable components

## Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) format.

```
<type>(<scope>): <subject>

<body>
```

### Types

| Type | When to use |
|------|-------------|
| `feat` | New feature or user-facing behavior |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `test` | Adding or updating tests |
| `chore` | Maintenance (dependencies, config, tooling) |
| `build` | Build system or dependency changes |
| `ci` | CI/CD pipeline changes |

### Scopes

Use the feature area as scope: `library`, `playback`, `playlists`, `metadata`, `sync`, `ui`, `db`, `nav`, `settings`. Scope is optional for cross-cutting changes.

### Rules

- Subject line in **imperative mood** ("Add feature" not "Added feature")
- Subject line **50 characters or less** (72 max)
- No period at the end of the subject line
- Blank line between subject and body
- Body wraps at **72 characters**
- Body explains **what and why**, not how
- Use `BREAKING CHANGE:` in the body or `!` after type/scope for breaking changes

### Examples

```
feat(library): scan device for audio files

Query MediaStore for audio files and display them in a
scrollable list with title, artist, and duration.
```

```
fix(playback): prevent crash when audio focus is lost
```

```
docs: add project plan and architecture overview
```

## Documentation

Project documentation lives in `docs/` at the project root.

```
docs/
└── features/   # Feature planning docs (one file per feature)
```

- Feature plans go in `docs/features/` (e.g., `docs/features/library-browsing.md`)
- Use these docs to capture requirements, design decisions, and scope before implementation

## Instructions for AI

- **Always enter plan mode first** before implementing any task. Explore the codebase, design the approach, and get approval before writing code. No exceptions.
- When planning a new feature, write the plan to `docs/features/<feature-name>.md` before implementing.
- Before implementing any feature, read its doc in `docs/features/` first. If no doc exists, create one during plan mode.
- Read relevant files before making changes. Do not guess at code structure.
- Follow the MVVM pattern described above. Do not introduce other patterns.
- Keep changes minimal and focused on what was asked. No drive-by refactors.
- When adding a new feature, place files in the correct package per the architecture above.
- Add new dependencies to `gradle/libs.versions.toml` — never hardcode versions in `build.gradle.kts`.
- Prefer extending existing code over creating new files when reasonable.
- Write idiomatic Kotlin: use data classes, sealed classes/interfaces for state, extension functions where natural.
- Do not add comments for self-explanatory code. Only comment non-obvious logic.
