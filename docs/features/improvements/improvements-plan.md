---
type: plan
feature: improvements
status: backlog
tags:
  - type/plan
  - status/backlog
  - feature/improvements
---

# Future Improvements — Plan

## Context

Phases 1–7 are complete. Several improvements were explicitly descoped during implementation to maintain focus. This document collects them alongside architectural observations that have accumulated. Items here are *intentional deferrals* — features or refactors that would improve the app but were not required to ship the current phase.

This is a backlog, not an implementation plan. Items should be extracted into their own feature plans before implementation.

**Completed in Phase 7:** Items 1, 5, 6, 8, 10 — see [`docs/features/phase7/plan.md`](../phase7/plan.md).

---

## Improvement Areas

### 1. Play Count Display on `SongListItem` ✅ Done in Phase 7 (Task 3.3)

**Source:** `metadata-tracking/design.md` → Known gaps
**Priority:** Medium
**Effort:** Small

**Context:** Phase 5 tracks play counts and stores them on the `Song` entity, but `SongListItem` does not display the count. Users have no way to see how many times a song has been played from the library list.

**What to build:**
- Add a `playCount` field to `SongListItem` (optional — show only when > 0, or always)
- Decide display format: subtitle text, badge, or icon with count
- Consider whether it belongs on all songs or only in the Top Plays tab

**Scope question:** Should play count appear on every `SongListItem` in every tab, or only in the "Top Plays" tab where it's relevant context?

---

### 2. Listening History Detail Screen

**Source:** `metadata-tracking/design.md` → Known gaps
**Priority:** Low
**Effort:** Medium

**Context:** The "Recent" library tab shows the last played song per title (deduped via `GROUP BY`). There is no screen to browse the full chronological listening log.

**What to build:**
- A `HistoryScreen` showing the complete `listening_history` table ordered by `playedAt DESC`
- Each row: song title, artist, and relative timestamp ("2 hours ago", "Yesterday")
- Navigation from a future Settings or Library screen
- Tap row to play the song

**Dependencies:** Relative timestamp formatting (consider `java.time.format.FormatStyle` or a third-party library).

---

### 3. Slide/Drag Gesture for Rating

**Source:** `metadata-tracking/design.md` → Known gaps
**Priority:** Low
**Effort:** Medium

**Context:** The current `StarRating` composable only supports tap. Going from 4★ to 3★ after Bug 6 is fixed will work with two taps (one to set the lower star directly), but a slide gesture would allow single-gesture rating changes without lifting the finger.

**What to build:**
- Replace tap-only `Row` of stars with a horizontal drag detector
- Map drag position to star index in real time
- On release, commit the hovered rating
- Keep tap behavior for backwards compatibility

**Approach options:**
- A: `Modifier.pointerInput` with `detectHorizontalDragGestures`
- B: Reuse a Compose drag semantics pattern

### 3.5. NowPlaying Album Art Swipe Gestures ⚠️ Functional but needs rethinking (2026-03-21)

**Source:** NowPlayingScreenContent analysis
**Priority:** Medium
**Effort:** Medium (needs redesign)
**Status:** Gestures work, animations need a full rethink

**Context:** NowPlayingScreen had no gesture support. Four directional swipe gestures were added scoped to the album art area using `Modifier.pointerInput` with `detectDragGestures`. A 3D tilt animation was added (rotationX/Y, dampened translation, scale, alpha via `graphicsLayer`) but the result is not satisfying — the interaction model needs a more considered design.

**What works:**
- Swipe left on album art → `PlayerEvent.SkipToNext`
- Swipe right on album art → `PlayerEvent.SkipToPrevious`
- Swipe up on album art → open queue sheet
- Swipe down on album art → dismiss NowPlaying (`onNavigateBack`)
- 80dp threshold, `Animatable<Offset>` for spring-back on cancel

**What needs rethinking:**
- Current 3D tilt + dampened translation animation doesn't feel right — needs a fundamentally different approach, not just parameter tuning
- Consider `HorizontalPager` for left/right (crossfade between actual album arts of prev/next tracks) instead of gesture-on-single-image
- Vertical gestures (up/down) may work better as full-screen `AnchoredDraggable` rather than album-art-only
- The animation feedback should match the *destination* of the gesture (e.g., down-swipe should feel like the whole screen is being dismissed, not just the art tilting)
- Explore what Spotify, Apple Music, and YouTube Music do for reference

