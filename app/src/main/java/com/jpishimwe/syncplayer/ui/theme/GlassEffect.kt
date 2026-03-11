package com.jpishimwe.syncplayer.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.frostedGlass(
    color: Color = myFrostedGlassSurface,
    alpha: Float = 0.7f,
    blurRadius: Dp = 10.dp,
): Modifier = this.blur(blurRadius).background(color.copy(alpha = alpha))

fun Modifier.frostedGlassRendered(
    color: Color = myFrostedGlassSurface,
    alpha: Float = 0.55f,
    blurRadius: Dp = 20.dp,
): Modifier =
    this
        .graphicsLayer {
            val blur = blurRadius.toPx()
            renderEffect = BlurEffect(blur, blur, TileMode.Clamp)
        }.background(color.copy(alpha = alpha))
