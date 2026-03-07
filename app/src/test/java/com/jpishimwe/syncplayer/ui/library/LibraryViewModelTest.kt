package com.jpishimwe.syncplayer.ui.library

import app.cash.turbine.test
import com.jpishimwe.syncplayer.MainDispatcherRule
import com.jpishimwe.syncplayer.data.FakeSongRepository
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun `state transitions to Loaded with songs`() =
        runTest {
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
    fun `Loaded state with empty lists when no songs`() =
        runTest {
            // MutableStateFlow starts with emptyList() so combine immediately emits Loaded
            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is LibraryUiState.Loaded)
                assertTrue((state as LibraryUiState.Loaded).songs.isEmpty())
            }
        }

    @Test
    fun `selectTab changes selected tab`() =
        runTest {
            assertEquals(LibraryTab.SONGS, viewModel.selectedTab.value)

            viewModel.selectTab(LibraryTab.ALBUMS)
            assertEquals(LibraryTab.ALBUMS, viewModel.selectedTab.value)

            viewModel.selectTab(LibraryTab.ARTISTS)
            assertEquals(LibraryTab.ARTISTS, viewModel.selectedTab.value)
        }

    @Test
    fun `refreshLibrary calls repository`() =
        runTest {
            viewModel.refreshLibrary()
            advanceUntilIdle()
            assertEquals(1, repository.refreshCallCount)

//            val state = viewModel.uiState.value
//            assertTrue(state is LibraryUiState.Loaded)
        }

    @Test
    fun `refreshLibrary handles error gracefully`() =
        runTest {
            repository.refreshError = RuntimeException("scan failed")
            viewModel.uiState.test {
                awaitItem() // initial Loaded (empty lists)
                viewModel.refreshLibrary()
                advanceUntilIdle()
                assertEquals(1, repository.refreshCallCount)
                assertTrue(awaitItem() is LibraryUiState.Error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `isRefreshing is true during refresh and false after`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            repository.refreshGate = gate

            viewModel.isRefreshing.test {
                assertEquals(false, awaitItem())   // initial state
                viewModel.refreshLibrary()          // launch fires; sets true, then suspends on gate
                assertEquals(true, awaitItem())    // true while refresh is in-flight
                gate.complete(Unit)                // unblock refresh
                assertEquals(false, awaitItem())   // false after completion
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    fun `blank search query uses getAllSongs`() =
        runTest {
            viewModel.uiState.test {
                awaitItem() // triggers subscription → getAllSongs called
                assertTrue(repository.getAllSongsCallCount > 0)
                assertEquals(0, repository.searchSongsCallCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `non-blank search query uses searchSongs`() =
        runTest {
            viewModel.onSearchQueryChanged("Beatles")
            viewModel.uiState.test {
                awaitItem() // non-blank query → searchSongs called
                assertTrue(repository.searchSongsCallCount > 0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `clearing search switches back to getAllSongs`() =
        runTest {
            viewModel.onSearchQueryChanged("Beatles")
            viewModel.onClearSearchQuery()
            val searchCountSnapshot = repository.searchSongsCallCount
            viewModel.uiState.test {
                awaitItem() // blank query again → getAllSongs
                assertTrue(repository.getAllSongsCallCount > 0)
                // searchSongs should not be called again after clear
                assertEquals(searchCountSnapshot, repository.searchSongsCallCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Sort ──────────────────────────────────────────────────────────────────

    @Test
    fun `sort by title orders songs alphabetically`() =
        runTest {
            repository.songsFlow.value =
                listOf(
                    testSong(1).copy(title = "Zebra"),
                    testSong(2).copy(title = "Apple"),
                    testSong(3).copy(title = "Mango"),
                )
            viewModel.onSortOrder(SortOrder.BY_TITLE)
            viewModel.uiState.test {
                val state = awaitItem() as LibraryUiState.Loaded
                assertEquals(listOf("Apple", "Mango", "Zebra"), state.songs.map { it.title })
            }
        }

    @Test
    fun `sort by artist orders songs by artist name`() =
        runTest {
            repository.songsFlow.value =
                listOf(
                    testSong(1).copy(artist = "ZZ Top"),
                    testSong(2).copy(artist = "ABBA"),
                    testSong(3).copy(artist = "Migos"),
                )
            viewModel.onSortOrder(SortOrder.BY_ARTIST)
            viewModel.uiState.test {
                val state = awaitItem() as LibraryUiState.Loaded
                assertEquals(listOf("ABBA", "Migos", "ZZ Top"), state.songs.map { it.artist })
            }
        }

    @Test
    fun `sort by album orders songs by album name`() =
        runTest {
            repository.songsFlow.value =
                listOf(
                    testSong(1).copy(album = "Thriller"),
                    testSong(2).copy(album = "Abbey Road"),
                    testSong(3).copy(album = "Kind of Blue"),
                )
            viewModel.onSortOrder(SortOrder.BY_ALBUM)
            viewModel.uiState.test {
                val state = awaitItem() as LibraryUiState.Loaded
                assertEquals(listOf("Abbey Road", "Kind of Blue", "Thriller"), state.songs.map { it.album })
            }
        }

    @Test
    fun `sort by duration orders songs ascending`() =
        runTest {
            repository.songsFlow.value =
                listOf(
                    testSong(1).copy(duration = 300_000L),
                    testSong(2).copy(duration = 100_000L),
                    testSong(3).copy(duration = 200_000L),
                )
            viewModel.onSortOrder(SortOrder.BY_DURATION)
            viewModel.uiState.test {
                val state = awaitItem() as LibraryUiState.Loaded
                assertEquals(listOf(100_000L, 200_000L, 300_000L), state.songs.map { it.duration })
            }
        }

    @Test
    fun `sort by play count orders songs descending`() =
        runTest {
            repository.songsFlow.value =
                listOf(
                    testSong(1).copy(playCount = 1),
                    testSong(2).copy(playCount = 5),
                    testSong(3).copy(playCount = 3),
                )
            viewModel.onSortOrder(SortOrder.BY_PLAY_COUNT)
            viewModel.uiState.test {
                val state = awaitItem() as LibraryUiState.Loaded
                assertEquals(listOf(5, 3, 1), state.songs.map { it.playCount })
            }
        }

    @Test
    fun `sort by date added orders songs descending`() =
        runTest {
            repository.songsFlow.value =
                listOf(
                    testSong(1).copy(dateAdded = 1000L),
                    testSong(2).copy(dateAdded = 3000L),
                    testSong(3).copy(dateAdded = 2000L),
                )
            viewModel.onSortOrder(SortOrder.BY_DATE_ADDED)
            viewModel.uiState.test {
                val state = awaitItem() as LibraryUiState.Loaded
                assertEquals(listOf(3000L, 2000L, 1000L), state.songs.map { it.dateAdded })
            }
        }

    // ── onAppResumed ──────────────────────────────────────────────────────────

    @Test
    fun `onAppResumed triggers refresh when never scanned before`() =
        runTest {
            // lastScanTimestamp starts at 0 — 24-hour window always elapsed
            viewModel.onAppResumed()
            advanceUntilIdle()
            assertEquals(1, repository.refreshCallCount)
        }

    @Test
    fun `onAppResumed does not trigger refresh when recently scanned`() =
        runTest {
            viewModel.refreshLibrary()
            advanceUntilIdle()
            val countAfterFirstRefresh = repository.refreshCallCount

            viewModel.onAppResumed() // immediately after — window not elapsed
            advanceUntilIdle()
            assertEquals(countAfterFirstRefresh, repository.refreshCallCount)
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
            albumArtUri = "content://media/external/audio/albumart/1",
        )

    private fun testAlbum(id: Long) =
        Album(
            id = id,
            name = "Album $id",
            artist = "Artist",
            songCount = 2,
            albumArtUri = "content://media/external/audio/albumart/$id",
        )

    private fun testArtist(name: String) =
        Artist(
            name = name,
            songCount = 2,
            albumCount = 1,
        )
}
