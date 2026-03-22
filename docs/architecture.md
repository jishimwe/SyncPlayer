# Architecture

SyncPlayer follows the MVVM (Model-View-ViewModel) architecture pattern with a single Activity and Jetpack Compose for UI.

## High-Level Overview

```
┌─────────────────────────────────────────┐
│           UI Layer (Compose)            │
│  Screens, Composables, ViewModels       │
└────────────┬────────────────────────────┘
             │ StateFlow / Events
             ▼
┌─────────────────────────────────────────┐
│         Domain Layer (optional)         │
│         Use Cases, Entities             │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│          Data Layer                     │
│    Repositories, Data Sources           │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│    Local Database (Room)                │
│    MediaStore Scanner                   │
└─────────────────────────────────────────┘
```

## Package Structure

```
app/src/main/java/com/jpishimwe/syncplayer/
├── ui/
│   ├── theme/              # Color, Theme, Type (pure theme definitions only)
│   ├── effect/             # Visual effects: BlurredBackground, GlassEffect, Modifiers
│   ├── home/               # Main screen coordinating all tabs
│   │   ├── HomeScreen.kt          # PermissionHandler + ViewModel wiring
│   │   └── tabs/                  # One file per tab
│   │       ├── SongsTabScreen.kt
│   │       ├── AlbumsTabScreen.kt
│   │       ├── ArtistsTabScreen.kt
│   │       ├── FavoriteTabScreen.kt
│   │       ├── PlaylistsTabScreen.kt
│   │       └── HistoryTabScreen.kt
│   ├── shared/             # ViewModels and shared detail components
│   │   ├── LibraryViewModel.kt    # Songs/albums/artists + LibraryUiState, SortOrder, LibraryTab
│   │   ├── MetadataViewModel.kt   # Favorites/mostPlayed/recentlyPlayed + MetadataUiState
│   │   ├── DetailHeroImage.kt     # Parallax hero image (shared by album + artist detail)
│   │   ├── DetailTopBar.kt        # Frosted-glass top bar (shared by album + artist detail)
│   │   └── PreviewData.kt         # Sample data for @Preview composables
│   ├── albumdetail/        # Album detail screen
│   │   ├── AlbumDetailScreen.kt
│   │   ├── AlbumDetailTopBar.kt
│   │   ├── AlbumActionBar.kt
│   │   └── AlbumDetailPreviews.kt
│   ├── artistdetail/       # Artist detail screen
│   │   ├── ArtistDetailScreen.kt
│   │   ├── ArtistDetailTopBar.kt
│   │   ├── ArtistSubTabHeader.kt
│   │   └── ArtistDetailPreviews.kt
│   ├── components/         # Reusable UI components used across screens
│   │   ├── SongListItem.kt        # SongItem, SongMenuAction, SongItemVariant
│   │   ├── SortFilterBar.kt       # Sort dropdown + shuffle/play-all
│   │   ├── AlphabeticalIndexSidebar.kt
│   │   ├── MiniPlayer.kt          # Floating pill + MiniPlayerPeek constant
│   │   ├── AlbumItem.kt           # Grid card for albums
│   │   ├── ArtistItem.kt          # Grid card for artists
│   │   ├── PlaylistItem.kt        # Row item for playlists
│   │   ├── PlaylistCollage.kt     # 4-image grid thumbnail
│   │   ├── CollapsibleSection.kt
│   │   ├── QueueSheet.kt          # Bottom sheet for queue
│   │   ├── PlayerControls.kt
│   │   ├── SeekBar.kt
│   │   ├── CircularArtistImage.kt
│   │   └── FrostedGlassPill.kt
│   ├── player/             # Now Playing screen, PlayerViewModel, PlayerEvent
│   │   ├── NowPlayingScreen.kt
│   │   ├── NowPlayingScreenContent.kt
│   │   ├── NowPlayingComponents.kt
│   │   ├── PlayerViewModel.kt
│   │   ├── PlayerUiState.kt
│   │   ├── PlaybackState.kt
│   │   └── PlayerEvent.kt
│   ├── playlists/          # Playlist list + detail, SongPickerSheet
│   ├── settings/           # Sign-in, sync status, manual sync trigger
│   │   ├── SettingsScreen.kt
│   │   ├── SettingsViewModel.kt
│   │   ├── SettingsUiState.kt
│   │   └── SettingsEvent.kt
│   └── navigation/         # NavGraph, Screen routes, top tab row
│       ├── NavGraph.kt            # Routes, CustomTabRow, DockedSearchBar
│       ├── Screens.kt             # Route sealed class + LibraryTab enum
│       └── TopBarComponents.kt
├── util/                   # Non-UI utilities
│   ├── DurationFormatter.kt      # formatDuration(ms) utility
│   └── PermissionHandler.kt      # READ_MEDIA_AUDIO permission flow
├── data/
│   ├── local/              # Room database, DAOs, entities
│   │   ├── SyncPlayerDatabase.kt
│   │   ├── SongDao.kt
│   │   ├── PlaylistDao.kt         # PlaylistEntity, PlaylistSongCrossRef
│   │   ├── QueueDao.kt            # QueueEntity
│   │   ├── ListeningHistoryDao.kt
│   │   ├── ArtistImageDao.kt      # Cached artist images from Deezer
│   │   └── MediaStoreScanner.kt   # Device audio file scanner
│   ├── remote/             # Remote API clients (Deezer artist images)
│   ├── sync/               # Firebase sync layer
│   │   ├── SongFingerprint.kt      # SHA-256 cross-device song key
│   │   ├── FirestoreModels.kt      # Firestore document data classes
│   │   ├── AuthRepository.kt       # AuthState, sign-in/out interface
│   │   ├── AuthRepositoryImpl.kt
│   │   ├── SyncRepository.kt       # Firestore push/pull interface
│   │   ├── SyncRepositoryImpl.kt
│   │   ├── ConflictResolver.kt     # Merge logic per data type
│   │   └── SyncOrchestrator.kt     # Push+pull coordination, SyncStatus
│   ├── SongRepository.kt       # Song + metadata CRUD interface
│   ├── SongRepositoryImpl.kt
│   ├── PlayerRepository.kt     # Media3 playback control interface
│   ├── PlayerRepositoryImpl.kt
│   ├── PlaylistRepository.kt
│   ├── PlaylistRepositoryImpl.kt
│   ├── ArtistImageRepository.kt    # Deezer artist image fetch + cache
│   └── ArtistImageRepositoryImpl.kt
├── model/                  # Domain models (Song, Album, Artist, Playlist, Rating, PlayerUiState, QueueItem, etc.)
├── service/                # PlaybackService (Media3 MediaSessionService)
├── di/                     # Hilt modules
│   ├── AppModule.kt        # Repository bindings
│   ├── DatabaseModule.kt   # Room + DAOs
│   └── SyncModule.kt       # Firebase, AuthRepository, SyncRepository, @ApplicationScope
└── MainActivity.kt         # Single activity; onResume triggers SyncOrchestrator
```

