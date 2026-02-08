# SyncPlayer

Local audio player for Android that syncs metadata (play counts, playlists, favorites) across devices.

## Quick Reference

**Before starting work, read the relevant docs:**
- 🏗️ **New feature?** → Read `docs/process.md` first
- 📦 **Adding dependencies?** → Read `docs/dependencies.md` first
- 💻 **Writing code?** → Reference `docs/style-guide.md`
- 🏛️ **Architecture questions?** → Read `docs/architecture.md`

## Project Essentials

- **Package**: `com.jpishimwe.syncplayer`
- **Min SDK**: 34 | **Target SDK**: 36
- **Language**: Kotlin 2.2.10 (bundled by AGP 9)
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM, single Activity
- **Build**: AGP 9.0.0, Gradle 9.1.0, Kotlin DSL

## Core Tech Stack

| Category | Library | Version |
|----------|---------|---------|
| DI | Hilt | 2.59 |
| DB | Room | 2.7.1 |
| KSP | KSP | 2.3.5 |
| Images | Coil | 3.1.0 |
| Navigation | Navigation Compose | 2.9.0 |
| Tests | JUnit 5 + Turbine | 5.11.4 / 1.2.1 |

**See `docs/dependencies.md` for full stack details and AGP 9 compatibility notes.**

## Build Commands

```bash
gradlew.bat assembleDebug        # Build debug APK
gradlew.bat test                 # Unit tests
gradlew.bat connectedAndroidTest # Instrumented tests
gradlew.bat --stop               # Kill daemons (file locks)
```

## Project Structure

```
app/src/main/java/com/jpishimwe/syncplayer/
├── ui/             # Screens, composables, ViewModels
│   ├── theme/      # Color, Theme, Type
│   ├── components/ # Reusable UI components
│   └── <feature>/  # Feature packages (library, player, etc.)
├── data/           # Repositories, local DB
│   └── local/      # Room database, DAOs
├── model/          # Data classes (Song, Album, Artist)
└── di/             # Hilt modules
```

## Quick Guidelines

### Naming
- Composables: `PascalCase` (e.g., `LibraryScreen`)
- ViewModels: `FeatureViewModel` (e.g., `LibraryViewModel`)
- Repositories: `FeatureRepository` (e.g., `SongRepository`)
- State: `FeatureUiState` (e.g., `LibraryUiState`)

### Patterns
- **State hoisting**: ViewModels own state, composables receive it
- **Testable composables**: Every `FooScreen()` needs `FooScreenContent()` for testing
- **StateFlow over LiveData**: Always use `StateFlow` for reactive state
- **Sealed interfaces**: For state variants and events

### File Organization
- One top-level composable per file
- ViewModels in same package as screen
- Max 300 lines per file

**See `docs/style-guide.md` for complete coding standards.**

## Commit Format

```
<type>(<scope>): <subject>

<body>
```

**Types**: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`  
**Scopes**: `library`, `playback`, `playlists`, `metadata`, `sync`, `ui`, `db`, `nav`, `settings`

**Examples:**
```
feat(library): add artist filtering to song list
fix(playback): prevent crash on missing audio file
docs(library): add album view feature plan
```

## Documentation Structure

```
CLAUDE.md              # This file
docs/
├── process.md         # Implementation workflow
├── architecture.md    # MVVM pattern details
├── dependencies.md    # Tech stack & compatibility
├── style-guide.md     # Coding conventions
└── features/
    └── <feature-name>/
        ├── plan.md    # Before implementation
        └── design.md  # After implementation
```

## AI Instructions

### Start Here

1. **Clarify first**: If request is ambiguous, ask questions before proceeding
2. **Read relevant docs**: Check the list at the top for what to read
3. **Follow the process**: See `docs/process.md` for step-by-step workflow

### Key Principles

- **Read before changing**: Always examine existing code first
- **Incremental builds**: Build after each layer (deps → models → VM → UI → tests)
- **Explain as you go**: Before each layer, briefly explain what and why
- **Stay focused**: No drive-by refactors, only requested changes
- **Use version catalog**: Never hardcode dependency versions

### When to Stop and Ask

- Request is ambiguous or has multiple valid interpretations
- Multiple approaches exist (present options with trade-offs)
- Design decision will significantly affect future features
- Unsure about scope boundaries

### Quality Checklist

Before marking any work complete:
- ✅ `assembleDebug` succeeds
- ✅ `test` passes
- ✅ Follows naming conventions
- ✅ Testable composable pattern used
- ✅ No hardcoded strings
- ✅ Design doc complete (if applicable)

## Current Status

- **Phase**: Early development
- **Focus**: Core music playback and library features
- **Next up**: Media scanning, playback service

---

**📚 Remember**: This file is just the overview. Read the specific docs before starting work!