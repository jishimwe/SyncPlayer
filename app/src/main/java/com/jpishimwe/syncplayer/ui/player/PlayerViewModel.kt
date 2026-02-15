package com.jpishimwe.syncplayer.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpishimwe.syncplayer.data.PlayerRepository
import com.jpishimwe.syncplayer.model.PlaybackState
import com.jpishimwe.syncplayer.model.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        private val playerRepository: PlayerRepository,
    ) : ViewModel() {
        val uiState: StateFlow<PlayerUiState> =
            playerRepository.playbackState
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = PlayerUiState(),
                )

        private val positionUpdateFlow =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(Unit)
                    delay(1000)
                }
            }

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
            }
        }

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
