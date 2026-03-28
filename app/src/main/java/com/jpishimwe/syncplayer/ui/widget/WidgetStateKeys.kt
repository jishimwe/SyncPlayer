package com.jpishimwe.syncplayer.ui.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetStateKeys {
    val TITLE = stringPreferencesKey("widget_title")
    val ARTIST = stringPreferencesKey("widget_artist")
    val ALBUM = stringPreferencesKey("widget_album")
    val ALBUM_ART_URI = stringPreferencesKey("widget_album_art_uri")
    val IS_PLAYING = booleanPreferencesKey("widget_is_playing")
}
