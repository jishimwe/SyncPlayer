# Phase 2: Playback - Implementation Plan

**Feature**: Audio playback with Media3, MediaSession, and background playback support  
**Status**: Planning  
**Created**: 2026-02-10

## Overview

Implement a complete audio playback system using Media3 ExoPlayer with MediaSessionService for background playback, notification controls, and lock screen integration. The system will support queue management, audio focus handling, and hardware event responses.

## Goals

### Primary Goals
- ✅ Play local audio files using Media3 ExoPlayer
- ✅ Background playback with MediaSessionService
- ✅ Now-playing screen with standard controls
- ✅ Notification controls (play/pause, next, previous)
- ✅ Lock screen controls via MediaSession
- ✅ Queue management (view, reorder, add next/add to end)
- ✅ Audio focus handling (phone calls, other apps)
- ✅ Hardware event handling (headphone disconnect, Bluetooth controls)
- ✅ Persistent playback state (resume on app restart)

### Future Enhancements (Out of Scope)
- ⏳ Advanced notification controls (shuffle, repeat toggles)
- ⏳ Custom notification layouts
- ⏳ Audio visualizer
- ⏳ Lyrics display
- ⏳ Equalizer/audio effects
- ⏳ Crossfade between tracks
- ⏳ Gapless playback

## Technical Architecture

### Components

```
PlaybackService (MediaSessionService)
    ├── ExoPlayer
    ├── MediaSession
    └── PlaybackStateManager
    
PlayerViewModel
    ├── PlayerUiState
    └── PlayerRepository
    
PlayerRepository
    ├── PlaybackService connection
    └── Queue management
    
UI Layer
    ├── NowPlayingScreen
    ├── MiniPlayer (collapsed state)
    └── PlayerControls (reusable)
```

### Data Flow

```
User Action (UI)
    ↓
PlayerViewModel
    ↓
PlayerRepository
    ↓
PlaybackService
    ↓
ExoPlayer
    ↓
MediaSession (notifies system)
    ↓
Notification / Lock Screen
```

## Implementation Layers

### Layer 1: Dependencies & Gradle Setup

**Files**: `libs.versions.toml`, `app/build.gradle.kts`

**Dependencies to add**:
```toml
[versions]
media3 = "1.5.0"  # Check latest stable when implementing

[libraries]
androidx-media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "media3" }
androidx-media3-session = { module = "androidx.media3:media3-session", version.ref = "media3" }
androidx-media3-ui = { module = "androidx.media3:media3-ui", version.ref = "media3" }
```

**Permissions** (AndroidManifest.xml):
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

**Build & verify**: `.\gradlew.bat assembleDebug`

---

### Layer 2: Data Models & State

**Files**: 
- `model/PlaybackState.kt`
- `model/QueueItem.kt`
- `ui/player/PlayerUiState.kt`

**Models**:

```kotlin
// model/PlaybackState.kt
enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

// model/QueueItem.kt
data class QueueItem(
    val song: Song,
    val id: String = UUID.randomUUID().toString() // Unique ID for reordering
)

// ui/player/PlayerUiState.kt
data class PlayerUiState(
    val currentSong: Song? = null,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val queue: List<QueueItem> = emptyList(),
    val currentQueueIndex: Int = -1,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val error: String? = null
)
```

**Build & verify**: `.\gradlew.bat assembleDebug`

---

### Layer 3: PlaybackService (MediaSessionService)

**Files**: 
- `service/PlaybackService.kt`
- `service/PlaybackNotificationManager.kt`
- `AndroidManifest.xml`

**PlaybackService responsibilities**:
- Initialize ExoPlayer
- Create and manage MediaSession
- Handle playback commands (play, pause, skip)
- Manage audio focus
- Handle "becoming noisy" (headphone disconnect)
- Foreground service lifecycle

**Key implementation points**:

```kotlin
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var audioFocusHandler: AudioFocusHandler
    
    override fun onCreate() {
        // 1. Initialize ExoPlayer
        // 2. Setup MediaSession with callback
        // 3. Register audio focus listener
        // 4. Register BecomingNoisyReceiver (headphone disconnect)
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    // Custom commands for queue management
    private val customCommandCallback = object : MediaSession.SessionCallback {
        override fun onCustomCommand(...) {
            // Handle: ADD_TO_QUEUE, PLAY_NEXT, REMOVE_FROM_QUEUE, REORDER_QUEUE
        }
    }
}
```

