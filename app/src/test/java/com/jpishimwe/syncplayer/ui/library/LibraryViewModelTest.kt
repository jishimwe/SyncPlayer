package com.jpishimwe.syncplayer.ui.library

import app.cash.turbine.test
import com.jpishimwe.syncplayer.MainDispatcherRule
import com.jpishimwe.syncplayer.data.FakeSongRepository
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

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
    fun `state transitions to Loaded with songs`() = runTest {
        repository.songsFlow.value = listOf(testSong(1), testSong(2))
        repository.albumsFlow.value = listOf(testAlbum(1))
        repository.artistsFlow.value = listOf(testArtist("Artist"))

        viewModel.uiState.test {
            val loaded = awaitItem()
            assertTrue(loaded is LibraryUiState.Loaded)
            assertEquals(2, (loaded as LibraryUiState.Loaded).songs.size)
            assertEquals(1, loaded.albums.size)
            assertEquals(1, loaded.artists.size)
        }
    }

    @Test
    fun `Loaded state with empty lists when no songs`() = runTest {
        // MutableStateFlow starts with emptyList() so combine immediately emits Loaded
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is LibraryUiState.Loaded)
            assertTrue((state as LibraryUiState.Loaded).songs.isEmpty())
        }
    }

    @Test
    fun `selectTab changes selected tab`() = runTest {
        assertEquals(LibraryTab.SONGS, viewModel.selectedTab.value)

        viewModel.selectTab(LibraryTab.ALBUMS)
        assertEquals(LibraryTab.ALBUMS, viewModel.selectedTab.value)

        viewModel.selectTab(LibraryTab.ARTISTS)
        assertEquals(LibraryTab.ARTISTS, viewModel.selectedTab.value)
    }

    @Test
    fun `refreshLibrary calls repository`() = runTest {
        viewModel.refreshLibrary()
        assertEquals(1, repository.refreshCallCount)
    }

    @Test
    fun `refreshLibrary handles error gracefully`() = runTest {
        repository.refreshError = RuntimeException("scan failed")
        viewModel.refreshLibrary()
        assertEquals(1, repository.refreshCallCount)
    }

    private fun testSong(id: Long) = Song(
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
        albumArtUri = "content://media/external/audio/albumart/1",
    )

    private fun testAlbum(id: Long) = Album(
        id = id,
        name = "Album $id",
        artist = "Artist",
        songCount = 2,
        albumArtUri = "content://media/external/audio/albumart/$id",
    )

    private fun testArtist(name: String) = Artist(
        name = name,
        songCount = 2,
        albumCount = 1,
    )
}
