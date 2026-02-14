package com.jpishimwe.syncplayer.data

import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSongRepository : SongRepository {
    val songsFlow = MutableStateFlow<List<Song>>(emptyList())
    val albumsFlow = MutableStateFlow<List<Album>>(emptyList())
    val artistsFlow = MutableStateFlow<List<Artist>>(emptyList())
    val songsByIdsFlow = MutableStateFlow<List<Song>>(emptyList())
    val songById = MutableStateFlow<Song?>(null)

    var refreshError: Exception? = null
    var refreshCallCount = 0

    override fun getSongsByIds(idList: List<Long>): Flow<List<Song>> = songsByIdsFlow

    override fun getSongById(id: Long): Flow<Song?> = songById

    override fun getAllSongs(): Flow<List<Song>> = songsFlow

    override fun getAllAlbums(): Flow<List<Album>> = albumsFlow

    override fun getAllArtists(): Flow<List<Artist>> = artistsFlow

    override fun getSongsByAlbum(albumId: Long): Flow<List<Song>> = MutableStateFlow(songsFlow.value.filter { it.albumId == albumId })

    override fun getSongsByArtist(artist: String): Flow<List<Song>> = MutableStateFlow(songsFlow.value.filter { it.artist == artist })

    override suspend fun refreshLibrary() {
        refreshCallCount++
        refreshError?.let { throw it }
    }
}
