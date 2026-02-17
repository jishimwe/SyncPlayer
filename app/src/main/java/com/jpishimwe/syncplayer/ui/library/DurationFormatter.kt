package com.jpishimwe.syncplayer.ui.library

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return if (hours > 12) {
        "${hours - 12}:${minutes.toString().padStart(2, '0')}"
    } else if (hours in 3..12) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
