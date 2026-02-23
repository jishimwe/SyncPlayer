package com.jpishimwe.syncplayer.ui.library

import app.cash.turbine.test
import com.jpishimwe.syncplayer.MainDispatcherRule
import com.jpishimwe.syncplayer.data.FakePlayerRepository
import com.jpishimwe.syncplayer.data.FakeSongRepository
import com.jpishimwe.syncplayer.model.PlaybackState
import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var repository: FakePlayerRepository
    private lateinit var songRepository: FakeSongRepository
    private lateinit var viewModel: PlayerViewModel

    @BeforeEach
    fun setup() {
        repository = FakePlayerRepository()
        songRepository = FakeSongRepository()
        viewModel = PlayerViewModel(repository, songRepository)
    }

    @Test
    fun `PlayPause calls play when paused`() =
        runTest {
            repository.emitState(PlayerUiState(playbackState = PlaybackState.PAUSED))

            viewModel.uiState.test {
                awaitItem() // activate upstream so uiState.value is current
                viewModel.onEvent(PlayerEvent.PlayPause)

                assertEquals(1, repository.playCallCount)
                assertEquals(0, repository.pauseCallCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `PlayPause calls pause when playing`() =
        runTest {
            repository.emitState(PlayerUiState(playbackState = PlaybackState.PLAYING))

            viewModel.uiState.test {
                awaitItem() // activate upstream so uiState.value is current
                viewModel.onEvent(PlayerEvent.PlayPause)

                assertEquals(1, repository.pauseCallCount)
                assertEquals(0, repository.playCallCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `SkipToNext calls skipToNext on repository`() {
        viewModel.onEvent(PlayerEvent.SkipToNext)
        assertEquals(1, repository.skipNextCallCount)
    }

    @Test
    fun `SkipToPrevious calls skipToPrevious on repository`() {
        viewModel.onEvent(PlayerEvent.SkipToPrevious)
        assertEquals(1, repository.skipPreviousCallCount)
    }

    @Test
    fun `SeekTo updates seek position on repository`() {
        viewModel.onEvent(PlayerEvent.SeekTo(30000L))
        assertEquals(30000L, repository.lastSeekPosition)
    }

    @Test
    fun `ToggleShuffle calls toggleShuffle on repository`() {
        viewModel.onEvent(PlayerEvent.ToggleShuffle)
        assertEquals(1, repository.shuffleToggleCount)
    }

    @Test
    fun `ToggleRepeat calls toggleRepeat on repository`() {
        viewModel.onEvent(PlayerEvent.ToggleRepeat)
        assertEquals(1, repository.repeatToggleCount)
    }

    @Test
    fun `PlaySongs calls playSongs on repository`() =
        runTest {
            val songs =
                listOf(
                    Song(
                        id = 1,
                        title = "Test",
                        artist = "Artist",
                        album = "Album",
                        albumArtUri = null,
                        duration = 1000L,
                        albumId = 1,
                        trackNumber = 1,
                        year = 1993,
                        dateAdded = 2024,
                        contentUri = null,
                    ),
                )

            viewModel.onEvent(PlayerEvent.PlaySongs(songs))

            advanceUntilIdle()
            assertEquals(songs, repository.lastPlayedSongs)
            assertEquals(0, repository.lastPlayedStartIndex)
        }

    @Test
    fun `PlaySongs passes startIndex to repository`() =
        runTest {
            val songs =
                listOf(
                    Song(
                        id = 1,
                        title = "Test",
                        artist = "Artist",
                        album = "Album",
                        albumArtUri = null,
                        duration = 1000L,
                        albumId = 1,
                        trackNumber = 1,
                        year = 1993,
                        dateAdded = 2024,
                        contentUri = null,
                    ),
                    Song(
                        id = 2,
                        title = "Test 2",
                        artist = "Artist",
                        album = "Album",
                        albumArtUri = null,
                        duration = 2000L,
                        albumId = 1,
                        trackNumber = 2,
                        year = 1993,
                        dateAdded = 2024,
                        contentUri = null,
                    ),
                )

            viewModel.onEvent(PlayerEvent.PlaySongs(songs, startIndex = 1))

            advanceUntilIdle()
            assertEquals(songs, repository.lastPlayedSongs)
            assertEquals(1, repository.lastPlayedStartIndex)
        }

    @Test
    fun `SeekToQueueItem calls seekToQueueItem on repository`() {
        viewModel.onEvent(PlayerEvent.SeekToQueueItem(3))
        assertEquals(3, repository.lastSeekToQueueItemIndex)
    }

    @Test
    fun `formatTime formats seconds correctly`() {
        assertEquals("0:32", viewModel.formatTime(32000))
    }

    @Test
    fun `formatTime formats minutes and seconds correctly`() {
        assertEquals("3:45", viewModel.formatTime(225000))
    }

    @Test
    fun `formatTime formats hours correctly`() {
        assertEquals("1:02:15", viewModel.formatTime(3735000))
    }

    @Test
    fun `uiState reflects repository state`() =
        runTest {
            val song =
                Song(
                    id = 1,
                    title = "Test",
                    artist = "Artist",
                    album = "Album",
                    albumArtUri = null,
                    duration = 1000L,
                    albumId = 1,
                    trackNumber = 1,
                    year = 1993,
                    dateAdded = 2023,
                    contentUri = null,
                )
            repository.emitState(PlayerUiState(currentSong = song))

            viewModel.uiState.test {
                assertEquals(song, awaitItem().currentSong)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `SetRating calls songRepository setRating with current song id and rating`() =
        runTest {
            val song = testSong(42L)
            repository.emitState(PlayerUiState(currentSong = song))

            viewModel.uiState.test {
                awaitItem() // activate upstream so uiState.value reflects currentSong
                viewModel.onEvent(PlayerEvent.SetRating(Rating.FAVORITE))
                advanceUntilIdle()

                assertEquals(42L, songRepository.lastSetRatingSongId)
                assertEquals(Rating.FAVORITE, songRepository.lastSetRating)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `SetRating does nothing when no song is playing`() =
        runTest {
            viewModel.uiState.test {
                awaitItem() // default state — no current song
                viewModel.onEvent(PlayerEvent.SetRating(Rating.FAVORITE))
                advanceUntilIdle()

                assertEquals(0, songRepository.setRatingCallCount)
                cancelAndIgnoreRemainingEvents()
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
            contentUri = null,
            albumArtUri = null,
        )
}