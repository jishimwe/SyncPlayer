package com.jpishimwe.syncplayer.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Test
    fun insertAll_and_getAllSongs() = runTest {
        val songs = listOf(testSong(1, "Beta"), testSong(2, "Alpha"))
        dao.insertAll(songs)

        val result = dao.getAllSongs().first()
        assertEquals(2, result.size)
        assertEquals("Alpha", result[0].title) // sorted by title ASC
        assertEquals("Beta", result[1].title)
    }

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

    @Test
    fun getSongsByAlbum_filtersCorrectly() = runTest {
        dao.insertAll(
            listOf(
                testSong(1, "S1", albumId = 10, trackNumber = 2),
                testSong(2, "S2", albumId = 10, trackNumber = 1),
                testSong(3, "S3", albumId = 20),
            )
        )

        val result = dao.getSongsByAlbum(10).first()
        assertEquals(2, result.size)
        assertEquals(1, result[0].trackNumber) // sorted by trackNumber ASC
    }

    @Test
    fun deleteAll_removesAllSongs() = runTest {
        dao.insertAll(listOf(testSong(1, "Song 1"), testSong(2, "Song 2")))
        dao.deleteAll()

        val result = dao.getAllSongs().first()
        assertTrue(result.isEmpty())
    }

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