## Layer Responsibilities

### UI Layer

**Location**: `ui/`

**Responsibilities**:
- Display data to the user
- Handle user interactions
- Observe state from ViewModels
- Navigate between screens

**Components**:
- **Screens**: Top-level composables that obtain ViewModels via Hilt
- **ScreenContent**: Stateless composables that receive state/events for testing
- **ViewModels**: Hold UI state, handle events, call repositories
- **UI State**: Data classes representing screen state
- **Events**: Sealed interfaces for user actions

**Example** (actual HomeScreen pattern):
```kotlin
// Screen composable — obtains ViewModels, collects state, delegates to content
@Composable
fun HomeScreen(
    selectedTab: LibraryTab,
    onNavigateToAlbumDetail: (Long, String) -> Unit,
    // ... other navigation callbacks
    libraryViewModel: LibraryViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    metadataViewModel: MetadataViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
    playerViewModel: PlayerViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    PermissionHandler {
        val libraryUiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
        val metadataUiState by metadataViewModel.uiState.collectAsStateWithLifecycle()
        val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
        HomeScreenContent(
            libraryUiState = libraryUiState,
            metadataUiState = metadataUiState,
            playerUiState = playerUiState,
            onSongClick = { songs, index -> playerViewModel.onEvent(PlayerEvent.PlaySongs(songs, index)) },
            // ...
        )
    }
}

// Testable content composable — pure, receives all state
@Composable
fun HomeScreenContent(
    libraryUiState: LibraryUiState,
    metadataUiState: MetadataUiState,
    playerUiState: PlayerUiState,
    onSongClick: (List<Song>, Int) -> Unit,
    // ...
) {
    HorizontalPager(state = pagerState) { page ->
        when (tab[page]) {
            LibraryTab.SONGS -> SongsTabScreen(...)
            LibraryTab.ALBUMS -> AlbumsTabScreen(...)
            // ...
        }
    }
}

// ViewModel — combines reactive flows into a single UiState
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val artistImageRepository: ArtistImageRepository,
) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> =
        combine(songsFlow, albumsFlow, artistsFlow, refreshError) { songs, albums, artists, error ->
            // ...
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)
}
```

