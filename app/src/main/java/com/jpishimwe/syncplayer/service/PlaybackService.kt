package com.jpishimwe.syncplayer.service

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class PlaybackService : MediaSessionService() {
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

    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession
}
