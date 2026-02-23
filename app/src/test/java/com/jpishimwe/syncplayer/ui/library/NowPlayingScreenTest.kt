package com.jpishimwe.syncplayer.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jpishimwe.syncplayer.model.PlaybackState
import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.Rating
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
                rating = Rating.NONE,
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
                rating = Rating.NONE,
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
                rating = Rating.NONE,
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
                rating = Rating.NONE,
            )
        }

        composeTestRule.onNodeWithContentDescription("Next").performClick()
        assertTrue(events.contains(PlayerEvent.SkipToNext))
    }

    @Test
    fun skipPrevious_triggersSkipToPreviousEvent() {
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

        composeTestRule.onNodeWithContentDescription("Previous").performClick()
        assertTrue(events.contains(PlayerEvent.SkipToPrevious))
    }

    @Test
    fun shuffleButton_triggersToggleShuffleEvent() {
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

        composeTestRule.onNodeWithContentDescription("Shuffle off").performClick()
        assertTrue(events.contains(PlayerEvent.ToggleShuffle))
    }

    @Test
    fun repeatButton_triggersToggleRepeatEvent() {
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

        composeTestRule.onNodeWithContentDescription("Repeat off").performClick()
        assertTrue(events.contains(PlayerEvent.ToggleRepeat))
    }
}
