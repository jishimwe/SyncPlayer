package com.jpishimwe.syncplayer.ui.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jpishimwe.syncplayer.ui.player.PlaybackState
import com.jpishimwe.syncplayer.ui.player.PlayerUiState
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NowPlayingScreenContentTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // ── track info display ────────────────────────────────────────────────────

    @Test
    fun displaysCurrentSongTitleAndArtist() {
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(currentSong = testSong(title = "Bohemian Rhapsody", artist = "Queen")),
                onEvent = {},
                onNavigateBack = {},
                formatTime = { "0:00" },
                rating = Rating.NONE,
            )
        }

        composeTestRule.onNodeWithText("Bohemian Rhapsody").assertIsDisplayed()
        composeTestRule.onNodeWithText("Queen").assertIsDisplayed()
    }

    @Test
    fun showsNowPlayingTitleWhenNoCurrentSong() {
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(currentSong = null),
                onEvent = {},
                onNavigateBack = {},
                formatTime = { "0:00" },
                rating = Rating.NONE,
            )
        }

        composeTestRule.onNodeWithText("Now Playing").assertIsDisplayed()
    }

    // ── play / pause ──────────────────────────────────────────────────────────

    @Test
    fun clickingPlayPause_firesPlayPauseEvent() {
        val events = mutableListOf<PlayerEvent>()
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(
                    currentSong = testSong(),
                    playbackState = PlaybackState.PAUSED,
                ),
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
    fun clickingPause_firesPlayPauseEvent() {
        val events = mutableListOf<PlayerEvent>()
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(
                    currentSong = testSong(),
                    playbackState = PlaybackState.PLAYING,
                ),
                onEvent = { events.add(it) },
                onNavigateBack = {},
                formatTime = { "0:00" },
                rating = Rating.NONE,
            )
        }

        composeTestRule.onNodeWithContentDescription("Pause").performClick()
        assertTrue(events.contains(PlayerEvent.PlayPause))
    }

    // ── skip ──────────────────────────────────────────────────────────────────

    @Test
    fun clickingSkipNext_firesSkipToNextEvent() {
        val events = mutableListOf<PlayerEvent>()
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(currentSong = testSong()),
                onEvent = { events.add(it) },
                onNavigateBack = {},
                formatTime = { "0:00" },
                rating = Rating.NONE,
            )
        }

        composeTestRule.onNodeWithContentDescription("Skip next").performClick()
        assertTrue(events.contains(PlayerEvent.SkipToNext))
    }

    @Test
    fun clickingSkipPrevious_firesSkipToPreviousEvent() {
        val events = mutableListOf<PlayerEvent>()
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(currentSong = testSong()),
                onEvent = { events.add(it) },
                onNavigateBack = {},
                formatTime = { "0:00" },
                rating = Rating.NONE,
            )
        }

        composeTestRule.onNodeWithContentDescription("Skip previous").performClick()
        assertTrue(events.contains(PlayerEvent.SkipToPrevious))
    }

    // ── star rating ───────────────────────────────────────────────────────────

    @Test
    fun starRatingButton_showsFavoriteWhenRatingIsFavorite() {
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(currentSong = testSong()),
                onEvent = {},
                onNavigateBack = {},
                formatTime = { "0:00" },
                rating = Rating.FAVORITE,
            )
        }

        composeTestRule.onNodeWithContentDescription("Remove from favorites").assertIsDisplayed()
    }

    // ── navigate back ─────────────────────────────────────────────────────────

    @Test
    fun clickingBackButton_invokesNavigateBack() {
        var navigatedBack = false
        composeTestRule.setContent {
            NowPlayingScreenContent(
                uiState = PlayerUiState(currentSong = testSong()),
                onEvent = {},
                onNavigateBack = { navigatedBack = true },
                formatTime = { "0:00" },
                rating = Rating.NONE,
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(navigatedBack)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun testSong(
        title: String = "Song Title",
        artist: String = "Artist",
    ) = Song(
        id = 1L,
        title = title,
        artist = artist,
        album = "Album",
        albumId = 1,
        duration = 200_000,
        trackNumber = 1,
        year = 2024,
        dateAdded = 1000L,
        contentUri = null,
        albumArtUri = null,
    )
}