**Note**: ViewModels are scoped to the Activity (`LocalActivity.current as ViewModelStoreOwner`) so they survive navigation between screens and are shared across HomeScreen, detail screens, and NavGraph.

### Domain Layer (Future)

**Location**: `domain/` (not yet implemented)

**Responsibilities**:
- Business logic
- Use cases / interactors
- Domain models

**Note**: Currently, business logic lives in ViewModels and Repositories. As the app grows, we may extract complex logic into use cases.

### Data Layer

**Location**: `data/`

**Responsibilities**:
- Single source of truth for data
- Coordinate between local database and MediaStore
- Expose data via repositories
- Data transformation (entities ↔ models)

**Components**:
- **Repositories**: Interface + Impl pairs for testability (e.g., `SongRepository` / `SongRepositoryImpl`)
- **DAOs**: Database access objects (Room)
- **Entities**: Room database tables
- **Data sources**: MediaStore scanner, Deezer API (artist images), Firebase (sync)

**Repositories**:

| Repository | Purpose |
|------------|---------|
| `SongRepository` | Song, album, artist CRUD; search; metadata (ratings, play counts) |
| `PlayerRepository` | Media3 playback control (play, pause, skip, queue, shuffle, repeat) |
| `PlaylistRepository` | Playlist CRUD, song associations, reorder |
| `ArtistImageRepository` | Deezer artist image fetch + local cache |
| `AuthRepository` | Firebase Auth sign-in/out |
| `SyncRepository` | Firestore push/pull for metadata sync |

**Example**:
```kotlin
// Repository interface (for testability — fakes in tests)
interface SongRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>>
    suspend fun refreshLibrary()
    suspend fun setRating(songId: Long, rating: Rating)
    // ...
}

// Implementation
class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val mediaStoreScanner: MediaStoreScanner,
) : SongRepository {
    override fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()
    override suspend fun refreshLibrary() {
        val songs = mediaStoreScanner.scan()
        songDao.upsertSongs(songs)
    }
}
```

### Model Layer

**Location**: `model/`

**Responsibilities**:
- Domain data classes
- Shared across layers
- Pure data, no logic

**Example**:
```kotlin
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val filePath: String,
    val albumArtUri: String?
)
```

## Data Flow

### Unidirectional Data Flow

```
User Interaction → Event → ViewModel → Repository → Database
                                ↓
                            StateFlow
                                ↓
                          UI Update
```

1. User interacts with UI (e.g., clicks a song)
2. UI emits an event to ViewModel
3. ViewModel processes event, calls repository if needed
4. Repository updates database
5. Database change flows back through repository
6. ViewModel updates StateFlow
7. UI recomposes with new state

### Example Flow: Playing a Song