**Current implementation location:** `NowPlayingScreenContent.kt` lines ~195-275

---

### 4. Consolidate Favorite Button and Star Rating

**Source:** `metadata-tracking/design.md` → Known gaps
**Priority:** Low (deferred to dogfooding)
**Effort:** Small–Medium

**Context:** `NowPlayingScreenContent` shows both a `FavoriteButton` (heart icon = shortcut to `Rating.FAVORITE`) and a `StarRating` bar. These are redundant. The favorite button is simpler for one-tap "love this song", but the star bar gives granular control.

**Design options:**
- A: Remove `FavoriteButton`, keep only `StarRating` — users tap 5 stars for favorite
- B: Remove `StarRating`, keep only `FavoriteButton` — simpler UX, loses granularity
- C: Keep both but visually integrate (favorite = always 5★ highlight, stars = normal rating)

**Defer until:** At least 2 weeks of dogfooding data to see which control users actually use.

---

### 5. `LibraryViewModel` 7-Flow Refactor ✅ Done in Phase 7 (Task 4.2)

**Source:** `metadata-tracking/design.md` → Known gaps
**Priority:** Medium
**Effort:** Large

**Context:** `LibraryViewModel` currently combines 7 `StateFlow`s using a nested `combine` workaround (Kotlin's `combine` max is 5 params). The ViewModel manages songs, albums, artists, favorites, top plays, recently played, and permissions — a sign it is doing too much.

**What to build:**

**Option A — Per-tab ViewModels:**
- `LibraryViewModel` handles songs/albums/artists + permission
- `MetadataViewModel` handles favorites/top plays/recently played
- Both are activity-scoped for sharing across detail screens

**Option B — Dedicated repository flows:**
- Keep single `LibraryViewModel` but simplify by exposing pre-combined data from `SongRepository`
- `SongRepository` returns a single `LibraryData` object combining all relevant queries

**Option C — Multi-flow with `zip` helper:**
- Write a Kotlin extension to `combine` 6+ flows using pairwise combination
- Avoids architectural restructuring

**Recommended:** Option A after Phase 6 sync is stable, since sync will likely add more flows.

**Impact:** Touches `LibraryViewModel`, `LibraryScreen`, `NavGraph` (ViewModel injection), and tests.

---

### 6. Test Coverage for Detail Screens ✅ Done in Phase 7 testing pass

**Source:** `library-playback-nav/design.md` → Known gaps
**Priority:** Medium
**Effort:** Small–Medium

**Context:** `AlbumDetailScreen` and `ArtistDetailScreen` have no unit or Compose UI tests. They were added in Phase 3 without test coverage because the data comes from the shared `LibraryViewModel` (already tested) and the UI is simple.

**What to build:**
- `AlbumDetailScreenContentTest` — verify song list renders, tap fires navigation event
- `ArtistDetailScreenContentTest` — verify song list renders, tap fires navigation event
- Both use Compose test rule + `FakePlayerRepository` and `FakeLibraryViewModel`

---

### 7. Audio Focus Edge Case Testing

**Source:** `playback/design.md` → Known gaps
**Priority:** Medium
**Effort:** Medium (requires device testing)

**Context:** `AudioFocusHandler` implements fade-in/out, transient loss with resume, permanent loss, and ducking — but these have not been manually verified on a real device with real interruptions.

**What to verify (manual checklist):**
- [ ] Incoming phone call → playback pauses → call ends → playback resumes
- [ ] Google Assistant invoked → playback ducks to 10% → assistant ends → playback restores
- [ ] Bluetooth headphones connected → playback continues
- [ ] Bluetooth headphones disconnected → playback pauses (BecomingNoisyReceiver)
- [ ] Another app plays audio → SyncPlayer ducks or pauses appropriately
- [ ] `AUDIOFOCUS_REQUEST_DELAYED` — play button tapped when focus can't be granted immediately

**Output:** Update `playback/design.md` with verified behavior and any bugs found.

---

### 8. Expose `clearQueue` on `PlayerRepository` Interface ✅ Done in Phase 7 (Task 4.1)

**Source:** `playback/design.md` → Known gaps
**Priority:** Low
**Effort:** Trivial

**Context:** `PlayerRepositoryImpl` has a private `clearQueue()` method used internally, but the `PlayerRepository` interface does not expose it. This prevents callers (e.g., a future "Clear Queue" button in `QueueSheet`) from triggering it without going through a workaround.

**Fix:**
- Add `suspend fun clearQueue()` to `PlayerRepository`
- Make the existing private method public
- Add a `ClearQueue` event to `PlayerEvent`
- Wire to a "Clear all" button in `QueueSheet`

---

### 9. Custom Notification Layout (PlaybackNotificationManager)

**Source:** `playback/design.md` → Known gaps
**Priority:** Low
**Effort:** Medium

**Context:** `service/PlaybackNotificationManager.kt` is an empty stub. Media3's `MediaSessionService` handles basic notification automatically (shows current song, play/pause, skip controls). Custom notification layout (album art, custom actions, colored background) requires implementing this class.

**What to build:**
- Implement `PlaybackNotificationManager` using `MediaNotification.Provider`
- Custom layout: large album art thumbnail, track title, artist, skip prev/next, close button
- Respect system dark/light theme in notification

**Dependencies:** No new dependencies — uses Media3 APIs already in the project.

---

### 10. Phase 6 — Cross-Device Sync ✅ Done in Phase 6

**Source:** `sync/plan.md` (existing, detailed plan already written)
**Priority:** High (next major phase)
**Effort:** Large

**Context:** A detailed implementation plan exists at `docs/features/sync/plan.md`. This is the next major feature after open bugs are resolved.

**High-level summary:**
- Firebase Auth with Google Sign-In
- Firestore for cross-device metadata sync
- Song fingerprinting (SHA-256 of title + artist + album + duration) to match songs across devices
- Conflict resolution: play count = max-wins, rating = last-write-wins, history = union merge
- Room migration 4 → 5 with explicit migrations (no more destructive)

**Prerequisites:**
- Firebase project created (user action required)
- `google-services.json` added to `app/`
- Open bugs resolved first (especially `songCount` which affects sync payload)

---

## Priority Summary

| #   | Improvement                        | Priority | Effort  | Status                        |
|-----|------------------------------------|----------|---------|-------------------------------|
| 1   | Play count on `SongListItem`       | Medium   | Small   | ✅ Done (Phase 7)              |
| 2   | History detail screen              | Low      | Medium  | Open                          |
| 3   | Slide gesture for rating           | Low      | Medium  | Open                          |
| 3.5 | NowPlaying swipe gestures          | Medium   | Medium  | ⚠️ Functional, animations need rethink |
| 4   | Consolidate favorite/star rating   | Low      | Small   | Open (needs dogfooding)       |
| 5   | `LibraryViewModel` 7-flow refactor | Medium   | Large   | ✅ Done (Phase 7)              |
| 6   | Detail screen tests                | Medium   | Small   | ✅ Done (Phase 7)              |
| 7   | Audio focus edge case testing      | Medium   | Medium  | Open                          |
| 8   | Expose `clearQueue` on interface   | Low      | Trivial | ✅ Done (Phase 7)              |
| 9   | Custom notification layout         | Low      | Medium  | Open                          |
| 10  | Phase 6 Sync                       | High     | Large   | ✅ Done (Phase 6)              |

---

## Recommended Order

Items 1, 5, 6, 8, 10 are done. Open bugs are fully resolved. Remaining open items:

1. **Audio focus edge case testing** (#7) — manual, device-only; do before any public release
2. **History detail screen** (#2) — medium effort, clear user value
3. **Custom notification layout** (#9) — polish; Media3 default is functional but plain
4. **Slide gesture for rating** (#3) — after sufficient dogfooding on the tap-only UX
5. **Consolidate favorite/star rating** (#4) — decide after 2+ weeks of use data

---

## Open Questions

- **Favorite vs. star rating (#4):** Should we collect user feedback before deciding, or pick a direction now? Suggestion: decide after 2 weeks of actual use.
- **Play count display (#1):** Show on all tabs or only Top Plays? Show always or only when count > 0?
- **History screen (#2):** Where does it live in navigation? New tab in Library, or a separate Settings section?
