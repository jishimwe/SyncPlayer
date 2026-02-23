package com.jpishimwe.syncplayer.ui.player

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.Rating
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class NowPlayingScreenContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `FavoriteButton sets Rating FAVORITE when not favorited`() {
        val events = mutableListOf<PlayerEvent>()

        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(),
                onEvent = { events.add(it) },
                onNavigateBack = {},
                formatTime = { "0:00" },
                rating = Rating.NONE,
            )
        }

        composeTestRule.onNodeWithContentDescription("Add to favorites").performClick()
        assertTrue(events.contains(PlayerEvent.SetRating(Rating.FAVORITE)))
    }

    @Test
    fun `FavoriteButton clears to Rating NONE when already at FAVORITE`() {
        val events = mutableListOf<PlayerEvent>()

        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(),
                onEvent = { events.add(it) },
                onNavigateBack = {},
                formatTime = { "0:00" },
                rating = Rating.FAVORITE,
            )
        }

        composeTestRule.onNodeWithContentDescription("Remove from favorites").performClick()
        assertTrue(events.contains(PlayerEvent.SetRating(Rating.NONE)))
    }

    @Test
    fun `FavoriteButton shows Add to favorites when rating is NONE`() {
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(),
                onEvent = {},
                onNavigateBack = {},
                formatTime = { "0:00" },
                rating = Rating.NONE,
            )
        }

        composeTestRule.onNodeWithContentDescription("Add to favorites").assertExists()
    }

    @Test
    fun `FavoriteButton shows Remove from favorites when rating is FAVORITE`() {
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(),
                onEvent = {},
                onNavigateBack = {},
                formatTime = { "0:00" },
                rating = Rating.FAVORITE,
            )
        }

        composeTestRule.onNodeWithContentDescription("Remove from favorites").assertExists()
    }
}