package com.jpishimwe.syncplayer.ui.artistdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.effect.frostedGlassRendered
import com.jpishimwe.syncplayer.ui.effect.gradientBorderStroke
import com.jpishimwe.syncplayer.ui.theme.myAccentColor
import com.jpishimwe.syncplayer.ui.effect.noRippleClickable

/** Sub-tab options shown in the artist detail sticky header. */
internal enum class ArtistSubTab { Songs, Albums }

private val TabBarShape = RoundedCornerShape(8.dp)

/**
 * Sticky sub-tab header for the artist detail screen.
 * Shows Songs | Albums toggle plus shuffle / play-all actions.
 */
@Composable
internal fun ArtistSubTabHeader(
    selectedSubTab: ArtistSubTab,
    onSubTabChanged: (ArtistSubTab) -> Unit,
    songs: List<Song>,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
    ) {
        // Outer container: accent border + frosted glass
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .gradientBorderStroke()
                    .clip(TabBarShape),
        ) {
            // Frosted glass background layer
            Box(modifier = Modifier.matchParentSize().frostedGlassRendered())

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Sub-tab labels
                ArtistSubTab.entries.forEach { tab ->
                    val isSelected = tab == selectedSubTab
                    Text(
                        text = tab.name,
                        style =
                            if (isSelected) {
                                MaterialTheme.typography.titleLarge
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                        color = if (isSelected) myAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .noRippleClickable { onSubTabChanged(tab) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }

                Spacer(Modifier.weight(1f))

                // Shuffle button
                IconButton(onClick = { if (songs.isNotEmpty()) onSongClick(songs.shuffled(), 0) }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Play all button
                IconButton(onClick = { if (songs.isNotEmpty()) onSongClick(songs, 0) }) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play all",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
