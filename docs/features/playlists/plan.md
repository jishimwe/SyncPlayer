# Playlists — Plan (Updated)

## Context

Phases 1-3 built library browsing, playback, and library→playback navigation. Users can browse songs/albums/artists and play them. Phase 4 adds playlist management: create, rename, delete playlists; add, remove, reorder songs within them; and a bottom navigation bar to switch between Library and Playlists.

## Changes from Original Plan

### Design Decisions Made During Implementation

1. **Song Picker Sync Behavior** (Layer 6)
   - Original plan: Add-only (filter out already-present songs)
   - **Updated**: Full sync — compute diff and call both `addSongToPlaylist` and `removeSongFromPlaylist`
   - Rationale: Better UX — unchecking an existing song should remove it from playlist
   - Implementation: `onConfirm` computes `toAdd` and `toRemove` sets and fires both events

2. **Navigation State Preservation** (Layer 7)
   - Original plan: Basic navigation without state restoration
   - **Updated**: Added `saveState`, `restoreState`, and `launchSingleTop` to bottom nav
   - Applies to: Both Library and Playlists tabs
   - Behavior: Switching between tabs preserves scroll position and navigation stack

3. **Repository Clear-Then-Replace Pattern** (Layer 3)
   - Clarification: `reorderSongs` uses `clearPlaylistSongs()` + `replacePlaylistSongs()` instead of individual updates
   - Reason: `PlaylistSongCrossRef` has auto-generated primary key, making individual position updates complex
   - Alternative considered: Using `@Update` (rejected because we don't know the auto-generated IDs)

4. **Callback Parameter Patterns** (Layers 5-6)
   - Rule established: Child composables capture data from scope when possible
   - Use `() -> Unit` when child just signals and parent has the data
   - Use `(T) -> Unit` when child generates new data (text input, user selection)
   - Examples:
     - `PlaylistListItem.onRenamePlaylist: () -> Unit` (parent has playlist object)
     - `RenamePlaylistDialog.onRenamePlaylist: (Long, String) -> Unit` (child has new name)

### Bug Fixes Applied

1. **PlaylistDao.getPlaylistById** return type: `Flow<PlaylistEntity?>` (nullable)
2. **PlaylistRepository.getPlaylistById** return type: `Flow<Playlist?>` (nullable)
3. **RenamePlaylist event** added `isNotBlank()` guard in ViewModel (consistency with CreatePlaylist)
4. **DeletePlaylistDialog** signature: `onDeletePlaylist: () -> Unit` (not `(Playlist) -> Unit`)
5. **Song Picker dismiss** calls `onDismiss()` after confirm button

## Scope

**Included:**
- Room entities for playlists and playlist-song associations
- Create / rename / delete playlists
- Add / remove / reorder songs in a playlist
- Playlist detail screen (tap song to play playlist from that index)
- Song picker dialog in PlaylistDetailScreen with full sync (add + remove)
- Bottom navigation bar (Library | Playlists) with state preservation, visible only on top-level screens
- MiniPlayer coexists with bottom nav (stacked in a Column)

**Excluded:**
- Adding songs to playlist from LibraryScreen context menus (future enhancement)
- Play button on playlist list items (noted as future improvement)
- Playlist import/export
- Smart playlists or auto-generated playlists
- Search within song picker

## Approach

Follow the exact patterns established in Phases 1-3:
- **Entity pattern**: `@Entity` data classes, no `@ForeignKey`, DAO with `Flow` reads and `suspend fun` writes
- **Repository pattern**: interface + `@Inject` impl, thin delegation over DAO
- **ViewModel pattern**: sealed `UiState` interface, `map`/`combine` + `stateIn`, event dispatch via `onEvent()`
- **Screen pattern**: `FooScreen()` (ViewModel-connected) + `FooScreenContent()` (testable, no ViewModel)
- **Navigation pattern**: `Screen` sealed class entries + `composable()` blocks in NavGraph
- **Activity-scoped ViewModels**: shared via `hiltViewModel(LocalActivity.current as ViewModelStoreOwner)`

`PlaylistViewModel` gets both `PlaylistRepository` and `SongRepository` injected — the latter provides `getAllSongs()` for the song picker without a separate ViewModel.

No new dependencies needed — `reorderable`, `material-icons-extended`, and Room are all in the build already.

## Tasks

### Layer 1: Entities and DAO

**New file:** `data/local/PlaylistDao.kt`

Entities and DAO co-located (same pattern as `QueueDao.kt`):

```kotlin
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "playlist_songs")
data class PlaylistSongCrossRef(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val songId: Long,
    val position: Int,
)

@Dao
interface PlaylistDao {
    // Playlist CRUD
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistById(playlistId: Long): Flow<PlaylistEntity?>

    // Song membership
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replacePlaylistSongs(songs: List<PlaylistSongCrossRef>)

    @Query("""
        SELECT songs.* FROM songs
        INNER JOIN playlist_songs ON songs.id = playlist_songs.songId
        WHERE playlist_songs.playlistId = :playlistId
        ORDER BY playlist_songs.position ASC
    """)
    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    fun getSongCountForPlaylist(playlistId: Long): Flow<Int>
}
```

**Modify:** `data/local/SyncPlayerDatabase.kt`
- Add `PlaylistEntity::class, PlaylistSongCrossRef::class` to `entities` array
- Bump `version` from 2 to 3 (`fallbackToDestructiveMigration` handles it — no migration script)
- Add `abstract fun playlistDao(): PlaylistDao`

**Modify:** `di/DatabaseModule.kt`
- Add `@Provides fun providePlaylistDao(db: SyncPlayerDatabase): PlaylistDao = db.playlistDao()`

**Build & verify:** `assembleDebug`

---

### Layer 2: Domain model

**New file:** `model/Playlist.kt`

```kotlin
data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songCount: Int = 0,
)
```

`songCount` is a convenience field for the list screen subtitle. Populated by the repository.

---

### Layer 3: Repository

**New file:** `data/PlaylistRepository.kt`

```kotlin
interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getPlaylistById(playlistId: Long): Flow<Playlist?>
    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>>
    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(playlistId: Long, newName: String)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    suspend fun reorderSongs(playlistId: Long, orderedSongIds: List<Long>)
}
```

**New file:** `data/PlaylistRepositoryImpl.kt`

```kotlin
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { entities ->
            entities.map { Playlist(it.id, it.name, it.createdAt) }
        }

    override fun getPlaylistById(playlistId: Long): Flow<Playlist?> =
        playlistDao.getPlaylistById(playlistId).map { entity ->
            entity?.let { Playlist(it.id, it.name, it.createdAt) }
        }

    override fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> =
        playlistDao.getSongsForPlaylist(playlistId)

    override suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name.trim(), createdAt = System.currentTimeMillis()))

    override suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val entity = playlistDao.getPlaylistById(playlistId).first() ?: return
        playlistDao.updatePlaylist(entity.copy(name = newName.trim()))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.clearPlaylistSongs(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val position = playlistDao.getSongCountForPlaylist(playlistId).first()
        playlistDao.addSongToPlaylist(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId, position = position)
        )
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) =
        playlistDao.removeSongFromPlaylist(playlistId, songId)

    override suspend fun reorderSongs(playlistId: Long, orderedSongIds: List<Long>) {
        playlistDao.clearPlaylistSongs(playlistId)
        playlistDao.replacePlaylistSongs(
            orderedSongIds.mapIndexed { index, songId ->
                PlaylistSongCrossRef(playlistId = playlistId, songId = songId, position = index)
            }
        )
    }
}
```

**Modify:** `di/AppModule.kt`
- Add `@Binds abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository`

**Build & verify:** `assembleDebug`

---

### Layer 4: ViewModel

**New file:** `ui/playlists/PlaylistEvent.kt`

```kotlin
sealed interface PlaylistEvent {
    data class CreatePlaylist(val name: String) : PlaylistEvent
    data class RenamePlaylist(val playlistId: Long, val newName: String) : PlaylistEvent
    data class DeletePlaylist(val playlistId: Long) : PlaylistEvent
    data class AddSongsToPlaylist(val playlistId: Long, val songIds: List<Long>) : PlaylistEvent
    data class RemoveSongFromPlaylist(val playlistId: Long, val songId: Long) : PlaylistEvent
    data class RemoveSongsFromPlaylist(val playlistId: Long, val songIds: List<Long>) : PlaylistEvent
    data class ReorderSongs(val playlistId: Long, val orderedSongIds: List<Long>) : PlaylistEvent
}
```

**New file:** `ui/playlists/PlaylistUiState.kt`

```kotlin
sealed interface PlaylistUiState {
    data object Loading : PlaylistUiState
    data class Loaded(val playlists: List<Playlist>) : PlaylistUiState
}
```

**New file:** `ui/playlists/PlaylistViewModel.kt`

```kotlin
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
) : ViewModel() {

    val uiState: StateFlow<PlaylistUiState> =
        playlistRepository.getAllPlaylists()
            .map { PlaylistUiState.Loaded(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistUiState.Loading)

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> =
        playlistRepository.getSongsForPlaylist(playlistId)

    fun getAllSongs(): Flow<List<Song>> =
        songRepository.getAllSongs()

    fun onEvent(event: PlaylistEvent) {
        when (event) {
            is PlaylistEvent.CreatePlaylist -> viewModelScope.launch {
                if (event.name.isNotBlank()) playlistRepository.createPlaylist(event.name)
            }
            is PlaylistEvent.RenamePlaylist -> viewModelScope.launch {
                if (event.newName.isNotBlank()) playlistRepository.renamePlaylist(event.playlistId, event.newName)
            }
            is PlaylistEvent.DeletePlaylist -> viewModelScope.launch {
                playlistRepository.deletePlaylist(event.playlistId)
            }
            is PlaylistEvent.AddSongsToPlaylist -> viewModelScope.launch {
                event.songIds.forEach { playlistRepository.addSongToPlaylist(event.playlistId, it) }
            }
            is PlaylistEvent.RemoveSongFromPlaylist -> viewModelScope.launch {
                playlistRepository.removeSongFromPlaylist(event.playlistId, event.songId)
            }
            is PlaylistEvent.RemoveSongsFromPlaylist -> viewModelScope.launch {
                event.songIds.forEach { playlistRepository.removeSongFromPlaylist(event.playlistId, it) }
            }
            is PlaylistEvent.ReorderSongs -> viewModelScope.launch {
                playlistRepository.reorderSongs(event.playlistId, event.orderedSongIds)
            }
        }
    }
}
```

**Build & verify:** `assembleDebug`

---

### Layer 5: UI — Playlists list screen

**New file:** `ui/playlists/PlaylistsScreen.kt`

Two composables following the testable pattern:

`PlaylistsScreen` — outer shell:
- Gets `PlaylistViewModel` activity-scoped
- Collects `uiState`
- Calls `viewModel.onEvent()` directly (no separate `onEvent` parameter)
- Wires `onNavigateToPlaylistDetail`

`PlaylistsScreenContent` — testable inner:
- Parameters: `uiState: PlaylistUiState`, `onCreatePlaylist`, `onRenamePlaylist`, `onDeletePlaylist`, `onPlaylistClick`
- `Scaffold` with `TopAppBar("Playlists")` + `FloatingActionButton` (Add icon)
- `when` statement handles `Loading` and `Loaded` states
- `LazyColumn` of playlist items when `Loaded`
- Each item: `ListItem` with playlist name, trailing `IconButton` (MoreVert) → `DropdownMenu` with Rename/Delete
- Empty state: "No playlists yet" message when list is empty
- Dialog state (`showCreateDialog`, `renameTarget: Playlist?`, `deleteTarget: Playlist?`) as local `remember` state
- `CreatePlaylistDialog`: `AlertDialog` + `TextField`, confirm enabled when non-blank
- `RenamePlaylistDialog`: same, pre-filled with `playlist.name`
- `DeleteConfirmationDialog`: destructive confirm with playlist name

**Callback pattern clarification:**
- `PlaylistListItem` uses `() -> Unit` callbacks (signals only, parent captures playlist from scope)
- Dialogs use typed callbacks when they generate new data:
  - `CreatePlaylistDialog`: `onCreatePlaylist: (String) -> Unit`
  - `RenamePlaylistDialog`: `onRenamePlaylist: (Long, String) -> Unit`
  - `DeletePlaylistDialog`: `onDeletePlaylist: () -> Unit`

**Build & verify:** `assembleDebug`

---

### Layer 6: UI — Playlist detail screen + song picker

**New file:** `ui/playlists/PlaylistDetailScreen.kt`

Same pattern as `AlbumDetailScreen`:

`PlaylistDetailScreen` — outer shell:
- Gets `PlaylistViewModel` + `PlayerViewModel` activity-scoped
- Collects `getSongsForPlaylist(playlistId)` and `getAllSongs()` via `collectAsStateWithLifecycle`
- Song tap fires `PlayerEvent.PlaySongs(songs, index)` + `onNavigateToNowPlaying()`

`PlaylistDetailScreenContent` — testable inner:
- Parameters: `playlistName`, `songs`, `allSongs`, `onSongClick`, `onRemoveSong`, `onRemoveSongs`, `onReorderSongs`, `onAddSongs`, `onNavigateBack`
- `TopAppBar` with back button and playlist name
- `FloatingActionButton` (Add icon) → opens `SongPickerSheet`
- Drag-to-reorder via `rememberReorderableLazyListState` (same pattern as `QueueSheet`)
- Each song: `PlaylistSongItem` + trailing delete `IconButton`
- `PlaylistSongItem`: `ListItem` with drag handle + album art in `leadingContent`, title/artist, delete button in `trailingContent`

**New file:** `ui/playlists/SongPickerSheet.kt`

`ModalBottomSheet` showing all songs with checkboxes:
- Pre-checks songs already in the playlist (via `selectedSongs: List<Song>`)
- Maintains local state: `var songsToAdd by remember { mutableStateOf(selectedSongs.map { it.id }) }`
- Confirm button computes diff and calls both:
  - `onAddSongs((songsToAdd - selectedSongIds).toSet().toList())`
  - `onRemoveSongs((selectedSongIds - songsToAdd.toSet()).toSet().toList())`
  - Then calls `onDismiss()`
- `LazyColumn` of `SongSelectorItem`s (song title + artist + checkbox + album art)
- Full-width `Button` at bottom with "Confirm" text

**Build & verify:** `assembleDebug`

---

### Layer 7: Bottom navigation

**Modify:** `ui/navigation/NavGraph.kt`

Add routes to `Screen` sealed class:
```kotlin
data object Playlists : Screen("playlists")
data object PlaylistDetail : Screen("playlist_detail/{playlistId}/{playlistName}") {
    fun createRoute(playlistId: Long, playlistName: String) =
        "playlist_detail/$playlistId/${Uri.encode(playlistName)}"
}
```

Add `BottomNavDestination` enum:
```kotlin
private enum class BottomNavDestination(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
) {
    LIBRARY(Screen.Library, "Library", Icons.Default.LibraryMusic),
    PLAYLISTS(Screen.Playlists, "Playlists", Icons.AutoMirrored.Filled.PlaylistPlay),
}
```

Replace `Scaffold.bottomBar` content with a `Column`:
```kotlin
bottomBar = {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomNav = currentRoute in setOf(Screen.Library.route, Screen.Playlists.route)

    Column {
        if (showBottomNav) {
            NavigationBar {
                BottomNavDestination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.screen.route,
                        onClick = {
                            navController.navigate(dest.screen.route) {
                                popUpTo(Screen.Library.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(dest.label) },
                    )
                }
            }
        }

        if (playerState.currentSong != null && !isOnNowPlayingScreen) {
            MiniPlayer(...)
        }
    }
}
```

Add `composable` blocks for `Screen.Playlists` and `Screen.PlaylistDetail` in `NavHost`.

**Navigation state preservation:**
- `saveState = true`: Remember scroll position and nav stack when leaving tab
- `restoreState = true`: Return to saved state when returning to tab
- `launchSingleTop = true`: Prevent duplicate screens when tapping current tab
- `popUpTo(Screen.Library.route)`: Clear back stack to top-level

**Build & verify:** `assembleDebug`

---

### Layer 8: Tests

**New file:** `test/data/FakePlaylistRepository.kt`
- `MutableStateFlow<List<Playlist>>` for `getAllPlaylists()`
- Records `createCallCount`, `lastCreatedName`, etc.
- `createPlaylist` mutates the flow (adds new playlist)
- Pattern mirrors `FakeSongRepository`

**New file:** `test/ui/playlists/PlaylistViewModelTest.kt`
- JUnit 5 with `MainDispatcherRule` (same as `LibraryViewModelTest`)
- Tests:
  - `uiState transitions to Loaded with playlists`
  - `CreatePlaylist event calls repository`
  - `CreatePlaylist with blank name does NOT call repository`
  - `RenamePlaylist event calls repository`
  - `RenamePlaylist with blank name does NOT call repository`
  - `DeletePlaylist event calls repository`

**Verify:** `gradlew.bat test`

---

## Files summary

### New files (12)

| File | Purpose |
|------|---------|
| `data/local/PlaylistDao.kt` | `PlaylistEntity`, `PlaylistSongCrossRef`, `PlaylistDao` |
| `model/Playlist.kt` | Domain model |
| `data/PlaylistRepository.kt` | Repository interface |
| `data/PlaylistRepositoryImpl.kt` | Repository implementation |
| `ui/playlists/PlaylistEvent.kt` | Event sealed interface |
| `ui/playlists/PlaylistUiState.kt` | UI state sealed interface |
| `ui/playlists/PlaylistViewModel.kt` | ViewModel |
| `ui/playlists/PlaylistsScreen.kt` | List screen + content + dialogs |
| `ui/playlists/PlaylistDetailScreen.kt` | Detail screen + content |
| `ui/playlists/SongPickerSheet.kt` | Song picker bottom sheet with sync logic |
| `test/data/FakePlaylistRepository.kt` | Fake for tests |
| `test/ui/playlists/PlaylistViewModelTest.kt` | ViewModel tests |

### Modified files (4)

| File | Change |
|------|--------|
| `data/local/SyncPlayerDatabase.kt` | Add 2 entities, bump version to 3, add `playlistDao()` |
| `di/DatabaseModule.kt` | Add `providePlaylistDao()` |
| `di/AppModule.kt` | Add `@Binds` for `PlaylistRepository` |
| `ui/navigation/NavGraph.kt` | Add routes, bottom nav bar with state preservation, 2 composable entries |

## Dependencies

No new dependencies. `reorderable`, `material-icons-extended`, and Room are already in the build.

## Verification

- `assembleDebug` after each layer
- `test` after Layer 8
- Manual: create playlist → appears in list
- Manual: rename and delete playlist via 3-dot menu
- Manual: open playlist detail → FAB → song picker → add/remove songs
- Manual: drag-reorder songs in playlist detail → persisted on re-open
- Manual: remove song from playlist
- Manual: tap song in playlist detail → NowPlaying with playlist queue
- Manual: bottom nav switches between Library and Playlists (state preserved)
- Manual: MiniPlayer visible below bottom nav when song is playing
- Manual: bottom nav hidden on detail screens and NowPlaying

## Implementation checklist

- [x] Layer 1: Entities, DAO, database migration, DI
- [x] Layer 2: Playlist domain model
- [x] Layer 3: Repository interface + impl + DI binding
- [x] Layer 4: PlaylistEvent, PlaylistUiState, PlaylistViewModel
- [x] Layer 5: PlaylistsScreen + PlaylistsScreenContent (list, dialogs)
- [x] Layer 6: PlaylistDetailScreen + PlaylistDetailScreenContent + SongPickerSheet
- [x] Layer 7: Bottom navigation + routes in NavGraph with state preservation
- [ ] Layer 8: FakePlaylistRepository + PlaylistViewModelTest

## Future Enhancements (Out of Scope)

1. Play button directly on playlist list items (skip navigation to detail)
2. Add to playlist from LibraryScreen context menus
3. Playlist import/export
4. Search within song picker
5. Smart/auto-generated playlists