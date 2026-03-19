package com.jpishimwe.syncplayer.ui.library

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.ui.components.FrostedGlassPill

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ArtistDetailTopBar(
    artistName: String,
    albumCount: Int,
    songCount: Int,
    onNavigateBack: () -> Unit,
    contentAlpha: Float,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .let { mod ->
                    if (sharedTransitionScope != null) {
                        with(sharedTransitionScope) {
                            mod.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
                        }
                    } else {
                        mod
                    }
                }.alpha(contentAlpha),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back button
        FrostedGlassPill(shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        // Artist name + metadata pill — top-left on the portrait
        FrostedGlassPill(
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$albumCount albums · $songCount songs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        // Overflow menu button
        FrostedGlassPill(
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            IconButton(
                onClick = { /* TODO: overflow menu */ },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}
