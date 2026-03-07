package com.jpishimwe.syncplayer.data.sync

import android.content.Context
import android.content.SharedPreferences
import com.jpishimwe.syncplayer.MainDispatcherRule
import com.jpishimwe.syncplayer.data.local.ListeningHistoryDao
import com.jpishimwe.syncplayer.data.local.ListeningHistoryEntity
import com.jpishimwe.syncplayer.data.local.PlaylistDao
import com.jpishimwe.syncplayer.data.local.PlaylistEntity
import com.jpishimwe.syncplayer.data.local.PlaylistSongCrossRef
import com.jpishimwe.syncplayer.data.local.SongDao
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Song
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class SyncOrchestratorTest {
    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var syncRepository: FakeSyncRepository
    private lateinit var songDao: FakeSongDao
    private lateinit var playlistDao: FakePlaylistDao
    private lateinit var historyDao: FakeListeningHistoryDao
    private lateinit var orchestrator: SyncOrchestrator

    @BeforeEach
    fun setup() {
        authRepository = FakeAuthRepository()
        syncRepository = FakeSyncRepository()
        songDao = FakeSongDao()
        playlistDao = FakePlaylistDao()
        historyDao = FakeListeningHistoryDao()

        // Mock Context + SharedPreferences so SyncOrchestrator can be instantiated in unit tests
        val prefs = mockk<SharedPreferences>(relaxed = true)
        every { prefs.getLong(any(), any()) } returns 0L
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { prefs.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor

        val context = mockk<Context>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns prefs

        orchestrator = SyncOrchestrator(context, authRepository, syncRepository, songDao, playlistDao, historyDao)
    }

    // ── syncIfSignedIn guard ──────────────────────────────────────────────────

    @Test
    fun `syncIfSignedIn does nothing when signed out`() = runTest {
        authRepository.emitSignedOut()

        orchestrator.syncIfSignedIn()
        advanceUntilIdle()

        assertEquals(0, syncRepository.pushSongCallCount)
        assertEquals(0, syncRepository.pushPlaylistCallCount)
        assertTrue(orchestrator.syncStatus.value is SyncStatus.Idle)
    }

    @Test
    fun `syncIfSignedIn runs sync when signed in`() = runTest {
        authRepository.emitSignedIn()

        orchestrator.syncIfSignedIn()
        advanceUntilIdle()

        assertTrue(orchestrator.syncStatus.value is SyncStatus.Success)
    }

    // ── push ──────────────────────────────────────────────────────────────────

    @Test
    fun `sync pushes modified songs`() = runTest {
        authRepository.emitSignedIn()
        songDao.songs = listOf(testSong(1, lastModified = 1000L)) // > lastSync (0)

        orchestrator.sync()
        advanceUntilIdle()

        assertEquals(1, syncRepository.pushSongCallCount)
    }

    @Test
    fun `sync does not push unmodified songs`() = runTest {
        authRepository.emitSignedIn()
        songDao.songs = listOf(testSong(1, lastModified = 0L)) // = lastSync

        orchestrator.sync()
        advanceUntilIdle()

        assertEquals(0, syncRepository.pushSongCallCount)
    }

    @Test
    fun `sync pushes modified playlists`() = runTest {
        authRepository.emitSignedIn()
        playlistDao.playlists = listOf(
            PlaylistEntity(id = 1, name = "My Mix", createdAt = 1000L, lastModified = 500L)
        )

        orchestrator.sync()
        advanceUntilIdle()

        assertEquals(1, syncRepository.pushPlaylistCallCount)
    }

    @Test
    fun `sync pushes listening history`() = runTest {
        authRepository.emitSignedIn()
        historyDao.history = listOf(ListeningHistoryEntity(songId = 1L, playedAt = 999L))

        orchestrator.sync()
        advanceUntilIdle()

        assertEquals(1, syncRepository.pushHistoryCallCount)
    }

    // ── pull: song conflict resolution ────────────────────────────────────────

    @Test
    fun `sync pulls and applies remote song metadata delta`() = runTest {
        authRepository.emitSignedIn()
        val song = testSong(1, lastModified = 1000L, playCount = 2, rating = 0)
        songDao.songs = listOf(song)
        val fingerprint = SongFingerprint.compute(song.title, song.artist, song.album, song.duration)

        // Remote has higher playCount and is newer
        syncRepository.remoteSongs = mapOf(
            fingerprint to FirestoreSongMetadata(playCount = 10, rating = 5, lastPlayed = 2000L, lastModified = 2000L)
        )

        orchestrator.sync()
        advanceUntilIdle()

        // applySyncDelta should have been called with the winning delta
        assertEquals(1, songDao.applySyncDeltaCallCount)
    }

    @Test
    fun `sync skips remote song not present on device`() = runTest {
        authRepository.emitSignedIn()
        songDao.songs = emptyList() // no songs on device
        syncRepository.remoteSongs = mapOf(
            "unknown-fingerprint" to FirestoreSongMetadata(playCount = 5, rating = 3, lastPlayed = 1000L, lastModified = 1000L)
        )

        orchestrator.sync()
        advanceUntilIdle()

        assertEquals(0, songDao.applySyncDeltaCallCount)
    }

    // ── pull: playlist operations ─────────────────────────────────────────────

    @Test
    fun `sync creates new playlist from remote when not present locally`() = runTest {
        authRepository.emitSignedIn()
        playlistDao.playlists = emptyList()
        syncRepository.remotePlaylists = mapOf(
            "remote-1" to FirestorePlaylist(name = "Remote Playlist", createdAt = 1000L, lastModified = 500L)
        )

        orchestrator.sync()
        advanceUntilIdle()

        assertEquals(1, playlistDao.insertPlaylistCallCount)
    }

    @Test
    fun `sync does not create remotely deleted playlist locally`() = runTest {
        authRepository.emitSignedIn()
        playlistDao.playlists = emptyList()
        syncRepository.remotePlaylists = mapOf(
            "remote-1" to FirestorePlaylist(name = "Gone", createdAt = 1000L, lastModified = 500L, deletedAt = 999L)
        )

        orchestrator.sync()
        advanceUntilIdle()

        assertEquals(0, playlistDao.insertPlaylistCallCount)
    }

    @Test
    fun `sync applies remote playlist update when remote wins`() = runTest {
        authRepository.emitSignedIn()
        playlistDao.playlists = listOf(
            PlaylistEntity(id = 1, name = "Old Name", createdAt = 1000L, remoteId = "remote-1", lastModified = 100L)
        )
        syncRepository.remotePlaylists = mapOf(
            "remote-1" to FirestorePlaylist(name = "New Name", createdAt = 1000L, lastModified = 9000L)
        )

        orchestrator.sync()
        advanceUntilIdle()

        assertEquals(1, playlistDao.updatePlaylistCallCount)
    }

    // ── pull: listening history ───────────────────────────────────────────────

    @Test
    fun `sync merges remote history events`() = runTest {
        authRepository.emitSignedIn()
        val song = testSong(1)
        songDao.songs = listOf(song)
        val fingerprint = SongFingerprint.compute(song.title, song.artist, song.album, song.duration)
        syncRepository.remoteHistory = listOf(FirestoreHistoryEvent(fingerprint = fingerprint, playedAt = 2000L))

        orchestrator.sync()
        advanceUntilIdle()

        assertEquals(1, historyDao.insertCallCount)
    }

    // ── error handling ────────────────────────────────────────────────────────

    @Test
    fun `sync sets Error status on exception`() = runTest {
        authRepository.emitSignedIn()
        // Provide a modified song so the push code path is exercised
        songDao.songs = listOf(testSong(1, lastModified = 1000L))
        syncRepository.throwOnPush = RuntimeException("network error")

        orchestrator.sync()
        advanceUntilIdle()

        assertTrue(orchestrator.syncStatus.value is SyncStatus.Error)
        assertEquals("network error", (orchestrator.syncStatus.value as SyncStatus.Error).message)
    }

    // ── status transitions ────────────────────────────────────────────────────

    @Test
    fun `sync transitions from Idle to Success on clean run`() = runTest {
        authRepository.emitSignedIn()

        assertTrue(orchestrator.syncStatus.value is SyncStatus.Idle)

        orchestrator.sync()
        advanceUntilIdle()

        assertTrue(orchestrator.syncStatus.value is SyncStatus.Success)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun testSong(
        id: Long,
        lastModified: Long = 0L,
        playCount: Int = 0,
        rating: Int = 0,
    ) = Song(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        albumId = 1,
        duration = 200_000,
        trackNumber = 1,
        year = 2024,
        dateAdded = 1000L,
        contentUri = null,
        albumArtUri = null,
        lastModified = lastModified,
        playCount = playCount,
        rating = rating,
    )
}

// ── Fake DAOs ──────────────────────────────────────────────────────────────────

class FakeSongDao : SongDao {
    var songs: List<Song> = emptyList()
    var applySyncDeltaCallCount = 0

    override fun getSongById(id: Long): Flow<Song?> = MutableStateFlow(songs.find { it.id == id })
    override fun getSongsByIds(idList: List<Long>): Flow<List<Song>> = MutableStateFlow(songs.filter { it.id in idList })
    override fun getAllSongs(): Flow<List<Song>> = MutableStateFlow(songs)
    override suspend fun getAllSongsList(): List<Song> = songs
    override fun getAllAlbums(): Flow<List<Album>> = MutableStateFlow(emptyList())
    override fun getAllArtists(): Flow<List<Artist>> = MutableStateFlow(emptyList())
    override fun getSongsByAlbum(albumId: Long): Flow<List<Song>> = MutableStateFlow(emptyList())
    override fun getSongsByArtist(artist: String): Flow<List<Song>> = MutableStateFlow(emptyList())
    override suspend fun insertAll(songs: List<Song>) {}
    override suspend fun insertAllIgnore(songs: List<Song>) {}
    override suspend fun deleteAll() {}
    override suspend fun updateAudioMetadata(id: Long, title: String, artist: String, album: String, albumId: Long, duration: Long, trackNumber: Int, year: Int, dateAdded: Long, contentUri: String?, albumArtUri: String?) {}
    override suspend fun upsertSongs(songs: List<Song>) {}
    override suspend fun incrementPlayCount(songId: Long, playedAt: Long, modifiedAt: Long) {}
    override suspend fun setRating(songId: Long, rating: Int, modifiedAt: Long) {}
    override fun getSongsByMinRating(minRating: Int): Flow<List<Song>> = MutableStateFlow(emptyList())
    override fun getMostPlayedSongs(limit: Int): Flow<List<Song>> = MutableStateFlow(emptyList())
    override fun getRating(songId: Long): Flow<Int?> = MutableStateFlow(null)
    override suspend fun applySyncDelta(songId: Long, playCount: Int, rating: Int, playedAt: Long, modifiedAt: Long) {
        applySyncDeltaCallCount++
    }
    override fun searchSongs(query: String): Flow<List<Song>> = MutableStateFlow(emptyList())
    override fun searchAlbums(query: String): Flow<List<Album>> = MutableStateFlow(emptyList())
    override fun searchArtists(query: String): Flow<List<Artist>> = MutableStateFlow(emptyList())
}

class FakePlaylistDao : PlaylistDao {
    var playlists: List<PlaylistEntity> = emptyList()
    var insertPlaylistCallCount = 0
    var updatePlaylistCallCount = 0
    private var nextId = 100L

    override suspend fun insertPlaylist(playlist: PlaylistEntity): Long {
        insertPlaylistCallCount++
        return nextId++
    }
    override suspend fun updatePlaylist(playlist: PlaylistEntity) { updatePlaylistCallCount++ }
    override suspend fun deletePlaylist(playlistId: Long) {}
    override fun getAllPlaylists(): Flow<List<PlaylistEntity>> = MutableStateFlow(playlists)
    override suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef) {}
    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {}
    override suspend fun clearPlaylistSongs(playlistId: Long) {}
    override suspend fun replacePlaylistSongs(songs: List<PlaylistSongCrossRef>) {}
    override fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> = MutableStateFlow(emptyList())
    override fun getSongCountForPlaylist(playlistId: Long): Flow<Int> = MutableStateFlow(0)
    override fun getPlaylistById(playlistId: Long): Flow<PlaylistEntity?> = MutableStateFlow(playlists.find { it.id == playlistId })
    override suspend fun touchPlaylist(playlistId: Long, modifiedAT: Long) {}
    override suspend fun getAllPlaylistsList(): List<PlaylistEntity> = playlists
    override suspend fun getSongsForPlaylistList(playlistId: Long): List<Song> = emptyList()
    override suspend fun softDeletePlaylist(playlistId: Long, deletedAt: Long) {}
    override fun getAllPlaylistsWithCount(): Flow<List<com.jpishimwe.syncplayer.model.Playlist>> = MutableStateFlow(emptyList())
}

class FakeListeningHistoryDao : ListeningHistoryDao {
    var history: List<ListeningHistoryEntity> = emptyList()
    var insertCallCount = 0

    override suspend fun insertListeningHistory(history: ListeningHistoryEntity) { insertCallCount++ }
    override fun getRecentlyPlayed(limit: Int): Flow<List<Song>> = MutableStateFlow(emptyList())
    override suspend fun clearAll() {}
    override suspend fun getHistorySince(since: Long): List<ListeningHistoryEntity> = history.filter { it.playedAt > since }
}
