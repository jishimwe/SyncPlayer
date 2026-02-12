package com.jpishimwe.syncplayer.service

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FadeConfig(
    val fadeInDuration: Long = 500L,
    val fadeOutDuration: Long = 500L,
    val duckDuration: Long = 300L,
    val fadeSteps: Int = 20,
    val enableFades: Boolean = true,
)

class AudioFocusHandler(
    private val audioManager: AudioManager,
    private val player: ExoPlayer,
    private val coroutineScope: CoroutineScope,
) {
    private var playbackDelayed = false
    private var playbackNowAuthorized = false
    private var resumeOnFocusGain = false

    private var volumeBeforeDuck = 1.0f
    private var fadeJob: Job? = null

    private val fadeDuration = 500L
    private val fadeSteps = 20
    private val fadeInterval = fadeDuration / fadeSteps

    private val audioFocusRequest =
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            ).setOnAudioFocusChangeListener { focusChange ->
                handleAudioFocusChange(focusChange)
            }.build()

    fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(audioFocusRequest)

        return when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                playbackNowAuthorized = true
                true
            }

            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                playbackDelayed = true
                false
            }

            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                playbackNowAuthorized = false
                false
            }

            else -> {
                false
            }
        }
    }

    /**
     * Abandon audio focus when stopping playback
     * Call this when:
     * - User stops playback completely
     * - Service is being destroyed
     * - App is going to background and won't resume
     */
    fun abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        playbackNowAuthorized = false
        playbackDelayed = false
        resumeOnFocusGain = false
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (playbackDelayed || resumeOnFocusGain) {
                    fadeIn { player.play() }
                    playbackDelayed = false
                    resumeOnFocusGain = false
                } else if (player.isPlaying) {
                    fadeVolumeTo(volumeBeforeDuck)
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                fadeOut {
                    player.pause()
                    abandonAudioFocus()
                }
                resumeOnFocusGain = false
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (player.isPlaying) {
                    fadeOut {
                        player.pause()
                    }
                    resumeOnFocusGain = true
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (player.isPlaying) {
                    volumeBeforeDuck = player.volume
                    player.volume = 0.1f
                }
            }
        }
    }

    private fun fadeIn(onComplete: () -> Unit = {}) {
        fadeJob?.cancel()

        val targetVolume = volumeBeforeDuck
        player.volume = 0f
        onComplete()

        fadeJob =
            coroutineScope.launch {
                val stepSize = targetVolume / fadeSteps
                repeat(fadeSteps) { step ->
                    delay(fadeInterval)
                    player.volume = (step + 1) * stepSize
                }
                player.volume = targetVolume
            }
    }

    private fun fadeOut(onComplete: () -> Unit = {}) {
        fadeJob?.cancel()

        val startVolume = player.volume

        fadeJob =
            coroutineScope.launch {
                val stepSize = startVolume / fadeSteps
                repeat(fadeSteps) { step ->
                    delay(fadeInterval)
                    player.volume = startVolume - (step + 1) * stepSize
                }
                player.volume = 0f
                onComplete()
            }
    }

    private fun fadeVolumeTo(targetVolume: Float) {
        fadeJob?.cancel()

        val startVolume = player.volume
        val volumeDelta = targetVolume - startVolume

        fadeJob =
            coroutineScope.launch {
                val stepSize = volumeDelta / fadeSteps
                repeat(fadeSteps) { step ->
                    delay(fadeInterval)
                    player.volume = startVolume + (step + 1) * stepSize
                }
                player.volume = targetVolume
            }
    }
}
