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
- ⏳ Configurable fade settings (duration, enable/disable)
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
    ├── AudioFocusHandler (with fade support)
    └── BecomingNoisyReceiver
    
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

### Audio Focus & Fade Strategy

**Why Manual Audio Focus?**

We implement custom audio focus handling instead of using ExoPlayer's built-in support to enable smooth volume fades during transitions. This provides:

- **Professional audio quality**: Fade in/out instead of abrupt starts/stops
- **Better UX**: Smooth volume ducking during interruptions (navigation, notifications)
- **Graceful transitions**: Phone calls fade out playback smoothly

**Fade Implementation Details**:

- **Fade In**: 500ms, 20 steps = smooth resume after interruptions
- **Fade Out**: 500ms, 20 steps = gentle pause on focus loss
- **Duck**: 300ms to 20% volume for brief interruptions
- **Performance**: Minimal CPU (~0.1%) and battery impact

**Trade-offs**:

| Approach | Pros | Cons |
|----------|------|------|
| **Manual (our approach)** | Smooth fades, full control, professional sound | Slightly more code (~100 lines) |
| **ExoPlayer built-in** | Less code, handles edge cases | Abrupt volume changes, no customization |

**Alternative**: If fade behavior becomes unwanted, simply change `setAudioAttributes(..., false)` to `setAudioAttributes(..., true)` in PlaybackService and remove AudioFocusHandler.

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
kotlinx-coroutines-guava = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-guava", version.ref = "coroutines" }
```

**Note**: `kotlinx-coroutines-guava` provides `.await()` on `ListenableFuture`, needed for `MediaController.Builder.buildAsync().await()` in `PlayerRepository`. Import as:
```kotlin
import kotlinx.coroutines.guava.await
```

**Permissions** (AndroidManifest.xml):
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Required for playback notification on Android 13+ (API 33+). -->
<!-- Must be requested at runtime before starting playback. -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**Build & verify**: `.\gradlew.bat assembleDebug`

---

### Layer 2: Data Models & State

**Files**: 
- `model/PlaybackState.kt`
- `model/QueueItem.kt`
- `model/Song.kt` (add extension functions)
- `data/local/entity/QueueEntity.kt`
- `data/local/dao/QueueDao.kt`
- `data/SongRepository.kt` (add `getSongsByIds`)
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
    val id: String = UUID.randomUUID().toString()
)

// model/Song.kt — add extension functions alongside the existing data class
fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .build()
    )
    .build()

// Returns null if mediaId is not a valid Song ID
fun MediaItem.toSong(): Song? {
    val id = mediaId.toLongOrNull() ?: return null
    return Song(
        id = id,
        title = mediaMetadata.title?.toString() ?: "",
        artist = mediaMetadata.artist?.toString() ?: "",
        album = mediaMetadata.albumTitle?.toString() ?: "",
        albumArtUri = mediaMetadata.artworkUri
    )
}

// data/local/entity/QueueEntity.kt
@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey val id: String,
    val songId: Long,
    var position: Int   // var to allow in-place mutation during reordering
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

**QueueDao**:

```kotlin
// data/local/dao/QueueDao.kt
@Dao
interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY position")
    suspend fun getQueue(): List<QueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToQueue(queueEntity: QueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(queueEntities: List<QueueEntity>)

    @Query("DELETE FROM queue WHERE id = :id")
    suspend fun deleteFromQueue(id: String)

    @Query("DELETE FROM queue")
    suspend fun clearQueue()
}
```

**Add `getSongsByIds` to SongRepository and SongDao**:

```kotlin
// data/SongRepository.kt — add to interface
suspend fun getSongsByIds(ids: List<Long>): Flow<List<Song>>

