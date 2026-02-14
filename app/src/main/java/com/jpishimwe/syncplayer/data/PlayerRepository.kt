package com.jpishimwe.syncplayer.data

import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

interface PlayerRepository {
    val playbackState: StateFlow<PlayerUiState>

    suspend fun initialize()

    fun play()

    fun pause()

    fun skipToNext()

    fun skipToPrevious()

    fun seekTo(positionMs: Long)

    fun toggleShuffle()

    fun toggleRepeat()

    suspend fun playSongs(
        songs: List<Song>,
        startIndex: Int = 0,
    )

    suspend fun addToQueue(song: Song)

    suspend fun playNext(song: Song)

    suspend fun removeFromQueue(queueItemId: String)

    suspend fun reorderQueue(
        queueItemId: String,
        newPosition: Int,
    )

    fun seekToQueueItem(index: Int)
}
