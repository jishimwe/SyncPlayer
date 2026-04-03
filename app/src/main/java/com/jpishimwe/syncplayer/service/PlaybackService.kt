package com.jpishimwe.syncplayer.service

import android.content.Intent
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.jpishimwe.syncplayer.data.SongRepository
import com.jpishimwe.syncplayer.data.local.QueueDao
import com.jpishimwe.syncplayer.model.toMediaItem
import com.jpishimwe.syncplayer.ui.widget.NowPlayingWidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.jpishimwe.syncplayer.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "com.jpishimwe.syncplayer.ACTION_SKIP_NEXT"
        const val ACTION_SKIP_PREVIOUS = "com.jpishimwe.syncplayer.ACTION_SKIP_PREVIOUS"
    }

    @Inject lateinit var browseTree: MediaBrowseTree
    @Inject lateinit var queueDao: QueueDao
    @Inject lateinit var songRepository: SongRepository

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var searchResults: List<MediaItem> = emptyList()

    // Call interruption: track whether we paused due to a call so we can resume.
    private var pausedForCall = false

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
                    true, // ExoPlayer manages audio focus
                ).build()

        player.setHandleAudioBecomingNoisy(true)

        mediaSession = MediaLibrarySession.Builder(this, player, librarySessionCallback)
            .build()

        player.addListener(widgetListener)
        registerCallListener()
    }

    // ─── Issue 1 & 2 fix: onConnect + onPlaybackResumption ───────────────────

    private val librarySessionCallback = object : MediaLibrarySession.Callback {

        // Explicitly accept all connections and expose the full command set so
        // Android Auto's browse UI and hardware buttons both work.
        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ConnectionResult {
            Log.d("PlaybackService", "onConnect: ${controller.packageName}")
            return ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS,
                )
                .build()
        }

        // Called when the car starts up and wants to resume the last session.
        @OptIn(UnstableApi::class)
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return serviceScope.future {
                val queue = queueDao.getQueue()
                if (queue.isEmpty()) {
                    // Nothing to resume — return an empty result
                    return@future MediaSession.MediaItemsWithStartPosition(
                        emptyList(),
                        0,
                        0L,
                    )
                }
                val songIds = queue.map { it.songId }
                val songs = songRepository.getSongsByIds(songIds).first()
                val songMap = songs.associateBy { it.id }
                val orderedMediaItems = queue
                    .sortedBy { it.position }
                    .mapNotNull { entity -> songMap[entity.songId]?.toMediaItem() }
                Log.d("PlaybackService", "onPlaybackResumption: restoring ${orderedMediaItems.size} items")
                MediaSession.MediaItemsWithStartPosition(orderedMediaItems, 0, 0L)
            }
        }

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
                Log.d("PlaybackService", "onGetChildren($parentId): ${children.size} items")
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

    // ─── Issue 3 fix: call interruption ──────────────────────────────────────

    // Car head units may send AUDIOFOCUS_LOSS (not AUDIOFOCUS_LOSS_TRANSIENT)
    // during phone calls, which means ExoPlayer won't auto-resume after the call.
    // We listen to phone state directly as a safety net.
    private var telephonyCallback: TelephonyCallback? = null

    @RequiresApi(31)
    private inner class CallStateCallback : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING,
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (player.isPlaying) {
                        pausedForCall = true
                        player.pause()
                        Log.d("PlaybackService", "Call started — pausing playback")
                    }
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (pausedForCall) {
                        pausedForCall = false
                        player.play()
                        Log.d("PlaybackService", "Call ended — resuming playback")
                    }
                }
            }
        }
    }

    private fun registerCallListener() {
        if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("PlaybackService", "READ_PHONE_STATE not granted — skipping call listener")
            return
        }
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val callback = CallStateCallback()
        telephonyCallback = callback
        telephonyManager.registerTelephonyCallback(
            Executors.newSingleThreadExecutor(),
            callback,
        )
    }

    private fun unregisterCallListener() {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        telephonyCallback = null
    }

    // ─── Widget ───────────────────────────────────────────────────────────────

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

    // ─── Lifecycle ────────────────────────────────────────────────────────────

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
        unregisterCallListener()
        serviceScope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaSession
}
