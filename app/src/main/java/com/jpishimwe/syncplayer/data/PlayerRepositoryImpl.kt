package com.jpishimwe.syncplayer.data

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.jpishimwe.syncplayer.data.local.QueueDao
import com.jpishimwe.syncplayer.data.local.QueueEntity
import com.jpishimwe.syncplayer.model.PlaybackState
import com.jpishimwe.syncplayer.model.PlayerUiState
import com.jpishimwe.syncplayer.model.RepeatMode
import com.jpishimwe.syncplayer.model.Song
import com.jpishimwe.syncplayer.model.toMediaItem
import com.jpishimwe.syncplayer.model.toSong
import com.jpishimwe.syncplayer.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlayerRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val queueDao: QueueDao,
        private val songRepository: SongRepository,
    ) : PlayerRepository {
        private val _playbackState = MutableStateFlow(PlayerUiState())
        override val playbackState: StateFlow<PlayerUiState> = _playbackState.asStateFlow()

        private var mediaController: MediaController? = null

        private val playerListener =
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.update {
                        it.copy(
                            playbackState =
                                if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED,
                        )
                    }
                }

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    _playbackState.update {
                        it.copy(
                            currentSong = mediaItem?.toSong(),
                            currentQueueIndex = mediaController?.currentMediaItemIndex ?: -1,
                        )
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _playbackState.update {
                        it.copy(
                            playbackState =
                                when (playbackState) {
                                    Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                                    Player.STATE_IDLE -> PlaybackState.IDLE
                                    Player.STATE_ENDED -> PlaybackState.ENDED
                                    else -> _playbackState.value.playbackState
                                },
                        )
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _playbackState.update {
                        it.copy(
                            playbackState = PlaybackState.ERROR,
                            error = error.message,
                        )
                    }
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _playbackState.update {
                        it.copy(
                            isShuffleEnabled = shuffleModeEnabled,
                        )
                    }
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _playbackState.update {
                        it.copy(
                            repeatMode =
                                when (repeatMode) {
                                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                                    Player.REPEAT_MODE_OFF -> RepeatMode.OFF
                                    else -> _playbackState.value.repeatMode
                                },
                        )
                    }
                }
            }

        private val repositoryScope = CoroutineScope((SupervisorJob() + Dispatchers.Main))

        private fun startPositionUdates() {
            repositoryScope.launch {
                while (isActive && mediaController?.isPlaying == true) {
                    delay(1000)
                    _playbackState.value.currentSong?.let {
                        _playbackState.update { it.copy(currentPosition = mediaController?.currentPosition ?: 0L) }
                    }
                }
            }
        }

        override suspend fun initialize() {
            val sessionToken =
                SessionToken(
                    context,
                    ComponentName(
                        context,
                        PlaybackService::class.java,
                    ),
                )

            mediaController =
                MediaController
                    .Builder(context, sessionToken)
                    .buildAsync()
                    .await()

            mediaController?.addListener(playerListener)

            restoreQueue()
        }

        override fun play() {
            mediaController?.play()
        }

        override fun pause() {
            mediaController?.pause()
        }

        override fun skipToNext() {
            mediaController?.seekToNext()
        }

        override fun skipToPrevious() {
            mediaController?.seekToPrevious()
        }

        override fun seekTo(positionMs: Long) {
            mediaController?.seekTo(positionMs)
        }

        override suspend fun playSongs(
            songs: List<Song>,
            startIndex: Int,
        ) {
            mediaController?.clearMediaItems()
            mediaController?.addMediaItems(songs.map { it.toMediaItem() })
            mediaController?.prepare()
            mediaController?.seekToDefaultPosition(startIndex)
            mediaController?.play()

            var index = startIndex
            queueDao.clearQueue()
            queueDao.insertList(songs.map { QueueEntity(it.id.toString(), it.id, index++) })
        }

        override suspend fun addToQueue(song: Song) {
            mediaController?.addMediaItem(song.toMediaItem())
            queueDao.addToQueue(QueueEntity(song.id.toString(), song.id, queueDao.getQueue().size))
        }

        override suspend fun playNext(song: Song) {
            mediaController?.addMediaItem(mediaController?.currentMediaItemIndex?.plus(1) ?: 0, song.toMediaItem())

            val currentSong = mediaController?.currentMediaItem?.toSong()
            val queue = queueDao.getQueue()

            val currentPosition = queue.find { it.id == currentSong?.id.toString() }?.position ?: 0
            queue.filter { it.position > currentPosition }.forEach { it.position++ }
            queueDao.clearQueue()
            queueDao.insertList(queue)
            queueDao.addToQueue(QueueEntity(song.id.toString(), song.id, currentPosition + 1))
        }

        override suspend fun removeFromQueue(queueItemId: String) {
            val mediaIndex =
                (0 until (mediaController?.mediaItemCount ?: 0)).find {
                    mediaController?.getMediaItemAt(it)?.mediaId == queueItemId
                } ?: -1

            val queue = queueDao.getQueue()
            val removedPosition = queue.find { it.id == queueItemId }?.position ?: -1
            if (removedPosition < 0 || mediaIndex < 0) {
                return
            }

            mediaController?.removeMediaItem(mediaIndex)

            queue.filter { it.position > removedPosition }.forEach { it.position-- }
            queueDao.clearQueue()
            queueDao.insertList(queue.filter { it.id != queueItemId })
        }

        override suspend fun reorderQueue(
            queueItemId: String,
            newPosition: Int,
        ) {
            val mediaIndex =
                (0 until (mediaController?.mediaItemCount ?: 0)).find {
                    mediaController?.getMediaItemAt(it)?.mediaId == queueItemId
                } ?: -1
            if (mediaIndex < 0) return

            mediaController?.moveMediaItem(mediaIndex, newPosition)

            val queue = queueDao.getQueue()
            queue.find { it.id == queueItemId }?.let { entity ->
                val oldPosition = entity.position
                if (oldPosition == newPosition) return
                if (oldPosition > newPosition) {
                    moveUp(queue, oldPosition, newPosition)
                } else {
                    moveDown(queue, oldPosition, newPosition)
                }
                entity.position = newPosition
                queueDao.clearQueue()
                queueDao.insertList(queue)
            }
        }

        override fun seekToQueueItem(index: Int) {
            TODO("Not yet implemented")
        }

        fun moveUp(
            queue: List<QueueEntity>,
            oldPosition: Int,
            newPosition: Int,
        ) {
            queue.filter { it.position in newPosition until oldPosition }.forEach {
                it.position++
            }
        }

        fun moveDown(
            queue: List<QueueEntity>,
            oldPosition: Int,
            newPosition: Int,
        ) {
            queue.filter { it.position in oldPosition until newPosition }.forEach {
                it.position--
            }
        }

        override fun toggleShuffle() {
            mediaController?.shuffleModeEnabled?.let { mediaController?.shuffleModeEnabled = !it }
        }

        override fun toggleRepeat() {
            val repeatMode = mediaController?.repeatMode ?: Player.REPEAT_MODE_OFF
            mediaController?.setRepeatMode(
                when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                    else -> Player.REPEAT_MODE_OFF
                },
            )
        }

        private suspend fun restoreQueue() {
            val queue = queueDao.getQueue()
            val songIdList = queue.map { it.songId }

            val songsFlow = songRepository.getSongsByIds(songIdList)
            val songs = songsFlow.first()
            val songsQueueMap: Map<Long, QueueEntity> = queue.associateBy { it.songId }

            val sortedSongs = songs.sortedBy { song -> songsQueueMap[song.id]?.position }

            mediaController?.clearMediaItems()
            mediaController?.addMediaItems(sortedSongs.map { it.toMediaItem() })
            mediaController?.prepare()
            mediaController?.seekToDefaultPosition(0)
        }

        private suspend fun clearQueue() {
            mediaController?.clearMediaItems()
            queueDao.clearQueue()
        }
    }
