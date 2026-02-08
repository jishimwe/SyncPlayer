package com.jpishimwe.syncplayer.data

import com.jpishimwe.syncplayer.data.local.SongDao
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val mediaStoreScanner: MediaStoreScanner
) : SongRepository {

    override fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    override fun getAllAlbums(): Flow<List<Album>> = songDao.getAllAlbums()

    override fun getAllArtists(): Flow<List<Artist>> = songDao.getAllArtists()

    override fun getSongsByAlbum(albumId: Long): Flow<List<Song>> =
        songDao.getSongsByAlbum(albumId)

    override fun getSongsByArtist(artist: String): Flow<List<Song>> =
        songDao.getSongsByArtist(artist)

    override suspend fun refreshLibrary() {
        val songs = mediaStoreScanner.scanSongs()
        songDao.deleteAll()
        songDao.insertAll(songs)
    }
}