```
1. User clicks song in LibraryScreen
2. UI calls onEvent(LibraryEvent.SongClicked(song))
3. LibraryViewModel processes event:
   - Updates playback state
   - Calls repository.incrementPlayCount(song.id)
4. Repository updates database
5. Database change flows back to ViewModel
6. ViewModel emits new state
7. UI shows song playing
```

## State Management

### StateFlow Pattern

All screen state is managed via `StateFlow<UiState>`:

```kotlin
private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
val state: StateFlow<LibraryUiState> = _state.asStateFlow()
```

### State Variants

Use sealed interfaces for different states:

```kotlin
sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Loaded(
        val songs: List<Song>,
        val albums: List<Album>,
        val artists: List<Artist>,
    ) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}
```

### Event Handling

Use sealed interfaces for user actions:

```kotlin
sealed interface LibraryEvent {
    data class SongClicked(val song: Song) : LibraryEvent
    data class FilterChanged(val filter: ArtistFilter) : LibraryEvent
    data object RefreshRequested : LibraryEvent
}

// In ViewModel
fun onEvent(event: LibraryEvent) {
    when (event) {
        is LibraryEvent.SongClicked -> handleSongClick(event.song)
        is LibraryEvent.FilterChanged -> applyFilter(event.filter)
        LibraryEvent.RefreshRequested -> refreshSongs()
    }
}
```

## Dependency Injection

Uses Hilt for dependency injection.

### Module Structure

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SyncPlayerDatabase {
        return Room.databaseBuilder(
            context,
            SyncPlayerDatabase::class.java,
            "syncplayer.db"
        ).build()
    }
    
    @Provides
    fun provideSongDao(database: SyncPlayerDatabase): SongDao {
        return database.songDao()
    }
}
```

### ViewModel Injection

```kotlin
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: SongRepository
) : ViewModel()
```

## Navigation

Uses Jetpack Navigation Compose. The top-level navigation is a horizontal scrollable tab row with `HorizontalPager` (replacing the original bottom nav bar after the UI redesign). Now Playing is shown as an `AnimatedVisibility` overlay, not a navigation route.

### Current Routes

| Route | Screen | Description |
|-------|--------|-------------|
| `home` | HomeScreen | HorizontalPager with 6 tabs: Songs, Albums, Artists, Favorites, Playlists, History |
| `album_detail/{albumId}/{albumName}` | AlbumDetailScreen | Songs in an album |
| `artist_detail/{artistName}` | ArtistDetailScreen | Songs by an artist |
| `playlist_detail/{playlistId}/{playlistName}` | PlaylistDetailScreen | Songs in a playlist with reorder/add/remove |
| `settings` | SettingsScreen | Sign-in, sync status, manual sync trigger |

Now Playing is an `AnimatedVisibility` overlay managed by `NavGraph`, not a composable route. A `CustomTabRow` at the top provides scrollable tab switching. A `DockedSearchBar` handles search. A persistent `MiniPlayer` floats above content on top-level screens.

### Basic Setup

```kotlin
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                selectedTab = selectedTab,
                onNavigateToAlbumDetail = { id, name -> navController.navigate(...) },
                onNavigateToArtistDetail = { name -> navController.navigate(...) },
                // ...
            )
        }
        composable("album_detail/{albumId}/{albumName}") { /* ... */ }
        composable("artist_detail/{artistName}") { /* ... */ }
        // ...
    }
    // Now Playing overlay (not a route)
    AnimatedVisibility(visible = showNowPlaying) {
        NowPlayingScreen(onDismiss = { showNowPlaying = false })
    }
}
```

## Testable Composables Pattern

Every screen follows this pattern for testability:

```kotlin
// 1. Screen composable (obtains ViewModel, collects state)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    SettingsScreenContent(
        uiState = uiState,
        snackbarMessage = snackbarMessage,
        onEvent = viewModel::onEvent,
    )
}

