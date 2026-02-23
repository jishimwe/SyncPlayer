package com.jpishimwe.syncplayer.data

import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSongRepository : SongRepository {
    val songsFlow = MutableStateFlow<List<Song>>(emptyList())
    val albumsFlow = MutableStateFlow<List<Album>>(emptyList())
    val artistsFlow = MutableStateFlow<List<Artist>>(emptyList())
    val songsByIdsFlow = MutableStateFlow<List<Song>>(emptyList())
    val songById = MutableStateFlow<Song?>(null)
    val favoritesFlow = MutableStateFlow<List<Song>>(emptyList())
    val mostPlayedFlow = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayedFlow = MutableStateFlow<List<Song>>(emptyList())
    private val ratingsMap = MutableStateFlow<Map<Long, Rating>>(emptyMap())

    var refreshError: Exception? = null
    var refreshCallCount = 0
    var setRatingCallCount = 0
    var lastSetRatingSongId: Long? = null
    var lastSetRating: Rating? = null
    var incrementPlayCountCallCount = 0
    var lastIncrementPlayCountSongId: Long? = null
    var recordListeningEventCallCount = 0

    fun setRatingForSong(songId: Long, rating: Rating) {
        ratingsMap.value = ratingsMap.value + (songId to rating)
    }

    override fun getSongsByIds(idList: List<Long>): Flow<List<Song>> = songsByIdsFlow

    override fun getSongById(id: Long): Flow<Song?> = songById

    override fun getAllSongs(): Flow<List<Song>> = songsFlow

    override fun getAllAlbums(): Flow<List<Album>> = albumsFlow

    override fun getAllArtists(): Flow<List<Artist>> = artistsFlow

    override fun getSongsByAlbum(albumId: Long): Flow<List<Song>> = MutableStateFlow(songsFlow.value.filter { it.albumId == albumId })

    override fun getSongsByArtist(artist: String): Flow<List<Song>> = MutableStateFlow(songsFlow.value.filter { it.artist == artist })

    override suspend fun incrementPlayCount(songId: Long) {
        incrementPlayCountCallCount++
        lastIncrementPlayCountSongId = songId
    }

    override suspend fun setRating(
        songId: Long,
        rating: Rating,
    ) {
        setRatingCallCount++
        lastSetRatingSongId = songId
        lastSetRating = rating
    }

    override fun getFavoriteSongs(): Flow<List<Song>> = favoritesFlow

    override fun getMostPlayedSongs(): Flow<List<Song>> = mostPlayedFlow

    override fun getRecentlyPlayed(): Flow<List<Song>> = recentlyPlayedFlow

    override fun getRating(songId: Long): Flow<Rating> = ratingsMap.map { it[songId] ?: Rating.NONE }

    override fun getSongsByMinRating(minRating: Rating): Flow<List<Song>> = favoritesFlow

    override suspend fun recordListeningEvent(songId: Long) {
        recordListeningEventCallCount++
    }

    override suspend fun refreshLibrary() {
        refreshCallCount++
        refreshError?.let { throw it }
    }
}