package com.jpishimwe.syncplayer.ui.library

import com.jpishimwe.syncplayer.MainDispatcherRule
import com.jpishimwe.syncplayer.data.FakePlayerRepository
import com.jpishimwe.syncplayer.model.PlaybackState
import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.ui.player.PlayerEvent
import com.jpishimwe.syncplayer.ui.player.PlayerViewModel
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakePlayerRepository
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setup() {
        repository = FakePlayerRepository()
        viewModel = PlayerViewModel(repository)
    }

    @Test
    fun `PlayPause calls play when paused`() =
        runTest {
            repository.emitState(PlayerUiState(playbackState = PlaybackState.PAUSED))

            viewModel.onEvent(PlayerEvent.PlayPause)

            assertEquals(1, repository.playCallCount)
            assertEquals(0, repository.pauseCallCount)
        }

    @Test
    fun `PlayPause calls pause when playing`() =
        runTest {
            repository.emitState(PlayerUiState(playbackState = PlaybackState.PLAYING))

            viewModel.onEvent(PlayerEvent.PlayPause)

            assertEquals(1, repository.pauseCallCount)
            assertEquals(0, repository.playCallCount)
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

            assertEquals(song, viewModel.uiState.value.currentSong)
        }
}
