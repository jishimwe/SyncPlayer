package com.jpishimwe.syncplayer.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongDaoTest {

    private lateinit var database: SyncPlayerDatabase
    private lateinit var dao: SongDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SyncPlayerDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.songDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ── getAllSongs ────────────────────────────────────────────────────────────

    @Test
    fun getAllSongs_returnsEmptyListWhenNothingInserted() = runTest {
        assertTrue(dao.getAllSongs().first().isEmpty())
    }

    @Test
    fun getAllSongs_returnsSortedByTitleAsc() = runTest {
        dao.insertAll(listOf(testSong(1, "Zebra"), testSong(2, "Apple"), testSong(3, "Mango")))

        val result = dao.getAllSongs().first()
        assertEquals(listOf("Apple", "Mango", "Zebra"), result.map { it.title })
    }

    // ── getSongById ───────────────────────────────────────────────────────────

    @Test
    fun getSongById_returnsCorrectSong() = runTest {
        dao.insertAll(listOf(testSong(42, "My Song")))

        val song = dao.getSongById(42).first()
        assertEquals(42L, song?.id)
        assertEquals("My Song", song?.title)
    }

    @Test
    fun getSongById_returnsNullForMissingId() = runTest {
        assertNull(dao.getSongById(999).first())
    }

    // ── getSongsByIds ─────────────────────────────────────────────────────────

    @Test
    fun getSongsByIds_returnsOnlyRequestedIds() = runTest {
        dao.insertAll(listOf(testSong(1, "A"), testSong(2, "B"), testSong(3, "C")))

        val result = dao.getSongsByIds(listOf(1L, 3L)).first()
        assertEquals(2, result.size)
        assertTrue(result.map { it.id }.containsAll(listOf(1L, 3L)))
    }

    // ── getAllAlbums ──────────────────────────────────────────────────────────

    @Test
    fun getAllAlbums_groupsByAlbumId() = runTest {
        dao.insertAll(
            listOf(
                testSong(1, "Song 1", albumId = 10, album = "Album A"),
                testSong(2, "Song 2", albumId = 10, album = "Album A"),
                testSong(3, "Song 3", albumId = 20, album = "Album B"),
            )
        )

        val albums = dao.getAllAlbums().first()
        assertEquals(2, albums.size)
        val albumA = albums.find { it.name == "Album A" }!!
        assertEquals(2, albumA.songCount)
        assertEquals(10L, albumA.id)
    }

    @Test
    fun getAllAlbums_sortedByAlbumNameAsc() = runTest {
        dao.insertAll(
            listOf(
                testSong(1, "S1", albumId = 2, album = "Zebra"),
                testSong(2, "S2", albumId = 1, album = "Apple"),
            )
        )

        val albums = dao.getAllAlbums().first()
        assertEquals("Apple", albums[0].name)
        assertEquals("Zebra", albums[1].name)
    }

    // ── getAllArtists ─────────────────────────────────────────────────────────

    @Test
    fun getAllArtists_groupsByArtist() = runTest {
        dao.insertAll(
            listOf(
                testSong(1, "S1", artist = "Artist X", albumId = 1),
                testSong(2, "S2", artist = "Artist X", albumId = 2),
                testSong(3, "S3", artist = "Artist Y", albumId = 3),
            )
        )

        val artists = dao.getAllArtists().first()
        assertEquals(2, artists.size)
        val artistX = artists.find { it.name == "Artist X" }!!
        assertEquals(2, artistX.songCount)
        assertEquals(2, artistX.albumCount)
    }

    // ── getSongsByAlbum ───────────────────────────────────────────────────────

    @Test
    fun getSongsByAlbum_filtersCorrectlyAndSortsByTrackNumber() = runTest {
        dao.insertAll(
            listOf(
                testSong(1, "S1", albumId = 10, trackNumber = 2),
                testSong(2, "S2", albumId = 10, trackNumber = 1),
                testSong(3, "S3", albumId = 20),
            )
        )

        val result = dao.getSongsByAlbum(10).first()
        assertEquals(2, result.size)
        assertEquals(1, result[0].trackNumber)
    }

    // ── getSongsByArtist ──────────────────────────────────────────────────────

    @Test
    fun getSongsByArtist_filtersCorrectly() = runTest {
        dao.insertAll(
            listOf(
                testSong(1, "S1", artist = "Beatles"),
                testSong(2, "S2", artist = "Beatles"),
                testSong(3, "S3", artist = "Rolling Stones"),
            )
        )

        val result = dao.getSongsByArtist("Beatles").first()
        assertEquals(2, result.size)
        assertTrue(result.all { it.artist == "Beatles" })
    }

    // ── deleteAll ─────────────────────────────────────────────────────────────

    @Test
    fun deleteAll_removesAllSongs() = runTest {
        dao.insertAll(listOf(testSong(1, "Song 1"), testSong(2, "Song 2")))
        dao.deleteAll()

        assertTrue(dao.getAllSongs().first().isEmpty())
    }

    // ── upsertSongs ───────────────────────────────────────────────────────────

    @Test
    fun upsertSongs_preservesRatingAndPlayCountOnExistingRow() = runTest {
        dao.insertAll(listOf(testSong(1, "Original Title")))
        dao.setRating(1L, 5, System.currentTimeMillis())
        dao.incrementPlayCount(1L, System.currentTimeMillis())

        val updated = testSong(1, "New Title")
        dao.upsertSongs(listOf(updated))

        val result = dao.getSongById(1L).first()!!
        assertEquals("New Title", result.title)
        assertEquals(5, result.rating)
        assertEquals(1, result.playCount)
    }

    @Test
    fun upsertSongs_insertsNewSongsWithDefaultRating() = runTest {
        dao.upsertSongs(listOf(testSong(99, "Brand New")))

        val song = dao.getSongById(99L).first()!!
        assertEquals("Brand New", song.title)
        assertEquals(0, song.rating)
        assertEquals(0, song.playCount)
    }

    // ── incrementPlayCount ────────────────────────────────────────────────────

    @Test
    fun incrementPlayCount_incrementsCorrectly() = runTest {
        dao.insertAll(listOf(testSong(1, "Song")))
        val now = System.currentTimeMillis()
        dao.incrementPlayCount(1L, now)
        dao.incrementPlayCount(1L, now)

        val song = dao.getSongById(1L).first()!!
        assertEquals(2, song.playCount)
    }

    // ── setRating ─────────────────────────────────────────────────────────────

    @Test
    fun setRating_updatesCorrectly() = runTest {
        dao.insertAll(listOf(testSong(1, "Song")))
        dao.setRating(1L, 5, System.currentTimeMillis())

        val song = dao.getSongById(1L).first()!!
        assertEquals(5, song.rating)
    }

    // ── getRating ─────────────────────────────────────────────────────────────

    @Test
    fun getRating_returnsCurrentRating() = runTest {
        dao.insertAll(listOf(testSong(1, "Song")))
        dao.setRating(1L, 3, System.currentTimeMillis())

        assertEquals(3, dao.getRating(1L).first())
    }

    @Test
    fun getRating_returnsNullForUnknownSong() = runTest {
        assertNull(dao.getRating(999L).first())
    }

    // ── applySyncDelta ────────────────────────────────────────────────────────

    @Test
    fun applySyncDelta_overwritesPlayCountAndRating() = runTest {
        dao.insertAll(listOf(testSong(1, "Song")))
        val now = System.currentTimeMillis()
        dao.applySyncDelta(songId = 1L, playCount = 42, rating = 5, playedAt = now, modifiedAt = now)

        val song = dao.getSongById(1L).first()!!
        assertEquals(42, song.playCount)
        assertEquals(5, song.rating)
    }

    // ── getFavoriteSongs ──────────────────────────────────────────────────────

    @Test
    fun getFavoriteSongs_returnsOnlySongsWithRatingAtLeast4() = runTest {
        dao.insertAll(listOf(testSong(1, "A"), testSong(2, "B"), testSong(3, "C")))
        val now = System.currentTimeMillis()
        dao.setRating(1L, 5, now)
        dao.setRating(2L, 4, now)
        dao.setRating(3L, 3, now)

        val favorites = dao.getFavoriteSongs().first()
        assertEquals(2, favorites.size)
        assertTrue(favorites.none { it.id == 3L })
    }

    // ── getMostPlayedSongs ────────────────────────────────────────────────────

    @Test
    fun getMostPlayedSongs_excludesSongsWithZeroPlayCount() = runTest {
        dao.insertAll(listOf(testSong(1, "A"), testSong(2, "B")))
        dao.incrementPlayCount(1L, System.currentTimeMillis())

        val result = dao.getMostPlayedSongs().first()
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun getMostPlayedSongs_orderedByPlayCountDesc() = runTest {
        dao.insertAll(listOf(testSong(1, "A"), testSong(2, "B"), testSong(3, "C")))
        val now = System.currentTimeMillis()
        dao.incrementPlayCount(1L, now)
        dao.incrementPlayCount(3L, now)
        dao.incrementPlayCount(3L, now)

        val result = dao.getMostPlayedSongs().first()
        assertEquals(3L, result[0].id) // play count 2
        assertEquals(1L, result[1].id) // play count 1
    }

    // ── searchSongs ───────────────────────────────────────────────────────────

    @Test
    fun searchSongs_matchesByTitle() = runTest {
        dao.insertAll(listOf(testSong(1, "Bohemian Rhapsody"), testSong(2, "Stairway to Heaven")))

        val result = dao.searchSongs("Bohemian").first()
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun searchSongs_matchesByArtist() = runTest {
        dao.insertAll(
            listOf(
                testSong(1, "Song A", artist = "Queen"),
                testSong(2, "Song B", artist = "Led Zeppelin"),
            )
        )

        val result = dao.searchSongs("Queen").first()
        assertEquals(1, result.size)
    }

    @Test
    fun searchSongs_isCaseInsensitive() = runTest {
        dao.insertAll(listOf(testSong(1, "Bohemian Rhapsody")))

        val result = dao.searchSongs("bohemian").first()
        assertEquals(1, result.size)
    }

    @Test
    fun searchSongs_returnsEmptyForNoMatch() = runTest {
        dao.insertAll(listOf(testSong(1, "Bohemian Rhapsody")))

        assertTrue(dao.searchSongs("xyz123").first().isEmpty())
    }

    // ── searchAlbums ──────────────────────────────────────────────────────────

    @Test
    fun searchAlbums_matchesByAlbumName() = runTest {
        dao.insertAll(
            listOf(
                testSong(1, "S1", albumId = 1, album = "Abbey Road"),
                testSong(2, "S2", albumId = 2, album = "Thriller"),
            )
        )

        val result = dao.searchAlbums("Abbey").first()
        assertEquals(1, result.size)
        assertEquals("Abbey Road", result[0].name)
    }

    // ── searchArtists ─────────────────────────────────────────────────────────

    @Test
    fun searchArtists_matchesByArtistName() = runTest {
        dao.insertAll(
            listOf(
                testSong(1, "S1", artist = "Queen"),
                testSong(2, "S2", artist = "Queen"),
                testSong(3, "S3", artist = "Beatles"),
            )
        )

        val result = dao.searchArtists("Queen").first()
        assertEquals(1, result.size)
        assertEquals("Queen", result[0].name)
        assertEquals(2, result[0].songCount)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun testSong(
        id: Long,
        title: String,
        artist: String = "Artist",
        album: String = "Album",
        albumId: Long = 1,
        trackNumber: Int = 1,
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = 200_000,
        trackNumber = trackNumber,
        year = 2024,
        dateAdded = 1000L,
        contentUri = "content://media/external/audio/media/$id",
        albumArtUri = "content://media/external/audio/albumart/$albumId",
    )
}
