package com.jpishimwe.syncplayer.data

import com.jpishimwe.syncplayer.data.local.ListeningHistoryDao
import com.jpishimwe.syncplayer.data.local.ListeningHistoryEntity
import com.jpishimwe.syncplayer.data.local.SongDao
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SongRepositoryImpl
    @Inject
    constructor(
        private val songDao: SongDao,
        private val mediaStoreScanner: MediaStoreScanner,
        private val listeningHistoryDao: ListeningHistoryDao,
    ) : SongRepository {
        override fun getSongById(id: Long): Flow<Song?> = songDao.getSongById(id)

        override fun getSongsByIds(idList: List<Long>): Flow<List<Song>> = songDao.getSongsByIds(idList)

        override fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

        override fun getAllAlbums(): Flow<List<Album>> = songDao.getAllAlbums()

        override fun getAllArtists(): Flow<List<Artist>> = songDao.getAllArtists()

        override fun getSongsByAlbum(albumId: Long): Flow<List<Song>> = songDao.getSongsByAlbum(albumId)

        override fun getSongsByArtist(artist: String): Flow<List<Song>> = songDao.getSongsByArtist(artist)

        override suspend fun incrementPlayCount(songId: Long) {
            songDao.incrementPlayCount(songId, System.currentTimeMillis())
        }

        override suspend fun setRating(
            songId: Long,
            rating: Rating,
        ) {
            songDao.setRating(songId, rating.value)
        }

        override fun getFavoriteSongs(): Flow<List<Song>> = songDao.getFavoriteSongs()

        override fun getMostPlayedSongs(): Flow<List<Song>> = songDao.getMostPlayedSongs()

        override fun getRecentlyPlayed(): Flow<List<Song>> = listeningHistoryDao.getRecentlyPlayed()

        override fun getRating(songId: Long): Flow<Rating> = songDao.getRating(songId).map { Rating.fromInt(it) }

        override fun getSongsByMinRating(minRating: Rating): Flow<List<Song>> =
            songDao
                .getSongsByMinRating(minRating.value)

        override suspend fun recordListeningEvent(songId: Long) {
            listeningHistoryDao.insertListeningHistory(ListeningHistoryEntity(songId = songId, playedAt = System.currentTimeMillis()))
        }

        override suspend fun refreshLibrary() {
            val songs = mediaStoreScanner.scanSongs()
            songDao.deleteAll()
            songDao.insertAll(songs)
        }
    }