**Audio Focus Handling**:
```kotlin
class AudioFocusHandler(private val audioManager: AudioManager) {
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setOnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> // Pause and abandon focus
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> // Pause temporarily
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> // Lower volume
                AudioManager.AUDIOFOCUS_GAIN -> // Resume playback
            }
        }
        .build()
}
```

**Headphone Disconnect**:
```kotlin
private val becomingNoisyReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            player.pause()
        }
    }
}
```

**Manifest declaration**:
```xml
<service
    android:name=".service.PlaybackService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="true">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

**Build & verify**: `.\gradlew.bat assembleDebug`

---

### Layer 4: PlayerRepository

**Files**: 
- `data/PlayerRepository.kt`
- `data/local/dao/QueueDao.kt` (for persistence)

**Responsibilities**:
- Connect to PlaybackService via MediaController
- Expose playback state as StateFlow
- Translate UI commands to MediaController commands
- Manage queue state
- Persist queue and playback position

**Key APIs**:
```kotlin
class PlayerRepository @Inject constructor(
    private val context: Context,
    private val queueDao: QueueDao
) {
    private val _playbackState = MutableStateFlow(PlayerUiState())
    val playbackState: StateFlow<PlayerUiState> = _playbackState.asStateFlow()
    
    private var mediaController: MediaController? = null
    
    suspend fun initialize() {
        // Build MediaController connected to PlaybackService
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        mediaController = MediaController.Builder(context, sessionToken).buildAsync().await()
        
        // Listen to player state changes
        mediaController?.addListener(playerListener)
        
        // Restore saved queue
        restoreQueue()
    }
    
    // Playback commands
    fun play() = mediaController?.play()
    fun pause() = mediaController?.pause()
    fun skipToNext() = mediaController?.seekToNext()
    fun skipToPrevious() = mediaController?.seekToPrevious()
    fun seekTo(positionMs: Long) = mediaController?.seekTo(positionMs)
    
    // Queue management
    suspend fun playSongs(songs: List<Song>, startIndex: Int = 0) { }
    suspend fun addToQueue(song: Song) { }
    suspend fun playNext(song: Song) { }
    suspend fun removeFromQueue(queueItemId: String) { }
    suspend fun reorderQueue(fromIndex: Int, toIndex: Int) { }
    
    // State management
    fun toggleShuffle() { }
    fun toggleRepeatMode() { }
    
    // Persistence
    private suspend fun saveQueue() { }
    private suspend fun restoreQueue() { }
}
```

**Database for persistence**:
```kotlin
@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey val id: String,
    val songId: Long,
    val position: Int
)

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY position")
    suspend fun getQueue(): List<QueueEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QueueEntity>)
    
    @Query("DELETE FROM queue")
    suspend fun clearQueue()
}
```

**Build & verify**: `.\gradlew.bat assembleDebug`

---

### Layer 5: PlayerViewModel

**Files**: 
- `ui/player/PlayerViewModel.kt`
- `ui/player/PlayerEvent.kt`

**Responsibilities**:
- Expose PlayerUiState to UI
- Handle UI events
- Manage playback position updates (for seek bar)
- Format time displays

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository
) : ViewModel() {
    
    val uiState: StateFlow<PlayerUiState> = playerRepository.playbackState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerUiState()
        )
    
    // Update position every second for seek bar
    private val positionUpdateFlow = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(1000)
        }
    }
    
    init {
        viewModelScope.launch {
            playerRepository.initialize()
        }
        
        // Continuously update current position
        viewModelScope.launch {
            positionUpdateFlow.collect {
                // Update position in state
            }
        }
    }
    
    fun onEvent(event: PlayerEvent) {
        when (event) {
            PlayerEvent.PlayPause -> togglePlayback()
            PlayerEvent.SkipNext -> playerRepository.skipToNext()
            PlayerEvent.SkipPrevious -> playerRepository.skipToPrevious()
            is PlayerEvent.Seek -> playerRepository.seekTo(event.positionMs)
            PlayerEvent.ToggleShuffle -> playerRepository.toggleShuffle()
            PlayerEvent.ToggleRepeat -> playerRepository.toggleRepeatMode()
            is PlayerEvent.PlaySongs -> playSongs(event.songs, event.startIndex)
            is PlayerEvent.AddToQueue -> playerRepository.addToQueue(event.song)
            is PlayerEvent.PlayNext -> playerRepository.playNext(event.song)
            is PlayerEvent.RemoveFromQueue -> playerRepository.removeFromQueue(event.queueItemId)
            is PlayerEvent.ReorderQueue -> playerRepository.reorderQueue(event.fromIndex, event.toIndex)
        }
    }
    
    private fun togglePlayback() {
        if (uiState.value.playbackState == PlaybackState.PLAYING) {
            playerRepository.pause()
        } else {
            playerRepository.play()
        }
    }
    
    // Utility functions
    fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 1000 / 60) % 60
        val hours = ms / 1000 / 60 / 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}

sealed interface PlayerEvent {
    data object PlayPause : PlayerEvent
    data object SkipNext : PlayerEvent
    data object SkipPrevious : PlayerEvent
    data class Seek(val positionMs: Long) : PlayerEvent
    data object ToggleShuffle : PlayerEvent
    data object ToggleRepeat : PlayerEvent
    data class PlaySongs(val songs: List<Song>, val startIndex: Int = 0) : PlayerEvent
    data class AddToQueue(val song: Song) : PlayerEvent
    data class PlayNext(val song: Song) : PlayerEvent
    data class RemoveFromQueue(val queueItemId: String) : PlayerEvent
    data class ReorderQueue(val fromIndex: Int, val toIndex: Int) : PlayerEvent
}
```

