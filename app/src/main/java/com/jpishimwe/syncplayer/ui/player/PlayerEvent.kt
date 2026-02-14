package com.jpishimwe.syncplayer.ui.player

import com.jpishimwe.syncplayer.model.Song

sealed interface PlayerEvent {
    data object PlayPause : PlayerEvent

    data object SkipToNext : PlayerEvent

    data object SkipToPrevious : PlayerEvent

    data class SeekTo(
        val positionMs: Long,
    ) : PlayerEvent

    data class SeekToQueueItem(
        val index: Int,
    ) : PlayerEvent

    data object ToggleShuffle : PlayerEvent

    data object ToggleRepeat : PlayerEvent

    data class PlaySongs(
        val songs: List<Song>,
        val startIndex: Int = 0,
    ) : PlayerEvent

    data class AddToQueue(
        val song: Song,
    ) : PlayerEvent

    data class PlayNext(
        val song: Song,
    ) : PlayerEvent

    data class RemoveFromQueue(
        val queueItemId: String,
    ) : PlayerEvent

    data class ReorderQueue(
        val queueItemId: String,
        val newPosition: Int,
    ) : PlayerEvent
}
