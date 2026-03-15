package com.jpishimwe.syncplayer.data

import com.jpishimwe.syncplayer.data.local.ArtistImageDao
import com.jpishimwe.syncplayer.data.remote.ArtistImageService
import com.jpishimwe.syncplayer.model.ArtistImage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistImageRepositoryImpl
    @Inject
    constructor(
        private val dao: ArtistImageDao,
        private val service: ArtistImageService,
    ) : ArtistImageRepository {
        override suspend fun getArtistImageUrl(artistName: String): String? {
            if (artistName.isBlank()) return null

            val cached = dao.getByArtistName(artistName)
            val now = System.currentTimeMillis()
            val sevenDays = 7 * 24 * 60 * 60 * 1000L

            if (cached != null && cached.imageUrl != null && (now - cached.fetchedAt) < sevenDays) {
                return cached.imageUrl
            }

            // Retry failed lookups (null imageUrl) after 1 day instead of 7
            val oneDay = 24 * 60 * 60 * 1000L
            if (cached != null && cached.imageUrl == null && (now - cached.fetchedAt) < oneDay) {
                return null
            }

            val url = service.fetchArtistImageUrl(artistName)
            if (url != null) {
                dao.insertOrReplace(
                    ArtistImage(
                        artistName = artistName,
                        imageUrl = url,
                        fetchedAt = now,
                    ),
                )
            }
            return url
        }
    }
