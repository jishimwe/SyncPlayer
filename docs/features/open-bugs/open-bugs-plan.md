---
type: plan
feature: open-bugs
phase: 7
status: complete
tags:
  - type/plan
  - status/complete
  - feature/open-bugs
---

# Open Bugs — Plan

**Status: All 8 bugs resolved as of Phase 7 ✅**

See `docs/features/phase7/plan.md` for implementation details.

## Context

Phases 1–7 are complete. During implementation, each phase's design doc recorded known gaps that were deferred to keep scope focused. This document collected all confirmed bugs — behavior that was definitively wrong — across playlists, metadata tracking, playback, and test infrastructure.

A pre-flight audit at the start of Phase 7 revealed that Bugs 2, 4, and 6 had already been fixed during prior implementation. The remaining five bugs (1, 3, 5, 7, 8) were resolved in Phase 7.

Bugs are distinct from feature gaps: a bug is something that was intended to work but doesn't (wrong data, wrong icon, missing wiring), not a feature that was explicitly descoped.

---

## Scope

**Included:**

- Playlist `songCount` always showing 0
- Playlist `createdAt` displaying raw epoch milliseconds
- Playlist `Loading` state rendering the Error icon
- Playlist names not trimmed of whitespace
- Empty state copy mismatch ("No playlists found" vs "No playlists yet")
- Rating downgrade requiring two taps to change (e.g., 4★ → 3★ requires tap 4 → tap 3)
- `NowPlayingScreenTest.kt` in wrong package (`ui.library` instead of `ui.player`)
- No test coverage for playlist add/remove/reorder events

**Excluded:**

- Play count display on `SongListItem` (descoped feature, not a bug — see improvements plan)
- History detail screen (descoped feature)
- `PlaybackNotificationManager` stub (Media3 handles basic notification; custom layouts are a feature)
- Dual favorite/star rating UX (design trade-off, not a bug)
- `PlayerRepositoryTest.kt` being empty (requires running service, impractical as unit test)
- Phase 6 Sync (separate feature)

---

## Approach

All fixes are isolated and low-risk. No new dependencies are needed. Each bug is fixed in the layer it belongs to (data → ViewModel → UI → tests) with a `assembleDebug` check after each layer.

---

## Tasks

### Bug 1 — Playlist `songCount` always 0 ✅ Fixed in Phase 7 (Task 2.4)

**Source:** `playlists/design.md` → Known gaps

**Root cause:** `PlaylistRepositoryImpl.getAllPlaylists()` maps `PlaylistEntity` to `Playlist` without calling `getSongCountForPlaylist`. The `Playlist.songCount` field is hardcoded to 0.

**Fix:** Replace the simple entity-to-domain mapping with a combined query per playlist:

```kotlin
// data/PlaylistRepositoryImpl.kt
override fun getAllPlaylists(): Flow<List<Playlist>> =
    playlistDao.getAllPlaylists().map { entities ->
        entities.map { entity ->
            Playlist(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt,
                songCount = playlistDao.getSongCountForPlaylist(entity.id).first(),
            )
        }
    }
```

If `getSongCountForPlaylist` returns a `Flow<Int>`, use `.first()` inside a `map` on a suspend function, or change `getAllPlaylists()` to a `suspend` that collects once. Alternatively, add a DAO query that joins and groups:

```sql
-- SongWithCount query (preferred — single DB round-trip)
SELECT p.*, COUNT(ps.songId) AS songCount
FROM playlists p
LEFT JOIN playlist_songs ps ON p.id = ps.playlistId
GROUP BY p.id
ORDER BY p.createdAt DESC
```

This requires a new `PlaylistWithCount` projection data class and `@Query` on `PlaylistDao`.

**Files:**
- `data/local/PlaylistDao.kt` — add `getAllPlaylistsWithCount(): Flow<List<PlaylistWithCount>>`
- `data/PlaylistRepositoryImpl.kt` — use the new DAO method
- `model/Playlist.kt` — no change needed if projection maps directly

