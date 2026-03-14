package com.jpishimwe.syncplayer.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    composed {
        clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick,
        )
    }

fun Modifier.gradientBorderStroke(
    colors: List<Color> =
        listOf(
            myAccentColor.copy(alpha = 0.24f),
            myAccentColor.copy(alpha = 0.75f),
            myAccentColor.copy(alpha = 0.24f),
        ),
    shape: Shape = RoundedCornerShape(8.dp),
): Modifier {
    val accentBorderBrush =
        Brush.linearGradient(
            colors = colors,
        )

    return border(BorderStroke(1.dp, accentBorderBrush), shape)
}
