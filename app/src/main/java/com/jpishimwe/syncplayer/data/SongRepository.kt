package com.jpishimwe.syncplayer.data

import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Rating
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSongById(id: Long): Flow<Song?>

    fun getSongsByIds(idList: List<Long>): Flow<List<Song>>

    fun getAllSongs(): Flow<List<Song>>

    fun getAllAlbums(): Flow<List<Album>>

    fun getAllArtists(): Flow<List<Artist>>

    fun getSongsByAlbum(albumId: Long): Flow<List<Song>>

    fun getSongsByArtist(artist: String): Flow<List<Song>>

    suspend fun incrementPlayCount(songId: Long)

    suspend fun setRating(
        songId: Long,
        rating: Rating,
    )

    fun getFavoriteSongs(): Flow<List<Song>>

    fun getMostPlayedSongs(): Flow<List<Song>>

    fun getRecentlyPlayed(): Flow<List<Song>>

    fun getRating(songId: Long): Flow<Rating>

    fun getSongsByMinRating(minRating: Rating): Flow<List<Song>>

    suspend fun recordListeningEvent(songId: Long)

    suspend fun refreshLibrary()
}
