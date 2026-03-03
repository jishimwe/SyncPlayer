package com.jpishimwe.syncplayer.data.sync

import com.jpishimwe.syncplayer.data.local.ListeningHistoryEntity
import com.jpishimwe.syncplayer.model.Song
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ConflictResolverTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun testSong(
        id: Long = 1L,
        playCount: Int = 0,
        rating: Int = 0,
        lastPlayed: Long = 0L,
        lastModified: Long = 0L,
    ) = Song(
        id = id,
        title = "Test Song",
        artist = "Test Artist",
        album = "Test Album",
        albumId = 0L,
        duration = 200_000L,
        trackNumber = 1,
        year = 2020,
        dateAdded = 0L,
        contentUri = null,
        albumArtUri = null,
        playCount = playCount,
        rating = rating,
        lastPlayed = lastPlayed,
        lastModified = lastModified,
    )

    private fun remote(
        playCount: Int = 0,
        rating: Int = 0,
        lastPlayed: Long = 0L,
        lastModified: Long = 0L,
    ) = FirestoreSongMetadata(
        playCount = playCount,
        rating = rating,
        lastPlayed = lastPlayed,
        lastModified = lastModified,
    )

    private fun history(songId: Long, playedAt: Long) =
        ListeningHistoryEntity(id = 0, songId = songId, playedAt = playedAt)

    private fun remoteEvent(fingerprint: String, playedAt: Long) =
        FirestoreHistoryEvent(fingerprint = fingerprint, playedAt = playedAt)

    // ── resolveSongMetadata ───────────────────────────────────────────────────

    @Nested
    inner class ResolveSongMetadata {

        @Test
        fun `returns null when local is already up to date`() {
            val local = testSong(playCount = 5, rating = 3, lastPlayed = 1000L, lastModified = 500L)
            val remoteData = remote(playCount = 5, rating = 3, lastPlayed = 1000L, lastModified = 500L)
            assertNull(ConflictResolver.resolveSongMetadata(local, remoteData))
        }

        @Test
        fun `remote higher playCount wins — max-wins rule`() {
            val local = testSong(playCount = 3, rating = 2, lastPlayed = 1000L, lastModified = 500L)
            val remoteData = remote(playCount = 7, rating = 2, lastPlayed = 1000L, lastModified = 500L)
            val delta = ConflictResolver.resolveSongMetadata(local, remoteData)
            assertNotNull(delta)
            assertEquals(7, delta!!.playCount)
        }

        @Test
        fun `local higher playCount is already correct — returns null`() {
            val local = testSong(playCount = 10, rating = 2, lastPlayed = 1000L, lastModified = 500L)
            val remoteData = remote(playCount = 5, rating = 2, lastPlayed = 1000L, lastModified = 500L)
            // resolved playCount = 10 = local → all values unchanged → null
            assertNull(ConflictResolver.resolveSongMetadata(local, remoteData))
        }

        @Test
        fun `remote newer lastModified means remote rating wins — last-write-wins`() {
            val local = testSong(playCount = 5, rating = 3, lastPlayed = 1000L, lastModified = 100L)
            val remoteData = remote(playCount = 5, rating = 5, lastPlayed = 1000L, lastModified = 200L)
            val delta = ConflictResolver.resolveSongMetadata(local, remoteData)
            assertNotNull(delta)
            assertEquals(5, delta!!.rating)
        }

        @Test
        fun `local newer lastModified keeps local rating — returns null`() {
            val local = testSong(playCount = 5, rating = 3, lastPlayed = 1000L, lastModified = 300L)
            val remoteData = remote(playCount = 5, rating = 5, lastPlayed = 1000L, lastModified = 200L)
            // local.lastModified(300) >= remote.lastModified(200) → local rating=3 preserved → unchanged
            assertNull(ConflictResolver.resolveSongMetadata(local, remoteData))
        }

        @Test
        fun `remote later lastPlayed wins — max-wins rule`() {
            val local = testSong(playCount = 5, rating = 2, lastPlayed = 1000L, lastModified = 500L)
            val remoteData = remote(playCount = 5, rating = 2, lastPlayed = 5000L, lastModified = 500L)
            val delta = ConflictResolver.resolveSongMetadata(local, remoteData)
            assertNotNull(delta)
            assertEquals(5000L, delta!!.lastPlayed)
        }

        @Test
        fun `local later lastPlayed is already correct — returns null`() {
            val local = testSong(playCount = 5, rating = 2, lastPlayed = 9000L, lastModified = 500L)
            val remoteData = remote(playCount = 5, rating = 2, lastPlayed = 1000L, lastModified = 500L)
            assertNull(ConflictResolver.resolveSongMetadata(local, remoteData))
        }

        @Test
        fun `resolved lastModified is the max of both timestamps`() {
            val local = testSong(playCount = 3, rating = 2, lastPlayed = 1000L, lastModified = 300L)
            val remoteData = remote(playCount = 7, rating = 2, lastPlayed = 1000L, lastModified = 200L)
            val delta = ConflictResolver.resolveSongMetadata(local, remoteData)
            assertNotNull(delta)
            assertEquals(300L, delta!!.lastModified) // max(300, 200) = 300
        }
    }

    // ── remotePlaylistWins ────────────────────────────────────────────────────

    @Nested
    inner class RemotePlaylistWins {

        @Test
        fun `remote newer timestamp — remote wins`() {
            assertTrue(ConflictResolver.remotePlaylistWins(local = 100L, remote = 200L))
        }

        @Test
        fun `local newer timestamp — local wins`() {
            assertFalse(ConflictResolver.remotePlaylistWins(local = 300L, remote = 200L))
        }

        @Test
        fun `equal timestamps — remote wins (trusts server clock)`() {
            assertTrue(ConflictResolver.remotePlaylistWins(local = 200L, remote = 200L))
        }
    }

    // ── mergeHistoryEvent ─────────────────────────────────────────────────────

    @Nested
    inner class MergeHistoryEvent {

        @Test
        fun `empty inputs returns empty list`() {
            val result = ConflictResolver.mergeHistoryEvent(emptyList(), emptyList(), emptyMap())
            assertTrue(result.isEmpty())
        }

        @Test
        fun `local only — returns all local events unchanged`() {
            val local = listOf(history(1L, 1000L), history(2L, 2000L))
            val result = ConflictResolver.mergeHistoryEvent(local, emptyList(), emptyMap())
            assertEquals(2, result.size)
        }

        @Test
        fun `remote events are mapped via fingerprint to local song IDs`() {
            val fingerprintMap = mapOf("fp-abc" to 42L)
            val remote = listOf(remoteEvent("fp-abc", 3000L))
            val result = ConflictResolver.mergeHistoryEvent(emptyList(), remote, fingerprintMap)
            assertEquals(1, result.size)
            assertEquals(42L, result[0].songId)
            assertEquals(3000L, result[0].playedAt)
        }

        @Test
        fun `duplicate (songId, playedAt) pair is deduplicated`() {
            val local = listOf(history(1L, 1000L))
            val fingerprintMap = mapOf("fp-abc" to 1L)
            val remote = listOf(remoteEvent("fp-abc", 1000L)) // same as local entry
            val result = ConflictResolver.mergeHistoryEvent(local, remote, fingerprintMap)
            assertEquals(1, result.size)
        }

        @Test
        fun `unique remote and local events are all included`() {
            val local = listOf(history(1L, 1000L))
            val fingerprintMap = mapOf("fp-abc" to 2L)
            val remote = listOf(remoteEvent("fp-abc", 2000L))
            val result = ConflictResolver.mergeHistoryEvent(local, remote, fingerprintMap)
            assertEquals(2, result.size)
        }

        @Test
        fun `same song at different timestamps are kept as separate events`() {
            val local = listOf(history(1L, 1000L), history(1L, 2000L))
            val fingerprintMap = mapOf("fp-abc" to 1L)
            val remote = listOf(remoteEvent("fp-abc", 3000L))
            val result = ConflictResolver.mergeHistoryEvent(local, remote, fingerprintMap)
            assertEquals(3, result.size)
        }
    }
}
