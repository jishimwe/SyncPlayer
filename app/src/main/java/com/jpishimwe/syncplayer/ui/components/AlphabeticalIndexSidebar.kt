package com.jpishimwe.syncplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme

/**
 * Vertical A–Z sidebar anchored to the right edge of its parent.
 * Touch/drag anywhere on the column fires [onLetterSelected] with the
 * letter under the finger, enabling fast-scroll behaviour.
 */
@Composable
fun AlphabeticalIndexSidebar(
    letters: List<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) return

    var columnHeightPx by remember { mutableIntStateOf(1) }
    val density = LocalDensity.current
    val pillShape = RoundedCornerShape(percent = 50)

    Box(
        modifier =
            modifier
                .width(20.dp)
                .wrapContentHeight()
                .onSizeChanged { columnHeightPx = it.height }
                .clip(pillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(letters) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (
                                event.type == PointerEventType.Press ||
                                event.type == PointerEventType.Move
                            ) {
                                val y =
                                    event.changes
                                        .firstOrNull()
                                        ?.position
                                        ?.y ?: continue
                                val fraction = (y / columnHeightPx).coerceIn(0f, 1f)
                                val index =
                                    (fraction * letters.size)
                                        .toInt()
                                        .coerceIn(0, letters.size - 1)
                                onLetterSelected(letters[index])
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            letters.forEach { letter ->
                Text(
                    text = letter.toString(),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111113, heightDp = 600)
@Composable
private fun AlphabeticalIndexSidebarPreview() {
    SyncPlayerTheme(darkTheme = true) {
        AlphabeticalIndexSidebar(
            letters = ('A'..'Z').toList(),
            onLetterSelected = {},
        )
    }
}
