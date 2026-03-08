package com.jpishimwe.syncplayer.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.frostedGlass(
    color: Color = primaryContainerDark,
    alpha: Float = 0.65f,
    blurRadius: Dp = 10.dp,
): Modifier = this.blur(blurRadius).background(color.copy(alpha = alpha))
