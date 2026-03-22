package com.jpishimwe.syncplayer.ui.artistdetail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jpishimwe.syncplayer.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArtistDetailScreenContentTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun displaysArtistNameInToolbar() {
        composeTestRule.setContent {
            ArtistDetailScreenContent(
                artistName = "Queen",
                songs = emptyList(),
                onSongClick = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Queen").assertIsDisplayed()
    }

    @Test
    fun displaysSongTitles() {
        composeTestRule.setContent {
            ArtistDetailScreenContent(
                artistName = "Queen",
                songs = listOf(
                    testSong(1, "Bohemian Rhapsody"),
                    testSong(2, "We Will Rock You"),
                ),
                onSongClick = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Bohemian Rhapsody").assertIsDisplayed()
        composeTestRule.onNodeWithText("We Will Rock You").assertIsDisplayed()
    }

    @Test
    fun clickingSong_passesCorrectIndex() {
        var clickedIndex: Int? = null
        composeTestRule.setContent {
            ArtistDetailScreenContent(
                artistName = "Queen",
                songs = listOf(
                    testSong(1, "Bohemian Rhapsody"),
                    testSong(2, "Radio Ga Ga"),
                ),
                onSongClick = { index -> clickedIndex = index },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Radio Ga Ga").performClick()
        assertEquals(1, clickedIndex)
    }

    @Test
    fun clickingBack_invokesNavigateBack() {
        var navigatedBack = false
        composeTestRule.setContent {
            ArtistDetailScreenContent(
                artistName = "Queen",
                songs = emptyList(),
                onSongClick = {},
                onNavigateBack = { navigatedBack = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(navigatedBack)
    }

    private fun testSong(id: Long, title: String) = Song(
        id = id,
        title = title,
        artist = "Queen",
        album = "Greatest Hits",
        albumId = 1,
        duration = 200_000,
        trackNumber = id.toInt(),
        year = 2024,
        dateAdded = 1000L,
        contentUri = null,
        albumArtUri = null,
    )
}