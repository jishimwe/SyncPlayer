package com.jpishimwe.syncplayer.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jpishimwe.syncplayer.model.PlaybackState
import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.player.NowPlayingScreenContent
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class NowPlayingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testSong =
        Song(
            id = 1,
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            albumArtUri = null,
            duration = 225000L,
            albumId = 1,
            trackNumber = 1,
            year = 1993,
            dateAdded = 2025,
            contentUri = null,
        )

    @Test
    fun nowPlayingScreen_displaysCurrentSong() {
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(currentSong = testSong),
                onEvent = {},
                onNavigateBack = {},
                formatTime = { "3:45" },
            )
        }

        composeTestRule.onNodeWithText("Test Song").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Artist").assertIsDisplayed()
    }

    @Test
    fun playButton_triggersPlayPauseEvent() {
        val events = mutableListOf<PlayerEvent>()

        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(playbackState = PlaybackState.PAUSED),
                onEvent = { events.add(it) },
                onNavigateBack = {},
                formatTime = { "0:00" },
            )
        }

        composeTestRule.onNodeWithContentDescription("Play").performClick()
        assertTrue(events.contains(PlayerEvent.PlayPause))
    }

    @Test
    fun pauseButton_triggersPlayPauseEvent() {
        val events = mutableListOf<PlayerEvent>()

        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(playbackState = PlaybackState.PLAYING),
                onEvent = { events.add(it) },
                onNavigateBack = {},
                formatTime = { "0:00" },
            )
        }

        composeTestRule.onNodeWithContentDescription("Pause").performClick()
        assertTrue(events.contains(PlayerEvent.PlayPause))
    }

    @Test
    fun skipNext_triggersSkipToNextEvent() {
        val events = mutableListOf<PlayerEvent>()

        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(),
                onEvent = { events.add(it) },
                onNavigateBack = {},
                formatTime = { "0:00" },
            )
        }

        composeTestRule.onNodeWithContentDescription("Next").performClick()
        assertTrue(events.contains(PlayerEvent.SkipToNext))
    }
}
