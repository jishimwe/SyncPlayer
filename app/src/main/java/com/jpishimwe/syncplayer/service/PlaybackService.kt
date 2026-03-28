package com.jpishimwe.syncplayer.service

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.jpishimwe.syncplayer.ui.widget.NowPlayingWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.jpishimwe.syncplayer.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "com.jpishimwe.syncplayer.ACTION_SKIP_NEXT"
        const val ACTION_SKIP_PREVIOUS = "com.jpishimwe.syncplayer.ACTION_SKIP_PREVIOUS"
    }
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        // 1. Initialize ExoPlayer
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

        // 2. Setup MediaSession with callback
//        TODO -> compare the two implementation with claude
//        mediaSession =
//            MediaSession
//                .Builder(this, player)
//                .setCallback(
//                    object : MediaSession.Callback {
//                        @OptIn(UnstableApi::class)
//                        override fun onConnect(
//                            session: MediaSession,
//                            controller: MediaSession.ControllerInfo,
//                        ): MediaSession.ConnectionResult =
//                            MediaSession.ConnectionResult
//                                .AcceptedResultBuilder(session)
//                                .build()
//
//                        override fun onCustomCommand(
//                            session: MediaSession,
//                            controller: MediaSession.ControllerInfo,
//                            customCommand: SessionCommand,
//                            args: Bundle,
//                        ): ListenableFuture<SessionResult> =
//                            super.onCustomCommand(
//                                session,
//                                controller,
//                                customCommand,
//                                args,
//                            )
//                    },
//                ).build()

        mediaSession = MediaSession.Builder(this, player).build()

        // 3. Widget update listener
        player.addListener(widgetListener)
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession
}
