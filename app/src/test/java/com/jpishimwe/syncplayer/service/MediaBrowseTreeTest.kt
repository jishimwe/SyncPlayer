package com.jpishimwe.syncplayer.service

import androidx.media3.common.MediaMetadata
import com.jpishimwe.syncplayer.data.PlaylistRepository
import com.jpishimwe.syncplayer.data.SongRepository
import com.jpishimwe.syncplayer.model.Album
import com.jpishimwe.syncplayer.model.Artist
import com.jpishimwe.syncplayer.model.Playlist
import com.jpishimwe.syncplayer.model.Song
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MediaBrowseTreeTest {

    private lateinit var songRepository: SongRepository
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var browseTree: MediaBrowseTree

    private val testSongs = (1..5).map { i ->
        Song(
            id = i.toLong(),
            title = "Song $i",
            artist = "Artist ${i % 2 + 1}",
            albumArtist = "Artist ${i % 2 + 1}",
            album = "Album ${i % 3 + 1}",
            albumId = (i % 3 + 1).toLong(),
            duration = 200_000L,
            trackNumber = i,
            year = 2024,
            dateAdded = i.toLong(),
            contentUri = "content://media/$i",
            albumArtUri = null,
            playCount = 0,
            rating = if (i <= 2) 5 else 0,
        )
    }

    private val testAlbums = listOf(
        Album(id = 1, name = "Album 1", artist = "Artist 1", songCount = 2, albumArtUri = null),
        Album(id = 2, name = "Album 2", artist = "Artist 2", songCount = 2, albumArtUri = null),
        Album(id = 3, name = "Album 3", artist = "Artist 1", songCount = 1, albumArtUri = null),
    )

    private val testArtists = listOf(
        Artist(name = "Artist 1", songCount = 3, albumCount = 2, artUri = null),
        Artist(name = "Artist 2", songCount = 2, albumCount = 1, artUri = null),
    )

    private val testPlaylists = listOf(
        Playlist(id = 1, name = "My Playlist", createdAt = 1000L, songCount = 3),
    )

    @BeforeEach
    fun setup() {
        songRepository = mockk {
            every { getAllSongs() } returns flowOf(testSongs)
            every { getAllAlbums() } returns flowOf(testAlbums)
            every { getAllArtists() } returns flowOf(testArtists)
            every { getFavoriteSongs() } returns flowOf(testSongs.filter { it.rating == 5 })
            every { getSongsByAlbum(1) } returns flowOf(testSongs.filter { it.albumId == 1L })
            every { getSongsByArtist("Artist 1") } returns flowOf(testSongs.filter { it.artist == "Artist 1" })
            every { searchSongs("Song 1") } returns flowOf(testSongs.filter { it.title == "Song 1" })
        }
        playlistRepository = mockk {
            every { getAllPlaylists() } returns flowOf(testPlaylists)
            every { getSongsForPlaylist(1) } returns flowOf(testSongs.take(3))
        }
        browseTree = MediaBrowseTree(songRepository, playlistRepository)
    }

    @Test
    fun `root children returns 6 categories`() = runTest {
        val children = browseTree.getChildren(MediaBrowseTree.ROOT_ID, null)
        assertEquals(6, children.size)

        val ids = children.map { it.mediaId }
        assertTrue(ids.contains(MediaBrowseTree.SONGS_ID))
        assertTrue(ids.contains(MediaBrowseTree.ALBUMS_ID))
        assertTrue(ids.contains(MediaBrowseTree.ARTISTS_ID))
        assertTrue(ids.contains(MediaBrowseTree.PLAYLISTS_ID))
        assertTrue(ids.contains(MediaBrowseTree.FAVORITES_ID))
        assertTrue(ids.contains(MediaBrowseTree.QUEUE_ID))

        children.forEach { item ->
            assertTrue(item.mediaMetadata.isBrowsable == true)
            assertFalse(item.mediaMetadata.isPlayable == true)
        }
    }

    @Test
    fun `songs node returns playable songs sorted by date added descending`() = runTest {
        val children = browseTree.getChildren(MediaBrowseTree.SONGS_ID, null)
        assertEquals(5, children.size)
        assertEquals("5", children.first().mediaId)

        children.forEach { item ->
            assertTrue(item.mediaMetadata.isPlayable == true)
            assertFalse(item.mediaMetadata.isBrowsable == true)
            assertEquals(MediaMetadata.MEDIA_TYPE_MUSIC, item.mediaMetadata.mediaType)
        }
    }

    @Test
    fun `albums node returns browsable albums`() = runTest {
        val children = browseTree.getChildren(MediaBrowseTree.ALBUMS_ID, null)
        assertEquals(3, children.size)

        children.forEach { item ->
            assertTrue(item.mediaMetadata.isBrowsable == true)
            assertFalse(item.mediaMetadata.isPlayable == true)
            assertEquals(MediaMetadata.MEDIA_TYPE_ALBUM, item.mediaMetadata.mediaType)
        }
    }

    @Test
    fun `album drill-down returns playable songs`() = runTest {
        val albumNodeId = MediaBrowseTree.albumNodeId(1)
        val children = browseTree.getChildren(albumNodeId, null)
        assertTrue(children.isNotEmpty())
        children.forEach { item ->
            assertTrue(item.mediaMetadata.isPlayable == true)
        }
    }

    @Test
    fun `artists node returns browsable artists`() = runTest {
        val children = browseTree.getChildren(MediaBrowseTree.ARTISTS_ID, null)
        assertEquals(2, children.size)

        children.forEach { item ->
            assertTrue(item.mediaMetadata.isBrowsable == true)
            assertEquals(MediaMetadata.MEDIA_TYPE_ARTIST, item.mediaMetadata.mediaType)
        }
    }

    @Test
    fun `artist drill-down returns playable songs`() = runTest {
        val artistNodeId = MediaBrowseTree.artistNodeId("Artist 1")
        val children = browseTree.getChildren(artistNodeId, null)
        assertTrue(children.isNotEmpty())
        children.forEach { item ->
            assertTrue(item.mediaMetadata.isPlayable == true)
        }
    }

    @Test
    fun `playlists node returns browsable playlists`() = runTest {
        val children = browseTree.getChildren(MediaBrowseTree.PLAYLISTS_ID, null)
        assertEquals(1, children.size)
        assertTrue(children.first().mediaMetadata.isBrowsable == true)
        assertEquals(MediaMetadata.MEDIA_TYPE_PLAYLIST, children.first().mediaMetadata.mediaType)
    }

    @Test
    fun `playlist drill-down returns playable songs`() = runTest {
        val playlistNodeId = MediaBrowseTree.playlistNodeId(1)
        val children = browseTree.getChildren(playlistNodeId, null)
        assertEquals(3, children.size)
        children.forEach { item ->
            assertTrue(item.mediaMetadata.isPlayable == true)
        }
    }

    @Test
    fun `favorites returns only songs with rating 5`() = runTest {
        val children = browseTree.getChildren(MediaBrowseTree.FAVORITES_ID, null)
        assertEquals(2, children.size)
        children.forEach { item ->
            assertTrue(item.mediaMetadata.isPlayable == true)
        }
    }

    @Test
    fun `queue returns empty when player is null`() = runTest {
        val children = browseTree.getChildren(MediaBrowseTree.QUEUE_ID, null)
        assertTrue(children.isEmpty())
    }

    @Test
    fun `search returns matching songs`() = runTest {
        val results = browseTree.search("Song 1")
        assertEquals(1, results.size)
        assertEquals("Song 1", results.first().mediaMetadata.title.toString())
    }

    @Test
    fun `unknown parent returns empty list`() = runTest {
        val children = browseTree.getChildren("[unknown]", null)
        assertTrue(children.isEmpty())
    }
}
