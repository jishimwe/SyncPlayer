package com.jpishimwe.syncplayer.model

enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR,
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL,
}