// 2. Content composable (pure, testable — all state is parameters)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    snackbarMessage: String?,
    onEvent: (SettingsEvent) -> Unit,
) {
    // UI implementation — no ViewModel references
}

// 3. Preview
@Preview
@Composable
private fun SettingsScreenPreview() {
    SyncPlayerTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(/* sample data */),
            snackbarMessage = null,
            onEvent = {},
        )
    }
}
```

**Benefits**:
- Screen can be tested without DI
- Previews work with sample data
- Clear separation of concerns
- ViewModels can be unit tested independently

## Error Handling

### Repository Level

```kotlin
suspend fun getSongById(id: String): Result<Song> {
    return try {
        val song = songDao.getSongById(id)
        if (song != null) {
            Result.success(song.toModel())
        } else {
            Result.failure(NotFoundException("Song not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### ViewModel Level

```kotlin
fun loadSongs() {
    viewModelScope.launch {
        _state.value = LibraryUiState.Loading
        repository.getSongs()
            .catch { error ->
                _state.value = LibraryUiState.Error(
                    message = error.message ?: "Unknown error"
                )
            }
            .collect { songs ->
                _state.value = LibraryUiState.Loaded(songs)
            }
    }
}
```

### UI Level

```kotlin
when (state) {
    LibraryUiState.Loading -> LoadingIndicator()
    is LibraryUiState.Loaded -> SongList(state.songs)
    is LibraryUiState.Error -> ErrorMessage(
        message = state.message,
        onRetry = { onEvent(LibraryEvent.RefreshRequested) }
    )
}
```

## Coroutines and Threading

### Repository

Use `Flow` for reactive data:
```kotlin
fun getSongs(): Flow<List<Song>> = songDao.getAllSongs()
    .map { entities -> entities.map { it.toModel() } }
```

Use `suspend` for one-time operations:
```kotlin
suspend fun addSong(song: Song) {
    songDao.insert(song.toEntity())
}
```

### ViewModel

Use `viewModelScope` for coroutines:
```kotlin
fun refreshSongs() {
    viewModelScope.launch {
        repository.scanMedia()
    }
}
```

### DAO

Room handles threading automatically for `suspend` and `Flow`:
```kotlin
@Query("SELECT * FROM songs")
fun getAllSongs(): Flow<List<SongEntity>>  // Observed on background thread

@Insert
suspend fun insert(song: SongEntity)  // Runs on background thread
```

## Future Architectural Considerations

### Domain Layer

As complexity grows, consider extracting use cases:
```kotlin
class GetFilteredSongsUseCase @Inject constructor(
    private val repository: SongRepository
) {
    operator fun invoke(filter: ArtistFilter): Flow<List<Song>> {
        return repository.getSongs()
            .map { songs -> songs.filter { it.matchesFilter(filter) } }
    }
}
```

### Offline-First with Sync

Implemented in Phase 6. Room is always the source of truth for reads. The `SyncOrchestrator` pushes local changes to Firestore and pulls remote changes on every app foreground.

```
┌──────────────────┐
│    Repository    │
└────────┬─────────┘
         │
         ├──────────► Local DB (Room) ──────► UI (StateFlow)
         │                  ▲
         │                  │ applySyncDelta
         │           SyncOrchestrator
         │            push ↕ pull
         └──────────► Cloud Firestore
                       (offline persistence)
```

**Conflict resolution per data type:**

| Field | Strategy |
|-------|----------|
| `playCount` | max-wins (can only grow) |
| `rating` | last-write-wins by `lastModified` |
| `lastPlayed` | max-wins |
| Playlist (name + songs) | last-write-wins per playlist |
| Listening history | append-only union merge |

**Song matching across devices:** Songs are identified by a SHA-256 fingerprint of `title + artist + album + duration-bucket`. MediaStore IDs are device-specific and are never synced.

### Multi-Module

If the app grows significantly, consider modularization:
```
:app                  # Application module
:feature:library      # Library feature module
:feature:player       # Player feature module
:core:database        # Shared database
:core:model           # Shared models
:core:ui              # Shared UI components
```
