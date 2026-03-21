# Playlists — Bugfix & Improvement Plan (Complete)

## Context

Phase 4 implemented playlist management end-to-end. Post-implementation analysis of `PlaylistsTabScreen.kt` and all related layers revealed several bugs, data integrity gaps, and UX issues. This plan addresses them in priority order.

## Bugs

### BUG 1 (Critical): Playlist info always shows "0 Songs • 0:00"

**Symptom**: Every playlist row in `PlaylistItem` displays `"0 Songs • 0:00"` regardless of how many songs are in the playlist.

**Root cause**: `PlaylistRepositoryImpl.getAllPlaylists()` (line 18-21) re-maps `Playlist` objects returned by `getAllPlaylistsWithCount()`, discarding `songCount` and `totalDuration`:

```kotlin
// BROKEN — drops songCount & totalDuration, they default to 0
playlistDao.getAllPlaylistsWithCount().map { entities ->
    entities.map { Playlist(it.id, it.name, it.createdAt) }
}
```

The DAO query `getAllPlaylistsWithCount()` correctly computes both fields via SQL aggregation (`COUNT`, `SUM`), but the repository throws them away by constructing new `Playlist` instances with only 3 fields.

**Fix**: Return the DAO flow directly — the query already returns `Playlist` UI model objects, no mapping needed.

**Files**: `data/PlaylistRepositoryImpl.kt`

---

### BUG 2 (Medium): Create playlist always hardcodes name "New Playlist"

**Symptom**: Tapping the create button always creates a playlist named "New Playlist" with no user input.

**Root cause**: `PlaylistsTabScreen.kt` line 78:
```kotlin
onCreatePlaylist = { viewModel.onEvent(PlaylistEvent.CreatePlaylist("New Playlist")) }
```

There is no dialog prompting the user for a name. The existing `RenamePlaylistDialog` pattern should be reused for creation.

**Fix**: Add a `CreatePlaylistDialog` (reuse `RenamePlaylistDialog` pattern) and wire it through `PlaylistsTabScreenContent`. Change `onCreatePlaylist: () -> Unit` to `onCreatePlaylist: (String) -> Unit`.

**Files**: `ui/home/tabs/PlaylistsTabScreen.kt`

---

### BUG 3 (Medium): Duplicate songs can be added to a playlist

**Symptom**: Adding the same song twice to a playlist creates two `PlaylistSongCrossRef` rows with different auto-generated IDs. The song appears twice in the playlist.

**Root cause**: `PlaylistSongCrossRef` uses `@PrimaryKey(autoGenerate = true)` and there is no unique constraint on `(playlistId, songId)`. The `OnConflictStrategy.REPLACE` on `addSongToPlaylist` replaces on primary key (the auto-generated `id`), not on the logical key.

**Fix**: Add a check-before-insert in `PlaylistRepositoryImpl.addSongToPlaylist()` — query existing songs and skip if already present. A schema-level unique index would be cleaner but requires a DB migration, so we use the repository guard for now.

**Files**: `data/PlaylistRepositoryImpl.kt`, `data/local/PlaylistDao.kt` (add `isSongInPlaylist` query)

---

### BUG 4 (Low): `reorderSongs` is not atomic

**Symptom**: If the app crashes between `clearPlaylistSongs()` and `replacePlaylistSongs()`, all songs are lost from the playlist.

**Root cause**: Two separate DAO calls without `@Transaction`.

**Fix**: Add a `@Transaction` annotated function in the DAO that wraps both operations.

**Files**: `data/local/PlaylistDao.kt`

---

## Gaps / Improvements

### GAP 1: No empty state when playlist list is empty

**Current**: Empty `LazyColumn` with only the action bar — looks broken.

**Fix**: Show an empty-state message (icon + text) when `playlists.isEmpty()`.

**Files**: `ui/home/tabs/PlaylistsTabScreen.kt`

---

### GAP 2: Unused `PlaylistState` sealed interface (dead code)

**Current**: `PlaylistViewModel.kt` lines 21-27 define `PlaylistState { Default, Playing, Paused }` which is never referenced anywhere. `PlaylistPlaybackState` in the UI components layer serves this role.