// data/local/dao/SongDao.kt — add query
@Query("SELECT * FROM songs WHERE id IN (:ids)")
fun getSongsByIds(ids: List<Long>): Flow<List<Song>>
```

**Build & verify**: `.\gradlew.bat assembleDebug`

---

### Layer 3: PlaybackService & Audio Focus

**Files**: 
- `service/PlaybackService.kt`
- `service/AudioFocusHandler.kt`
- `service/BecomingNoisyReceiver.kt`
- `AndroidManifest.xml`

**PlaybackService responsibilities**:
- Initialize ExoPlayer with manual audio focus handling
- Create and manage MediaSession
- Handle playback commands via Player.Listener
- Integrate AudioFocusHandler with smooth volume fades
- Handle "becoming noisy" (headphone disconnect)
- Foreground service lifecycle

**Complete PlaybackService Implementation**:

```kotlin
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var audioFocusHandler: AudioFocusHandler
    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver
    
    // Service coroutine scope for audio focus fades
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // 1. Initialize ExoPlayer with manual audio focus
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false // Manual audio focus handling for custom fades
            )
            .build()
        
        // 2. Setup AudioFocusHandler with fade support
        audioFocusHandler = AudioFocusHandler(
            audioManager = getSystemService(AudioManager::class.java),
            player = player,
            coroutineScope = serviceScope
        )
        
        // 3. Request focus when playback starts
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    audioFocusHandler.requestAudioFocus()
                }
            }
        })
        
        // 4. Setup MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .build()
                }
                
                // Handle custom commands for queue management
                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    // Handle: ADD_TO_QUEUE, PLAY_NEXT, REMOVE_FROM_QUEUE, REORDER_QUEUE
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            })
            .build()
        
        // 5. Register headphone disconnect receiver
        becomingNoisyReceiver = BecomingNoisyReceiver(player)
        registerReceiver(
            becomingNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }
    
    override fun onDestroy() {
        // Clean up in correct order
        audioFocusHandler.abandonAudioFocus()
        unregisterReceiver(becomingNoisyReceiver)
        serviceScope.cancel() // Cancel all fade jobs
        player.release()
        mediaSession.release()
        super.onDestroy()
    }
}
```

**AudioFocusHandler with Smooth Volume Fades**:

This implementation provides professional-quality audio transitions with configurable fade in/out.

```kotlin
class AudioFocusHandler(
    private val audioManager: AudioManager,
    private val player: ExoPlayer,
    private val coroutineScope: CoroutineScope
) {
    private var resumeOnFocusGain = false
    private var volumeBeforeDuck = 1.0f
    private var fadeJob: Job? = null
    
    // Fade configuration - can be made configurable later
    private val fadeInDuration = 500L
    private val fadeOutDuration = 500L
    private val duckDuration = 300L
    private val fadeSteps = 20
    
    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            // android.media.AudioAttributes (framework) — NOT androidx.media3.common.AudioAttributes
            // AudioFocusRequest is a framework API and requires the framework type
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setOnAudioFocusChangeListener { focusChange ->
            handleAudioFocusChange(focusChange)
        }
        .build()
    
    /**
     * Request audio focus before starting playback.
     * @return true if focus granted, false otherwise
     */
    fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(audioFocusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    
    /**
     * Abandon audio focus when stopping playback.
     * Call when: service destroyed, user stops playback, or permanent focus loss.
     */
    fun abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        resumeOnFocusGain = false
        fadeJob?.cancel()
    }
    
    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Gained focus
                if (resumeOnFocusGain) {
                    // Resume after transient loss (e.g., phone call ended)
                    fadeIn {
                        player.play()
                    }
                    resumeOnFocusGain = false
                } else if (player.volume < volumeBeforeDuck) {
                    // Restore volume after ducking
                    fadeVolumeTo(volumeBeforeDuck)
                }
            }
            
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent focus loss (another app started playing)
                fadeOut {
                    player.pause()
                    abandonAudioFocus() // Release focus completely
                }
            }
            
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss (e.g., phone call, alarm)
                if (player.isPlaying) {
                    fadeOut {
                        player.pause()
                        resumeOnFocusGain = true // Resume when focus returns
                    }
                }
            }
            
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Brief interruption (e.g., navigation voice)
                if (player.isPlaying) {
                    volumeBeforeDuck = player.volume
                    fadeVolumeTo(0.2f) // Duck to 20% volume
                }
            }
        }
    }
    
    /**
     * Fade volume in from 0 to target volume.
     * Used when resuming playback after transient focus loss.
     */
    private fun fadeIn(onComplete: () -> Unit = {}) {
        fadeJob?.cancel()
        
        val targetVolume = volumeBeforeDuck
        player.volume = 0f
        onComplete() // Start playing at volume 0
        
        fadeJob = coroutineScope.launch {
            val stepSize = targetVolume / fadeSteps
            val interval = fadeInDuration / fadeSteps
            
            repeat(fadeSteps) { step ->
                delay(interval)
                player.volume = (step + 1) * stepSize
            }
            player.volume = targetVolume // Ensure exact target
        }
    }
    
    /**
     * Fade volume out to 0.
     * Used when losing focus or pausing due to interruption.
     */
    private fun fadeOut(onComplete: () -> Unit = {}) {
        fadeJob?.cancel()
        
        val startVolume = player.volume
        
        fadeJob = coroutineScope.launch {
            val stepSize = startVolume / fadeSteps
            val interval = fadeOutDuration / fadeSteps
            
            repeat(fadeSteps) { step ->
                delay(interval)
                player.volume = startVolume - ((step + 1) * stepSize)
            }
            player.volume = 0f
            onComplete() // Pause after fade completes
        }
    }
    
    /**
     * Fade volume to a specific level.
     * Used for ducking during brief interruptions.
     */
    private fun fadeVolumeTo(targetVolume: Float) {
        fadeJob?.cancel()
        
        val startVolume = player.volume
        val volumeDelta = targetVolume - startVolume
        
        fadeJob = coroutineScope.launch {
            val stepSize = volumeDelta / fadeSteps
            val interval = duckDuration / fadeSteps
            
            repeat(fadeSteps) { step ->
                delay(interval)
                player.volume = startVolume + ((step + 1) * stepSize)
            }
            player.volume = targetVolume
        }
    }
}
```

**Headphone Disconnect Receiver**:

```kotlin
class BecomingNoisyReceiver(
    private val player: ExoPlayer
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            // Headphones disconnected - pause immediately
            player.pause()
        }
    }
}
```

**Manifest Declaration**:

```xml
<service
    android:name=".service.PlaybackService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="true"
    android:permission="android.permission.BIND_MEDIA_BROWSER_SERVICE">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

