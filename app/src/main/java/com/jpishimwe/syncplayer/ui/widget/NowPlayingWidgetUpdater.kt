package com.jpishimwe.syncplayer.ui.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition

private const val TAG = "WidgetUpdater"

object NowPlayingWidgetUpdater {

    suspend fun update(
        context: Context,
        title: String?,
        artist: String?,
        album: String?,
        albumArtUri: String?,
        isPlaying: Boolean,
    ) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(NowPlayingWidget::class.java)
            if (glanceIds.isEmpty()) return

            for (glanceId in glanceIds) {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        setOrRemove(WidgetStateKeys.TITLE, title)
                        setOrRemove(WidgetStateKeys.ARTIST, artist)
                        setOrRemove(WidgetStateKeys.ALBUM, album)
                        setOrRemove(WidgetStateKeys.ALBUM_ART_URI, albumArtUri)
                        this[WidgetStateKeys.IS_PLAYING] = isPlaying
                    }
                }
            }

            NowPlayingWidget().updateAll(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget", e)
        }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.setOrRemove(
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
        value: String?,
    ) {
        if (value != null) this[key] = value else remove(key)
    }
}
