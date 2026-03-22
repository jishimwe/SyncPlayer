package com.jpishimwe.syncplayer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.ui.theme.myAccentColor
import com.jpishimwe.syncplayer.ui.effect.noRippleClickable

@Composable
fun SongMenuOverflow(
    actions: List<SongMenuAction>,
    onAction: (SongMenuAction) -> Unit,
    isPlaying: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val iconTint =
        if (isPlaying) {
            myAccentColor.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        }

    Box {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = "More options",
            tint = iconTint,
            modifier = Modifier.size(48.dp).noRippleClickable { expanded = true }.padding(8.dp),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text =
                                when (action) {
                                    SongMenuAction.PlayNext -> "Play next"
                                    SongMenuAction.PlayNow -> "Play now"
                                    SongMenuAction.AddToQueue -> "Add to queue"
                                    SongMenuAction.AddToPlaylist -> "Add to playlist"
                                    SongMenuAction.GoToArtist -> "Go to artist"
                                    SongMenuAction.GoToAlbum -> "Go to album"
                                },
                        )
                    },
                    onClick = {
                        expanded = false
                        onAction(action)
                    },
                )
            }
        }
    }
}
