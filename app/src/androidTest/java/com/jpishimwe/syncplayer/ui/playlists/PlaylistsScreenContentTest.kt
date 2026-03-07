package com.jpishimwe.syncplayer.ui.playlists

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jpishimwe.syncplayer.model.Playlist
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaylistsScreenContentTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingState_showsLoadingIndicator() {
        composeTestRule.setContent {
            PlaylistsScreenContent(
                uiState = PlaylistUiState.Loading,
                onCreatePlaylist = {},
                onRenamePlaylist = { _, _ -> },
                onDeletePlaylist = {},
                onPlaylistClick = { _, _ -> },
            )
        }

        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun loadedState_withPlaylists_displaysPlaylistNames() {
        composeTestRule.setContent {
            PlaylistsScreenContent(
                uiState = PlaylistUiState.Loaded(
                    playlists = listOf(
                        testPlaylist(1, "Favorites"),
                        testPlaylist(2, "Workout"),
                    )
                ),
                onCreatePlaylist = {},
                onRenamePlaylist = { _, _ -> },
                onDeletePlaylist = {},
                onPlaylistClick = { _, _ -> },
            )
        }

        composeTestRule.onNodeWithText("Favorites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Workout").assertIsDisplayed()
    }

    @Test
    fun loadedState_withNoPlaylists_showsEmptyMessage() {
        composeTestRule.setContent {
            PlaylistsScreenContent(
                uiState = PlaylistUiState.Loaded(playlists = emptyList()),
                onCreatePlaylist = {},
                onRenamePlaylist = { _, _ -> },
                onDeletePlaylist = {},
                onPlaylistClick = { _, _ -> },
            )
        }

        composeTestRule.onNodeWithText("No playlists yet").assertIsDisplayed()
    }

    @Test
    fun clickingPlaylist_invokesOnPlaylistClick() {
        var clickedId: Long? = null
        composeTestRule.setContent {
            PlaylistsScreenContent(
                uiState = PlaylistUiState.Loaded(
                    playlists = listOf(testPlaylist(42, "My Mix"))
                ),
                onCreatePlaylist = {},
                onRenamePlaylist = { _, _ -> },
                onDeletePlaylist = {},
                onPlaylistClick = { id, _ -> clickedId = id },
            )
        }

        composeTestRule.onNodeWithText("My Mix").performClick()
        assertTrue(clickedId == 42L)
    }

    private fun testPlaylist(id: Long, name: String) = Playlist(
        id = id,
        name = name,
        createdAt = 1000L,
        songCount = 0,
    )
}