**Build & verify**: `.\gradlew.bat assembleDebug`

---

### Layer 6: UI Components

**Files**: 
- `ui/player/NowPlayingScreen.kt`
- `ui/player/NowPlayingScreenContent.kt` (testable)
- `ui/player/components/PlayerControls.kt`
- `ui/player/components/SeekBar.kt`
- `ui/player/components/MiniPlayer.kt`
- `ui/player/components/QueueSheet.kt`

#### NowPlayingScreen Layout

```
┌─────────────────────────────┐
│      [< Back]    [Queue]    │  ← Top bar
├─────────────────────────────┤
│                             │
│      ┌─────────────┐        │
│      │             │        │
│      │  Album Art  │        │  ← 300.dp square
│      │             │        │
│      └─────────────┘        │
│                             │
│      Song Title             │  ← Typography.headlineSmall
│      Artist Name            │  ← Typography.bodyMedium
│                             │
│  ────●──────────────  3:45  │  ← Seek bar + duration
│  0:32                       │  ← Current position
│                             │
│      [Shuffle] [Repeat]     │  ← Toggle buttons
│                             │
│   [⏮️]  [⏯️]  [⏭️]          │  ← Playback controls
│                             │
└─────────────────────────────┘
```

**Component breakdown**:

```kotlin
// NowPlayingScreen.kt
@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    NowPlayingScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        formatTime = viewModel::formatTime
    )
}

@Composable
fun NowPlayingScreenContent(
    uiState: PlayerUiState,
    onEvent: (PlayerEvent) -> Unit,
    onNavigateBack: () -> Unit,
    formatTime: (Long) -> String
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = { BackButton(onClick = onNavigateBack) },
                actions = { QueueButton(onClick = { /* Show queue sheet */ }) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.5f))
            
            // Album Art
            AlbumArtwork(
                song = uiState.currentSong,
                modifier = Modifier.size(300.dp)
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Track Info
            TrackInfo(song = uiState.currentSong)
            
            Spacer(Modifier.height(24.dp))
            
            // Seek Bar
            SeekBar(
                currentPosition = uiState.currentPosition,
                duration = uiState.duration,
                onSeek = { onEvent(PlayerEvent.Seek(it)) },
                formatTime = formatTime
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Shuffle & Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ShuffleButton(
                    isEnabled = uiState.isShuffleEnabled,
                    onClick = { onEvent(PlayerEvent.ToggleShuffle) }
                )
                RepeatButton(
                    mode = uiState.repeatMode,
                    onClick = { onEvent(PlayerEvent.ToggleRepeat) }
                )
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Playback Controls
            PlayerControls(
                playbackState = uiState.playbackState,
                onPlayPause = { onEvent(PlayerEvent.PlayPause) },
                onSkipNext = { onEvent(PlayerEvent.SkipNext) },
                onSkipPrevious = { onEvent(PlayerEvent.SkipPrevious) }
            )
            
            Spacer(Modifier.weight(1f))
        }
    }
}
```

**Reusable Components**:

