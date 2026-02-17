# Playlists — Design

## Overview

Added playlist management to SyncPlayer: users can create, rename, and delete playlists; add, remove, and drag-reorder songs within them; and tap a song in a playlist to start playback from that position. A bottom navigation bar switches between the Library and Playlists tabs, with the MiniPlayer stacked below it on top-level screens.

## What was built

### Data layer

- `data/local/PlaylistDao.kt`: `PlaylistEntity`, `PlaylistSongCrossRef` entities and `PlaylistDao` (9 methods). No `@ForeignKey` — deletion is handled manually in the repository. Song retrieval uses `INNER JOIN` ordered by `position ASC`.
- `model/Playlist.kt`: Domain model with `id`, `name`, `createdAt`, `songCount`.
- `data/PlaylistRepository.kt`: Interface — `Flow` reads, `suspend` writes.
- `data/PlaylistRepositoryImpl.kt`: Thin delegation over `PlaylistDao`. `deletePlaylist` clears songs then deletes the playlist. `addSongToPlaylist` appends at `getSongCountForPlaylist().first()`. `reorderSongs` uses clear-then-replace.
- `data/local/SyncPlayerDatabase.kt`: Added `PlaylistEntity` and `PlaylistSongCrossRef` to entities, bumped version to 3 (destructive migration).
- `di/DatabaseModule.kt`: Added `providePlaylistDao`.
- `di/AppModule.kt`: Added `@Binds` for `PlaylistRepository`.

### ViewModel layer

- `ui/playlists/PlaylistEvent.kt`: 7 event variants — `CreatePlaylist`, `RenamePlaylist`, `DeletePlaylist`, `AddSongsToPlaylist`, `RemoveSongFromPlaylist`, `RemoveSongsFromPlaylist`, `ReorderSongs`.
- `ui/playlists/PlaylistUiState.kt`: `Loading` and `Loaded(playlists)`.
- `ui/playlists/PlaylistViewModel.kt`: Injects both `PlaylistRepository` and `SongRepository`. Exposes `uiState` via `map` + `stateIn`, `getSongsForPlaylist()` and `getAllSongs()` as raw flows for detail/picker screens. `CreatePlaylist` and `RenamePlaylist` guard against blank names.

### UI layer

- `ui/playlists/PlaylistsScreen.kt`: Outer shell — activity-scoped ViewModel, collects state, wires callbacks.
- `ui/playlists/PlaylistsScreenContent.kt`: Scaffold with TopAppBar + FAB. Contains `PlaylistListItem`, `CreatePlaylistDialog`, `RenamePlaylistDialog`, `DeletePlaylistDialog`, and `EmptyState` composables. Dialog state managed via local `remember` vars.
- `ui/playlists/PlaylistDetailScreen.kt`: Outer shell + `PlaylistDetailScreenContent`. Uses `rememberReorderableLazyListState` for drag-to-reorder. FAB opens song picker. Song tap fires `PlayerEvent.PlaySongs` then navigates to NowPlaying.
- `ui/playlists/PlaylistSongItem.kt`: Drag handle + album art + title/artist + delete button. Visual elevation feedback when dragging.
- `ui/playlists/SongPickerSheet.kt`: `ModalBottomSheet` with checkboxes. Pre-checks songs already in playlist. On confirm, computes added and removed sets and dispatches both `AddSongsToPlaylist` and `RemoveSongsFromPlaylist` events.

### Navigation

- `ui/navigation/NavGraph.kt`: Added `Screen.Playlists` and `Screen.PlaylistDetail` routes. Added `BottomNavDestination` enum (Library, Playlists). Bottom bar is a `Column` of `NavigationBar` + `MiniPlayer`, shown only on top-level routes. Navigation uses `popUpTo` + `saveState` + `restoreState` for tab switching.

### Tests

- `test/data/FakePlaylistRepository.kt`: `MutableStateFlow`-backed fake with call counters and argument recording. `createPlaylist` mutates the flow for state-based assertions.
- `test/ui/playlists/PlaylistViewModelTest.kt`: 7 JUnit 5 tests covering state transitions, event dispatch, and blank-name guards.

## Design decisions

- **Activity-scoped `PlaylistViewModel`**: Shared across `PlaylistsScreen` and `PlaylistDetailScreen` via `hiltViewModel(LocalActivity.current)`. Avoids creating separate ViewModels for the list and detail screens, since both need the same repository and state.

- **`SongPickerSheet` computes add/remove diffs**: Rather than replacing the entire song list, the picker tracks which songs were toggled on and off and dispatches separate `AddSongsToPlaylist` and `RemoveSongsFromPlaylist` events. This allows removing songs from the picker without navigating back to the detail screen.

- **`RemoveSongsFromPlaylist` added beyond plan**: The plan had only `RemoveSongFromPlaylist` (single). A batch `RemoveSongsFromPlaylist` variant was added to support the picker's diff-based removal, keeping the event dispatch simple.

- **`PlaylistSongItem` extracted to own file**: The plan placed it inside `PlaylistDetailScreen.kt`, but extracting it follows the style guide's "one top-level composable per file" rule and keeps `PlaylistDetailScreen.kt` under 300 lines.

- **`PlaylistsScreenContent` split from `PlaylistsScreen`**: The plan listed them in one file, but the implementation correctly separates the 24-line outer shell from the 272-line content composable for clarity.

- **Clear-then-replace for reorder**: `reorderSongs` deletes all `playlist_songs` rows and re-inserts with new positions. Simple and correct given no foreign keys; avoids complex position-swapping SQL.

- **No cascading foreign keys**: Manual two-step deletion (`clearPlaylistSongs` then `deletePlaylist`) follows the existing pattern from `QueueDao`. Keeps entities simple and avoids Room FK constraint complexity.

- **Bottom nav visibility**: `NavigationBar` is shown only when `currentRoute` is `Library` or `Playlists`. Hidden on all detail and NowPlaying screens, matching standard Android UX.

## Known gaps

- **`songCount` never populated**: `PlaylistRepositoryImpl.getAllPlaylists()` maps entities without querying `getSongCountForPlaylist`, so `Playlist.songCount` is always 0. The `PlaylistListItem` displays it but it shows "0" for all playlists.
- **No `.trim()` on playlist names**: The plan called for trimming whitespace in `createPlaylist` and `renamePlaylist`, but the implementation passes names through as-is.
- **`createdAt` displayed as raw Long**: `PlaylistListItem` shows the epoch millisecond in the overline — needs date formatting.
- **`Loading` state shows Error icon**: `PlaylistsScreenContent` renders `Icons.Default.Error` for the `Loading` state — a placeholder, not intentional UX.
- **Empty state text differs from plan**: Shows "No playlists found" instead of the planned "No playlists yet".
- **No tests for add/remove/reorder events**: `FakePlaylistRepository` has call counters for `addSongToPlaylist`, `removeSongFromPlaylist`, and `reorderSongs`, but no tests exercise them.
