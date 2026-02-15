package com.jpishimwe.syncplayer.model

import java.util.UUID

data class QueueItem(
    val song: Song,
    val index: Int,
    // Unique ID for reordering
    val id: String = UUID.randomUUID().toString(),
)
