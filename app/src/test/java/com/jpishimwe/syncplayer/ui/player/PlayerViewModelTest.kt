package com.jpishimwe.syncplayer.ui.player

import app.cash.turbine.test
import com.jpishimwe.syncplayer.MainDispatcherRule
import com.jpishimwe.syncplayer.data.FakePlayerRepository
import com.jpishimwe.syncplayer.data.FakeSongRepository
import com.jpishimwe.syncplayer.ui.player.PlaybackState
import com.jpishimwe.syncplayer.ui.player.PlayerUiState
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
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

    // ── PlayPause ─────────────────────────────────────────────────────────────

    @Test
    fun `PlayPause calls play when paused`() =
        runTest {
            repository.emitState(PlayerUiState(playbackState = PlaybackState.PAUSED))

            viewModel.uiState.test {
                awaitItem()
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
                awaitItem()
                viewModel.onEvent(PlayerEvent.PlayPause)

                assertEquals(1, repository.pauseCallCount)
                assertEquals(0, repository.playCallCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Skip / Seek ───────────────────────────────────────────────────────────

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
    fun `SeekToQueueItem calls seekToQueueItem on repository`() {
        viewModel.onEvent(PlayerEvent.SeekToQueueItem(3))
        assertEquals(3, repository.lastSeekToQueueItemIndex)
    }

    // ── Shuffle / Repeat ──────────────────────────────────────────────────────

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

    // ── Queue management ──────────────────────────────────────────────────────

    @Test
    fun `PlaySongs calls playSongs on repository`() =
        runTest {
            val songs = listOf(testSong(1L))
            viewModel.onEvent(PlayerEvent.PlaySongs(songs))
            advanceUntilIdle()
            assertEquals(songs, repository.lastPlayedSongs)
            assertEquals(0, repository.lastPlayedStartIndex)
        }

    @Test
    fun `PlaySongs passes startIndex to repository`() =
        runTest {
            val songs = listOf(testSong(1L), testSong(2L))
            viewModel.onEvent(PlayerEvent.PlaySongs(songs, startIndex = 1))
            advanceUntilIdle()
            assertEquals(songs, repository.lastPlayedSongs)
            assertEquals(1, repository.lastPlayedStartIndex)
        }

    @Test
    fun `AddToQueue event calls playerRepository addToQueue with correct song`() =
        runTest {
            val song = testSong(10L)
            viewModel.onEvent(PlayerEvent.AddToQueue(song))
            advanceUntilIdle()
            assertEquals(song, repository.lastQueuedSong)
        }

    @Test
    fun `PlayNext event calls playerRepository playNext with correct song`() =
        runTest {
            val song = testSong(11L)
            viewModel.onEvent(PlayerEvent.PlayNext(song))
            advanceUntilIdle()
            assertEquals(song, repository.lastPlayNextSong)
        }

    @Test
    fun `RemoveFromQueue event calls playerRepository removeFromQueue with correct id`() =
        runTest {
            viewModel.onEvent(PlayerEvent.RemoveFromQueue("queue-id-42"))
            advanceUntilIdle()
            assertEquals("queue-id-42", repository.lastRemovedId)
        }

    @Test
    fun `ReorderQueue event calls playerRepository reorderQueue with correct id and position`() =
        runTest {
            viewModel.onEvent(PlayerEvent.ReorderQueue("queue-id-7", newPosition = 3))
            advanceUntilIdle()
            assertEquals("queue-id-7", repository.lastReorderedId)
            assertEquals(3, repository.lastReorderPosition)
        }

    @Test
    fun `ClearQueue event calls playerRepository clearQueue`() =
        runTest {
            viewModel.onEvent(PlayerEvent.ClearQueue)
            advanceUntilIdle()
            assertEquals(1, repository.clearQueueCallCount)
        }

    // ── Rating ────────────────────────────────────────────────────────────────

    @Test
    fun `SetRating calls songRepository setRating with current song id and rating`() =
        runTest {
            val song = testSong(42L)
            repository.emitState(PlayerUiState(currentSong = song))

            viewModel.uiState.test {
                awaitItem()
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
                awaitItem()
                viewModel.onEvent(PlayerEvent.SetRating(Rating.FAVORITE))
                advanceUntilIdle()

                assertEquals(0, songRepository.setRatingCallCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── currentSongRating ─────────────────────────────────────────────────────

    @Test
    fun `currentSongRating is NONE when no song is playing`() =
        runTest {
            viewModel.currentSongRating.test {
                assertEquals(Rating.NONE, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `currentSongRating reflects rating of current song`() =
        runTest {
            val song = testSong(42L)
            songRepository.setRatingForSong(42L, Rating.FAVORITE)
            repository.emitState(PlayerUiState(currentSong = song))

            viewModel.currentSongRating.test {
                assertEquals(Rating.FAVORITE, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `currentSongRating updates when current song changes`() =
        runTest {
            val song1 = testSong(1L)
            val song2 = testSong(2L)
            songRepository.setRatingForSong(1L, Rating.GOOD)
            songRepository.setRatingForSong(2L, Rating.FAVORITE)

            viewModel.currentSongRating.test {
                awaitItem() // initial NONE (no song)
                repository.emitState(PlayerUiState(currentSong = song1))
                assertEquals(Rating.GOOD, awaitItem())
                repository.emitState(PlayerUiState(currentSong = song2))
                assertEquals(Rating.FAVORITE, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── uiState passthrough ───────────────────────────────────────────────────

    @Test
    fun `uiState reflects repository state`() =
        runTest {
            val song = testSong(1L)
            repository.emitState(PlayerUiState(currentSong = song))

            viewModel.uiState.test {
                assertEquals(song, awaitItem().currentSong)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── formatTime ────────────────────────────────────────────────────────────

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

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun testSong(id: Long) =
        Song(
            id = id,
            title = "Song $id",
            artist = "Artist",
            albumArtist = "Artist",
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
