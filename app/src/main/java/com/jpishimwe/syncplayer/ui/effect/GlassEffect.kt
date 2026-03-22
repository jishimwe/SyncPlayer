package com.jpishimwe.syncplayer.ui.effect

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jpishimwe.syncplayer.ui.theme.myFrostedGlassSurface

fun Modifier.frostedGlass(
    color: Color = myFrostedGlassSurface,
    alpha: Float = 0.7f,
    blurRadius: Dp = 10.dp,
): Modifier = this.blur(blurRadius).background(color.copy(alpha = alpha))

fun Modifier.frostedGlassRendered(
    color: Color = myFrostedGlassSurface,
    alpha: Float = 0.75f,
    blurRadius: Dp = 20.dp,
): Modifier =
    this
        .graphicsLayer {
            val blur = blurRadius.toPx()
            renderEffect = BlurEffect(blur, blur, TileMode.Clamp)
        }.background(
            Brush.verticalGradient(
                colors =
                    listOf(
                        color.copy(alpha = alpha + 0.1f),
                        color.copy(alpha = alpha - 0.1f),
                    ),
            ),
        )
