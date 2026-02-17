package com.jpishimwe.syncplayer.ui.playlists

sealed interface PlaylistEvent {
    data class CreatePlaylist(
        val name: String,
    ) : PlaylistEvent

    data class RenamePlaylist(
        val playlistId: Long,
        val newName: String,
    ) : PlaylistEvent

    data class DeletePlaylist(
        val playlistId: Long,
    ) : PlaylistEvent

    data class AddSongsToPlaylist(
        val playlistId: Long,
        val songIds: List<Long>,
    ) : PlaylistEvent

    data class RemoveSongFromPlaylist(
        val playlistId: Long,
        val songId: Long,
    ) : PlaylistEvent

    data class RemoveSongsFromPlaylist(
        val playlistId: Long,
        val songIds: List<Long>,
    ) : PlaylistEvent

    data class ReorderSongs(
        val playlistId: Long,
        val orderedSongIds: List<Long>,
    ) : PlaylistEvent
}
