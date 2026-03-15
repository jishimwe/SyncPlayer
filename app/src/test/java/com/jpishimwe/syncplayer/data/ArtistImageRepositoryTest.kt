package com.jpishimwe.syncplayer.data

import com.jpishimwe.syncplayer.data.local.ArtistImageDao
import com.jpishimwe.syncplayer.data.remote.ArtistImageService
import com.jpishimwe.syncplayer.model.ArtistImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ArtistImageRepositoryTest {
    private lateinit var dao: FakeArtistImageDao
    private lateinit var service: FakeArtistImageService
    private lateinit var repository: ArtistImageRepositoryImpl

    @BeforeEach
    fun setup() {
        dao = FakeArtistImageDao()
        service = FakeArtistImageService()
        repository = ArtistImageRepositoryImpl(dao, service)
    }

    @Test
    fun `cache hit returns cached url without calling service`() =
        runTest {
            val cached =
                ArtistImage(
                    artistName = "NMIXX",
                    imageUrl = "https://example.com/nmixx.jpg",
                    fetchedAt = System.currentTimeMillis(),
                )
            dao.insertOrReplace(cached)

            val result = repository.getArtistImageUrl("NMIXX")

            assertEquals("https://example.com/nmixx.jpg", result)
            assertEquals(0, service.fetchCount)
        }

    @Test
    fun `cache miss calls service and caches result`() =
        runTest {
            service.nextResult = "https://example.com/weeknd.jpg"

            val result = repository.getArtistImageUrl("The Weeknd")

            assertEquals("https://example.com/weeknd.jpg", result)
            assertEquals(1, service.fetchCount)
            assertEquals("https://example.com/weeknd.jpg", dao.getByArtistName("The Weeknd")?.imageUrl)
        }

    @Test
    fun `stale cache refreshes from service`() =
        runTest {
            val eightDaysAgo = System.currentTimeMillis() - 8 * 24 * 60 * 60 * 1000L
            val stale =
                ArtistImage(
                    artistName = "Daft Punk",
                    imageUrl = "https://example.com/old.jpg",
                    fetchedAt = eightDaysAgo,
                )
            dao.insertOrReplace(stale)
            service.nextResult = "https://example.com/new.jpg"

            val result = repository.getArtistImageUrl("Daft Punk")

            assertEquals("https://example.com/new.jpg", result)
            assertEquals(1, service.fetchCount)
        }

    @Test
    fun `service returns null caches null`() =
        runTest {
            service.nextResult = null

            val result = repository.getArtistImageUrl("Unknown")

            assertNull(result)
            assertEquals(1, service.fetchCount)
            assertNull(dao.getByArtistName("Unknown")?.imageUrl)
        }
}

private class FakeArtistImageDao : ArtistImageDao {
    private val store = mutableMapOf<String, ArtistImage>()

    override suspend fun getByArtistName(name: String): ArtistImage? = store[name]

    override suspend fun insertOrReplace(entity: ArtistImage) {
        store[entity.artistName] = entity
    }

    override fun getAll(): Flow<List<ArtistImage>> = MutableStateFlow(store.values.toList())
}

private class FakeArtistImageService : ArtistImageService() {
    var nextResult: String? = null
    var fetchCount = 0

    override suspend fun fetchArtistImageUrl(artistName: String): String? {
        fetchCount++
        return nextResult
    }
}
