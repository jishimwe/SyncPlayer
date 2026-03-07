package com.jpishimwe.syncplayer.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jpishimwe.syncplayer.model.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {

    private lateinit var database: SyncPlayerDatabase
    private lateinit var dao: PlaylistDao
    private lateinit var songDao: SongDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SyncPlayerDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.playlistDao()
        songDao = database.songDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ── create / read ─────────────────────────────────────────────────────────

    @Test
    fun insertPlaylist_andGetAllPlaylists() = runTest {
        dao.insertPlaylist(PlaylistEntity(name = "My Playlist", createdAt = 1000L))

        val playlists = dao.getAllPlaylists().first()
        assertEquals(1, playlists.size)
        assertEquals("My Playlist", playlists[0].name)
    }

    @Test
    fun getAllPlaylists_sortedByNameAsc() = runTest {
        dao.insertPlaylist(PlaylistEntity(name = "Workout", createdAt = 1000L))
        dao.insertPlaylist(PlaylistEntity(name = "Chill", createdAt = 1000L))
        dao.insertPlaylist(PlaylistEntity(name = "Party", createdAt = 1000L))

        val playlists = dao.getAllPlaylists().first()
        assertEquals(listOf("Chill", "Party", "Workout"), playlists.map { it.name })
    }

    @Test
    fun getAllPlaylists_excludesSoftDeleted() = runTest {
        val id = dao.insertPlaylist(PlaylistEntity(name = "Gone", createdAt = 1000L))
        dao.insertPlaylist(PlaylistEntity(name = "Active", createdAt = 1000L))
        dao.softDeletePlaylist(id, deletedAt = System.currentTimeMillis())

        val playlists = dao.getAllPlaylists().first()
        assertEquals(1, playlists.size)
        assertEquals("Active", playlists[0].name)
    }

    @Test
    fun getPlaylistById_returnsCorrectPlaylist() = runTest {
        val id = dao.insertPlaylist(PlaylistEntity(name = "Favorites", createdAt = 1000L))

        val playlist = dao.getPlaylistById(id).first()
        assertNotNull(playlist)
        assertEquals("Favorites", playlist!!.name)
    }

    @Test
    fun getPlaylistById_returnsNullForMissingId() = runTest {
        assertNull(dao.getPlaylistById(999L).first())
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    fun updatePlaylist_changesName() = runTest {
        val id = dao.insertPlaylist(PlaylistEntity(name = "Old Name", createdAt = 1000L))
        val existing = dao.getPlaylistById(id).first()!!
        dao.updatePlaylist(existing.copy(name = "New Name"))

        val updated = dao.getPlaylistById(id).first()
        assertEquals("New Name", updated?.name)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    fun deletePlaylist_hardDeleteRemovesRow() = runTest {
        val id = dao.insertPlaylist(PlaylistEntity(name = "Temp", createdAt = 1000L))
        dao.deletePlaylist(id)

        assertNull(dao.getPlaylistById(id).first())
    }

    // ── softDelete ────────────────────────────────────────────────────────────

    @Test
    fun softDeletePlaylist_setsDeletedAt() = runTest {
        val id = dao.insertPlaylist(PlaylistEntity(name = "Soft", createdAt = 1000L))
        val ts = 999_999L
        dao.softDeletePlaylist(id, deletedAt = ts)

        val entity = dao.getPlaylistById(id).first()
        assertEquals(ts, entity?.deletedAt)
        assertEquals(ts, entity?.lastModified)
    }

    // ── getAllPlaylistsList ────────────────────────────────────────────────────

    @Test
    fun getAllPlaylistsList_returnsAllRowsIncludingDeleted() = runTest {
        val id = dao.insertPlaylist(PlaylistEntity(name = "Active", createdAt = 1000L))
        dao.insertPlaylist(PlaylistEntity(name = "Deleted", createdAt = 1000L))
        dao.softDeletePlaylist(id, deletedAt = System.currentTimeMillis())

        val all = dao.getAllPlaylistsList()
        assertEquals(2, all.size)
    }

    // ── song membership ───────────────────────────────────────────────────────

    @Test
    fun addSongToPlaylist_andGetSongsForPlaylist() = runTest {
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Mix", createdAt = 1000L))
        songDao.insertAll(listOf(testSong(1, "Song A"), testSong(2, "Song B")))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 1L, position = 0))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 2L, position = 1))

        val songs = dao.getSongsForPlaylist(playlistId).first()
        assertEquals(2, songs.size)
    }

    @Test
    fun getSongsForPlaylist_sortedByPositionAsc() = runTest {
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Mix", createdAt = 1000L))
        songDao.insertAll(listOf(testSong(1, "First"), testSong(2, "Second")))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 2L, position = 0))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 1L, position = 1))

        val songs = dao.getSongsForPlaylist(playlistId).first()
        assertEquals(2L, songs[0].id)
        assertEquals(1L, songs[1].id)
    }

    @Test
    fun removeSongFromPlaylist_removesOnlyTargetSong() = runTest {
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Mix", createdAt = 1000L))
        songDao.insertAll(listOf(testSong(1, "A"), testSong(2, "B")))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 1L, position = 0))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 2L, position = 1))

        dao.removeSongFromPlaylist(playlistId, songId = 1L)

        val songs = dao.getSongsForPlaylist(playlistId).first()
        assertEquals(1, songs.size)
        assertEquals(2L, songs[0].id)
    }

    @Test
    fun clearPlaylistSongs_removesAllSongsFromPlaylist() = runTest {
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Mix", createdAt = 1000L))
        songDao.insertAll(listOf(testSong(1, "A"), testSong(2, "B")))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 1L, position = 0))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 2L, position = 1))

        dao.clearPlaylistSongs(playlistId)

        assertTrue(dao.getSongsForPlaylist(playlistId).first().isEmpty())
    }

    @Test
    fun replacePlaylistSongs_reordersCorrectly() = runTest {
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Mix", createdAt = 1000L))
        songDao.insertAll(listOf(testSong(1, "A"), testSong(2, "B"), testSong(3, "C")))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 1L, position = 0))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 2L, position = 1))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 3L, position = 2))

        // Reorder: 3, 1, 2
        dao.clearPlaylistSongs(playlistId)
        dao.replacePlaylistSongs(
            listOf(
                PlaylistSongCrossRef(playlistId = playlistId, songId = 3L, position = 0),
                PlaylistSongCrossRef(playlistId = playlistId, songId = 1L, position = 1),
                PlaylistSongCrossRef(playlistId = playlistId, songId = 2L, position = 2),
            )
        )

        val songs = dao.getSongsForPlaylist(playlistId).first()
        assertEquals(listOf(3L, 1L, 2L), songs.map { it.id })
    }

    // ── getSongCountForPlaylist ───────────────────────────────────────────────

    @Test
    fun getSongCountForPlaylist_returnsCorrectCount() = runTest {
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Mix", createdAt = 1000L))
        songDao.insertAll(listOf(testSong(1, "A"), testSong(2, "B"), testSong(3, "C")))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 1L, position = 0))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 2L, position = 1))

        assertEquals(2, dao.getSongCountForPlaylist(playlistId).first())
    }

    // ── getAllPlaylistsWithCount ───────────────────────────────────────────────

    @Test
    fun getAllPlaylistsWithCount_showsCorrectSongCounts() = runTest {
        val id1 = dao.insertPlaylist(PlaylistEntity(name = "Big", createdAt = 1000L))
        val id2 = dao.insertPlaylist(PlaylistEntity(name = "Small", createdAt = 1000L))
        songDao.insertAll(listOf(testSong(1, "A"), testSong(2, "B"), testSong(3, "C")))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = id1, songId = 1L, position = 0))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = id1, songId = 2L, position = 1))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = id1, songId = 3L, position = 2))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = id2, songId = 1L, position = 0))

        val playlists = dao.getAllPlaylistsWithCount().first()
        val big = playlists.find { it.name == "Big" }!!
        val small = playlists.find { it.name == "Small" }!!
        assertEquals(3, big.songCount)
        assertEquals(1, small.songCount)
    }

    @Test
    fun getAllPlaylistsWithCount_excludesSoftDeleted() = runTest {
        val id = dao.insertPlaylist(PlaylistEntity(name = "Gone", createdAt = 1000L))
        dao.insertPlaylist(PlaylistEntity(name = "Active", createdAt = 1000L))
        dao.softDeletePlaylist(id, deletedAt = System.currentTimeMillis())

        val playlists = dao.getAllPlaylistsWithCount().first()
        assertEquals(1, playlists.size)
        assertEquals("Active", playlists[0].name)
    }

    // ── touchPlaylist ─────────────────────────────────────────────────────────

    @Test
    fun touchPlaylist_updatesLastModified() = runTest {
        val id = dao.insertPlaylist(PlaylistEntity(name = "Touch Me", createdAt = 1000L))
        dao.touchPlaylist(id, modifiedAT = 42_000L)

        val entity = dao.getPlaylistById(id).first()
        assertEquals(42_000L, entity?.lastModified)
    }

    // ── getSongsForPlaylistList (suspend) ─────────────────────────────────────

    @Test
    fun getSongsForPlaylistList_returnsOrderedSongs() = runTest {
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Mix", createdAt = 1000L))
        songDao.insertAll(listOf(testSong(1, "A"), testSong(2, "B")))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 2L, position = 0))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 1L, position = 1))

        val songs = dao.getSongsForPlaylistList(playlistId)
        assertEquals(2L, songs[0].id)
        assertEquals(1L, songs[1].id)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun testSong(
        id: Long,
        title: String,
    ) = Song(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        albumId = 1,
        duration = 200_000,
        trackNumber = 1,
        year = 2024,
        dateAdded = 1000L,
        contentUri = "content://media/external/audio/media/$id",
        albumArtUri = "content://media/external/audio/albumart/1",
    )
}
