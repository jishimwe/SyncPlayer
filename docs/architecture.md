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
│   ├── theme/              # Color, Theme, Type, Shape
│   ├── components/         # Reusable UI components (MiniPlayer, SongListItem, etc.)
│   ├── library/            # Library tabs: Songs, Albums, Artists, Favs, Top Plays, Recent
│   │   ├── LibraryScreen.kt
│   │   ├── LibraryViewModel.kt
│   │   ├── LibraryUiState.kt
│   │   └── LibraryEvent.kt
│   ├── player/             # Now Playing screen, PlayerViewModel, PlayerEvent
│   ├── playlists/          # Playlist list + detail, SongPickerSheet
│   ├── settings/           # Sign-in, sync status, manual sync trigger
│   │   ├── SettingsScreen.kt
│   │   ├── SettingsViewModel.kt
│   │   ├── SettingsUiState.kt
│   │   └── SettingsEvent.kt
│   └── navigation/         # NavGraph, Screen routes, bottom nav
├── data/
│   ├── local/              # Room database, DAOs, entities
│   │   ├── SyncPlayerDatabase.kt
│   │   ├── SongDao.kt
│   │   ├── PlaylistDao.kt      # PlaylistEntity, PlaylistSongCrossRef
│   │   ├── QueueDao.kt         # QueueEntity
│   │   └── ListeningHistoryDao.kt
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
│   ├── PlaylistRepository.kt
│   ├── PlaylistRepositoryImpl.kt
│   ├── PlayerRepository.kt     # Media3 playback control interface
│   └── PlayerRepositoryImpl.kt
├── model/                  # Domain models (Song, Album, Artist, Playlist, Rating, etc.)
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

**Example**:
```kotlin
// Screen with ViewModel
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryScreenContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

// Testable content composable
@Composable
fun LibraryScreenContent(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit
) {
    // UI implementation
}

// ViewModel
class LibraryViewModel @Inject constructor(
    private val repository: SongRepository
) : ViewModel() {
    private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()
    
    fun onEvent(event: LibraryEvent) {
        // Handle events
    }
}
```

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
- **Repositories**: Public API for data access
- **DAOs**: Database access objects
- **Entities**: Room database tables
- **Data sources**: MediaStore scanner, future network sync

**Example**:
```kotlin
// Repository
class SongRepository @Inject constructor(
    private val songDao: SongDao,
    private val mediaStoreScanner: MediaStoreScanner
) {
    fun getSongs(): Flow<List<Song>> = songDao.getAllSongs()
        .map { entities -> entities.map { it.toModel() } }
    
    suspend fun scanMedia() {
        val songs = mediaStoreScanner.scan()
        songDao.insertAll(songs.map { it.toEntity() })
    }
}

// DAO
@Dao
interface SongDao {
    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<SongEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)
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
    data class Success(
        val songs: List<Song>,
        val selectedFilter: ArtistFilter = ArtistFilter.All
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

Uses Jetpack Navigation Compose with type-safe routes.

### Basic Setup

```kotlin
@Composable
fun SyncPlayerNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "library"
    ) {
        composable("library") {
            LibraryScreen(
                onNavigateToPlayer = { navController.navigate("player") }
            )
        }
        composable("player") {
            PlayerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

## Testable Composables Pattern

Every screen follows this pattern for testability:

```kotlin
// 1. Screen composable (uses ViewModel)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onNavigateToPlayer: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToPlayer = onNavigateToPlayer
    )
}

// 2. Content composable (pure, testable)
@Composable
fun LibraryScreenContent(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    // UI implementation
}

// 3. Preview
@Preview
@Composable
private fun LibraryScreenPreview() {
    LibraryScreenContent(
        state = LibraryUiState.Success(
            songs = listOf(/* sample data */)
        ),
        onEvent = {},
        onNavigateToPlayer = {}
    )
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
                _state.value = LibraryUiState.Success(songs)
            }
    }
}
```

### UI Level

```kotlin
when (state) {
    LibraryUiState.Loading -> LoadingIndicator()
    is LibraryUiState.Success -> SongList(state.songs)
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
