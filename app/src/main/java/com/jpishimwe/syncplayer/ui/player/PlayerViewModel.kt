package com.jpishimwe.syncplayer.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpishimwe.syncplayer.data.PlayerRepository
import com.jpishimwe.syncplayer.data.SongRepository
import com.jpishimwe.syncplayer.model.PlaybackState
import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.Rating
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        private val playerRepository: PlayerRepository,
        private val songRepository: SongRepository,
    ) : ViewModel() {
        val uiState: StateFlow<PlayerUiState> =
            playerRepository.playbackState
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = PlayerUiState(),
                )

        init {
            viewModelScope.launch {
                playerRepository.initialize()
            }
        }

        fun onEvent(event: PlayerEvent) {
            when (event) {
                PlayerEvent.PlayPause -> {
                    togglePlayback()
                }

                PlayerEvent.SkipToNext -> {
                    playerRepository.skipToNext()
                }

                PlayerEvent.SkipToPrevious -> {
                    playerRepository.skipToPrevious()
                }

                is PlayerEvent.SeekTo -> {
                    playerRepository.seekTo(event.positionMs)
                }

                is PlayerEvent.SeekToQueueItem -> {
                    playerRepository.seekToQueueItem(event.index)
                }

                PlayerEvent.ToggleShuffle -> {
                    playerRepository.toggleShuffle()
                }

                PlayerEvent.ToggleRepeat -> {
                    playerRepository.toggleRepeat()
                }

                is PlayerEvent.PlaySongs -> {
                    viewModelScope.launch {
                        playerRepository.playSongs(event.songs, event.startIndex)
                    }
                }

                is PlayerEvent.AddToQueue -> {
                    viewModelScope.launch {
                        playerRepository.addToQueue(event.song)
                    }
                }

                is PlayerEvent.PlayNext -> {
                    viewModelScope.launch { playerRepository.playNext(event.song) }
                }

                is PlayerEvent.RemoveFromQueue -> {
                    viewModelScope.launch {
                        playerRepository.removeFromQueue(event.queueItemId)
                    }
                }

                is PlayerEvent.ReorderQueue -> {
                    viewModelScope.launch {
                        playerRepository.reorderQueue(event.queueItemId, event.newPosition)
                    }
                }

                is PlayerEvent.SetRating -> {
                    viewModelScope.launch {
                        val songId = uiState.value.currentSong?.id ?: return@launch
                        songRepository.setRating(songId, event.rating)
                    }
                }
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        val currentSongRating: StateFlow<Rating> =
            playerRepository.playbackState
                .map { it.currentSong?.id }
                .distinctUntilChanged()
                .flatMapLatest { songId ->
                    if (songId == null) {
                        flowOf(Rating.NONE)
                    } else {
                        songRepository.getRating(songId)
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), Rating.NONE)

        private fun togglePlayback() {
            if (uiState.value.playbackState == PlaybackState.PLAYING) {
                playerRepository.pause()
            } else {
                playerRepository.play()
            }
        }

        fun formatTime(ms: Long): String {
            val totalSeconds = (ms / 1000) % 60
            val minutes = (ms / (1000 * 60)) % 60
            val hours = (ms / (1000 * 60 * 60))
            return if (hours > 0) {
                String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, totalSeconds)
            } else {
                String.format(Locale.getDefault(), "%d:%02d", minutes, totalSeconds)
            }
        }
    }
