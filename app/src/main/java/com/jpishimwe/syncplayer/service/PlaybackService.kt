package com.jpishimwe.syncplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var audioFocusHandler: AudioFocusHandler
    private lateinit var broadCastReceiver: BecomingNoisyReceiver
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
                    false,
                ).build()

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

        // 3. Register audio focus listener
        audioFocusHandler = AudioFocusHandler(getSystemService(AUDIO_SERVICE) as AudioManager, player, serviceScope)
        // 4. Register BecomingNoisyReceiver (headphone disconnect)
        broadCastReceiver = BecomingNoisyReceiver(player)
    }

    override fun onDestroy() {
        audioFocusHandler.abandonAudioFocus()
        serviceScope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession
}

private class BecomingNoisyReceiver(
    val player: ExoPlayer,
) : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            player.pause()
        }
    }
}
