package com.jpishimwe.syncplayer.ui.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WidgetStateKeysTest {

    @Test
    fun `key names are stable and distinct`() {
        val keys = listOf(
            WidgetStateKeys.TITLE,
            WidgetStateKeys.ARTIST,
            WidgetStateKeys.ALBUM,
            WidgetStateKeys.ALBUM_ART_URI,
            WidgetStateKeys.IS_PLAYING,
        )
        val names = keys.map { it.name }
        assertEquals(names.size, names.toSet().size, "All widget state keys must have unique names")
    }

    @Test
    fun `TITLE key has expected name`() {
        assertEquals("widget_title", WidgetStateKeys.TITLE.name)
    }

    @Test
    fun `ARTIST key has expected name`() {
        assertEquals("widget_artist", WidgetStateKeys.ARTIST.name)
    }

    @Test
    fun `ALBUM key has expected name`() {
        assertEquals("widget_album", WidgetStateKeys.ALBUM.name)
    }

    @Test
    fun `ALBUM_ART_URI key has expected name`() {
        assertEquals("widget_album_art_uri", WidgetStateKeys.ALBUM_ART_URI.name)
    }

    @Test
    fun `IS_PLAYING key has expected name`() {
        assertEquals("widget_is_playing", WidgetStateKeys.IS_PLAYING.name)
    }
}
