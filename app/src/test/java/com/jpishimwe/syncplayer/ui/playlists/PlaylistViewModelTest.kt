package com.jpishimwe.syncplayer.ui.playlists

import app.cash.turbine.test
import com.jpishimwe.syncplayer.MainDispatcherRule
import com.jpishimwe.syncplayer.data.FakePlaylistRepository
import com.jpishimwe.syncplayer.data.FakeSongRepository
import com.jpishimwe.syncplayer.model.Playlist
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModelTest {
    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var playlistRepository: FakePlaylistRepository
    private lateinit var songRepository: FakeSongRepository
    private lateinit var viewModel: PlaylistViewModel

    @BeforeEach
    fun setup() {
        playlistRepository = FakePlaylistRepository()
        songRepository = FakeSongRepository()
        viewModel = PlaylistViewModel(playlistRepository, songRepository)
    }

    @Test
    fun `uiState transitions to Loaded with playlists`() =
        runTest {
            playlistRepository.playlistsFlow.value =
                listOf(
                    testPlaylist(1, "Favorites"),
                    testPlaylist(2, "Workout"),
                )

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is PlaylistUiState.Loaded)
                assertEquals(2, (state as PlaylistUiState.Loaded).playlists.size)
                assertEquals("Favorites", state.playlists[0].name)
                assertEquals("Workout", state.playlists[1].name)
            }
        }

    @Test
    fun `Loaded state with empty list when no playlists`() =
        runTest {
            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is PlaylistUiState.Loaded)
                assertTrue((state as PlaylistUiState.Loaded).playlists.isEmpty())
            }
        }

    @Test
    fun `CreatePlaylist event calls repository`() =
        runTest {
            viewModel.onEvent(PlaylistEvent.CreatePlaylist("My Playlist"))
            advanceUntilIdle()

            assertEquals(1, playlistRepository.createCallCount)
            assertEquals("My Playlist", playlistRepository.lastCreatedName)
        }

    @Test
    fun `CreatePlaylist with blank name does NOT call repository`() =
        runTest {
            viewModel.onEvent(PlaylistEvent.CreatePlaylist("   "))
            advanceUntilIdle()

            assertEquals(0, playlistRepository.createCallCount)
        }

    @Test
    fun `RenamePlaylist event calls repository`() =
        runTest {
            viewModel.onEvent(PlaylistEvent.RenamePlaylist(playlistId = 1L, newName = "New Name"))
            advanceUntilIdle()

            assertEquals(1, playlistRepository.renameCallCount)
            assertEquals(1L, playlistRepository.lastRenamedId)
            assertEquals("New Name", playlistRepository.lastRenamedName)
        }

    @Test
    fun `RenamePlaylist with blank name does NOT call repository`() =
        runTest {
            viewModel.onEvent(PlaylistEvent.RenamePlaylist(playlistId = 1L, newName = ""))
            advanceUntilIdle()

            assertEquals(0, playlistRepository.renameCallCount)
        }

    @Test
    fun `DeletePlaylist event calls repository`() =
        runTest {
            viewModel.onEvent(PlaylistEvent.DeletePlaylist(playlistId = 42L))
            advanceUntilIdle()

            assertEquals(1, playlistRepository.deleteCallCount)
            assertEquals(42L, playlistRepository.lastDeletedId)
        }

    private fun testPlaylist(id: Long, name: String) =
        Playlist(
            id = id,
            name = name,
            createdAt = 1000L,
            songCount = 0,
        )
}
