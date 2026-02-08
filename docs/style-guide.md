# Style Guide

Coding conventions and standards for SyncPlayer.

## General Principles

1. **Compose-first UI** — No XML layouts, no Fragments
2. **State hoisting** — ViewModels own state, composables receive it
3. **Unidirectional data flow** — State flows down, events flow up
4. **Package by feature** — Group related files under `ui/<feature>/`
5. **Prefer `StateFlow`** over `LiveData` for reactive state
6. **Coroutines** for async work — No callbacks, no RxJava
7. **Keep it simple** — Favor readability over cleverness

## Naming Conventions

### Composables

**Format:** `PascalCase`

**Screens:** `<Feature>Screen`
```kotlin
@Composable
fun LibraryScreen()

@Composable
fun PlayerScreen()

@Composable
fun PlaylistDetailScreen()
```

**Content variants:** `<Feature>ScreenContent`
```kotlin
@Composable
fun LibraryScreenContent(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit
)
```

**Reusable components:** Descriptive noun phrases
```kotlin
@Composable
fun SongListItem()

@Composable
fun AlbumCard()

@Composable
fun NowPlayingControls()
```

### ViewModels

**Format:** `<Feature>ViewModel`

```kotlin
class LibraryViewModel
class PlayerViewModel
class PlaylistDetailViewModel
```

### Repositories

**Format:** `<Feature>Repository`

```kotlin
interface SongRepository
class SongRepositoryImpl

interface PlaylistRepository
class PlaylistRepositoryImpl
```

### State Classes

**Format:** `<Feature>UiState`

```kotlin
sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(val songs: List<Song>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}
```

### Event Classes

**Format:** `<Feature>Event`

```kotlin
sealed interface LibraryEvent {
    data class SongClicked(val song: Song) : LibraryEvent
    data object RefreshRequested : LibraryEvent
}
```

### Data Classes

**Format:** `PascalCase` nouns

```kotlin
data class Song(...)
data class Album(...)
data class Playlist(...)
```

### Database Entities

**Format:** `<Model>Entity`

```kotlin
@Entity(tableName = "songs")
data class SongEntity(...)

@Entity(tableName = "playlists")
data class PlaylistEntity(...)
```

### DAOs

**Format:** `<Model>Dao`

```kotlin
@Dao
interface SongDao

@Dao
interface PlaylistDao
```

### Test Files

**Format:** `<ClassName>Test`

```kotlin
class LibraryViewModelTest
class SongRepositoryTest
```

### Constants and Enums

**Constants:** `UPPER_SNAKE_CASE`
```kotlin
const val DEFAULT_PAGE_SIZE = 20
const val MAX_RETRY_ATTEMPTS = 3
```

**Enums:** `PascalCase` for type, `PascalCase` for values
```kotlin
enum class SortOrder {
    Alphabetical,
    DateAdded,
    PlayCount
}
```

## File Organization

### One Top-Level Composable Per File

**Good:**
```
LibraryScreen.kt  → LibraryScreen composable
SongListItem.kt   → SongListItem composable
AlbumCard.kt      → AlbumCard composable
```

**Exception:** Related helper composables can share a file
```kotlin
// SongListItem.kt
@Composable
fun SongListItem(...) {
    // Uses SongArtwork internally
}

@Composable
private fun SongArtwork(...) {
    // Small helper used only by SongListItem
}
```

### Package Structure

**By feature under `ui/`:**
```
ui/
├── library/
│   ├── LibraryScreen.kt
│   ├── LibraryViewModel.kt
│   ├── LibraryUiState.kt
│   ├── LibraryEvent.kt
│   └── SongListItem.kt
├── player/
│   ├── PlayerScreen.kt
│   ├── PlayerViewModel.kt
│   └── ...
└── components/       # Shared across features
    ├── AppTopBar.kt
    └── LoadingIndicator.kt
```

### ViewModel in Same Package as Screen

**Good:**
```
ui/library/
├── LibraryScreen.kt
├── LibraryViewModel.kt
```

**Bad:**
```
ui/library/LibraryScreen.kt
viewmodel/LibraryViewModel.kt  # Don't separate
```

### Max 300 Lines Per File

If a file exceeds 300 lines, split it:

**Before:**
```kotlin
// LibraryScreen.kt (500 lines)
```

**After:**
```kotlin
// LibraryScreen.kt (200 lines)
// SongList.kt (150 lines)
// LibraryTopBar.kt (100 lines)
```

## Code Style

### Follow Kotlin Conventions

