package com.jpishimwe.syncplayer.model

enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR,
    READY,
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL,
}
