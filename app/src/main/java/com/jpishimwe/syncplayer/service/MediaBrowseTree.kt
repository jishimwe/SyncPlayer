package com.jpishimwe.syncplayer.service

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.jpishimwe.syncplayer.data.PlaylistRepository
import com.jpishimwe.syncplayer.data.SongRepository
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Playlist
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MediaBrowseTree @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
) {
    companion object {
        const val ROOT_ID = "[root]"
        const val SONGS_ID = "[songs]"
        const val ALBUMS_ID = "[albums]"
        const val ARTISTS_ID = "[artists]"
        const val PLAYLISTS_ID = "[playlists]"
        const val FAVORITES_ID = "[favorites]"
        const val QUEUE_ID = "[queue]"

        private const val ALBUM_PREFIX = "[album]"
        private const val ARTIST_PREFIX = "[artist]"
        private const val PLAYLIST_PREFIX = "[playlist]"

        private const val MAX_ITEMS = 100

        fun albumNodeId(albumId: Long): String = "$ALBUM_PREFIX$albumId"
        fun artistNodeId(artistName: String): String = "$ARTIST_PREFIX$artistName"
        fun playlistNodeId(playlistId: Long): String = "$PLAYLIST_PREFIX$playlistId"

        fun isAlbumNode(parentId: String): Boolean = parentId.startsWith(ALBUM_PREFIX)
        fun isArtistNode(parentId: String): Boolean = parentId.startsWith(ARTIST_PREFIX)
        fun isPlaylistNode(parentId: String): Boolean = parentId.startsWith(PLAYLIST_PREFIX)

        fun extractAlbumId(parentId: String): Long = parentId.removePrefix(ALBUM_PREFIX).toLong()
        fun extractArtistName(parentId: String): String = parentId.removePrefix(ARTIST_PREFIX)
        fun extractPlaylistId(parentId: String): Long = parentId.removePrefix(PLAYLIST_PREFIX).toLong()
    }

    fun getRootChildren(): List<MediaItem> = listOf(
        buildBrowsableItem(SONGS_ID, "Songs"),
        buildBrowsableItem(ALBUMS_ID, "Albums"),
        buildBrowsableItem(ARTISTS_ID, "Artists"),
        buildBrowsableItem(PLAYLISTS_ID, "Playlists"),
        buildBrowsableItem(FAVORITES_ID, "Favorites"),
        buildBrowsableItem(QUEUE_ID, "Queue"),
    )

    suspend fun getChildren(parentId: String, player: Player?): List<MediaItem> =
        when {
            parentId == ROOT_ID -> getRootChildren()
            parentId == SONGS_ID -> getSongItems()
            parentId == ALBUMS_ID -> getAlbumItems()
            parentId == ARTISTS_ID -> getArtistItems()
            parentId == PLAYLISTS_ID -> getPlaylistItems()
            parentId == FAVORITES_ID -> getFavoriteItems()
            parentId == QUEUE_ID -> getQueueItems(player)
            isAlbumNode(parentId) -> getAlbumSongItems(extractAlbumId(parentId))
            isArtistNode(parentId) -> getArtistSongItems(extractArtistName(parentId))
            isPlaylistNode(parentId) -> getPlaylistSongItems(extractPlaylistId(parentId))
            else -> emptyList()
        }

    suspend fun search(query: String): List<MediaItem> {
        val songs = songRepository.searchSongs(query).first().take(MAX_ITEMS)
        return songs.map { it.toAutoMediaItem() }
    }

    private suspend fun getSongItems(): List<MediaItem> {
        val songs = songRepository.getAllSongs().first()
            .sortedByDescending { it.dateAdded }
            .take(MAX_ITEMS)
        return songs.map { it.toAutoMediaItem() }
    }

    private suspend fun getAlbumItems(): List<MediaItem> {
        val albums = songRepository.getAllAlbums().first()
            .sortedBy { it.name }
            .take(MAX_ITEMS)
        return albums.map { it.toAutoMediaItem() }
    }

    private suspend fun getArtistItems(): List<MediaItem> {
        val artists = songRepository.getAllArtists().first()
            .sortedBy { it.name }
            .take(MAX_ITEMS)
        return artists.map { it.toAutoMediaItem() }
    }

    private suspend fun getPlaylistItems(): List<MediaItem> {
        val playlists = playlistRepository.getAllPlaylists().first()
            .take(MAX_ITEMS)
        return playlists.map { it.toAutoMediaItem() }
    }

    private suspend fun getFavoriteItems(): List<MediaItem> {
        val songs = songRepository.getFavoriteSongs().first().take(MAX_ITEMS)
        return songs.map { it.toAutoMediaItem() }
    }

    private fun getQueueItems(player: Player?): List<MediaItem> {
        if (player == null) return emptyList()
        val count = minOf(player.mediaItemCount, MAX_ITEMS)
        return (0 until count).map { index ->
            val item = player.getMediaItemAt(index)
            val metadata = item.mediaMetadata
            MediaItem.Builder()
                .setMediaId(item.mediaId)
                .setUri(item.localConfiguration?.uri)
                .setMediaMetadata(
                    metadata.buildUpon()
                        .setIsPlayable(true)
                        .setIsBrowsable(false)
                        .build(),
                )
                .build()
        }
    }

    private suspend fun getAlbumSongItems(albumId: Long): List<MediaItem> {
        val songs = songRepository.getSongsByAlbum(albumId).first().take(MAX_ITEMS)
        return songs.map { it.toAutoMediaItem() }
    }

    private suspend fun getArtistSongItems(artistName: String): List<MediaItem> {
        val songs = songRepository.getSongsByArtist(artistName).first().take(MAX_ITEMS)
        return songs.map { it.toAutoMediaItem() }
    }

    private suspend fun getPlaylistSongItems(playlistId: Long): List<MediaItem> {
        val songs = playlistRepository.getSongsForPlaylist(playlistId).first().take(MAX_ITEMS)
        return songs.map { it.toAutoMediaItem() }
    }
}

private fun buildBrowsableItem(id: String, title: String): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .build(),
        )
        .build()

private fun Song.toAutoMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(contentUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri?.toUri())
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build(),
        )
        .build()

private fun Album.toAutoMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(MediaBrowseTree.albumNodeId(id))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name)
                .setArtist(artist)
                .setArtworkUri(albumArtUri?.toUri())
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                .build(),
        )
        .build()

private fun Artist.toAutoMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(MediaBrowseTree.artistNodeId(name))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name)
                .setArtworkUri(artUri?.toUri())
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                .build(),
        )
        .build()

private fun Playlist.toAutoMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(MediaBrowseTree.playlistNodeId(id))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                .build(),
        )
        .build()