Use [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- 4 spaces for indentation
- Max 120 characters per line
- Use trailing commas in multi-line declarations
- Prefer expression functions for single expressions

### Expression Functions

**Good:**
```kotlin
fun getSongTitle(song: Song): String = song.title

fun isPlaying(): Boolean = playbackState == PlaybackState.PLAYING
```

**Avoid:**
```kotlin
fun getSongTitle(song: Song): String {
    return song.title  // Unnecessary braces
}
```

### Trailing Commas

Use trailing commas in multi-line structures:

```kotlin
data class Song(
    val id: String,
    val title: String,
    val artist: String,  // ← Trailing comma
)

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,  // ← Trailing comma
) {
    // ...
}
```

### Data Classes for State

Use data classes for state and models:

```kotlin
data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### Sealed Interfaces for State Variants

Prefer sealed interfaces over sealed classes:

```kotlin
// ✅ Good
sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(val songs: List<Song>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

// ❌ Avoid
sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(val songs: List<Song>) : LibraryUiState()
}
```

### Extension Functions

Use when natural and improves readability:

```kotlin
// ✅ Good
fun SongEntity.toModel(): Song = Song(
    id = id,
    title = title,
    // ...
)

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    title = title,
    // ...
)

// ❌ Avoid forcing it
fun String.toSong(): Song {  // Weird extension
    // ...
}
```

## Comments

### When to Comment

**Do comment:**
- Non-obvious logic or algorithms
- Workarounds for bugs
- Complex business rules
- Why something is done a certain way (if not obvious)

**Don't comment:**
- Self-explanatory code
- Obvious function names
- What the code does (code should show that)

### Examples

**Good:**
```kotlin
// Use exponential backoff to avoid overwhelming the server
// after multiple failed sync attempts
private fun calculateRetryDelay(attempt: Int): Long {
    return (2.0.pow(attempt) * 1000).toLong()
}

// MediaStore requires READ_EXTERNAL_STORAGE on API < 33,
// READ_MEDIA_AUDIO on API >= 33
private fun getRequiredPermission(): String {
    return if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}
```

**Bad:**
```kotlin
// Get song by ID
fun getSongById(id: String): Song {  // Function name already says this
    // ...
}

// Increment play count
playCount++  // Obviously incrementing
```

### TODOs

Format: `// TODO: Description`

```kotlin
// TODO: Add pagination when song library exceeds 1000 items
fun loadSongs() {
    // ...
}
```

## Compose Patterns

### State Hoisting

Composables should be stateless and receive state via parameters:

```kotlin
// ✅ Good - stateless
@Composable
fun SongListItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ...
}

// ❌ Bad - stateful
@Composable
fun SongListItem(song: Song) {
    var isPlaying by remember { mutableStateOf(false) }  // Don't manage state here
    // ...
}
```

### Testable Composables

Every screen has two composables:

```kotlin
// 1. Screen (uses ViewModel)
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

// 2. Content (pure, testable, previewable)
@Composable
fun LibraryScreenContent(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit
) {
    // UI implementation
}
```

### Modifier Parameter

Always include `modifier: Modifier = Modifier` as last parameter:

```kotlin
@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier  // ← Last parameter, default value
) {
    Row(modifier = modifier) {  // ← Use it on root element
        // ...
    }
}
```

### Remember Keys

Use keys when remembering based on dynamic values:

```kotlin
// ✅ Good
val scrollState = rememberLazyListState(
    initialFirstVisibleItemIndex = remember(songs) { 
        songs.indexOfFirst { it.id == currentSongId }
    }
)

// ❌ Bad - will only calculate once
val scrollState = rememberLazyListState(
    initialFirstVisibleItemIndex = songs.indexOfFirst { it.id == currentSongId }
)
```

## ViewModel Patterns

### StateFlow for UI State

```kotlin
class LibraryViewModel @Inject constructor(
    private val repository: SongRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()
    
    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.SongClicked -> handleSongClick(event.song)
            // ...
        }
    }
}
```

### Single Event Handler

Use a single `onEvent` function instead of multiple callbacks:

```kotlin
// ✅ Good
sealed interface LibraryEvent {
    data class SongClicked(val song: Song) : LibraryEvent
    data class FilterChanged(val filter: String) : LibraryEvent
}

fun onEvent(event: LibraryEvent) {
    when (event) {
        is LibraryEvent.SongClicked -> handleSongClick(event.song)
        is LibraryEvent.FilterChanged -> applyFilter(event.filter)
    }
}

// ❌ Bad - multiple callbacks
class LibraryViewModel {
    fun onSongClicked(song: Song) { }
    fun onFilterChanged(filter: String) { }
    fun onRefreshRequested() { }
    // Harder to track and test
}
```

