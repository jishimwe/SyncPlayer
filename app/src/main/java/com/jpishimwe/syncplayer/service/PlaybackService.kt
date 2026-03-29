package com.jpishimwe.syncplayer.service

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.jpishimwe.syncplayer.ui.widget.NowPlayingWidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.jpishimwe.syncplayer.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "com.jpishimwe.syncplayer.ACTION_SKIP_NEXT"
        const val ACTION_SKIP_PREVIOUS = "com.jpishimwe.syncplayer.ACTION_SKIP_PREVIOUS"
    }

    @Inject lateinit var browseTree: MediaBrowseTree

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var searchResults: List<MediaItem> = emptyList()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        player =
            ExoPlayer
                .Builder(this)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                ).build()

        player.setHandleAudioBecomingNoisy(true)

        mediaSession = MediaLibrarySession.Builder(this, player, librarySessionCallback)
            .build()

        player.addListener(widgetListener)
    }

    private val librarySessionCallback = object : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(MediaBrowseTree.ROOT_ID)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build(),
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return serviceScope.future {
                val children = browseTree.getChildren(parentId, player)
                LibraryResult.ofItemList(ImmutableList.copyOf(children), params)
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return serviceScope.future {
                val allChildren = browseTree.getChildren(MediaBrowseTree.ROOT_ID, player)
                val item = allChildren.find { it.mediaId == mediaId }
                if (item != null) {
                    LibraryResult.ofItem(item, null)
                } else {
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                }
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            return serviceScope.future {
                searchResults = browseTree.search(query)
                session.notifySearchResultChanged(browser, query, searchResults.size, params)
                LibraryResult.ofVoid(params)
            }
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(searchResults), params),
            )
        }
    }

    private val widgetListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateWidget()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateWidget()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                updateWidget()
            }
        }
    }

    private fun updateWidget() {
        val currentItem = player.currentMediaItem
        val metadata = currentItem?.mediaMetadata
        serviceScope.launch {
            NowPlayingWidgetUpdater.update(
                context = this@PlaybackService,
                title = metadata?.title?.toString(),
                artist = metadata?.artist?.toString(),
                album = metadata?.albumTitle?.toString(),
                albumArtUri = metadata?.artworkUri?.toString(),
                isPlaying = player.isPlaying,
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
                Log.d("PlaybackService", "Widget: play/pause toggled")
            }
            ACTION_SKIP_NEXT -> {
                player.seekToNext()
                Log.d("PlaybackService", "Widget: skip next")
            }
            ACTION_SKIP_PREVIOUS -> {
                player.seekToPrevious()
                Log.d("PlaybackService", "Widget: skip previous")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaSession
}
