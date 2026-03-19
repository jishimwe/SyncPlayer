package com.jpishimwe.syncplayer.ui.player

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
