# Metadata Tracking — Plan

## Context

Phases 1-4 built library browsing, playback, library→playback navigation, and playlist management. Users can browse and play songs, but there's no tracking of listening behavior — no play counts, no ratings, no history. Phase 5 adds metadata tracking: play count increments on threshold, a 5-star rating system with a favorite shortcut, and a timestamped listening history log. These are surfaced as new Library tabs (Favs, Recent, Top Plays).

## Scope

**Included:**
- Play count tracking with threshold (150 seconds or 70% of duration, whichever comes first)
- 5-star rating system per song (`Rating` enum on Song entity) replacing boolean favorite
- Favorite button as shortcut to `Rating.FAVORITE` (5 stars) alongside full `StarRating` bar
- Listening history log (new `ListeningHistoryEntity` with timestamps)
- Favorite button + star rating bar on NowPlaying screen
- Three new Library tabs: Favs (4+ stars), Recent, Top Plays
- `lastPlayed` timestamp on Song entity
- Unit tests for tracking logic and ViewModel (delegated to Claude Code)

**Excluded:**
- Analytics/charts (plays per week, listening time graphs)
- Date-range filters on Most Played (all-time only for now)
- Play count display on SongListItem (deferred — can be added later)
- Rating from Library screen long-press/context menu
- Listening history detail screen (full scrollable log)

## Approach

### Play count threshold

Track elapsed playback time in `PlayerRepositoryImpl`. The position update loop already runs every 500ms while playing. Add a flag per song that fires once the threshold is crossed (`minOf(150_000L, duration * 70 / 100)`). When the threshold is met:
1. Increment `playCount` on the Song entity
2. Set `lastPlayed` to current timestamp
3. Insert a `ListeningHistoryEntity` record

The `hasCountedCurrentSong` flag ensures the event fires exactly once per song load. The flag resets on `onMediaItemTransition` so the next song (or the same song navigated back to via prev) can be counted again. The `threshold > 0` guard handles the case where `duration` is `-1` (unknown stream duration).

### Rating system

Replace `isFavorite: Boolean` with a `Rating` enum (`NONE, POOR, FAIR, GOOD, GREAT, FAVORITE`). The favorite button on NowPlaying is a shortcut that sets `Rating.FAVORITE` — tapping again clears to `Rating.NONE`. A full `StarRating` bar sits alongside it for granular rating. Tapping an unselected star sets that rating; tapping the active star clears back to `Rating.NONE`. The Favs tab shows songs with `rating >= Rating.GREAT` (4+ stars).

> **Design note:** Revisit the dual favorite/star system after dogfooding — may want a dedicated star picker or may drop one of the two systems.

> **Future improvement:** Replace tap-based `StarRating` with a slide gesture using `detectHorizontalDragGestures` for smoother UX (downgrading a rating currently requires two taps).

### Listening history

A separate `ListeningHistoryEntity` records each play event with a timestamp. This supports the Recently Played tab and is structured for future sync (Phase 6). The history is append-only. Deduplication is handled in the query using `GROUP BY songs.id` + `ORDER BY MAX(playedAt) DESC` — not in the repository layer.

### Library tabs

