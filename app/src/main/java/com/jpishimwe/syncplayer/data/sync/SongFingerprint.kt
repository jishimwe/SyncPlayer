package com.jpishimwe.syncplayer.data.sync

import java.security.MessageDigest

/**
 * Generates a stable, cross-device identifier for a song based on normalized metadata.
 *
 * Duration is bucketed to ±2 seconds to absorb minor encoding-length differences
 * between rips of the same track (e.g., 3:22.1 vs 3:22.4).
 *
 * Limitations:
 * - Songs with identical title + artist + album + duration will share a fingerprint.
 * - Songs where metadata differs across devices (different tagging conventions)
 *   produce different fingerprints and will not be matched.
 */
object SongFingerprint {
    fun compute(
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
    ): String {
        val durationBucket = (durationMs / 2000L) * 2L
        val input = "${title.trim().lowercase()}|${artist.trim().lowercase()}|${album.trim().lowercase()}|$durationBucket"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))

        return bytes.take(8).joinToString("") { "%02x".format(it) }
    }
}
