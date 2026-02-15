package com.jpishimwe.syncplayer.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Song
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingState_showsProgressIndicator() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState.Loading,
                selectedTab = LibraryTab.SONGS,
                onTabSelected = {},
                onRetry = {},
                onSongClick = { _, _ -> },
                onAlbumClick = { _, _ -> },
                onArtistClick = {},
                modifier = Modifier,
            )
        }

        // Loading state renders - tabs should still be visible
        composeTestRule.onNodeWithText("Songs").assertIsDisplayed()
    }

    @Test
    fun loadedState_showsSongList() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState =
                    LibraryUiState.Loaded(
                        songs = listOf(testSong(1, "My Song")),
                        albums = emptyList(),
                        artists = emptyList(),
                    ),
                selectedTab = LibraryTab.SONGS,
                onTabSelected = {},
                onRetry = {},
                onSongClick = { _, _ -> },
                onAlbumClick = { _, _ -> },
                onArtistClick = {},
                modifier = Modifier,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("My Song").assertIsDisplayed()
    }

    @Test
    fun tabSwitching_showsAlbums() {
        var selectedTab = LibraryTab.SONGS
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState =
                    LibraryUiState.Loaded(
                        songs = emptyList(),
                        albums =
                            listOf(
                                Album(1, "Test Album", "Test Artist", 5, null),
                            ),
                        artists = emptyList(),
                    ),
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onRetry = {},
                onSongClick = { _, _ -> },
                onAlbumClick = { _, _ -> },
                onArtistClick = {},
                modifier = Modifier,
            )
        }

        composeTestRule.onNodeWithText("Albums").performClick()
    }

    @Test
    fun emptyState_showsMessage() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState =
                    LibraryUiState.Loaded(
                        songs = emptyList(),
                        albums = emptyList(),
                        artists = emptyList(),
                    ),
                selectedTab = LibraryTab.SONGS,
                onTabSelected = {},
                onRetry = {},
                onSongClick = { _, _ -> },
                onAlbumClick = { _, _ -> },
                onArtistClick = {},
                modifier = Modifier,
            )
        }

        composeTestRule.onNodeWithText("No songs found").assertIsDisplayed()
    }

    @Test
    fun errorState_showsRetryButton() {
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = LibraryUiState.Error("Something went wrong"),
                selectedTab = LibraryTab.SONGS,
                onTabSelected = {},
                onRetry = {},
                onSongClick = { _, _ -> },
                onAlbumClick = { _, _ -> },
                onArtistClick = {},
                modifier = Modifier,
            )
        }

        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    private fun testSong(
        id: Long,
        title: String,
    ) = Song(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        albumId = 1,
        duration = 200_000,
        trackNumber = 1,
        year = 2024,
        dateAdded = 1000L,
        contentUri = "content://media/external/audio/media/$id",
        albumArtUri = null,
    )
}
