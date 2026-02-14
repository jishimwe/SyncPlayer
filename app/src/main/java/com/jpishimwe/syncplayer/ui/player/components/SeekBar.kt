package com.jpishimwe.syncplayer.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun SeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    var tempPosition by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Slider(
            value = (tempPosition ?: currentPosition).toFloat(),
            onValueChange = { tempPosition = it.toLong() },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            onValueChangeFinished = {
                tempPosition?.let { onSeek(it) }
                tempPosition = null
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(tempPosition ?: currentPosition),
                style = MaterialTheme.typography.bodySmall,
            )

            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
