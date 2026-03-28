package com.jpishimwe.syncplayer.ui.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.jpishimwe.syncplayer.service.PlaybackService

class PlayPauseCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startService(
            Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_PLAY_PAUSE),
        )
    }
}

class SkipNextCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startService(
            Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_SKIP_NEXT),
        )
    }
}

class SkipPreviousCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startService(
            Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_SKIP_PREVIOUS),
        )
    }
}
