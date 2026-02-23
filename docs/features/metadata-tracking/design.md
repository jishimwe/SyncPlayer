# Metadata Tracking — Design

## Overview

Phase 5 adds listening metadata to SyncPlayer: play count increments when a threshold is reached (150s or 70% of duration, whichever is less), a 5-star rating system with a favorite shortcut on the NowPlaying screen, and a timestamped listening history log. These are surfaced in three new Library tabs — Faves, Top Plays, and Recent — backed by new Room queries and a new `listening_history` table.

## What was built

| File | Change |
|------|--------|
| `model/Song.kt` | Replaced `isFavorite: Boolean` with `rating: Int = 0`; added `playCount: Int = 0`, `lastPlayed: Long = 0`; added `Rating` enum and `RatingConverter` |
| `data/local/SyncPlayerDatabase.kt` | Bumped to v4 (destructive migration), added `ListeningHistoryEntity`, `listeningHistoryDao()`, `@TypeConverters(RatingConverter::class)` |
| `data/local/ListeningHistoryDao.kt` | New file — `ListeningHistoryEntity` + `ListeningHistoryDao` with `insertListeningHistory`, `getRecentlyPlayed` (GROUP BY deduplication), `clearAll` |
| `data/local/SongDao.kt` | Added `incrementPlayCount`, `setRating`, `getSongsByMinRating`, `getMostPlayedSongs`, `getRating`; `getFavoriteSongs()` as a default method delegating to `getSongsByMinRating(4)` |
| `di/DatabaseModule.kt` | Added `provideListeningHistoryDao()` |
| `data/SongRepository.kt` | Added metadata methods to the interface |
| `data/SongRepositoryImpl.kt` | Implemented metadata methods; injected `ListeningHistoryDao` |
| `data/PlayerRepositoryImpl.kt` | Added `hasCountedCurrentSong` flag, threshold check inside the 500ms position loop |
| `ui/player/PlayerEvent.kt` | Added `SetRating(rating: Rating)` |
| `ui/player/PlayerViewModel.kt` | Added `SetRating` handler; added `currentSongRating: StateFlow<Rating>` via `flatMapLatest` |
| `ui/player/NowPlayingScreen.kt` | Collects `currentSongRating`, passes `rating` down to content |
| `ui/player/NowPlayingScreenContent.kt` | Added `rating: Rating` param; added `FavoriteButton` and `StarRating` composables |
| `ui/library/LibraryViewModel.kt` | Added `FAVORITES`, `MOST_PLAYED`, `RECENTLY_PLAYED` tabs; expanded `Loaded` state; nested `combine` workaround |
| `ui/library/LibraryScreen.kt` | Added `FavoriteTab`, `MostPlayedTab`, `RecentlyPlayedTab` composables |
| `test/data/FakeSongRepository.kt` | Added stubs for all metadata methods |
| `test/ui/library/PlayerViewModelTest.kt` | Converted to JUnit 5; fixed state-dependent tests with Turbine; added `SetRating` tests |
| `test/ui/player/NowPlayingScreenContentTest.kt` | New — tests favorite button toggle behavior |
| `test/ui/library/LibraryViewModelMetadataTest.kt` | New — tests favorites, mostPlayed, recentlyPlayed flow through to `Loaded` state |

## Design decisions

- **`rating` stored as `Int` in the Song entity, not `Rating`**: Room's `@TypeConverter` applies to entity fields but not to raw scalar `@Query` return types (e.g., `SELECT rating FROM songs WHERE id = ?`). Storing `Int` keeps the DAO pure and avoids subtle converter-not-applied bugs. The `RatingConverter` is registered on the database so it does apply to full entity reads/writes. The repository maps `Flow<Int>` → `Flow<Rating>` via `.map { Rating.fromInt(it) }`.

- **`getFavoriteSongs()` as a DAO default method**: Rather than a separate `@Query`, `getFavoriteSongs()` delegates to `getSongsByMinRating(4)`. This keeps the threshold in one place and avoids duplicating the ORDER BY clause.

- **Toggle logic in `NowPlayingScreenContent`, not in `FavoriteButton`**: `FavoriteButton` is a pure display component — it receives an `onClick` lambda and shows the appropriate icon. The NONE↔FAVORITE toggle logic lives in the screen content so it can fire `onEvent(SetRating(...))` directly. Any non-NONE rating clears to NONE on click (not just FAVORITE), which is consistent with the star rating bar's tap-to-clear behavior.

- **`currentSongRating` uses `distinctUntilChanged` before `flatMapLatest`**: The position update loop emits `playbackState` every 500ms. Without `distinctUntilChanged()` on the song ID, `flatMapLatest` would resubscribe to `getRating` on every tick, creating a new Flow subscription at 2 Hz. Deduplicating on song ID limits resubscription to actual song transitions.

- **Nested `combine` for 7 flows**: Kotlin's `combine` stdlib overloads only go up to 5 parameters. A `metadataFlows` intermediate combines the 3 metadata flows into a `Triple`, then the outer `combine` handles the remaining 4 flows. This is a workaround, not a design preference — see Known gaps.

- **`System.currentTimeMillis()` owned by the repository, not the DAO**: `incrementPlayCount` and `recordListeningEvent` receive no timestamp from the caller — the repository stamps the time. This keeps the DAO a pure data layer and makes the repository the correct boundary for system calls.

- **Play count threshold guards `threshold > 0`**: `mediaController.duration` returns `-1` for streams with unknown duration. Without the guard, `minOf(150_000, -1 * 70 / 100)` = `minOf(150_000, -1)` = `-1`, and `position >= -1` is always true — every 500ms tick would fire the count. The guard skips counting until a valid duration is available.

## Known gaps

- **`LibraryViewModel` combining 7 flows**: The nested `combine` is a sign the ViewModel is doing too much. Splitting into per-tab ViewModels is deferred to a broader UI/UX redesign pass.
- **Dual favorite/star rating UX**: The favorite button and star bar coexist on NowPlaying. Whether one should replace the other is deferred to dogfooding. The plan's design note on this stands.
- **Downgrading a rating requires two taps**: To go from 4 stars to 3, the user must tap 4 (clears to NONE) then tap 3. A slide gesture would be smoother — deferred as noted in the plan.
- **Play count not displayed on `SongListItem`**: Deferred per plan scope.
- **History detail screen**: A full scrollable listening history log is out of scope for this phase.
- **`NowPlayingScreenTest.kt` wrong package**: Existing compose UI tests are in `ui.library` instead of `ui.player`. Not fixed here to stay focused on scope.