```kotlin
// PlayerControls.kt
@Composable
fun PlayerControls(
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSkipPrevious) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(48.dp)
            )
        }
        
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                imageVector = if (playbackState == PlaybackState.PLAYING) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                contentDescription = if (playbackState == PlaybackState.PLAYING) "Pause" else "Play",
                modifier = Modifier.size(48.dp)
            )
        }
        
        IconButton(onClick = onSkipNext) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

// SeekBar.kt
@Composable
fun SeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier
) {
    var tempPosition by remember { mutableStateOf<Long?>(null) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = (tempPosition ?: currentPosition).toFloat(),
            onValueChange = { tempPosition = it.toLong() },
            onValueChangeFinished = {
                tempPosition?.let { onSeek(it) }
                tempPosition = null
            },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(tempPosition ?: currentPosition),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// MiniPlayer.kt (for bottom of library/other screens)
@Composable
fun MiniPlayer(
    uiState: PlayerUiState,
    onEvent: (PlayerEvent) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small album art
            AsyncImage(
                model = uiState.currentSong?.albumArtUri,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = uiState.currentSong?.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = uiState.currentSong?.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Play/Pause
            IconButton(onClick = { onEvent(PlayerEvent.PlayPause) }) {
                Icon(
                    imageVector = if (uiState.playbackState == PlaybackState.PLAYING) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = "Play/Pause"
                )
            }
            
            // Next
            IconButton(onClick = { onEvent(PlayerEvent.SkipNext) }) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next"
                )
            }
        }
    }
}
```

**Queue Sheet** (Bottom sheet modal):
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<QueueItem>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onRemove: (String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Queue (${queue.size})",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(Modifier.height(16.dp))
            
            LazyColumn {
                itemsIndexed(
                    items = queue,
                    key = { _, item -> item.id }
                ) { index, item ->
                    QueueItemRow(
                        item = item,
                        isPlaying = index == currentIndex,
                        onRemove = { onRemove(item.id) }
                    )
                }
            }
        }
    }
}
```

**Build & verify**: `.\gradlew.bat assembleDebug`

---

### Layer 7: Navigation Integration

**Files**: 
- `ui/navigation/NavGraph.kt` (update existing)
- `ui/library/LibraryScreen.kt` (add MiniPlayer)

**Add route**:
```kotlin
sealed class Screen(val route: String) {
    // ... existing routes
    data object NowPlaying : Screen("now_playing")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
        modifier = modifier
    ) {
        // ... existing composables
        
        composable(Screen.NowPlaying.route) {
            NowPlayingScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
```

**Add MiniPlayer to Library**:
```kotlin
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(), // Shared scope
    onNavigateToNowPlaying: () -> Unit
) {
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        bottomBar = {
            if (playerState.currentSong != null) {
                MiniPlayer(
                    uiState = playerState,
                    onEvent = playerViewModel::onEvent,
                    onClick = onNavigateToNowPlaying
                )
            }
        }
    ) { padding ->
        // ... library content
    }
}
```

**Build & verify**: `.\gradlew.bat assembleDebug`

---

### Layer 8: Testing

**Files**: 
- `test/.../PlayerViewModelTest.kt`
- `test/.../PlayerRepositoryTest.kt`
- `androidTest/.../NowPlayingScreenTest.kt`

**Unit Tests**:

```kotlin
class PlayerViewModelTest {
    
    @Test
    fun `playPause toggles playback state`() = runTest {
        // Given
        val repository = FakePlayerRepository()
        val viewModel = PlayerViewModel(repository)
        
        // When
        viewModel.onEvent(PlayerEvent.PlayPause)
        
        // Then
        assertEquals(PlaybackState.PLAYING, viewModel.uiState.value.playbackState)
        
        // When
        viewModel.onEvent(PlayerEvent.PlayPause)
        
        // Then
        assertEquals(PlaybackState.PAUSED, viewModel.uiState.value.playbackState)
    }
    
    @Test
    fun `seek updates current position`() = runTest {
        val repository = FakePlayerRepository()
        val viewModel = PlayerViewModel(repository)
        
        viewModel.onEvent(PlayerEvent.Seek(30000L))
        
        assertEquals(30000L, repository.lastSeekPosition)
    }
    
    @Test
    fun `formatTime displays correct format`() {
        val viewModel = PlayerViewModel(FakePlayerRepository())
        
        assertEquals("0:32", viewModel.formatTime(32000))
        assertEquals("3:45", viewModel.formatTime(225000))
        assertEquals("1:02:15", viewModel.formatTime(3735000))
    }
}
```

**UI Tests**:
```kotlin
class NowPlayingScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun nowPlayingScreen_displaysCurrentSong() {
        val song = Song(
            id = 1,
            title = "Test Song",
            artist = "Test Artist",
            // ...
        )
        
