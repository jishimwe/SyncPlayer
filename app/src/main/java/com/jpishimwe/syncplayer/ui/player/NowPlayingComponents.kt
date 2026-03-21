package com.jpishimwe.syncplayer.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jpishimwe.syncplayer.R
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song

@Composable
fun FavoriteButton(
    rating: Rating,
    onClick: () -> Unit,
) {
    val isFavorite = rating == Rating.FAVORITE
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(
            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
fun StarRating(
    rating: Rating,
    onSetRating: (stars: Rating) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Rating.entries
            .filter { it != Rating.NONE }
            .forEach { star ->
                IconButton(onClick = { onSetRating(if (rating == star) Rating.NONE else star) }) {
                    Icon(
                        if (star.value <= rating.value) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "${star.value} stars",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
    }
}

@Composable
fun TrackInfo(song: Song?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val titleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        val albumShape = RoundedCornerShape(4.dp)
        val artistShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(titleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = song?.title ?: "",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(albumShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = song?.album ?: "-",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Light),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(4.dp))

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(artistShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = song?.artist ?: "",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Light),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AlbumArtwork(
    song: Song?,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = song?.albumArtUri,
        contentDescription = "Album art",
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Image(
                    painter = painterResource(R.drawable.album_default_foreground),
                    contentDescription = "Loading album art",
                )
            }
        },
        error = {
            Image(
                painter = painterResource(R.drawable.album_default_foreground),
                contentDescription = "No album art",
            )
        },
    )
}
