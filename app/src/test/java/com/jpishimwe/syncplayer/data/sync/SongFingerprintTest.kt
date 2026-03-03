package com.jpishimwe.syncplayer.data.sync

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SongFingerprintTest {

    @Test
    fun `same metadata returns same fingerprint`() {
        val fp1 = SongFingerprint.compute("Song", "Artist", "Album", 200_000L)
        val fp2 = SongFingerprint.compute("Song", "Artist", "Album", 200_000L)
        assertEquals(fp1, fp2)
    }

    @Test
    fun `fingerprint is 16 lowercase hex characters`() {
        val fp = SongFingerprint.compute("Song", "Artist", "Album", 200_000L)
        assertEquals(16, fp.length)
        assertTrue(fp.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `title is normalized to lowercase`() {
        val fp1 = SongFingerprint.compute("Hello World", "Artist", "Album", 200_000L)
        val fp2 = SongFingerprint.compute("HELLO WORLD", "Artist", "Album", 200_000L)
        assertEquals(fp1, fp2)
    }

    @Test
    fun `artist is normalized to lowercase`() {
        val fp1 = SongFingerprint.compute("Song", "The Beatles", "Album", 200_000L)
        val fp2 = SongFingerprint.compute("Song", "THE BEATLES", "Album", 200_000L)
        assertEquals(fp1, fp2)
    }

    @Test
    fun `album is normalized to lowercase`() {
        val fp1 = SongFingerprint.compute("Song", "Artist", "Abbey Road", 200_000L)
        val fp2 = SongFingerprint.compute("Song", "Artist", "ABBEY ROAD", 200_000L)
        assertEquals(fp1, fp2)
    }

    @Test
    fun `leading and trailing whitespace is trimmed`() {
        val fp1 = SongFingerprint.compute("Song", "Artist", "Album", 200_000L)
        val fp2 = SongFingerprint.compute("  Song  ", "  Artist  ", "  Album  ", 200_000L)
        assertEquals(fp1, fp2)
    }

    @Test
    fun `durations within the same 2 second bucket match`() {
        // Bucket = (durationMs / 2000L) * 2L
        // 200_000ms and 201_999ms both yield bucket value 200 (floor(x/2000) = 100, *2 = 200)
        val fp1 = SongFingerprint.compute("Song", "Artist", "Album", 200_000L)
        val fp2 = SongFingerprint.compute("Song", "Artist", "Album", 201_999L)
        assertEquals(fp1, fp2)
    }

    @Test
    fun `durations crossing a bucket boundary produce different fingerprints`() {
        // 200_000ms → bucket 200; 202_000ms → bucket 202
        val fp1 = SongFingerprint.compute("Song", "Artist", "Album", 200_000L)
        val fp2 = SongFingerprint.compute("Song", "Artist", "Album", 202_000L)
        assertNotEquals(fp1, fp2)
    }

    @Test
    fun `different title produces different fingerprint`() {
        val fp1 = SongFingerprint.compute("Song A", "Artist", "Album", 200_000L)
        val fp2 = SongFingerprint.compute("Song B", "Artist", "Album", 200_000L)
        assertNotEquals(fp1, fp2)
    }

    @Test
    fun `different artist produces different fingerprint`() {
        val fp1 = SongFingerprint.compute("Song", "Artist A", "Album", 200_000L)
        val fp2 = SongFingerprint.compute("Song", "Artist B", "Album", 200_000L)
        assertNotEquals(fp1, fp2)
    }

    @Test
    fun `different album produces different fingerprint`() {
        val fp1 = SongFingerprint.compute("Song", "Artist", "Album A", 200_000L)
        val fp2 = SongFingerprint.compute("Song", "Artist", "Album B", 200_000L)
        assertNotEquals(fp1, fp2)
    }
}
