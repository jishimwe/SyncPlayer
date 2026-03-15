package com.jpishimwe.syncplayer.data

interface ArtistImageRepository {
    suspend fun getArtistImageUrl(artistName: String): String?
}