`BIND_MEDIA_BROWSER_SERVICE` is a system-level permission held only by the Android system and trusted media controllers (lock screen, Bluetooth, Android Auto). Third-party apps cannot bind to the service. This satisfies the `ExportedService` lint warning without suppressing it.

**Key Points**:

1. **Manual Audio Focus**: We disable ExoPlayer's built-in audio focus (`false` in `setAudioAttributes`) to implement custom fade behavior.

2. **Smooth Transitions**: All volume changes use coroutine-based fades (500ms by default) for professional audio quality.

3. **Focus Lifecycle**:
   - Request focus when `player.play()` is called
   - Don't abandon on pause (user might resume)
   - Abandon on permanent loss or service destruction

4. **Performance**: Only 20 volume steps over 500ms = minimal CPU/battery impact.

5. **Future Enhancement**: Fade durations can be made user-configurable through settings.

**Build & verify**: `.\gradlew.bat assembleDebug`

---

### Layer 4: PlayerRepository

**Files**: 
- `data/PlayerRepository.kt`

**Responsibilities**:
- Connect to PlaybackService via MediaController
- Expose playback state as StateFlow via `Player.Listener` bridge
- Translate UI commands to MediaController commands
- Manage queue state (play, add, reorder, remove)
- Persist queue to Room and restore on app restart

**Complete Implementation**:

