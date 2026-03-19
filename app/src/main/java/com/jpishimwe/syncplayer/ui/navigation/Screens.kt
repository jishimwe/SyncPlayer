package com.jpishimwe.syncplayer.ui.navigation

import android.net.Uri

sealed class Screen(
    val route: String,
) {
    data object Home : Screen("home")

    // NowPlaying removed — no longer a navigation route, now an AnimatedContent overlay

    data object AlbumDetail : Screen("album_detail/{albumId}/{albumName}") {
        fun createRoute(
            albumId: Long,
            albumName: String,
        ) = "album_detail/$albumId/$albumName"
    }

    data object ArtistDetail : Screen("artist_detail/{artistName}") {
        fun createRoute(artistName: String) = "artist_detail/$artistName"
    }

    data object PlaylistDetail : Screen("playlist_detail/{playlistId}/{playlistName}") {
        fun createRoute(
            playlistId: Long,
            playlistName: String,
        ) = "playlist_detail/$playlistId/${Uri.encode(playlistName)}"
    }

    data object Settings : Screen("settings")
}

enum class LibraryTab(
    val label: String,
) {
    HISTORY("History"),
    FAVORITES("Faves"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
}

data class TabBounds(
    val x: Int,
    val width: Int,
)
