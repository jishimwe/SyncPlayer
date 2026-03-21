package com.jpishimwe.syncplayer.ui.navigation

import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpishimwe.syncplayer.ui.theme.LocalExtendedColorScheme
import com.jpishimwe.syncplayer.ui.theme.SyncPlayerTheme
import com.jpishimwe.syncplayer.ui.theme.noRippleClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockedSearchBarContent(
    query: String = "",
    active: Boolean = true,
    onQueryChanged: (String) -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onClearSearchQuery: () -> Unit,
) {
    DockedSearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = { onQueryChanged(it) },
                onSearch = {},
                expanded = active,
                onExpandedChange = { onActiveChanged(it) },
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            )
        },
        expanded = active,
        onExpandedChange = {
            onActiveChanged(it)
            if (!it) {
                onQueryChanged("")
                onClearSearchQuery()
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 64.dp)
                .statusBarsPadding(),
    ) {}
}

@Composable
fun TopAppBarContent(
    searchActive: Boolean,
    onSearchActiveChanged: (Boolean) -> Unit,
    onSettingsClicked: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .statusBarsPadding()
                .background(
                    Brush.verticalGradient(
                        colorStops =
                            arrayOf(
                                0f to MaterialTheme.colorScheme.background,
                                0.5f to MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                                1f to Color.Transparent,
                            ),
                    ),
                ).padding(bottom = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SyncPlayer",
                maxLines = 1,
                style = MaterialTheme.typography.displayMedium,
                color = LocalExtendedColorScheme.current.accentColor.color,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onSearchActiveChanged(!searchActive) }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onSettingsClicked) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun CustomTabRow(
    selectedTab: LibraryTab,
    onSelectedTabChanged: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val activeFontSize = MaterialTheme.typography.displaySmall.fontSize
    val inactiveFontSize = MaterialTheme.typography.headlineMedium.fontSize
    val textHeight = MaterialTheme.typography.displaySmall.lineHeight.value.dp
    val tabBounds = remember { mutableStateMapOf<LibraryTab, TabBounds>() }

    val density = LocalDensity.current

    var viewPortWidth by remember { mutableIntStateOf(0) }
    val halfViewportDp = with(density) { (viewPortWidth / 4).toDp() }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .onGloballyPositioned { viewPortWidth = it.size.width }
                .horizontalScroll(scrollState)
                .height(with(LocalDensity.current) { activeFontSize.toDp() + 24.dp })
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // ── Leading spacer: allows the FIRST tab to scroll to center ──
        Spacer(Modifier.width(halfViewportDp))

        LibraryTab.entries.forEach { tab ->
            val fontSize by animateFloatAsState(
                targetValue = if (selectedTab == tab) activeFontSize.value else inactiveFontSize.value,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "tabFontSize",
            )
            Text(
                text = tab.label,
                modifier =
                    Modifier
                        .onGloballyPositioned { coordinates ->
                            // Position relative to the Row (excludes the spacer?—no:
                            // coordinates are relative to the parent Row, so tabX already
                            // accounts for the leading spacer in the layout. But we
                            // want the offset relative to the first real tab, so we
                            // record positionInParent which IS the Row-local offset.)
                            val pos = coordinates.positionInParent()
                            tabBounds[tab] = TabBounds(pos.x.toInt(), coordinates.size.width)
                        }.alignByBaseline()
                        .noRippleClickable { onSelectedTabChanged(tab) }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .height(textHeight),
                style =
                    if (selectedTab == tab) {
                        MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize.sp,
                        )
                    } else {
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Thin,
                            fontSize = fontSize.sp,
                        )
                    },
                color =
                    if (selectedTab == tab) {
                        LocalExtendedColorScheme.current.accentColor.color
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
        // ── Trailing spacer: allows the LAST tab to scroll to center ──
        Spacer(Modifier.width(halfViewportDp))
    }

    // Sync scroll when tab changes from pager swipe (not tap)
    LaunchedEffect(selectedTab, viewPortWidth) {
        Log.e("CustomTabRow", "LaunchedEffect: $selectedTab")
        val bounds = tabBounds[selectedTab] ?: return@LaunchedEffect
        if (viewPortWidth == 0) return@LaunchedEffect

        val (tabX, tabWidth) = bounds
        val targetScroll = tabX + (tabWidth / 2) - viewPortWidth / 2
        tabBounds[selectedTab]?.let { bounds ->
            Log.e(
                "LaunchedEffect",
                "onSelectedTabChanged: $selectedTab  | bounds -> ${tabBounds[selectedTab]} | $bounds.x scrollState ${scrollState.maxValue} | viewPortWidth $viewPortWidth",
            )
            scrollState.animateScrollTo(targetScroll.coerceIn(0, scrollState.maxValue))
        }
    }
}

@Preview
@Composable
fun TopAppBarContentPreview() {
    SyncPlayerTheme(darkTheme = true) {
        TopAppBarContent(
            searchActive = false,
            onSearchActiveChanged = {},
            onSettingsClicked = {},
        )
    }
}

@Preview
@Composable
fun CustomTabRowPreview() {
    SyncPlayerTheme(darkTheme = true) {
        CustomTabRow(selectedTab = LibraryTab.SONGS, onSelectedTabChanged = {})
    }
}