### Loading Data

```kotlin
init {
    loadSongs()
}

private fun loadSongs() {
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

## Repository Patterns

### Flow for Reactive Data

```kotlin
interface SongRepository {
    fun getSongs(): Flow<List<Song>>
    fun getSongById(id: String): Flow<Song?>
}
```

### Suspend for One-Time Operations

```kotlin
interface SongRepository {
    suspend fun addSong(song: Song)
    suspend fun deleteSong(id: String)
    suspend fun incrementPlayCount(id: String)
}
```

### Mapping Entities to Models

```kotlin
class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao
) : SongRepository {
    
    override fun getSongs(): Flow<List<Song>> {
        return songDao.getAllSongs()
            .map { entities -> entities.map { it.toModel() } }
    }
}

private fun SongEntity.toModel(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    // ...
)
```

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
private fun loadSongs() {
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
    LibraryUiState.Loading -> {
        LoadingIndicator()
    }
    is LibraryUiState.Success -> {
        SongList(songs = state.songs)
    }
    is LibraryUiState.Error -> {
        ErrorMessage(
            message = state.message,
            onRetry = { onEvent(LibraryEvent.RefreshRequested) }
        )
    }
}
```

## Resources

### String Resources

Never hardcode user-facing strings:

```kotlin
// ❌ Bad
Text("No songs found")

// ✅ Good
Text(stringResource(R.string.library_empty_state))
```

### Color and Dimension Resources

Use Material Theme values:

```kotlin
// ✅ Good
Text(
    text = song.title,
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurface
)

// ❌ Bad
Text(
    text = song.title,
    fontSize = 16.sp,  // Use typography
    color = Color.Black  // Use colorScheme
)
```

## Testing

### ViewModel Tests

```kotlin
class LibraryViewModelTest {
    
    @Test
    fun `initial state is loading`() = runTest {
        val viewModel = LibraryViewModel(fakeRepository)
        
        assertEquals(LibraryUiState.Loading, viewModel.state.value)
    }
    
    @Test
    fun `loads songs successfully`() = runTest {
        val songs = listOf(testSong1, testSong2)
        val repository = FakeSongRepository(songs)
        val viewModel = LibraryViewModel(repository)
        
        viewModel.state.test {
            assertEquals(LibraryUiState.Loading, awaitItem())
            assertEquals(LibraryUiState.Success(songs), awaitItem())
        }
    }
}
```

### Use Turbine for Flow Testing

```kotlin
@Test
fun `emits songs when loaded`() = runTest {
    repository.getSongs().test {
        assertEquals(emptyList(), awaitItem())
        repository.addSong(testSong)
        assertEquals(listOf(testSong), awaitItem())
    }
}
```

## Anti-Patterns to Avoid

### ❌ Don't Use XML Layouts

Compose only. No `activity_main.xml`, no `fragment_library.xml`.

### ❌ Don't Use LiveData

Use `StateFlow` for consistency:

```kotlin
// ❌ Bad
private val _songs = MutableLiveData<List<Song>>()
val songs: LiveData<List<Song>> = _songs

// ✅ Good
private val _songs = MutableStateFlow<List<Song>>(emptyList())
val songs: StateFlow<List<Song>> = _songs.asStateFlow()
```

### ❌ Don't Use Fragments

Navigation Compose only. No `Fragment`, no `FragmentManager`.

### ❌ Don't Hardcode Strings

Use string resources.

### ❌ Don't Create God Classes

Keep files under 300 lines. Extract components/functions when needed.

### ❌ Don't Make Drive-By Refactors

Stay focused on the requested change. No "while I'm here" changes.

### ❌ Don't Use Deprecated APIs

- No `AsyncTask`
- No `Handler` (use coroutines)
- No `TabRow` (use `PrimaryTabRow`)

### ❌ Don't Hardcode Dependency Versions

Use `gradle/libs.versions.toml`.

## Code Review Checklist

Before considering code complete:

- ✅ Follows naming conventions
- ✅ Testable composable pattern used (Screen + ScreenContent)
- ✅ No hardcoded strings
- ✅ Error states handled in UI
- ✅ Files under 300 lines
- ✅ No drive-by refactors
- ✅ `assembleDebug` succeeds
- ✅ `test` passes
- ✅ No new warnings