**Verification:** Create 2 playlists, add 3 songs to one and 1 to the other. Playlist list should show "3 songs" / "1 song".

---

### Bug 2 — Playlist `createdAt` displays raw milliseconds ✅ Pre-flight confirmed already fixed

**Source:** `playlists/design.md` → Known gaps

**Root cause:** `PlaylistListItem` passes `playlist.createdAt` (a `Long` epoch ms) directly to a `Text()` composable without formatting.

**Fix:** Format the timestamp in the UI layer using `java.time.Instant` and `DateTimeFormatter`:

```kotlin
// ui/playlists/PlaylistListItem.kt (or PlaylistsScreenContent.kt depending on where the composable lives)
val formattedDate = remember(playlist.createdAt) {
    val instant = Instant.ofEpochMilli(playlist.createdAt)
    val formatter = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
    formatter.format(instant)
}
Text(text = formattedDate)
```

No new dependencies needed — `java.time` is available from API 26+ (min SDK is 34).

**Files:**
- `ui/playlists/PlaylistsScreenContent.kt` or `PlaylistListItem.kt` — add formatting

**Verification:** Newly created playlist shows a human-readable date (e.g., "Feb 25, 2026") in the overline.

---

### Bug 3 — Playlist `Loading` state shows Error icon ✅ Fixed in Phase 7 (Task 2.1)

**Source:** `playlists/design.md` → Known gaps

**Root cause:** `PlaylistsScreenContent` renders `Icons.Default.Error` for the `Loading` state — a placeholder left from initial scaffolding.

**Fix:** Replace with a `CircularProgressIndicator` centered on screen:

```kotlin
// ui/playlists/PlaylistsScreenContent.kt
is PlaylistUiState.Loading -> {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
```

**Files:**
- `ui/playlists/PlaylistsScreenContent.kt`

**Verification:** Loading state renders a spinner, not an error icon.

---

### Bug 4 — Playlist names not trimmed ✅ Pre-flight confirmed already fixed in dialogs; VM-level hardening added in Phase 7 (Task 2.3)

**Source:** `playlists/design.md` → Known gaps

**Root cause:** The plan called for `.trim()` on names in `CreatePlaylist` and `RenamePlaylist` handlers, but the implementation passes names as-is.

**Fix:** Trim in `PlaylistViewModel` before the blank-name guard:

```kotlin
// ui/playlists/PlaylistViewModel.kt
PlaylistEvent.CreatePlaylist(name) -> {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return@launch
    playlistRepository.createPlaylist(trimmed)
}
PlaylistEvent.RenamePlaylist(id, name) -> {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return@launch
    playlistRepository.renamePlaylist(id, trimmed)
}
```

**Files:**
- `ui/playlists/PlaylistViewModel.kt`

**Verification:** Creating a playlist named `"  My Playlist  "` saves it as `"My Playlist"`.

---

### Bug 5 — Empty state copy mismatch ✅ Fixed in Phase 7 (Task 2.2)

**Source:** `playlists/design.md` → Known gaps

**Root cause:** `PlaylistsScreenContent` shows `"No playlists found"` instead of the planned `"No playlists yet"`.

**Fix:** Update the string literal in the empty state composable.

**Files:**
- `ui/playlists/PlaylistsScreenContent.kt`

**Note:** This will also require checking `strings.xml` if the string is externalized.

**Verification:** When no playlists exist, the empty state reads "No playlists yet".

---

### Bug 6 — Rating downgrade requires two taps ✅ Pre-flight confirmed already fixed

**Source:** `metadata-tracking/design.md` → Known gaps

**Root cause:** Tapping a star that is already the current rating clears to `NONE`. To go from 4★ to 3★, the user must tap 4★ (clear) then tap 3★. There is no direct downgrade path.

**Fix:** Change the tap logic in `NowPlayingScreenContent` so that tapping a lower-rated star sets that rating directly instead of clearing:

