package com.jpishimwe.syncplayer.ui.albumdetail

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

class AlbumDetailScreenContentTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun displaysAlbumNameInToolbar() {
        composeTestRule.setContent {
            AlbumDetailScreenContent(
                albumName = "Abbey Road",
                songs = emptyList(),
                onSongClick = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Abbey Road").assertIsDisplayed()
    }

    @Test
    fun displaysSongTitles() {
        composeTestRule.setContent {
            AlbumDetailScreenContent(
                albumName = "Thriller",
                songs = listOf(
                    testSong(1, "Beat It"),
                    testSong(2, "Billie Jean"),
                ),
                onSongClick = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Beat It").assertIsDisplayed()
        composeTestRule.onNodeWithText("Billie Jean").assertIsDisplayed()
    }

    @Test
    fun clickingSong_passesCorrectIndex() {
        var clickedIndex: Int? = null
        composeTestRule.setContent {
            AlbumDetailScreenContent(
                albumName = "Abbey Road",
                songs = listOf(
                    testSong(1, "Come Together"),
                    testSong(2, "Something"),
                ),
                onSongClick = { index -> clickedIndex = index },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Something").performClick()
        assertEquals(1, clickedIndex)
    }

    @Test
    fun clickingBack_invokesNavigateBack() {
        var navigatedBack = false
        composeTestRule.setContent {
            AlbumDetailScreenContent(
                albumName = "Abbey Road",
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
        artist = "Artist",
        album = "Album",
        albumId = 1,
        duration = 200_000,
        trackNumber = id.toInt(),
        year = 2024,
        dateAdded = 1000L,
        contentUri = null,
        albumArtUri = null,
    )
}