```kotlin
class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueDao: QueueDao,
    private val songRepository: SongRepository
) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var mediaController: MediaController? = null

    // Defined as a property so initialize() can reference it
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update {
                it.copy(
                    playbackState = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _uiState.update { it.copy(currentSong = mediaItem?.toSong()) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.update {
                it.copy(
                    playbackState = when (playbackState) {
                        Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                        Player.STATE_ENDED -> PlaybackState.ENDED
                        Player.STATE_IDLE -> PlaybackState.IDLE
                        else -> _uiState.value.playbackState
                    }
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.update { it.copy(error = error.message) }
        }
    }

    suspend fun initialize() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        mediaController = MediaController.Builder(context, sessionToken)
            .buildAsync()
            .await() // requires kotlinx-coroutines-guava

        mediaController?.addListener(playerListener)
        restoreQueue()
    }

    // --- Playback commands ---

    fun play() = mediaController?.play()
    fun pause() = mediaController?.pause()
    fun skipToNext() = mediaController?.seekToNext()
    fun skipToPrevious() = mediaController?.seekToPrevious()
    fun seekTo(positionMs: Long) = mediaController?.seekTo(positionMs)

    fun toggleShuffle() {
        mediaController?.shuffleModeEnabled?.let {
            mediaController?.shuffleModeEnabled = !it
        }
    }

    fun toggleRepeat() {
        val repeatMode = mediaController?.repeatMode ?: Player.REPEAT_MODE_OFF
        mediaController?.repeatMode = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF
        }
    }

    // --- Queue management ---

    suspend fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        mediaController?.clearMediaItems()
        mediaController?.addMediaItems(songs.map { it.toMediaItem() })
        mediaController?.prepare()
        mediaController?.seekToDefaultPosition(startIndex)
        mediaController?.play()

        var index = startIndex
        queueDao.clearQueue()
        queueDao.insertList(songs.map { QueueEntity(it.id.toString(), it.id, index++) })
    }

    suspend fun addToQueue(song: Song) {
        mediaController?.addMediaItem(song.toMediaItem())
        queueDao.addToQueue(QueueEntity(song.id.toString(), song.id, queueDao.getQueue().size))
    }

    suspend fun playNext(song: Song) {
        val insertIndex = (mediaController?.currentMediaItemIndex ?: 0) + 1
        mediaController?.addMediaItem(insertIndex, song.toMediaItem())

        val currentSongId = mediaController?.currentMediaItem?.mediaId
        val queue = queueDao.getQueue()
        val currentPosition = queue.find { it.id == currentSongId }?.position ?: 0

        queue.filter { it.position > currentPosition }.forEach { it.position++ }

        queueDao.clearQueue()
        queueDao.insertList(queue + QueueEntity(song.id.toString(), song.id, currentPosition + 1))
    }

    suspend fun removeFromQueue(queueItemId: String) {
        val queue = queueDao.getQueue()
        val removedPosition = queue.find { it.id == queueItemId }?.position ?: return

        val mediaItemIndex = (0 until (mediaController?.mediaItemCount ?: 0))
            .find { mediaController?.getMediaItemAt(it)?.mediaId == queueItemId }
        mediaItemIndex?.let { mediaController?.removeMediaItem(it) }

        queue.filter { it.position > removedPosition }.forEach { it.position-- }

        queueDao.clearQueue()
        queueDao.insertList(queue.filter { it.id != queueItemId })
    }

    suspend fun reorderQueue(queueItemId: String, newPosition: Int) {
        val mediaIndex = (0 until (mediaController?.mediaItemCount ?: 0))
            .find { mediaController?.getMediaItemAt(it)?.mediaId == queueItemId } ?: return
        mediaController?.moveMediaItem(mediaIndex, newPosition)

        val queue = queueDao.getQueue()
        queue.find { it.id == queueItemId }?.let { entity ->
            val oldPosition = entity.position
            if (oldPosition == newPosition) return

            if (oldPosition > newPosition) {
                moveUp(queue, oldPosition, newPosition)
            } else {
                moveDown(queue, oldPosition, newPosition)
            }
            entity.position = newPosition

            queueDao.clearQueue()
            queueDao.insertList(queue)
        }
    }

    suspend fun clearQueue() {
        mediaController?.clearMediaItems()
        queueDao.clearQueue()
    }

    // --- Queue reorder helpers ---

    // Moving item up (oldPosition > newPosition): shift items between newPosition..oldPosition down
    private fun moveUp(queue: List<QueueEntity>, oldPosition: Int, newPosition: Int) {
        queue.filter { it.position in newPosition until oldPosition }.forEach { it.position++ }
    }

    // Moving item down (oldPosition < newPosition): shift items between oldPosition..newPosition up
    private fun moveDown(queue: List<QueueEntity>, oldPosition: Int, newPosition: Int) {
        queue.filter { it.position in (oldPosition + 1)..newPosition }.forEach { it.position-- }
    }

    // --- Persistence ---

    private suspend fun restoreQueue() {
        val queue = queueDao.getQueue()
        if (queue.isEmpty()) return

        val songIdList = queue.map { it.songId }
        val songs = songRepository.getSongsByIds(songIdList).first()
        val positionMap = queue.associateBy { it.songId }
        val sortedSongs = songs.sortedBy { positionMap[it.id]?.position }

        mediaController?.clearMediaItems()
        mediaController?.addMediaItems(sortedSongs.map { it.toMediaItem() })
        mediaController?.prepare()
        mediaController?.seekToDefaultPosition(0)
    }

    // --- Cleanup ---

    fun release() {
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        mediaController = null
    }
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
- [ ] Implement PlaybackService with ExoPlayer (manual audio focus)
- [ ] Implement AudioFocusHandler with fade support
- [ ] Setup MediaSession
- [ ] Add BecomingNoisyReceiver for headphone disconnect
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
- [ ] Test AudioFocusHandler fade behavior with phone calls
- [ ] Verify smooth fade out on headphone disconnect
- [ ] Test Bluetooth controls
- [ ] Test fade behavior with other apps (YouTube, Spotify)
- [ ] Add shuffle and repeat modes
- [ ] Verify no audio glitches during fades

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
- ✅ Smooth fade out on phone call, fade in when call ends
- ✅ Smooth fade out on headphone disconnect
- ✅ Bluetooth controls work
- ✅ Volume ducking during brief interruptions (navigation, etc.)

### Technical Requirements
- ✅ All tests pass
- ✅ No memory leaks (service properly released)
- ✅ Audio focus handled correctly with smooth fades
- ✅ No audio glitches or pops during volume transitions
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

5. **Fade Configuration**: Should fade durations be user-configurable?
   - **Proposal**: Hard-code 500ms for now, make configurable in settings later if users request it
   - **Rationale**: 500ms is industry standard (Spotify, Apple Music), avoid premature optimization

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