        val uiState = PlayerUiState(currentSong = song)
        
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = uiState,
                onEvent = {},
                onNavigateBack = {},
                formatTime = { "0:00" }
            )
        }
        
        composeTestRule.onNodeWithText("Test Song").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Artist").assertIsDisplayed()
    }
    
    @Test
    fun playButton_triggersPlayPauseEvent() {
        val events = mutableListOf<PlayerEvent>()
        
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(),
                onEvent = { events.add(it) },
                onNavigateBack = {},
                formatTime = { "0:00" }
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Play").performClick()
        
        assertTrue(events.contains(PlayerEvent.PlayPause))
    }
}
```

**Run tests**: `.\gradlew.bat test`

---

## Implementation Checklist

### Phase 2.1: Core Playback (Days 1-3)
- [ ] Add Media3 dependencies
- [ ] Create PlaybackState and PlayerUiState models
- [ ] Implement PlaybackService with ExoPlayer
- [ ] Setup MediaSession
- [ ] Add foreground service with notification
- [ ] Create PlayerRepository with MediaController
- [ ] Implement basic play/pause/skip commands

### Phase 2.2: UI Layer (Days 4-5)
- [ ] Create PlayerViewModel
- [ ] Build NowPlayingScreen layout
- [ ] Implement SeekBar component
- [ ] Add PlayerControls component
- [ ] Create MiniPlayer component
- [ ] Add navigation to NowPlaying

### Phase 2.3: Queue Management (Day 6)
- [ ] Implement queue operations in repository
- [ ] Add QueueDao for persistence
- [ ] Build QueueSheet UI
- [ ] Add reordering with drag-and-drop
- [ ] Implement "play next" and "add to end"

### Phase 2.4: Audio Focus & Hardware (Day 7)
- [ ] Implement AudioFocusHandler
- [ ] Handle phone calls (pause/resume)
- [ ] Handle headphone disconnect
- [ ] Test Bluetooth controls
- [ ] Add shuffle and repeat modes

### Phase 2.5: Testing & Polish (Day 8)
- [ ] Write unit tests for ViewModel
- [ ] Write unit tests for Repository
- [ ] Write UI tests for NowPlayingScreen
- [ ] Test background playback
- [ ] Test notification controls
- [ ] Test lock screen controls
- [ ] End-to-end playback testing

## Success Criteria

### Functional Requirements
- ✅ Can play audio files from library
- ✅ Playback continues in background
- ✅ Notification shows current track with controls
- ✅ Lock screen shows playback controls
- ✅ Can seek within track
- ✅ Queue can be viewed and modified
- ✅ Playback state persists across app restarts
- ✅ Pauses on phone call, resumes after
- ✅ Pauses on headphone disconnect
- ✅ Bluetooth controls work

### Technical Requirements
- ✅ All tests pass
- ✅ No memory leaks (service properly released)
- ✅ Audio focus handled correctly
- ✅ Smooth UI with no jank
- ✅ Follows MVVM architecture
- ✅ Testable composable pattern used

### Performance
- ⚡ Playback starts within 500ms
- ⚡ Seek responds within 100ms
- ⚡ UI updates at 60fps
- ⚡ Battery drain < 5% per hour of playback

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Media3 API changes with AGP 9 | High | Test with latest stable Media3 before committing to version |
| Audio focus conflicts | Medium | Thoroughly test with other apps (YouTube, Spotify) |
| Service killed by system | High | Implement proper state saving/restoration |
| Queue reordering performance | Low | Use lazy lists with keys, test with large queues (1000+ items) |
| Notification not showing | High | Test on Android 13+ with notification permissions |

## Dependencies

**Blocked by**: Phase 1 (Library & Data Layer) must be complete

**Blocks**: 
- Phase 3 (Playlists) - needs playback to work
- Phase 4 (Metadata Sync) - needs play counts from playback

## Open Questions

1. **Media3 Version**: Confirm Media3 1.5.0 is compatible with AGP 9.0.0
   - **Resolution**: Check compatibility during Layer 1 implementation
   
2. **Queue Size Limits**: Should we limit queue size to prevent memory issues?
   - **Proposal**: No hard limit, but warn user if queue > 1000 items
   
3. **Playback Speed**: Should we support variable playback speed?
   - **Decision**: Out of scope for Phase 2, add to future enhancements

4. **Casting Support**: Chromecast/other casting protocols?
   - **Decision**: Out of scope, evaluate in future phases

## Resources

- [Media3 Documentation](https://developer.android.com/media/media3)
- [MediaSession Guide](https://developer.android.com/media/media3/session/control-playback)
- [Audio Focus](https://developer.android.com/media/optimize/audio-focus)
- [Background Playback](https://developer.android.com/media/media3/session/background-playback)

---

**Next Steps**: 
1. Review this plan for completeness
2. Confirm Media3 version compatibility
3. Begin Layer 1 implementation
4. Create `design.md` after Phase 2 is complete