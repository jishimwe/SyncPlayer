package com.jpishimwe.syncplayer.ui.library

import app.cash.turbine.test
import com.jpishimwe.syncplayer.MainDispatcherRule
import com.jpishimwe.syncplayer.data.FakeSongRepository
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelMetadataTest {
    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var repository: FakeSongRepository
    private lateinit var viewModel: LibraryViewModel

    @BeforeEach
    fun setup() {
        repository = FakeSongRepository()
        viewModel = LibraryViewModel(repository)
    }

    @Test
    fun `uiState includes favorites when loaded`() =
        runTest {
            val favSong = testSong(10L)
            repository.favoritesFlow.value = listOf(favSong)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is LibraryUiState.Loaded)
                assertEquals(listOf(favSong), (state as LibraryUiState.Loaded).favorites)
            }
        }

    @Test
    fun `uiState includes mostPlayed when loaded`() =
        runTest {
            val topSong = testSong(20L)
            repository.mostPlayedFlow.value = listOf(topSong)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is LibraryUiState.Loaded)
                assertEquals(listOf(topSong), (state as LibraryUiState.Loaded).mostPlayed)
            }
        }

    @Test
    fun `uiState includes recentlyPlayed when loaded`() =
        runTest {
            val recentSong = testSong(30L)
            repository.recentlyPlayedFlow.value = listOf(recentSong)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is LibraryUiState.Loaded)
                assertEquals(listOf(recentSong), (state as LibraryUiState.Loaded).recentlyPlayed)
            }
        }

    private fun testSong(id: Long) =
        Song(
            id = id,
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            albumId = 1,
            duration = 200_000,
            trackNumber = id.toInt(),
            year = 2024,
            dateAdded = 1000L,
            contentUri = "content://media/external/audio/media/$id",
            albumArtUri = null,
        )
}