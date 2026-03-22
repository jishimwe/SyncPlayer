# M3U Import/Export — Plan

## Context

Playlists in SyncPlayer are currently Room-only with no way to share them between apps or devices. M3U is the de facto universal playlist format — supported by virtually every music player (VLC, foobar2000, Poweramp, AIMP, Winamp, MusicBee, MediaMonkey, etc.). Google also deprecated `MediaStore.Audio.Playlists` in API 31 and recommends M3U files as the replacement, making this the forward-looking standard for Android.

Adding M3U import/export lets users:
- Migrate playlists from other music players
- Back up playlists as portable text files
- Share playlists across devices (where file structure matches)

## Scope

**Included:**
- Import `.m3u` / `.m3u8` files into app playlists
- Export app playlists as `.m3u8` files (UTF-8)
- Extended M3U support (`#EXTM3U`, `#EXTINF` with duration and title)
- SAF-based file picker for scoped storage compliance (SDK 34+)
- User feedback on import results (songs found vs. skipped)

**Excluded:**
- Other playlist formats (PLS, XSPF, ASX)
- Streaming URLs (http/https entries) — local files only
- Playlist sync via M3U (this is a manual import/export, not auto-sync)
- Batch import of multiple M3U files at once

## Approach

### Song reference bridging

The core challenge: M3U uses file paths, the app uses MediaStore content URIs.

**Export path:** Query MediaStore `DATA` column using the song's ID to get the absolute file path. Write that path into the M3U file.

**Import path:** For each path in the M3U file:
1. Query MediaStore by `DATA` column (exact match)
2. Fallback: match by filename + duration (from `#EXTINF`)
3. If no match: skip and report to user

This approach avoids storing file paths in the Song entity — paths are resolved on-demand from MediaStore, keeping the data model clean.

### File access

Use Android SAF (`ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT`) for both import and export. This is required for scoped storage on SDK 34+ and avoids needing `READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE` permissions.

### Architecture

- **`M3uParser`** — Pure utility, no Android dependencies. Parses an `InputStream` into a list of `M3uEntry(path, title, durationSecs)`. Writes a list of entries to an `OutputStream`.
- **`M3uSongResolver`** — Takes `M3uEntry` list + `ContentResolver`, resolves each to a Song ID via MediaStore queries. Returns matched IDs + unresolved entries.
- **Repository/ViewModel** — Orchestrates import (parse → resolve → create playlist) and export (get songs → resolve paths → write M3U).
- **UI** — Import button on playlists list screen, export option in playlist detail screen.

## Tasks

### Layer 1: Dependencies and Build Config
No new dependencies required. M3U parsing is plain string processing; SAF is part of the Android framework.

### Layer 2: M3U Parser Utility
1. Create `data/m3u/M3uEntry.kt` — data class with `path: String`, `title: String?`, `durationSecs: Int?`
2. Create `data/m3u/M3uParser.kt`:
   - `fun parse(input: InputStream): List<M3uEntry>` — handles `#EXTM3U`, `#EXTINF`, comment lines, blank lines, relative/absolute paths
   - `fun write(output: OutputStream, entries: List<M3uEntry>)` — writes extended M3U format
3. Build and verify

### Layer 3: Song Resolver
1. Create `data/m3u/M3uSongResolver.kt`:
   - `suspend fun resolve(entries: List<M3uEntry>, contentResolver: ContentResolver): M3uResolveResult`
   - `M3uResolveResult` contains `resolved: List<Pair<M3uEntry, Long>>` (entry → song ID) and `unresolved: List<M3uEntry>`
   - Primary match: query `MediaStore.Audio.Media.DATA = ?`
   - Fallback match: query by `DISPLAY_NAME` + compare duration
2. Build and verify

### Layer 4: Repository and ViewModel
1. Add to `PlaylistRepository`:
   - `suspend fun importM3uPlaylist(name: String, songIds: List<Long>): Long`
   - `suspend fun getFilePathsForPlaylist(playlistId: Long, contentResolver: ContentResolver): List<M3uEntry>`
2. Add import/export events to `PlaylistEvent`:
   - `ImportM3u(uri: Uri)` — triggered after SAF picker returns
   - `ExportM3u(playlistId: Long, uri: Uri)` — triggered after SAF picker returns
3. Add state for import results (success message, unresolved count)
4. Handle events in `PlaylistViewModel`
5. Build and verify

### Layer 5: UI Integration
1. **Playlists list screen**: Add "Import M3U" option (overflow menu or FAB action)
   - Launches `ACTION_OPEN_DOCUMENT` with MIME type `audio/x-mpegurl` (also `audio/mpegurl`, `application/x-mpegurl` for compat)
   - On result: fires `ImportM3u` event
2. **Playlist detail screen**: Add "Export as M3U" option in action bar / overflow menu
   - Launches `ACTION_CREATE_DOCUMENT` with suggested filename `{playlist-name}.m3u8`
   - On result: fires `ExportM3u` event
3. **Import result dialog/snackbar**: Shows "Imported X of Y songs" with option to view unresolved
4. Build and verify

### Layer 6: Tests
1. Unit test `M3uParser` — parse valid M3U, extended M3U, empty lines, comments, malformed input
2. Unit test `M3uParser.write` — verify output format
3. Unit test `M3uSongResolver` — mock ContentResolver, verify matching logic
4. Unit test ViewModel import/export event handling
5. Run tests and verify

## Open Questions

1. **Playlist naming on import**: Use the M3U filename (minus extension) as the playlist name? Or prompt the user?
2. **Duplicate handling**: If an imported playlist name already exists, auto-suffix (e.g., "My Playlist (2)") or ask the user?
3. **Relative paths in M3U**: Should export use relative paths (more portable but fragile) or absolute paths (less portable but reliable)?

## Verification

- `assembleDebug` succeeds after each layer
- `test` passes all tests
- Manual: Import a `.m3u` file created by another player (e.g., VLC) → verify songs appear in new playlist
- Manual: Export a playlist → open the `.m3u8` file in a text editor → verify paths and `#EXTINF` lines
- Manual: Import an M3U with some missing songs → verify partial import with user feedback