```kotlin
// Current toggle logic (simplified):
// tap star N: if currentRating.value == N → set NONE, else → set N

// Fixed logic:
// tap star N: if currentRating.value == N → set NONE (clear), else → set N (direct set)
// This is actually what the code does already for upgrade — the issue is only when N < currentRating
// The fix: always set the tapped rating, only clear if tapping the same star
```

Looking at the actual tap handler — the `StarRating` composable likely iterates stars and fires `onRatingChange(tappedRating)` with a toggle only for exact matches. If the current rating is `GOOD` (4) and the user taps `FAIR` (3), the handler should set `FAIR` directly.

**Exact fix depends on current implementation** — read `NowPlayingScreenContent.kt` before modifying. The likely change is in the `onClick` passed to each star in `StarRating`:

```kotlin
// Change from:
onClick = { if (currentRating == starRating) onRatingChange(Rating.NONE) else onRatingChange(starRating) }

// To:
// This is what it already does — double check that it already handles direct downgrade
// If not, the fix is: direct set always works, clear only on same star tap
```

**Files:**
- `ui/player/NowPlayingScreenContent.kt` — verify and fix `StarRating` tap logic

**Verification:** Playing a 4-star song → tap 3rd star → song is now 3 stars (not cleared to 0).

---

### Bug 7 — `NowPlayingScreenTest.kt` in wrong package ✅ Fixed in Phase 7 (Task 2.5)

**Source:** `metadata-tracking/design.md` → Known gaps

**Root cause:** `NowPlayingScreenTest.kt` (Compose UI tests) was placed in `ui.library` instead of `ui.player`.

**Fix:** Move the file to the correct package directory:

```
test/ui/player/NowPlayingScreenTest.kt   ← correct location
test/ui/library/NowPlayingScreenTest.kt  ← current (wrong) location
```

Update the `package` declaration in the file.

**Files:**
- `test/ui/player/NowPlayingScreenTest.kt` (new location)
- Delete `test/ui/library/NowPlayingScreenTest.kt` (old location)

**Verification:** `gradlew.bat test` passes; test appears under `ui.player` in test results.

---

### Bug 8 — No tests for playlist add/remove/reorder events ✅ Fixed in Phase 7 (Task 2.6)

**Source:** `playlists/design.md` → Known gaps

**Root cause:** `FakePlaylistRepository` has call counters for `addSongToPlaylist`, `removeSongFromPlaylist`, `removeSongsFromPlaylist`, and `reorderSongs`, but `PlaylistViewModelTest` only covers the 7 initial test cases (state transitions, blank-name guards).

**Fix:** Add tests in `PlaylistViewModelTest`:

1. `AddSongsToPlaylist event calls addSongToPlaylist for each song`
2. `RemoveSongFromPlaylist event calls removeSongFromPlaylist with correct id and songId`
3. `RemoveSongsFromPlaylist event calls removeSongFromPlaylist for each song`
4. `ReorderSongs event calls reorderSongs with correct playlistId and song list`

**Files:**
- `test/ui/playlists/PlaylistViewModelTest.kt`

**Verification:** `gradlew.bat test` — 4 new tests pass.

---

## Dependencies

None. All fixes use existing libraries.

---

## Verification

- `gradlew.bat assembleDebug` after each bug fix
- `gradlew.bat test` after Bug 7 and Bug 8
- Manual checks listed per bug above

### Manual verification checklist

- [x] Bug 1: Playlist list shows correct song count
- [x] Bug 2: Playlist list shows human-readable date (not raw number)
- [x] Bug 3: Playlist screen loading state shows spinner
- [x] Bug 4: Whitespace-padded playlist name is trimmed on save
- [x] Bug 5: Empty playlist list shows "No playlists yet"
- [x] Bug 6: Tapping a lower star directly sets that rating without clearing first
- [x] Bug 7: `gradlew.bat test` — NowPlaying tests appear in `ui.player` package
- [x] Bug 8: `gradlew.bat test` — 4 new playlist event tests pass

---

**Design doc**: [[open-bugs-design]] · See also: [[ui-library-analysis]] · [[data-analysis]]
