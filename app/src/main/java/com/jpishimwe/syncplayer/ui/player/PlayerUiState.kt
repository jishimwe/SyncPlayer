package com.jpishimwe.syncplayer.ui.player

import com.jpishimwe.syncplayer.model.QueueItem
import com.jpishimwe.syncplayer.model.Song

data class PlayerUiState(
    val currentSong: Song? = null,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val queue: List<QueueItem> = emptyList(),
    val currentQueueIndex: Int = -1,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val error: String? = null,
)