**Fix**: Delete the unused sealed interface.

**Files**: `ui/playlists/PlaylistViewModel.kt`

---

### GAP 3: `getPlaylistById` is fragile and incomplete

**Current**: `PlaylistRepositoryImpl.getPlaylistById()` calls `.first()` on a separate `getSongCountForPlaylist` flow inside a `.map {}`, and doesn't include `totalDuration`.

**Fix**: Add a `getPlaylistByIdWithCount(playlistId)` DAO query mirroring `getAllPlaylistsWithCount()` but filtered by ID. Use it in the repository.

**Files**: `data/local/PlaylistDao.kt`, `data/PlaylistRepositoryImpl.kt`, `data/PlaylistRepository.kt`

---

## Implementation Plan

### Phase 1: Fix critical playlist info bug
1. **`PlaylistRepositoryImpl.getAllPlaylists()`** — Remove the redundant `.map` that strips `songCount`/`totalDuration`. Return `playlistDao.getAllPlaylistsWithCount()` directly.

### Phase 2: Fix create playlist UX
2. **`PlaylistsTabScreenContent`** — Add `showCreateDialog` state variable. Add `CreatePlaylistDialog` composable (reuse `RenamePlaylistDialog` pattern with empty initial name and different title/confirm strings).
3. **Wire through**: Change `onCreatePlaylist` from `() -> Unit` to `(String) -> Unit`. Update `PlaylistsTabScreen` to pass the name to `PlaylistEvent.CreatePlaylist`.
4. **`PlaylistsActionBar`** — No change needed (already calls `onCreatePlaylist`).

### Phase 3: Data integrity fixes
5. **`PlaylistDao`** — Add `isSongInPlaylist(playlistId, songId): Boolean` suspend query.
6. **`PlaylistRepositoryImpl.addSongToPlaylist()`** — Guard with `if (!playlistDao.isSongInPlaylist(...))` before inserting.
7. **`PlaylistDao`** — Add `@Transaction` annotated `reorderPlaylistSongs()` function wrapping `clearPlaylistSongs` + `replacePlaylistSongs`.
8. **`PlaylistRepositoryImpl.reorderSongs()`** — Call the new transactional DAO method.

### Phase 4: Polish
9. **Empty state** — In `PlaylistsTabScreenContent`, when `playlists.isEmpty()`, show a centered Column with a playlist icon and "No playlists yet" text below the action bar.
10. **Dead code** — Remove `PlaylistState` sealed interface from `PlaylistViewModel.kt`.
11. **`getPlaylistById`** — Add `getPlaylistByIdWithCount(playlistId)` DAO query and use it in repository.

### Phase 5: Verify
12. Build: `gradlew.bat assembleDebug`
13. Test: `gradlew.bat test`

## Files Summary

### Modified files

| File | Changes |
|------|---------|
| `data/PlaylistRepositoryImpl.kt` | Fix `getAllPlaylists()` mapping, guard duplicate songs, use transactional reorder, fix `getPlaylistById` |
| `data/local/PlaylistDao.kt` | Add `isSongInPlaylist`, `reorderPlaylistSongs` (@Transaction), `getPlaylistByIdWithCount` |
| `ui/home/tabs/PlaylistsTabScreen.kt` | Add create dialog, empty state, update `onCreatePlaylist` signature |
| `ui/playlists/PlaylistViewModel.kt` | Remove dead `PlaylistState` sealed interface |
| `res/values/strings.xml` | Add `create_playlist_title`, `create_playlist_confirm`, `empty_playlists` strings |
| `test/.../SyncOrchestratorTest.kt` | Add missing method stubs to `FakePlaylistDao` |

### No new files needed

## Verification Checklist

- [x] Playlist rows show correct song count and total duration
- [x] Create playlist opens a name dialog; name is used
- [x] Adding the same song twice to a playlist doesn't create duplicates
- [x] Reordering songs survives app kill mid-operation (atomic)
- [x] Empty playlist list shows informative message
- [x] `assembleDebug` succeeds
- [x] `test` passes