Add `FAVORITES`, `RECENTLY_PLAYED`, `MOST_PLAYED` to the existing `LibraryTab` enum with short labels. `LibraryViewModel` adds three new flows via a nested `combine` workaround (Kotlin's `combine` only has overloads up to 5 flows — not a technical limit, just where the stdlib authors stopped writing overloads; a helper `metadataFlows` combines the 3 new flows into a `Triple` first).

> **Future improvement:** `LibraryViewModel` combining 7 flows via nested combine is a sign it's doing too much. Split into per-tab ViewModels as part of a broader UI/UX redesign pass to improve app aesthetics and architecture.

### No new dependencies

All changes use existing Room, Compose, and Material 3 libraries.

---

## Tasks

### Layer 1: Song entity + migration

**Modified:** `model/Song.kt`
- Replaced `isFavorite: Boolean` with `rating: Rating = Rating.NONE`
- Added `playCount: Int = 0` and `lastPlayed: Long = 0`
- Added `Rating` enum and `RatingConverter`:

```kotlin
enum class Rating(val value: Int) {
    NONE(0),
    POOR(1),
    FAIR(2),
    GOOD(3),
    GREAT(4),
    FAVORITE(5);

    companion object {
        fun fromInt(value: Int): Rating = entries.find { it.value == value } ?: NONE
    }
}

class RatingConverter {
    @TypeConverter
    fun toInt(rating: Rating): Int = rating.value

    @TypeConverter
    fun fromInt(value: Int): Rating = Rating.fromInt(value)
}
```

**Modified:** `data/local/SyncPlayerDatabase.kt`
- Bumped version from 3 to 4 (destructive migration)
- Added `@TypeConverters(RatingConverter::class)` as a separate annotation on the class (cannot be nested inside `@Database`)
- Added `ListeningHistoryEntity::class` to entities array
- Added `abstract fun listeningHistoryDao(): ListeningHistoryDao`

> Note: Type converters work for entity fields but NOT for raw `@Query` scalar return types — those need manual `.map { Rating.fromInt(it) }` in the repository.

---

### Layer 2: ListeningHistory entity + DAO

**New file:** `data/local/ListeningHistoryDao.kt`

```kotlin
@Entity(tableName = "listening_history")
data class ListeningHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
)

@Dao
interface ListeningHistoryDao {
    @Insert
    suspend fun insertListeningHistory(history: ListeningHistoryEntity)

    @Query("""
        SELECT songs.* FROM listening_history
        INNER JOIN songs ON songs.id = listening_history.songId
        GROUP BY songs.id
        ORDER BY MAX(listening_history.playedAt) DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<Song>>

    @Query("DELETE FROM listening_history")
    suspend fun clearAll()
}
```

> Note: `DISTINCT songs.*` does NOT properly deduplicate — two plays of the same song are distinct rows in `listening_history` so they always pass DISTINCT. `GROUP BY songs.id` is the correct approach.
> Note: `Flow`-returning queries must NOT have `suspend`. Only one-shot write operations use `suspend`.

**Modified:** `di/DatabaseModule.kt`
- Add `@Provides fun provideListeningHistoryDao(db): ListeningHistoryDao`

---

### Layer 3: SongDao metadata queries

**Modified:** `data/local/SongDao.kt`

```kotlin
@Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :playedAt WHERE id = :songId")
suspend fun incrementPlayCount(songId: Long, playedAt: Long)

@Query("UPDATE songs SET rating = :rating WHERE id = :songId")
suspend fun setRating(songId: Long, rating: Int)

@Query("SELECT * FROM songs WHERE rating >= :minRating ORDER BY rating DESC, title ASC")
fun getSongsByMinRating(minRating: Int = 4): Flow<List<Song>>

@Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT :limit")
fun getMostPlayedSongs(limit: Int = 50): Flow<List<Song>>

@Query("SELECT rating FROM songs WHERE id = :songId")
fun getRating(songId: Long): Flow<Int>
```

> Note: `getFavoriteSongs()` replaced with `getSongsByMinRating(minRating: Int)` to support both the Favs tab (minRating=4) and future use cases needing a different threshold.
> Note: DAO takes `Int` for rating — domain type conversion happens at the repository layer.

---

### Layer 4: Repository updates

**Modified:** `data/SongRepository.kt` (interface)

```kotlin
suspend fun incrementPlayCount(songId: Long)
suspend fun setRating(songId: Long, rating: Rating)
fun getSongsByMinRating(minRating: Rating): Flow<List<Song>>
fun getMostPlayedSongs(): Flow<List<Song>>
fun getRecentlyPlayed(): Flow<List<Song>>
fun getRating(songId: Long): Flow<Rating>
suspend fun recordListeningEvent(songId: Long)
```

**Modified:** `data/SongRepositoryImpl.kt`

```kotlin
override suspend fun incrementPlayCount(songId: Long) {
    songDao.incrementPlayCount(songId, System.currentTimeMillis())
}

override suspend fun setRating(songId: Long, rating: Rating) {
    songDao.setRating(songId, rating.value)
}

override fun getSongsByMinRating(minRating: Rating) =
    songDao.getSongsByMinRating(minRating.value)

override fun getMostPlayedSongs() = songDao.getMostPlayedSongs()

override fun getRecentlyPlayed() = listeningHistoryDao.getRecentlyPlayed()

override fun getRating(songId: Long): Flow<Rating> =
    songDao.getRating(songId).map { Rating.fromInt(it) }

override suspend fun recordListeningEvent(songId: Long) {
    listeningHistoryDao.insertListeningHistory(
        ListeningHistoryEntity(songId = songId, playedAt = System.currentTimeMillis())
    )
}
```

> Note: Repository owns `System.currentTimeMillis()` — keeps DAO pure and testable.
> Note: Repository maps `Flow<Int>` to `Flow<Rating>` via `.map { Rating.fromInt(it) }` since type converters don't apply to raw scalar query returns.

---

### Layer 5: Playback tracking logic

**Modified:** `data/PlayerRepositoryImpl.kt`

Added tracking state:
```kotlin
private var hasCountedCurrentSong: Boolean = false
```

Reset in `onMediaItemTransition`:
```kotlin
override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
    hasCountedCurrentSong = false
    // ... existing code
}
```

Threshold check inside `startPositionUpdates` loop (piggybacks on existing 500ms polling):
```kotlin
if (!hasCountedCurrentSong) {
    val position = mediaController?.currentPosition ?: 0L
    val duration = mediaController?.duration ?: 0L
    val threshold = minOf(150_000L, duration * 70 / 100)
    if (threshold > 0 && position >= threshold) {
        hasCountedCurrentSong = true
        val songId = mediaController?.currentMediaItem?.mediaId?.toLongOrNull()
        if (songId != null) {
            repositoryScope.launch {
                songRepository.incrementPlayCount(songId)
                songRepository.recordListeningEvent(songId)
            }
        }
    }
}
```

---

### Layer 6: Rating event + PlayerViewModel

**Modified:** `ui/player/PlayerEvent.kt`

```kotlin
data class SetRating(val rating: Rating) : PlayerEvent
```

**Modified:** `ui/player/PlayerViewModel.kt`

```kotlin
is PlayerEvent.SetRating -> {
    viewModelScope.launch {
        val songId = uiState.value.currentSong?.id ?: return@launch
        songRepository.setRating(songId, event.rating)
    }
}

val currentSongRating: StateFlow<Rating> =
    playerRepository.playbackState
        .map { it.currentSong?.id }
        .distinctUntilChanged()  // prevents flatMapLatest resubscribing on every playback tick
        .flatMapLatest { songId ->
            if (songId != null) songRepository.getRating(songId) else flowOf(Rating.NONE)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), Rating.NONE)
```

---

### Layer 7: NowPlaying UI

**Modified:** `ui/player/NowPlayingScreen.kt`

```kotlin
val rating = viewModel.currentSongRating.collectAsStateWithLifecycle()

NowPlayingScreenContent(
    uiState = uiState.value,
    onEvent = viewModel::onEvent,
    onNavigateBack = onNavigateBack,
    formatTime = viewModel::formatTime,
    rating = rating.value,
)
```

**Modified:** `ui/player/NowPlayingScreenContent.kt`

Added `FavoriteButton` and `StarRating` composables:

```kotlin
@Composable
fun FavoriteButton(rating: Rating, onClick: () -> Unit) {
    val isFavorite = rating == Rating.FAVORITE
    IconButton(onClick = onClick) {
        Icon(
            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = if (isFavorite) MaterialTheme.colorScheme.error else LocalContentColor.current,
        )
    }
}

// Tapping active star clears to NONE; tapping inactive star sets that rating
@Composable
fun StarRating(rating: Rating, onSetRating: (Rating) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Rating.entries.filter { it != Rating.NONE }.forEach { star ->
            IconButton(onClick = { onSetRating(if (rating == star) Rating.NONE else star) }) {
                Icon(
                    if (star.value <= rating.value) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "${star.value} stars",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
```

> Note: `NowPlayingScreenContent` receives `onEvent` directly rather than individual lambdas per action. This couples it to `PlayerEvent` but reduces parameter threading. Decoupling deferred to the UI redesign pass.

---

### Layer 8: Library tabs

**Modified:** `ui/library/LibraryViewModel.kt`

```kotlin
enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FAVORITES("Favs"),
    MOST_PLAYED("Top Plays"),
    RECENTLY_PLAYED("Recent"),
}
```

`LibraryUiState.Loaded` — no defaults, compiler enforces all fields are provided:
```kotlin
data class Loaded(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val favorites: List<Song>,
    val mostPlayed: List<Song>,
    val recentlyPlayed: List<Song>,
) : LibraryUiState
```

Nested combine to work around Kotlin's 5-flow `combine` limit:
```kotlin
val metadataFlows = combine(
    songRepository.getSongsByMinRating(Rating.GREAT),
    songRepository.getMostPlayedSongs(),
    songRepository.getRecentlyPlayed(),
) { favorites, mostPlayed, recentlyPlayed -> Triple(favorites, mostPlayed, recentlyPlayed) }

val uiState = combine(
    songRepository.getAllSongs(),
    songRepository.getAllAlbums(),
    songRepository.getAllArtists(),
    refreshError,
    metadataFlows,
) { songs, albums, artists, error, (favorites, mostPlayed, recentlyPlayed) ->
    if (error != null && songs.isEmpty()) LibraryUiState.Error(error)
    else LibraryUiState.Loaded(songs, albums, artists, favorites, mostPlayed, recentlyPlayed)
}.stateIn(...)
```

**Modified:** `ui/library/LibraryScreen.kt`

Each tab composable uses its own list for both display and `onSongClick`:
```kotlin
LibraryTab.FAVORITES -> FavoriteTab(uiState, onSongClick)           // uses state.favorites
LibraryTab.MOST_PLAYED -> MostPlayedTab(uiState, onSongClick)       // uses state.mostPlayed
LibraryTab.RECENTLY_PLAYED -> RecentlyPlayedTab(uiState, onSongClick) // uses state.recentlyPlayed
```

---

### Layer 9: Tests (delegated to Claude Code)

Tests to be generated by Claude Code covering:

**`FakeSongRepository.kt`** — stubs for: `setRating`, `incrementPlayCount`, `recordListeningEvent`, `getSongsByMinRating`, `getMostPlayedSongs`, `getRecentlyPlayed`, `getRating`

**`PlayerViewModelTest.kt`:**
- `SetRating calls songRepository.setRating with current song id and rating`
- `SetRating does nothing when no song is playing`

**`NowPlayingScreenContentTest.kt`:**
- `FavoriteButton sets Rating.FAVORITE when not favorited`
- `FavoriteButton clears to Rating.NONE when already at Rating.FAVORITE`

**`LibraryViewModelMetadataTest.kt`:**
- `uiState includes favorites when loaded`
- `uiState includes recentlyPlayed when loaded`
- `uiState includes mostPlayed when loaded`

---

## Files summary

### New files (3)

| File | Purpose |
|------|---------|
| `data/local/ListeningHistoryDao.kt` | `ListeningHistoryEntity` + `ListeningHistoryDao` |
| `test/ui/library/LibraryViewModelMetadataTest.kt` | Metadata tab tests |
| `test/ui/player/NowPlayingScreenContentTest.kt` | Favorite button UI logic tests |

### Modified files (13)

| File | Change |
|------|--------|
| `model/Song.kt` | Replace `isFavorite` with `rating: Rating`; add `playCount`, `lastPlayed`; add `Rating` enum + `RatingConverter` |
| `data/local/SyncPlayerDatabase.kt` | Add `ListeningHistoryEntity`, bump to v4, add `listeningHistoryDao()`, register `@TypeConverters(RatingConverter::class)` |
| `data/local/SongDao.kt` | Add `incrementPlayCount`, `setRating`, `getSongsByMinRating`, `getMostPlayedSongs`, `getRating` |
| `di/DatabaseModule.kt` | Add `provideListeningHistoryDao()` |
| `data/SongRepository.kt` | Add metadata methods to interface |
| `data/SongRepositoryImpl.kt` | Implement metadata methods, inject `ListeningHistoryDao` |
| `data/PlayerRepositoryImpl.kt` | Add threshold tracking (150s or 70%) inside position loop |
| `ui/player/PlayerEvent.kt` | Add `SetRating(rating: Rating)` |
| `ui/player/PlayerViewModel.kt` | Handle `SetRating`, inject `SongRepository`, expose `currentSongRating` |
| `ui/player/NowPlayingScreen.kt` | Collect and pass `rating` |
| `ui/player/NowPlayingScreenContent.kt` | Add `FavoriteButton` shortcut + `StarRating` bar |
| `ui/library/LibraryViewModel.kt` | Add 3 tabs to enum with labels, expand `Loaded` state, nested combine |
| `ui/library/LibraryScreen.kt` | Add `FavoriteTab`, `MostPlayedTab`, `RecentlyPlayedTab` composables |
| `test/data/FakeSongRepository.kt` | Add stubs for metadata methods |
| `test/ui/player/PlayerViewModelTest.kt` | Add `SetRating` tests |

## Dependencies

No new dependencies. All changes use existing Room, Compose, and Material 3.

## Verification

- `assembleDebug` succeeds after each layer
- `test` passes all tests
- Manual:
  - Play a song past threshold (150s or 70%) → play count increments, appears in Top Plays tab
  - Skip a song before threshold → play count does NOT increment
  - Tap favorite button → icon fills, song appears in Favs tab (`Rating.FAVORITE`)
  - Tap again → icon unfills, song removed from Favs tab (`Rating.NONE`)
  - Tap 4-star rating → song appears in Favs tab (`Rating.GREAT`)
  - Tap active star → rating clears to `Rating.NONE`
  - Play several songs → Recent tab shows them deduplicated in reverse chronological order
  - All Library tabs accessible and render correctly