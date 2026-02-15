package com.jpishimwe.syncplayer.data

import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePlayerRepository : PlayerRepository {
    private val _playbackState = MutableStateFlow(PlayerUiState())
    override val playbackState: StateFlow<PlayerUiState> = _playbackState.asStateFlow()

    // Recorded calls for assertions
    var lastSeekPosition: Long? = null
    var playCallCount = 0
    var pauseCallCount = 0
    var skipNextCallCount = 0
    var skipPreviousCallCount = 0
    var lastPlayedSongs: List<Song>? = null
    var lastPlayedStartIndex: Int? = null
    var lastQueuedSong: Song? = null
    var lastPlayNextSong: Song? = null
    var lastRemovedId: String? = null
    var lastReorderedId: String? = null
    var lastReorderPosition: Int? = null
    var shuffleToggleCount = 0
    var repeatToggleCount = 0
    var lastSeekToQueueItemIndex: Int? = null

    override suspend fun initialize() {}

    override fun play() {
        playCallCount++
    }

    override fun pause() {
        pauseCallCount++
    }

    override fun skipToNext() {
        skipNextCallCount++
    }

    override fun skipToPrevious() {
        skipPreviousCallCount++
    }

    override fun seekTo(positionMs: Long) {
        lastSeekPosition = positionMs
    }

    override fun toggleShuffle() {
        shuffleToggleCount++
    }

    override fun toggleRepeat() {
        repeatToggleCount++
    }

    override suspend fun playSongs(
        songs: List<Song>,
        startIndex: Int,
    ) {
        lastPlayedSongs = songs
        lastPlayedStartIndex = startIndex
    }

    override suspend fun addToQueue(song: Song) {
        lastQueuedSong = song
    }

    override suspend fun playNext(song: Song) {
        lastPlayNextSong = song
    }

    override suspend fun removeFromQueue(queueItemId: String) {
        lastRemovedId = queueItemId
    }

    override suspend fun reorderQueue(
        queueItemId: String,
        newPosition: Int,
    ) {
        lastReorderedId = queueItemId
        lastReorderPosition = newPosition
    }

    override fun seekToQueueItem(index: Int) {
        lastSeekToQueueItemIndex = index
    }

    // Test helpers
    fun emitState(state: PlayerUiState) {
        _playbackState.value = state
    }
}
